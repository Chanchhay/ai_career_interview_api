# API Plan


The backend exposes REST APIs from Spring Boot.

Both frontend apps call the same backend API using RTK Query.

## API Base URLs

Development:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Production examples:

```text
https://api.example.com
```

## Standard API Response

All backend responses should follow this shape unless streaming/file download requires otherwise.

```json
{
  "success": true,
  "message": "Request completed successfully",
  "data": {}
}
```

For paginated responses:

```json
{
  "success": true,
  "message": "Request completed successfully",
  "data": {
    "items": [],
    "page": 1,
    "size": 20,
    "totalItems": 100,
    "totalPages": 5
  }
}
```

## Authentication

Protected endpoints require:

```http
Authorization: Bearer <keycloak_access_token>
```

The frontend must attach this token through RTK Query `baseQueryWithAuth`.

The backend validates JWT through Spring Security OAuth2 Resource Server.

## API Ownership by Frontend App

### Public Web

`apps/public-web` uses endpoints for:

- Public job browsing
- Job detail
- Seeker profile
- Resume/CV
- Portfolio
- Favorite jobs
- Job applications
- AI interview start/answer/submit/result
- Recruiter company registration
- Recruiter company document upload
- Recruiter job publishing
- Recruiter forwarded candidates
- Recruiter hiring report

### Internal Console

`apps/internal-console` uses endpoints for:

- Current internal user
- Admin user management
- Company verification
- Job reports/takedown
- AI interview result review
- Human interview scheduling/result
- Application forwarding
- Project assignment/review
- Hiring confirmation
- Commission and invoice management
- System settings

## RTK Query API Files

### Public Web

```text
apps/public-web/features/auth/authApi.ts
apps/public-web/features/jobs/jobsApi.ts
apps/public-web/features/company/companyApi.ts
apps/public-web/features/applications/applicationsApi.ts
apps/public-web/features/interviews/aiInterviewApi.ts
apps/public-web/features/resumes/resumesApi.ts
apps/public-web/features/portfolios/portfoliosApi.ts
apps/public-web/features/recruiter/recruiterApi.ts
```

### Internal Console

```text
apps/internal-console/features/auth/authApi.ts
apps/internal-console/features/admin/adminApi.ts
apps/internal-console/features/moderation/companyVerificationApi.ts
apps/internal-console/features/moderation/jobReportsApi.ts
apps/internal-console/features/interviews/aiInterviewReviewApi.ts
apps/internal-console/features/interviews/humanInterviewApi.ts
apps/internal-console/features/forwarding/forwardingApi.ts
apps/internal-console/features/projects/projectsApi.ts
apps/internal-console/features/finance/financeApi.ts
```

## Current User API

### `GET /api/v1/me`

Returns current authenticated user and role/profile summary.

Used by both frontend apps.

Response data:

```json
{
  "userAccountId": 1,
  "keycloakUserId": "uuid",
  "email": "user@example.com",
  "fullName": "User Name",
  "roles": ["ROLE_JOB_SEEKER"],
  "profiles": {
    "jobSeekerProfileId": 10,
    "recruiterProfileId": null,
    "moderatorProfileId": null,
    "adminProfileId": null,
    "financeProfileId": null
  }
}
```

## Public Job APIs

### `GET /api/v1/public/jobs`

Public job search.

Query params:

```text
keyword
location
categoryId
skillIds
workMode
jobType
page
size
sort
```

Only returns `PUBLISHED` jobs.

### `GET /api/v1/public/jobs/{id}`

Public job detail.

## Recruiter APIs

### `POST /api/v1/recruiter/companies`

Create company profile.

### `PUT /api/v1/recruiter/companies/{id}`

Update own company profile.

### `POST /api/v1/recruiter/companies/{id}/documents`

Upload company document.

### `GET /api/v1/recruiter/companies/me`

Get recruiter's company/company list.

### `POST /api/v1/recruiter/jobs`

Create job draft.

### `PUT /api/v1/recruiter/jobs/{id}`

Update own job draft/published job.

### `POST /api/v1/recruiter/jobs/{id}/publish`

Publish job.

Backend rule:

```text
company.verification_status must be APPROVED
```

### `POST /api/v1/recruiter/jobs/{id}/pause`

Pause own job.

### `POST /api/v1/recruiter/jobs/{id}/close`

Close own job.

### `GET /api/v1/recruiter/forwarded-applications`

Get candidates forwarded to this recruiter.

### `POST /api/v1/recruiter/forwarded-applications/{applicationId}/report-hired`

Report that forwarded seeker was hired.

## Seeker APIs

### `GET /api/v1/seeker/profile`

Get seeker profile.

### `PUT /api/v1/seeker/profile`

Update seeker profile.

### `POST /api/v1/seeker/resumes`

Upload/create resume.

### `GET /api/v1/seeker/resumes`

List own resumes.

### `POST /api/v1/seeker/jobs/{jobId}/favorite`

Save job.

### `DELETE /api/v1/seeker/jobs/{jobId}/favorite`

Unsave job.

### `POST /api/v1/seeker/jobs/{jobId}/apply`

Apply to job.

Creates application with status:

```text
AI_INTERVIEW_PENDING
```

### `GET /api/v1/seeker/applications`

List own applications.

### `GET /api/v1/seeker/applications/{id}`

Get own application detail.

## AI Interview APIs

### `POST /api/v1/seeker/applications/{applicationId}/ai-interview/start`

Start AI interview.

Backend loads application, job, seeker profile, and resume.

Creates AI interview session and generated questions.

### `GET /api/v1/seeker/ai-interviews/{sessionId}`

Get AI interview session/questions.

### `POST /api/v1/seeker/ai-interviews/{sessionId}/answers`

Submit answer(s).

### `POST /api/v1/seeker/ai-interviews/{sessionId}/submit`

Finish AI interview and trigger scoring.

Backend updates application to `AI_PASSED` or `AI_FAILED`.

## Moderator APIs

### Company Verification

#### `GET /api/v1/moderator/companies/pending`

List pending companies.

#### `POST /api/v1/moderator/companies/{companyId}/verify`

Body:

```json
{
  "decision": "APPROVED",
  "note": "Company documents verified."
}
```

### Job Reports

#### `GET /api/v1/moderator/job-reports`

List job reports.

#### `POST /api/v1/moderator/jobs/{jobId}/actions`

Actions:

```text
FLAG
REMOVE
RESTORE
CLOSE
```

### Human Interview

#### `GET /api/v1/moderator/applications/ai-passed`

List applications eligible for human interview.

#### `POST /api/v1/moderator/applications/{applicationId}/human-interviews`

Schedule human interview.

Backend rule:

```text
application.status must be AI_PASSED
```

#### `PUT /api/v1/moderator/human-interviews/{id}`

Reschedule/update interview.

#### `POST /api/v1/moderator/human-interviews/{id}/result`

Record result.

Backend updates application to `HUMAN_PASSED` or `HUMAN_FAILED`.

### Forwarding

#### `POST /api/v1/moderator/applications/{applicationId}/forward`

Forward platform-verified candidate to recruiter.

Backend rule:

```text
application.status must be HUMAN_PASSED or PLATFORM_VERIFIED
```

Creates `application_forwarding_logs`.

Updates application to `FORWARDED_TO_RECRUITER`.

## Project APIs

### `POST /api/v1/moderator/applications/{applicationId}/projects`

Assign project.

### `POST /api/v1/seeker/projects/{projectAssignmentId}/submit`

Submit project.

### `POST /api/v1/moderator/project-submissions/{submissionId}/review`

Review project.

## Finance APIs

### `GET /api/v1/finance/hiring-records`

List hiring reports.

### `POST /api/v1/finance/hiring-records/{id}/confirm`

Confirm hiring.

Creates commission record.

### `POST /api/v1/finance/hiring-records/{id}/reject`

Reject hiring report.

### `GET /api/v1/finance/commissions`

List commission records.

### `POST /api/v1/finance/invoices`

Generate invoice.

### `GET /api/v1/finance/invoices`

List invoices.

### `POST /api/v1/finance/invoices/{id}/payments`

Record payment.

## Admin APIs

### `GET /api/v1/admin/users`

List users.

### `POST /api/v1/admin/users/{id}/suspend`

Suspend user.

### `POST /api/v1/admin/users/{id}/activate`

Activate user.

### `GET /api/v1/admin/audit-logs`

View audit logs.

### `GET /api/v1/admin/system-settings`

List settings.

### `PUT /api/v1/admin/system-settings/{id}`

Update setting.

## API Rules

- Controllers return DTOs, never entities.
- Services enforce business rules.
- Backend validates role and ownership on every protected endpoint.
- Public endpoints must never leak private seeker/recruiter/internal data.
- Recruiter endpoints must only access own company/job/forwarded candidates.
- Seeker endpoints must only access own profile/applications/interviews.
- Internal endpoints must require Admin, Moderator, or Finance role as appropriate.
