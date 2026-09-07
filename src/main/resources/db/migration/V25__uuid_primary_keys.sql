-- Converts every primary key and foreign key in the schema from bigint to uuid,
-- in place, preserving the rows and the relationships between them.
--
-- Driven off the catalog rather than a hand-written list of tables: the schema
-- was built by ddl-auto for most of its life, so a list here would be a guess
-- about what is actually in the database. Every step discovers its targets from
-- pg_catalog and skips what has already been converted, which also makes the
-- migration safe to re-run after a partial failure.
--
-- Order matters. Every table gets its uuid first, then foreign keys are
-- translated by joining through the old bigint value, then the loose textual
-- references are remapped, and only at the very end are the old columns
-- dropped. Converting one table at a time would strand a foreign key whose
-- parent had not been converted yet, and dropping the old ids any earlier
-- would destroy the mapping the later steps join through.

-- gen_random_uuid() is core from PostgreSQL 13 on; this covers older servers.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Scratch space for what has to survive between the DO blocks below. Dropped at
-- the end; it is a real table rather than a temp one so that it is visible to
-- every statement in the migration.
CREATE TABLE IF NOT EXISTS _uuid_fk_rebuild (
    child_table  text NOT NULL,
    child_column text NOT NULL,
    parent_table text NOT NULL,
    on_delete    "char" NOT NULL
);

-- ---------------------------------------------------------------------------
-- 1. Give every bigint-keyed table a uuid alongside its existing id.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    target record;
BEGIN
    FOR target IN
        SELECT c.relname AS table_name
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        JOIN pg_constraint pk ON pk.conrelid = c.oid AND pk.contype = 'p'
        JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = pk.conkey[1]
        WHERE n.nspname = current_schema()
          AND c.relkind = 'r'
          AND c.relname <> 'flyway_schema_history'
          AND array_length(pk.conkey, 1) = 1
          AND a.attname = 'id'
          AND a.atttypid = 'bigint'::regtype
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ADD COLUMN IF NOT EXISTS id_uuid uuid NOT NULL DEFAULT gen_random_uuid()',
            target.table_name
        );
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 2. Translate every foreign key into a uuid column carrying the same link.
--
--    Populated by joining the old bigint value back to the parent's freshly
--    minted uuid, so a row keeps pointing at exactly the row it pointed at
--    before. Nullability and ON DELETE behaviour are carried across, so an
--    optional relationship stays optional and a cascade stays a cascade.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    fk  record;
    dup record;
BEGIN
    -- DISTINCT ON collapses duplicate constraints: a schema that ddl-auto has
    -- been updating for months accumulates several foreign keys on the same
    -- column under different generated names. Handling the column once is what
    -- matters here; every constraint on it is dropped inside the loop.
    -- A rule other than 'a' (no action) wins the ordering, so a cascade that
    -- one of the duplicates carries is not silently downgraded.
    FOR fk IN
        SELECT DISTINCT ON (child.relname, child_attr.attname)
            child.relname         AS child_table,
            child_attr.attname    AS child_column,
            child_attr.attnotnull AS child_required,
            parent.relname        AS parent_table,
            con.confdeltype       AS on_delete
        FROM pg_constraint con
        JOIN pg_class child  ON child.oid  = con.conrelid
        JOIN pg_class parent ON parent.oid = con.confrelid
        JOIN pg_namespace n  ON n.oid = child.relnamespace
        JOIN pg_attribute child_attr
          ON child_attr.attrelid = child.oid AND child_attr.attnum = con.conkey[1]
        JOIN pg_attribute parent_attr
          ON parent_attr.attrelid = parent.oid AND parent_attr.attnum = con.confkey[1]
        WHERE con.contype = 'f'
          AND n.nspname = current_schema()
          AND array_length(con.conkey, 1) = 1
          AND child_attr.atttypid = 'bigint'::regtype
          AND parent_attr.attname = 'id'
        ORDER BY
            child.relname,
            child_attr.attname,
            (con.confdeltype = 'a'),
            con.conname
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS %I uuid',
                       fk.child_table, fk.child_column || '_uuid');

        EXECUTE format(
            'UPDATE %I child SET %I = parent.id_uuid FROM %I parent WHERE parent.id = child.%I',
            fk.child_table, fk.child_column || '_uuid', fk.parent_table, fk.child_column
        );

        -- Every constraint on this column, not just the one the outer query
        -- happened to pick, so the bigint column can be dropped in step 5.
        -- The single replacement is added in step 6.
        FOR dup IN
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_attribute a
              ON a.attrelid = con.conrelid AND a.attnum = con.conkey[1]
            WHERE con.contype = 'f'
              AND con.conrelid = format('%I', fk.child_table)::regclass
              AND array_length(con.conkey, 1) = 1
              AND a.attname = fk.child_column
        LOOP
            EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', fk.child_table, dup.conname);
        END LOOP;

        IF fk.child_required THEN
            EXECUTE format('ALTER TABLE %I ALTER COLUMN %I SET NOT NULL',
                           fk.child_table, fk.child_column || '_uuid');
        END IF;

        INSERT INTO _uuid_fk_rebuild (child_table, child_column, parent_table, on_delete)
        VALUES (fk.child_table, fk.child_column, fk.parent_table, fk.on_delete);
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 3. Remap the loose textual references.
--
--    notifications and audit_logs point at other rows with an (entity_name,
--    entity_id) pair of plain strings rather than a foreign key, and a
--    notification's action_url embeds the same id in a path. None of that is
--    visible to the catalog, so it is remapped by name here. Anything whose
--    entity_name is not listed keeps its old value: it is display text at that
--    point, and guessing at a table for it would be worse than leaving it.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    mapping record;
BEGIN
    FOR mapping IN
        SELECT * FROM (VALUES
            ('Company',            'companies'),
            ('JobApplication',     'job_applications'),
            ('AiInterviewSession', 'ai_interview_sessions'),
            ('Invoice',            'invoices'),
            ('JobPost',            'job_posts')
        ) AS t(entity_name, table_name)
    LOOP
        -- Skipped once the parent has been converted: the join below reads the
        -- old bigint id, so on a re-run there is nothing left to remap and the
        -- comparison would be uuid = bigint.
        IF to_regclass(mapping.table_name) IS NULL
           OR NOT EXISTS (
               SELECT 1
               FROM pg_attribute
               WHERE attrelid = to_regclass(mapping.table_name)
                 AND attname = 'id'
                 AND atttypid = 'bigint'::regtype
           )
        THEN
            CONTINUE;
        END IF;

        -- action_url first: it is rewritten by substituting the old id inside
        -- the path, so it still needs entity_id to hold the old bigint.
        EXECUTE format($fmt$
            UPDATE notifications n
            SET action_url = replace(n.action_url, n.entity_id, parent.id_uuid::text)
            FROM %I parent
            WHERE n.entity_name = %L
              AND n.entity_id ~ '^[0-9]+$'
              AND parent.id = n.entity_id::bigint
              AND n.action_url IS NOT NULL
              AND position(n.entity_id in n.action_url) > 0
        $fmt$, mapping.table_name, mapping.entity_name);

        EXECUTE format($fmt$
            UPDATE notifications n
            SET entity_id = parent.id_uuid::text
            FROM %I parent
            WHERE n.entity_name = %L
              AND n.entity_id ~ '^[0-9]+$'
              AND parent.id = n.entity_id::bigint
        $fmt$, mapping.table_name, mapping.entity_name);

        EXECUTE format($fmt$
            UPDATE audit_logs a
            SET entity_id = parent.id_uuid::text
            FROM %I parent
            WHERE a.entity_name = %L
              AND a.entity_id ~ '^[0-9]+$'
              AND parent.id = a.entity_id::bigint
        $fmt$, mapping.table_name, mapping.entity_name);
    END LOOP;
END $$;

-- A uuid is 36 characters; these columns were sized for bigints.
ALTER TABLE notifications ALTER COLUMN entity_id TYPE varchar(100);
ALTER TABLE audit_logs    ALTER COLUMN entity_id TYPE varchar(100);

-- ---------------------------------------------------------------------------
-- 4. Swap the primary keys over.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    target record;
BEGIN
    FOR target IN
        SELECT c.relname AS table_name, pk.conname AS pk_name
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        JOIN pg_constraint pk ON pk.conrelid = c.oid AND pk.contype = 'p'
        JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = pk.conkey[1]
        JOIN pg_attribute u ON u.attrelid = c.oid AND u.attname = 'id_uuid' AND u.attnum > 0
        WHERE n.nspname = current_schema()
          AND c.relkind = 'r'
          AND array_length(pk.conkey, 1) = 1
          AND a.attname = 'id'
          AND a.atttypid = 'bigint'::regtype
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', target.table_name, target.pk_name);
        EXECUTE format('ALTER TABLE %I DROP COLUMN id', target.table_name);
        EXECUTE format('ALTER TABLE %I RENAME COLUMN id_uuid TO id', target.table_name);
        -- The default stays on the column: Hibernate assigns the uuid itself,
        -- but a row inserted by a migration or by hand still gets a valid key.
        EXECUTE format('ALTER TABLE %I ADD PRIMARY KEY (id)', target.table_name);
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 5/6. Drop the old bigint foreign keys, rename the uuid ones into their place,
--      and rebuild each constraint with the ON DELETE behaviour it had.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    fk record;
    action text;
BEGIN
    FOR fk IN SELECT DISTINCT * FROM _uuid_fk_rebuild LOOP
        -- Skip anything already carried over, so a re-run after a partial
        -- failure does not try to rename a column that is no longer there.
        IF NOT EXISTS (
            SELECT 1 FROM pg_attribute
            WHERE attrelid = format('%I', fk.child_table)::regclass
              AND attname = fk.child_column || '_uuid'
              AND NOT attisdropped
        ) THEN
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I DROP COLUMN IF EXISTS %I', fk.child_table, fk.child_column);
        EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I',
                       fk.child_table, fk.child_column || '_uuid', fk.child_column);

        action := CASE fk.on_delete
                      WHEN 'c' THEN ' ON DELETE CASCADE'
                      WHEN 'n' THEN ' ON DELETE SET NULL'
                      WHEN 'd' THEN ' ON DELETE SET DEFAULT'
                      WHEN 'r' THEN ' ON DELETE RESTRICT'
                      ELSE ''
                  END;

        -- Named deterministically, so a re-run recognises its own work rather
        -- than adding a second constraint beside the first.
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = left('fk_' || fk.child_table || '_' || fk.child_column, 63)
              AND conrelid = format('%I', fk.child_table)::regclass
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I (id)%s',
                fk.child_table,
                left('fk_' || fk.child_table || '_' || fk.child_column, 63),
                fk.child_column,
                fk.parent_table,
                action
            );
        END IF;
    END LOOP;
END $$;

DROP TABLE _uuid_fk_rebuild;

-- ---------------------------------------------------------------------------
-- 7. The invoice number used to be derived from the invoice's own sequential
--    id. A uuid carries no counter, so the number gets a sequence of its own,
--    started past the highest number already issued so no invoice is reused.
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS invoice_no_seq AS bigint START WITH 1;

SELECT setval(
    'invoice_no_seq',
    GREATEST(
        (SELECT COALESCE(MAX(NULLIF(regexp_replace(invoice_no, '^INV-\d+-', ''), '')::bigint), 0)
         FROM invoices
         WHERE invoice_no ~ '^INV-\d+-\d+$'),
        1
    )
);
