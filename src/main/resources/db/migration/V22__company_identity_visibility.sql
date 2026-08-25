-- Let an administrator hide which company is behind a job.
--
-- Candidate-facing responses then show a placeholder instead of the name, and
-- withhold the company id and logo. Nothing about the recruiter's own view,
-- moderation, or finance changes: they are working on the company's behalf and
-- need to know who it is.
--
-- Existing companies keep being named. Masking is a deliberate act, never
-- something a release does to somebody's listings by default.

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS identity_visibility VARCHAR(50);

UPDATE companies
SET identity_visibility = 'VISIBLE'
WHERE identity_visibility IS NULL;

ALTER TABLE companies
    ALTER COLUMN identity_visibility SET DEFAULT 'VISIBLE',
    ALTER COLUMN identity_visibility SET NOT NULL;
