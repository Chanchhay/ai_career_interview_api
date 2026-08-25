-- The four layouts the builder already offers.
--
-- The frontend has shipped these template ids since before there was a catalog,
-- and the PDF renderer switches on the same strings. Seeding them here gives
-- administrators a row to retire a layout with, and gives the public catalog
-- something to serve, without changing what the editor already writes into
-- resume_data.template_id.
--
-- Matched on the templateKey inside template_schema rather than on name, so
-- renaming a template in the UI never causes a duplicate insert here.
DO $$
BEGIN
    IF to_regclass('resume_templates') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO resume_templates (name, preview_image_url, template_schema, status, created_at, updated_at)
    SELECT seed.name, NULL, seed.schema::jsonb, 'ACTIVE', now(), now()
    FROM (
        VALUES
            ('Classic',
             '{"templateKey":"classic","layout":"sidebar","description":"Photo header with a contact and skills sidebar."}'),
            ('Minimal',
             '{"templateKey":"minimal","layout":"single","description":"One column, no photo — the safest for resume scanners."}'),
            ('Modern',
             '{"templateKey":"modern","layout":"header","description":"Colored header band with a timeline of your experience."}'),
            ('Elegant',
             '{"templateKey":"elegant","layout":"centered","description":"Centered serif header for a more formal document."}')
    ) AS seed(name, schema)
    WHERE NOT EXISTS (
        SELECT 1
        FROM resume_templates existing
        WHERE existing.template_schema ->> 'templateKey' = seed.schema::jsonb ->> 'templateKey'
    );

    -- Resumes created before source_type existed all came from the builder.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'resumes' AND column_name = 'source_type'
    ) THEN
        UPDATE resumes SET source_type = 'PLATFORM_TEMPLATE' WHERE source_type IS NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'resumes' AND column_name = 'file_version'
    ) THEN
        UPDATE resumes SET file_version = 0 WHERE file_version IS NULL;
    END IF;
END $$;
