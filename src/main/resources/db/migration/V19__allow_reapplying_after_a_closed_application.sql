-- Let a candidate apply again once an earlier attempt is closed.
--
-- The old rule was "one application per candidate per job, ever", enforced by a
-- plain unique constraint on (job_post_id, job_seeker_profile_id). A rejected
-- candidate could therefore never re-apply, even months later or after the
-- recruiter reposted the role.
--
-- The rule is now "one *live* application per candidate per job". A partial
-- unique index says exactly that: it constrains only rows that are still in
-- play, and lets any number of REJECTED or WITHDRAWN attempts coexist beside
-- them. History is kept rather than overwritten, so who was rejected and why
-- survives a second attempt.
--
-- This must stay in step with ApplicationStatus.isClosed(), which is where the
-- API makes the same decision.
DO $$
BEGIN
    IF to_regclass('job_applications') IS NULL THEN
        RETURN;
    END IF;

    -- Drop the old blanket constraint. Both spellings are checked: Flyway V5
    -- created it by name, while ddl-auto may have generated its own.
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'job_applications'::regclass
          AND conname = 'uk_job_applications_job_profile'
    ) THEN
        ALTER TABLE job_applications DROP CONSTRAINT uk_job_applications_job_profile;
    END IF;

    -- Any other unique constraint over exactly these two columns, whatever
    -- Hibernate happened to name it.
    DECLARE
        generated_name text;
    BEGIN
        FOR generated_name IN
            SELECT constraint_name.conname
            FROM pg_constraint constraint_name
            WHERE constraint_name.conrelid = 'job_applications'::regclass
              AND constraint_name.contype = 'u'
              AND (
                  -- ::text because attname is `name`, which has no array
                  -- equality operator against a text[] literal.
                  SELECT array_agg(attribute.attname::text ORDER BY attribute.attname::text)
                  FROM unnest(constraint_name.conkey) AS column_id
                  JOIN pg_attribute attribute
                    ON attribute.attrelid = constraint_name.conrelid
                   AND attribute.attnum = column_id
              ) = ARRAY['job_post_id', 'job_seeker_profile_id']
        LOOP
            EXECUTE format('ALTER TABLE job_applications DROP CONSTRAINT %I', generated_name);
            RAISE NOTICE 'Dropped blanket unique constraint %', generated_name;
        END LOOP;
    END;

    -- One live application per candidate per job.
    IF NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'uk_job_applications_live'
    ) THEN
        CREATE UNIQUE INDEX uk_job_applications_live
            ON job_applications (job_post_id, job_seeker_profile_id)
            WHERE status NOT IN ('REJECTED', 'WITHDRAWN');
    END IF;
END $$;
