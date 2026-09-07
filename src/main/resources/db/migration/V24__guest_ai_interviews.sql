-- Let someone try an AI interview without an account.
--
-- A guest has no user account, so job_seeker_id has to be allowed to be empty
-- and a token their browser holds stands in for the owner. That token is the
-- only thing authorising reads and writes on the interview, so every guest
-- lookup matches on it as well as the id.
--
-- The IP is stored only as a salted hash, and only to count how many interviews
-- start from one network in a day. It is a counting key, never an address to
-- read back.

ALTER TABLE ai_interview_sessions
    ALTER COLUMN job_seeker_id DROP NOT NULL;

ALTER TABLE ai_interview_sessions
    ADD COLUMN IF NOT EXISTS guest_token   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS guest_ip_hash VARCHAR(64);

-- Every guest read is "this session, for this token".
CREATE INDEX IF NOT EXISTS idx_ai_interview_sessions_guest_token
    ON ai_interview_sessions (guest_token);

-- And the network cap counts by hash within a time window.
CREATE INDEX IF NOT EXISTS idx_ai_interview_sessions_guest_ip
    ON ai_interview_sessions (guest_ip_hash, created_at);
