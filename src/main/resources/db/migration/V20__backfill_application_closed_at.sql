-- Closure timestamps for applications that were closed before the column
-- existed.
--
-- The re-apply cooldown counts from closed_at. Rows closed earlier have none,
-- and an unknown closure date cannot be turned into a wait — the service treats
-- those as not blocking, which would let anyone rejected before today re-apply
-- immediately regardless of the setting.
--
-- updated_at is the best evidence available: for a closed application the last
-- write was almost always the rejection or withdrawal itself. It is an estimate,
-- and only ever applied to rows that have no closed_at at all.
DO $$
DECLARE
    stamped bigint;
BEGIN
    IF to_regclass('job_applications') IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'job_applications' AND column_name = 'closed_at'
    ) THEN
        RETURN;
    END IF;

    UPDATE job_applications
    SET closed_at = COALESCE(updated_at, created_at, applied_at)
    WHERE status IN ('REJECTED', 'WITHDRAWN')
      AND closed_at IS NULL;

    GET DIAGNOSTICS stamped = ROW_COUNT;

    IF stamped > 0 THEN
        RAISE NOTICE 'Stamped closed_at on % previously closed application(s)', stamped;
    END IF;
END $$;
