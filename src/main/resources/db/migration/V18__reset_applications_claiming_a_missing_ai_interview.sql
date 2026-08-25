-- Applications whose status claims an AI interview that does not exist.
--
-- Two states are only ever reached by running an interview:
--   AI_INTERVIEW_IN_PROGRESS  - set when a session is created
--   MODERATOR_REVIEW_PENDING  - set when a session is scored
--
-- Before interviews were attached to applications (V17), a candidate could sit
-- one from the job page and have it recorded against nothing. Combined with a
-- status left behind by an earlier attempt, that produced a deadlock: starting
-- an interview was refused because the status implied one existed, and approval
-- was refused because no session did. Neither path was open.
--
-- Sending these back to SUBMITTED reopens the interview, which is the only step
-- that can put the application back into a consistent state. Anything with a
-- completed session attached is left alone — its status is telling the truth.
--
-- Deliberately does not touch SHORTLISTED or HUMAN_INTERVIEW_SCHEDULED: a
-- moderator put those there on purpose, and rewinding somebody's decision is
-- not this migration's call to make.
DO $$
DECLARE
    reset_count bigint;
BEGIN
    IF to_regclass('job_applications') IS NULL
       OR to_regclass('ai_interview_sessions') IS NULL THEN
        RETURN;
    END IF;

    UPDATE job_applications application
    SET status = 'SUBMITTED'
    WHERE application.status IN ('AI_INTERVIEW_IN_PROGRESS', 'MODERATOR_REVIEW_PENDING')
      AND NOT EXISTS (
          SELECT 1
          FROM ai_interview_sessions session
          WHERE session.application_id = application.id
            AND session.status = 'COMPLETED'
      );

    GET DIAGNOSTICS reset_count = ROW_COUNT;

    IF reset_count > 0 THEN
        RAISE NOTICE
            'Reset % application(s) whose status claimed an AI interview that does not exist',
            reset_count;
    END IF;
END $$;
