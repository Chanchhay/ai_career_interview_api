-- Mirrors V7 for recruiters so both roles carry a profile photo, and /me can
-- resolve one avatar regardless of which profile the account owns.
ALTER TABLE recruiter_profiles
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
