Read the existing company, recruiter job-post, moderator profile, security,
migration, repository, DTO, service, controller and integration-test patterns.

CONFIRMED BUSINESS RULE

Moderator verification applies to the COMPANY only.

Once a company is APPROVED and ACTIVE, its recruiter may create and publish
jobs directly. Individual jobs do not require moderator review.

Remove all planned job-review submission and moderator job-moderation scope.

Implement:

PHASE 1 — COMPANY DOCUMENTS

POST   /api/v1/recruiter/companies/{companyId}/documents
GET    /api/v1/recruiter/companies/{companyId}/documents
DELETE /api/v1/recruiter/companies/{companyId}/documents/{documentId}
POST   /api/v1/recruiter/companies/{companyId}/submit-verification

Rules:
- Recruiter must own the company.
- Submission requires required documents.
- Submitted company becomes PENDING_VERIFICATION.
- Company documents remain private.

PHASE 2 — MODERATOR COMPANY VERIFICATION

GET  /api/v1/moderator/companies
GET  /api/v1/moderator/companies/{companyId}
POST /api/v1/moderator/companies/{companyId}/approve
POST /api/v1/moderator/companies/{companyId}/reject
POST /api/v1/moderator/companies/{companyId}/request-revision

Rules:
- MODERATOR role required.
- Every decision creates a company verification history record.
- Approval sets verificationStatus to APPROVED.
- Reject and request-revision require a note.
- Recruiters cannot perform verification actions.

PHASE 3 — DIRECT RECRUITER JOB PUBLICATION

Keep:
POST /api/v1/recruiter/jobs/{id}/publish

Add rules:
- Recruiter owns the job.
- Company verificationStatus must be APPROVED.
- Company status must be ACTIVE.
- Job must be complete and non-expired.
- Publish directly without moderator job review.
- Pause, resume and close remain recruiter-owned actions.
- Do not use PENDING, APPROVED or REJECTED as active job workflow states.

PHASE 4 — PUBLIC JOB DISCOVERY

Add permit-all endpoints:

GET /api/v1/public/jobs
GET /api/v1/public/jobs/{jobId}
GET /api/v1/public/job-categories
GET /api/v1/public/skills
GET /api/v1/public/industries

Return jobs only when:
- job status is PUBLISHED
- company is APPROVED
- company is ACTIVE
- job is not expired

Do not expose:
- company documents
- verification history
- recruiter Keycloak/user identifiers
- applications
- AI interview data
- moderator information

Tests:
- Unverified company cannot publish.
- Approved company can publish directly.
- Recruiter cannot publish another recruiter's job.
- Suspended company jobs do not appear publicly.
- Public API excludes drafts, paused, closed and expired jobs.
- Public API excludes jobs belonging to unapproved companies.
- Existing application and AI interview tests remain green.

Run:
./gradlew compileJava
./gradlew test