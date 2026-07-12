DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'ai_interview_sessions'
          AND column_name = 'application_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE ai_interview_sessions
            ALTER COLUMN application_id DROP NOT NULL;
    END IF;
END $$;
