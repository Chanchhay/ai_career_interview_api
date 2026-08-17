-- Recruiters can post a job by uploading the PDF job description they already
-- have; the AI extraction fills the form, and the original document is kept so
-- the recruiter can check what the extracted fields came from.
--
-- Stored under the MinIO private prefix and deliberately never exposed on the
-- public job endpoints: a source JD often carries internal notes or salary
-- bands the recruiter did not choose to publish.
ALTER TABLE job_posts
    ADD COLUMN IF NOT EXISTS source_file_url VARCHAR(500);
