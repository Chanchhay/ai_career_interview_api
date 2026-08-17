-- Voice interviews are answered through Vapi, whose webhook identifies a call
-- by id and carries nothing of ours. Resolving a session from that call id is
-- the hot path of every end-of-call report, and the id must never point at two
-- interviews, so the lookup is a unique index rather than a plain one.
--
-- The column itself already existed on ai_interview_sessions; only the index is
-- new. Partial, because sessions answered by typing never get a call id and
-- must not collide with each other on NULL.
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_interview_sessions_call_session_id
    ON ai_interview_sessions (call_session_id)
    WHERE call_session_id IS NOT NULL;
