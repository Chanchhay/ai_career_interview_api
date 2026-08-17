-- Portfolios are built in the app now, not just linked to an external site, so
-- the seeker picks a template and accent color for how theirs is presented.
-- Those choices are frontend-owned presentation settings rather than new
-- columns, so they live in one JSON document the same way resume_data does on
-- the resumes table.
ALTER TABLE portfolios
    ADD COLUMN IF NOT EXISTS portfolio_data JSONB;
