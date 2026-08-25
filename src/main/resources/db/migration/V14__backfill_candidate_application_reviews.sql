-- Applications submitted before review rows were created at submission time.
--
-- The moderator queue reads candidate_application_reviews, and until now a row
-- was only written when an AI interview was scored. Every application whose
-- candidate never took one is therefore invisible to moderators. This gives
-- each of them the PENDING review they should have had from the start.
--
-- Guarded by NOT EXISTS so it is safe to re-run and cannot collide with the
-- unique constraint on application_id.
DO $$
BEGIN
    IF to_regclass('candidate_application_reviews') IS NOT NULL
       AND to_regclass('job_applications') IS NOT NULL THEN

        INSERT INTO candidate_application_reviews (
            application_id,
            review_status,
            created_at,
            updated_at
        )
        SELECT
            application.id,
            'PENDING',
            -- Dated from the application, not from this migration: the queue is
            -- ordered by age, and stamping everything "now" would shuffle a
            -- backlog into arbitrary order.
            COALESCE(application.applied_at, application.created_at, now()),
            now()
        FROM job_applications application
        WHERE NOT EXISTS (
            SELECT 1
            FROM candidate_application_reviews review
            WHERE review.application_id = application.id
        );

    END IF;
END $$;
