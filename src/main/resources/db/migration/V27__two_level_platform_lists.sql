-- Introduce parent categories for industries, job categories and skills.
-- Existing rows become subcategories IN PLACE: their UUIDs and every company,
-- job and resume reference stay unchanged. V25 has already converted the IDs.
-- General is a new row, even if an existing leaf is already named General.
-- Like the earlier migrations, tolerate columns created by ddl-auto first.
DO $$
DECLARE
    list_table text;
    root_id uuid;
    root_name text;
    suffix integer;
    already_seeded boolean;
    name_exists boolean;
BEGIN
    FOREACH list_table IN ARRAY ARRAY['industries', 'job_categories', 'skills'] LOOP
        root_id := CASE list_table
            WHEN 'industries' THEN 'a7210000-0000-4000-8000-000000000001'::uuid
            WHEN 'job_categories' THEN 'a7210000-0000-4000-8000-000000000002'::uuid
            ELSE 'a7210000-0000-4000-8000-000000000003'::uuid
        END;
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS parent_id uuid', list_table);
        -- A second run must not move parent categories created after migration.
        EXECUTE format('SELECT EXISTS (SELECT 1 FROM %I WHERE id = $1)', list_table)
            INTO STRICT already_seeded USING root_id;
        IF NOT already_seeded THEN
            root_name := 'General';
            suffix := 0;
            LOOP
                EXECUTE format('SELECT EXISTS (SELECT 1 FROM %I WHERE lower(name) = lower($1))', list_table)
                    INTO name_exists USING root_name;
                EXIT WHEN NOT name_exists;
                suffix := suffix + 1;
                root_name := 'General (group ' || suffix || ')';
            END LOOP;
            IF list_table = 'industries' THEN
                EXECUTE format('INSERT INTO %I (id, name, status, created_at, updated_at) VALUES ($1, $2, ''ACTIVE'', now(), now())', list_table)
                    USING root_id, root_name;
            ELSE
                EXECUTE format('INSERT INTO %I (id, name, created_at, updated_at) VALUES ($1, $2, now(), now())', list_table)
                    USING root_id, root_name;
            END IF;
            EXECUTE format('UPDATE %I SET parent_id = $1 WHERE id <> $1 AND parent_id IS NULL', list_table)
                USING root_id;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = list_table::regclass AND conname = 'fk_' || list_table || '_parent') THEN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (parent_id) REFERENCES %I(id) ON DELETE RESTRICT',
                list_table, 'fk_' || list_table || '_parent', list_table);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = list_table::regclass AND conname = 'ck_' || list_table || '_not_own_parent') THEN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I CHECK (parent_id IS NULL OR parent_id <> id)',
                list_table, 'ck_' || list_table || '_not_own_parent');
        END IF;
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I(parent_id)', 'idx_' || list_table || '_parent', list_table);
    END LOOP;
END $$;
