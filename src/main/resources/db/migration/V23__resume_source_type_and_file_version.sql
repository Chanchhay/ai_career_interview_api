-- Columns the Resume entity has carried for a while with no migration behind
-- them. On a developer's empty database ddl-auto added them silently; on a
-- server with real resumes the same DDL fails -- "column contains null values"
-- -- because a NOT NULL column cannot be bolted onto existing rows without a
-- value for them. The columns were therefore simply missing in production.
--
-- Each step is written to be a no-op where ddl-auto already won, so the same
-- script is correct on a fresh database, a developer's database and a server.

-- Whether a resume is rendered from resume_data or is a file the seeker
-- uploaded. Every row that predates the column came from the builder.
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS source_type varchar(50);
UPDATE resumes SET source_type = 'PLATFORM_TEMPLATE' WHERE source_type IS NULL;
ALTER TABLE resumes ALTER COLUMN source_type SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'resumes'::regclass
          AND conname = 'resumes_source_type_check'
    ) THEN
        ALTER TABLE resumes
            ADD CONSTRAINT resumes_source_type_check
                CHECK (source_type IN ('PLATFORM_TEMPLATE', 'USER_UPLOAD'));
    END IF;
END $$;

-- Bumped on every regeneration, so an application can tell whether the resume
-- it sent has been edited since. Existing files count as version 0.
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS file_version integer;
UPDATE resumes SET file_version = 0 WHERE file_version IS NULL;
ALTER TABLE resumes ALTER COLUMN file_version SET NOT NULL;

-- When the current resume_file_url was rendered. Nullable by design: uploads
-- were never rendered, and neither were the rows that predate the column.
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS generated_at timestamp with time zone;
