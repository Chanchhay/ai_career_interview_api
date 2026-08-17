Read the existing JobSeekerProfile, Resume, Portfolio, PortfolioProject,
JobApplication, public talent, publication, authentication, repository,
service, DTO, mapper, migration and integration-test patterns first.

The following flows are already complete:
- company verification
- direct job publishing by approved companies
- public job discovery
- public talent publication and recruiter discovery
- applications and application-linked AI interviews
- moderator candidate review and human interview
- explicit forwarding to recruiters

Implement the next backend slice:

JOB SEEKER PROFILE
→ RESUME MANAGEMENT
→ PORTFOLIO MANAGEMENT

Reason:
The current API can publish and consume candidate resources, but it does not
provide complete owner-facing CRUD APIs for creating and maintaining them.

PHASE 1 — PROFILE

Add:

GET   /api/v1/job-seeker/profile
PATCH /api/v1/job-seeker/profile

Rules:
- Resolve the seeker from JWT.
- Never accept jobSeekerProfileId.
- Update only seeker-editable fields already present in the entity.
- Do not allow updates to verificationStatus, internal status, audit fields,
  or profileVisibility.
- Keep profile publication in the existing publication endpoint.
- Preserve the existing publicProfileSlug.

PHASE 2 — RESUMES

Add:

POST   /api/v1/job-seeker/resumes
GET    /api/v1/job-seeker/resumes
GET    /api/v1/job-seeker/resumes/{resumeId}
PATCH  /api/v1/job-seeker/resumes/{resumeId}
DELETE /api/v1/job-seeker/resumes/{resumeId}
POST   /api/v1/job-seeker/resumes/{resumeId}/default

Rules:
- Owner only.
- New resumes default to PRIVATE.
- Only one default resume per seeker.
- Setting default is transactional.
- Default and publication are independent.
- Keep publication in the existing endpoint.
- Do not delete a resume referenced by an application.
- For this phase, do not replace the file URL when the resume is already
  referenced by an application.
- Reuse the existing storage/file URL approach; do not introduce a second
  storage provider.

PHASE 3 — PORTFOLIOS

Add:

POST   /api/v1/job-seeker/portfolios
GET    /api/v1/job-seeker/portfolios
GET    /api/v1/job-seeker/portfolios/{portfolioId}
PATCH  /api/v1/job-seeker/portfolios/{portfolioId}
DELETE /api/v1/job-seeker/portfolios/{portfolioId}

Rules:
- Owner only.
- New portfolios default to PRIVATE.
- Keep publication in the existing endpoint.
- Recruiter public talent APIs continue returning only PUBLIC and ACTIVE
  portfolios.

PHASE 4 — PORTFOLIO PROJECTS

Add:

POST   /api/v1/job-seeker/portfolios/{portfolioId}/projects
PATCH  /api/v1/job-seeker/portfolios/{portfolioId}/projects/{projectId}
DELETE /api/v1/job-seeker/portfolios/{portfolioId}/projects/{projectId}

Rules:
- Verify ownership through the parent portfolio.
- Projects inherit the portfolio visibility.
- Private portfolio projects must never appear in recruiter responses.
- Preserve displayOrder.

Do not implement:
- frontend
- CV generation
- resume parsing
- chat
- notifications
- finance
- Vapi
- project assessments
- unrelated refactors

Run:
./gradlew clean test
./gradlew compileJava

Regenerate the latest OpenAPI JSON after implementation.

Report:
- changed files
- migration changes
- endpoints
- ownership rules
- sample requests
- test results
- known limitations