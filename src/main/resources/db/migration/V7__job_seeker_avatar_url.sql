-- Profile photos previously had nowhere to live: the frontend wrote them to
-- Better Auth's in-memory adapter, so the reference was lost on restart while
-- the file itself sat in MinIO. Store the app-relative file URL alongside the
-- rest of the profile instead.
ALTER TABLE job_seeker_profiles
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
