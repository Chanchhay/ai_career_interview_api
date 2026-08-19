-- The answer a candidate could have given, written when the interview is
-- scored and shown on the result screen so they can prepare better next time.
ALTER TABLE ai_interview_answers
    ADD COLUMN IF NOT EXISTS model_answer TEXT;
