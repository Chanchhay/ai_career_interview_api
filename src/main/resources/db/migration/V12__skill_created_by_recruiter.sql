-- Recruiters can now name skills the admins have not entered yet, so a job post
-- can carry the technologies it actually asks for. Those rows land in the same
-- curated list job seekers filter by, and until now nothing said where one came
-- from: a recruiter's typo looked exactly like a considered admin entry.
--
-- Null means the skill was entered by an admin, which is every row that existed
-- before this column.
ALTER TABLE skills
    ADD COLUMN IF NOT EXISTS created_by_recruiter_profile_id BIGINT;

ALTER TABLE skills
    DROP CONSTRAINT IF EXISTS fk_skills_created_by_recruiter_profile;

ALTER TABLE skills
    ADD CONSTRAINT fk_skills_created_by_recruiter_profile
        FOREIGN KEY (created_by_recruiter_profile_id)
        REFERENCES recruiter_profiles (id)
        ON DELETE SET NULL;

-- Admins review what came in this way; the filter is on the column being set.
CREATE INDEX IF NOT EXISTS idx_skills_created_by_recruiter_profile
    ON skills (created_by_recruiter_profile_id)
    WHERE created_by_recruiter_profile_id IS NOT NULL;
