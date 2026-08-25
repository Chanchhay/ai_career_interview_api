-- Let an administrator write a job's interview questions by hand.
--
-- Until now every question a candidate saw was generated per session. These
-- rows are the template a session copies from, which is why they live apart
-- from ai_interview_questions: editing the bank must never rewrite the
-- transcript of an interview that already happened.
--
-- job_posts gains the rule for what the AI still does on top. It is only
-- consulted once a job actually has written questions, so the default here
-- changes nothing for the jobs that already exist.

CREATE TABLE IF NOT EXISTS job_interview_questions (
    id              BIGSERIAL PRIMARY KEY,
    job_post_id     BIGINT       NOT NULL,
    question_text   TEXT         NOT NULL,
    question_type   VARCHAR(80)  NOT NULL,
    expected_answer TEXT,
    max_score       INTEGER      NOT NULL DEFAULT 10,
    display_order   INTEGER      NOT NULL DEFAULT 1,
    created_at      timestamp with time zone NOT NULL,
    updated_at      timestamp with time zone,
    created_by      varchar(100),
    updated_by      varchar(100),
    CONSTRAINT fk_job_interview_questions_job_post
        FOREIGN KEY (job_post_id) REFERENCES job_posts (id) ON DELETE CASCADE
);

-- Every read is "this job's questions, in order".
CREATE INDEX IF NOT EXISTS idx_job_interview_questions_job_post
    ON job_interview_questions (job_post_id, display_order);

ALTER TABLE job_posts
    ADD COLUMN IF NOT EXISTS manual_question_mode VARCHAR(50);

-- Two statements rather than adding the column with a default: an existing row
-- needs the value too, and only then can the column be made NOT NULL.
UPDATE job_posts
SET manual_question_mode = 'MANUAL_PLUS_AI'
WHERE manual_question_mode IS NULL;

ALTER TABLE job_posts
    ALTER COLUMN manual_question_mode SET DEFAULT 'MANUAL_PLUS_AI',
    ALTER COLUMN manual_question_mode SET NOT NULL;
