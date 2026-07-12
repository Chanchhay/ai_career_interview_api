Read the current job seeker profile, portfolio, portfolio project, resume,
authentication, controller, service, repository, DTO and mapper patterns before
editing code.

Implement backend-only public talent publishing and recruiter talent discovery.

CONFIRMED BUSINESS RULES

1. A job seeker may explicitly publish their profile, portfolio and resume.
2. Recruiters may browse those published resources.
3. Moderator candidate verification is not required for public talent discovery.
4. Public talent discovery is completely separate from the private job
   application workflow.
5. Recruiters must never receive applications, AI interview data or moderator
   review data through the talent discovery APIs.

PHASE 1 — PUBLICATION

Add or reuse VisibilityStatus with PRIVATE and PUBLIC.

JobSeekerProfile:
- Reuse profileVisibility.
- Only ACTIVE + PUBLIC profiles are discoverable.

Portfolio:
- Reuse visibility.
- Only ACTIVE + PUBLIC portfolios are discoverable.

Resume:
- Add visibility, default PRIVATE.
- Add publishedAt if consistent with existing entity patterns.
- A resume cannot become PUBLIC when resumeFileUrl is null or blank.

Add:

PATCH /api/v1/job-seeker/profile/publication
PATCH /api/v1/job-seeker/portfolios/{portfolioId}/publication
PATCH /api/v1/job-seeker/resumes/{resumeId}/publication

Request:
{
"visibility": "PUBLIC"
}

Rules:
- Resolve the authenticated job seeker profile from the JWT.
- Never accept jobSeekerProfileId from the request.
- Only the owner can publish or unpublish a resource.
- Unpublishing does not delete any data or stored file.

PHASE 2 — RECRUITER TALENT DIRECTORY

Add:

GET /api/v1/recruiter/talent
GET /api/v1/recruiter/talent/{publicProfileSlug}
GET /api/v1/recruiter/talent/{publicProfileSlug}/resumes/{resumeId}/download

List endpoint:
- Pagination
- Optional keyword
- Optional preferred location
- Optional availability status
- Return only ACTIVE + PUBLIC profiles

Detail endpoint:
- Return the public profile
- Return only ACTIVE + PUBLIC portfolios and their projects
- Return only PUBLIC resumes
- Respect salaryVisibility
- Do not expose private contact information unless an existing visibility rule
  explicitly allows it

Resume download:
- RECRUITER role required
- Profile must be PUBLIC
- Resume must belong to the profile
- Resume must be PUBLIC
- Return the file or a short-lived authorized URL

SECURITY BOUNDARY

Never return:
- applications
- cover letters
- AI interview sessions
- AI questions
- AI answers
- AI scores or feedback
- moderator reviews
- Keycloak identifiers
- internal audit values

Use dedicated response DTOs. Do not return JPA entities directly.

Do not implement:
- job application workflow
- moderator candidate review
- forwarded recruiter applications
- frontend
- unrelated refactors

Add tests for publication ownership, visibility filtering, recruiter role
authorization, resume download authorization and prevention of private
application/AI data leakage.

Run:
./gradlew compileJava
./gradlew test

Report changed files, migrations, endpoints, sample requests, authorization
rules, test results and known limitations.