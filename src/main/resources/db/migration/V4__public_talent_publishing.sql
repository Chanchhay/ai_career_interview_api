DO $$
BEGIN
    IF to_regclass('job_seeker_profiles') IS NOT NULL THEN
        ALTER TABLE job_seeker_profiles
            ADD COLUMN IF NOT EXISTS public_profile_slug varchar(120),
            ADD COLUMN IF NOT EXISTS profile_visibility varchar(50),
            ADD COLUMN IF NOT EXISTS published_at timestamp with time zone;

        UPDATE job_seeker_profiles
        SET profile_visibility = 'PRIVATE'
        WHERE profile_visibility IS NULL;

        ALTER TABLE job_seeker_profiles
            ALTER COLUMN profile_visibility SET DEFAULT 'PRIVATE',
            ALTER COLUMN profile_visibility SET NOT NULL;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conrelid = 'job_seeker_profiles'::regclass
              AND conname = 'uk_job_seeker_profiles_public_profile_slug'
        ) THEN
            ALTER TABLE job_seeker_profiles
                ADD CONSTRAINT uk_job_seeker_profiles_public_profile_slug UNIQUE (public_profile_slug);
        END IF;
    END IF;

    IF to_regclass('portfolios') IS NOT NULL THEN
        ALTER TABLE portfolios
            ADD COLUMN IF NOT EXISTS published_at timestamp with time zone;

        UPDATE portfolios
        SET visibility = 'PRIVATE'
        WHERE visibility IS NULL;

        ALTER TABLE portfolios
            ALTER COLUMN visibility SET DEFAULT 'PRIVATE',
            ALTER COLUMN visibility SET NOT NULL;
    END IF;

    IF to_regclass('resumes') IS NOT NULL THEN
        ALTER TABLE resumes
            ADD COLUMN IF NOT EXISTS visibility varchar(50),
            ADD COLUMN IF NOT EXISTS published_at timestamp with time zone;

        UPDATE resumes
        SET visibility = 'PRIVATE'
        WHERE visibility IS NULL;

        ALTER TABLE resumes
            ALTER COLUMN visibility SET DEFAULT 'PRIVATE',
            ALTER COLUMN visibility SET NOT NULL;
    END IF;
END $$;
