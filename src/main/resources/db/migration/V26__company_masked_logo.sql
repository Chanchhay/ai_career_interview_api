-- Give a masked company a stand-in logo an administrator controls.
--
-- Masking withholds the real logo, because a logo names an employer as surely
-- as its name does. That left every confidential posting with a blank space
-- where a mark should be. This column holds a neutral image an administrator
-- sets on the company's behalf; it is shown to candidates ONLY while the
-- company is masked, and the recruiter cannot write it.
--
-- Null is the ordinary state and stays the default: no existing company gains
-- a stand-in from this release, and a masked company without one keeps the
-- blank placeholder the frontend already draws.

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS masked_logo_url VARCHAR(500);
