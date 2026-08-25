-- AI interviews the candidate sat for a job they had also applied to, but which
-- were never attached to the application.
--
-- Interviews started from the job page hard-coded a null application, and that
-- entry point is reachable after applying. The result: a candidate could sit
-- and pass the interview, and the moderator would still be told "Approval
-- requires a completed AI interview", because the approval check looks for a
-- session attached to the application.
--
-- Matching is unambiguous. job_applications is unique on
-- (job_post_id, job_seeker_profile_id), so a session's job plus its candidate
-- identify at most one application. Only unattached sessions are touched, so
-- re-running changes nothing.
DO $$
DECLARE
    attached bigint;
BEGIN
    IF to_regclass('ai_interview_sessions') IS NULL
       OR to_regclass('job_applications') IS NULL
       OR to_regclass('job_seeker_profiles') IS NULL THEN
        RETURN;
    END IF;

    UPDATE ai_interview_sessions session
    SET application_id = matched.application_id
    FROM (
        SELECT
            candidate_session.id AS session_id,
            application.id       AS application_id
        FROM ai_interview_sessions candidate_session
        JOIN job_seeker_profiles profile
          ON profile.user_account_id = candidate_session.job_seeker_id
        JOIN job_applications application
          ON application.job_post_id = candidate_session.job_post_id
         AND application.job_seeker_profile_id = profile.id
        WHERE candidate_session.application_id IS NULL
    ) AS matched
    WHERE session.id = matched.session_id;

    GET DIAGNOSTICS attached = ROW_COUNT;

    IF attached > 0 THEN
        RAISE NOTICE 'Attached % AI interview session(s) to their application', attached;
    END IF;

    -- A completed interview means the case is waiting on a moderator. Anything
    -- still sitting at SUBMITTED was left behind by the same gap, since the
    -- status is only advanced for attached sessions.
    IF to_regclass('candidate_application_reviews') IS NOT NULL THEN
        UPDATE job_applications application
        SET status = 'MODERATOR_REVIEW_PENDING'
        WHERE application.status = 'SUBMITTED'
          AND EXISTS (
              SELECT 1
              FROM ai_interview_sessions session
              WHERE session.application_id = application.id
                AND session.status = 'COMPLETED'
          );
    END IF;
END $$;
