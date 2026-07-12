DO $$
BEGIN
    IF to_regclass('ai_interview_sessions') IS NOT NULL THEN
        ALTER TABLE ai_interview_sessions
            DROP CONSTRAINT IF EXISTS ai_interview_sessions_status_check;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conrelid = 'ai_interview_sessions'::regclass
              AND conname = 'ai_interview_sessions_status_check'
        ) THEN
            ALTER TABLE ai_interview_sessions
                ADD CONSTRAINT ai_interview_sessions_status_check
                    CHECK (
                        status IN (
                            'PREPARING',
                            'READY',
                            'PENDING',
                            'IN_PROGRESS',
                            'COMPLETED',
                            'FAILED',
                            'CANCELLED'
                        )
                    );
        END IF;
    END IF;
END $$;
