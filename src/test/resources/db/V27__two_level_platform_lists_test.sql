-- Run with psql -v ON_ERROR_STOP=1 -f <this file> against a disposable PostgreSQL database.
-- Everything is rolled back. Flyway is disabled in the H2 application tests,
-- so this separately exercises the actual PostgreSQL migration and backfill.
BEGIN;
CREATE SCHEMA v27_hierarchy_test;
SET LOCAL search_path TO v27_hierarchy_test;
CREATE TABLE industries (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(150) NOT NULL UNIQUE,
    description text, status varchar(50) NOT NULL DEFAULT 'ACTIVE', created_at timestamptz NOT NULL, updated_at timestamptz);
CREATE TABLE job_categories (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(150) NOT NULL UNIQUE,
    description text, created_at timestamptz NOT NULL, updated_at timestamptz);
CREATE TABLE skills (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(100) NOT NULL UNIQUE,
    skill_type varchar(255), created_at timestamptz NOT NULL, updated_at timestamptz);
INSERT INTO industries (name,description,status,created_at) VALUES ('General','Original industry','INACTIVE','2025-01-01');
INSERT INTO job_categories (name,description,created_at) VALUES ('General','Original job category','2025-01-01');
INSERT INTO skills (name,skill_type,created_at) VALUES ('General','TECHNICAL','2025-01-01');
CREATE TABLE original_rows (list_name text, id uuid, row_data jsonb);
INSERT INTO original_rows SELECT 'industries',id,to_jsonb(i) FROM industries i;
INSERT INTO original_rows SELECT 'job_categories',id,to_jsonb(c) FROM job_categories c;
INSERT INTO original_rows SELECT 'skills',id,to_jsonb(s) FROM skills s;
CREATE TABLE preserved_links (industry_id uuid REFERENCES industries(id), category_id uuid REFERENCES job_categories(id), skill_id uuid REFERENCES skills(id));
INSERT INTO preserved_links SELECT i.id,c.id,s.id FROM industries i CROSS JOIN job_categories c CROSS JOIN skills s;

\ir ../../../main/resources/db/migration/V27__two_level_platform_lists.sql

DO $$
DECLARE original record; actual jsonb; parent uuid; parent_name text;
BEGIN
    FOR original IN SELECT * FROM original_rows LOOP
        EXECUTE format('SELECT to_jsonb(item) - ''parent_id'', parent_id FROM %I item WHERE id=$1', original.list_name)
            INTO actual, parent USING original.id;
        IF actual IS DISTINCT FROM original.row_data OR parent IS NULL THEN
            RAISE EXCEPTION 'Backfill changed or lost original data for %', original.list_name;
        END IF;
        EXECUTE format('SELECT name FROM %I WHERE id=$1 AND parent_id IS NULL', original.list_name)
            INTO parent_name USING parent;
        IF parent_name <> 'General (group 1)' THEN RAISE EXCEPTION 'Existing General name was not preserved'; END IF;
        BEGIN
            EXECUTE format('DELETE FROM %I WHERE id=$1', original.list_name) USING parent;
            RAISE EXCEPTION 'Parent with a child was deleted';
        EXCEPTION WHEN foreign_key_violation OR restrict_violation THEN NULL;
        END;
        BEGIN
            EXECUTE format('DELETE FROM %I WHERE id=$1', original.list_name) USING original.id;
            RAISE EXCEPTION 'Original linked subcategory was deleted';
        EXCEPTION WHEN foreign_key_violation OR restrict_violation THEN NULL;
        END;
        BEGIN
            EXECUTE format('UPDATE %I SET parent_id=id WHERE id=$1', original.list_name) USING original.id;
            RAISE EXCEPTION 'Self parenting was allowed';
        EXCEPTION WHEN check_violation THEN NULL;
        END;
        EXECUTE format('INSERT INTO %I (id,name,created_at) VALUES (''bbbbbbbb-0000-4000-8000-000000000001'', ''New parent'', now())', original.list_name);
    END LOOP;
    IF (SELECT count(*) FROM preserved_links l JOIN industries i ON i.id=l.industry_id
        JOIN job_categories c ON c.id=l.category_id JOIN skills s ON s.id=l.skill_id) <> 1 THEN
        RAISE EXCEPTION 'An existing association was lost';
    END IF;
END $$;

-- Idempotency includes preserving parent groups created after the first run.
\ir ../../../main/resources/db/migration/V27__two_level_platform_lists.sql
DO $$
DECLARE t text; n integer;
BEGIN
    FOREACH t IN ARRAY ARRAY['industries','job_categories','skills'] LOOP
        EXECUTE format('SELECT count(*) FROM %I WHERE parent_id IS NULL', t) INTO n;
        IF n <> 2 THEN RAISE EXCEPTION 'Rerun moved or duplicated a parent in %', t; END IF;
    END LOOP;
END $$;

CREATE SCHEMA v27_empty_lists_test;
SET LOCAL search_path TO v27_empty_lists_test;
CREATE TABLE industries (LIKE v27_hierarchy_test.industries INCLUDING ALL);
CREATE TABLE job_categories (LIKE v27_hierarchy_test.job_categories INCLUDING ALL);
CREATE TABLE skills (LIKE v27_hierarchy_test.skills INCLUDING ALL);
\ir ../../../main/resources/db/migration/V27__two_level_platform_lists.sql
DO $$
DECLARE t text; n integer;
BEGIN
    FOREACH t IN ARRAY ARRAY['industries','job_categories','skills'] LOOP
        EXECUTE format('SELECT count(*) FROM %I WHERE name=''General'' AND parent_id IS NULL', t) INTO n;
        IF n <> 1 THEN RAISE EXCEPTION 'Empty list % did not receive a General parent', t; END IF;
    END LOOP;
END $$;
ROLLBACK;
