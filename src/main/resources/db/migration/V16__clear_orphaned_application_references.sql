-- Rows pointing at job applications that no longer exist.
--
-- These block startup rather than merely being untidy: `ddl-auto: update` adds
-- the foreign keys Hibernate derives from the entities, and PostgreSQL refuses
-- to add one while a violating row is present. The failure surfaces as
-- "Key (application_id)=(N) is not present in table job_applications" during
-- schema migration, and the application never finishes booting.
--
-- They exist because the FK was never actually created on this database: V5
-- guarded its ALTER behind a NOT EXISTS check that evidently did not fire, so
-- nothing stopped an application from being deleted while its children stayed.
--
-- Every statement is guarded on the table existing and runs child-first, so a
-- database that never had a given feature simply skips it. Re-running is a
-- no-op once the orphans are gone.
DO $$
DECLARE
    removed bigint;
BEGIN
    IF to_regclass('job_applications') IS NULL THEN
        RETURN;
    END IF;

    /* --------------------------------------------------- nullable links --- */
    -- These rows are meaningful without an application: a practice AI interview
    -- is never tied to one, and a conversation outlives the case it began with.
    -- Blanking the reference keeps the record; deleting it would lose history.

    IF to_regclass('ai_interview_sessions') IS NOT NULL THEN
        UPDATE ai_interview_sessions session
        SET application_id = NULL
        WHERE session.application_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM job_applications application
              WHERE application.id = session.application_id
          );
    END IF;

    IF to_regclass('conversations') IS NOT NULL THEN
        UPDATE conversations conversation
        SET application_id = NULL
        WHERE conversation.application_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM job_applications application
              WHERE application.id = conversation.application_id
          );
    END IF;

    /* ------------------------------------------- the finance chain first --- */
    -- A hiring record cannot be blanked — it exists to say who was hired for
    -- which application — so an orphan is deleted, and its commission and any
    -- invoice line must go with it or they become orphans in turn.

    IF to_regclass('hiring_records') IS NOT NULL THEN
        IF to_regclass('invoice_items') IS NOT NULL AND to_regclass('commission_records') IS NOT NULL THEN
            DELETE FROM invoice_items item
            WHERE item.commission_record_id IN (
                SELECT commission.id
                FROM commission_records commission
                JOIN hiring_records hire ON hire.id = commission.hiring_record_id
                WHERE NOT EXISTS (
                    SELECT 1 FROM job_applications application
                    WHERE application.id = hire.application_id
                )
            );
        END IF;

        IF to_regclass('commission_records') IS NOT NULL THEN
            DELETE FROM commission_records commission
            WHERE commission.hiring_record_id IN (
                SELECT hire.id
                FROM hiring_records hire
                WHERE NOT EXISTS (
                    SELECT 1 FROM job_applications application
                    WHERE application.id = hire.application_id
                )
            );
        END IF;

        DELETE FROM hiring_records hire
        WHERE NOT EXISTS (
            SELECT 1 FROM job_applications application
            WHERE application.id = hire.application_id
        );

        GET DIAGNOSTICS removed = ROW_COUNT;
        IF removed > 0 THEN
            RAISE NOTICE 'Removed % orphaned hiring_records', removed;
        END IF;
    END IF;

    /* ------------------------------------------------ required links --- */
    -- Each of these is meaningless without its application, and unreadable
    -- besides: loading one makes Hibernate fetch an application that is not
    -- there, which fails the request rather than returning a partial row.

    IF to_regclass('project_assignments') IS NOT NULL THEN
        IF to_regclass('project_reviews') IS NOT NULL AND to_regclass('project_submissions') IS NOT NULL THEN
            DELETE FROM project_reviews review
            WHERE review.project_submission_id IN (
                SELECT submission.id
                FROM project_submissions submission
                JOIN project_assignments assignment ON assignment.id = submission.project_assignment_id
                WHERE NOT EXISTS (
                    SELECT 1 FROM job_applications application
                    WHERE application.id = assignment.application_id
                )
            );
        END IF;

        IF to_regclass('project_submissions') IS NOT NULL THEN
            DELETE FROM project_submissions submission
            WHERE submission.project_assignment_id IN (
                SELECT assignment.id
                FROM project_assignments assignment
                WHERE NOT EXISTS (
                    SELECT 1 FROM job_applications application
                    WHERE application.id = assignment.application_id
                )
            );
        END IF;

        DELETE FROM project_assignments assignment
        WHERE NOT EXISTS (
            SELECT 1 FROM job_applications application
            WHERE application.id = assignment.application_id
        );
    END IF;

    IF to_regclass('human_interviews') IS NOT NULL THEN
        DELETE FROM human_interviews interview
        WHERE NOT EXISTS (
            SELECT 1 FROM job_applications application
            WHERE application.id = interview.application_id
        );

        GET DIAGNOSTICS removed = ROW_COUNT;
        IF removed > 0 THEN
            RAISE NOTICE 'Removed % orphaned human_interviews', removed;
        END IF;
    END IF;

    IF to_regclass('candidate_application_reviews') IS NOT NULL THEN
        DELETE FROM candidate_application_reviews review
        WHERE NOT EXISTS (
            SELECT 1 FROM job_applications application
            WHERE application.id = review.application_id
        );

        GET DIAGNOSTICS removed = ROW_COUNT;
        IF removed > 0 THEN
            RAISE NOTICE 'Removed % orphaned candidate_application_reviews', removed;
        END IF;
    END IF;
END $$;
