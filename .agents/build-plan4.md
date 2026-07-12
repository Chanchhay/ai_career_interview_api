Read the existing job post, job seeker, resume, AI interview, moderator,
recruiter, authentication, repository, DTO, service, controller and migration
patterns before changing code.

The public talent publishing and recruiter talent-discovery APIs are complete.
They must remain separate from private job applications.

Implement the next backend vertical slice:
PRIVATE JOB APPLICATIONS → AI INTERVIEW → MODERATOR GATE → FORWARDING.

CONFIRMED RULES

1. Recruiters may browse profiles, portfolios and resumes explicitly published
   through the public talent APIs.
2. Public visibility never grants access to private applications, cover
   letters, AI interview data or moderator reviews.
3. Recruiters cannot see an application until a moderator explicitly forwards
   it.
4. Candidate approval is application-specific. Do not rely only on the global
   JobSeekerProfile verificationStatus.

PHASE 1 — JOB-SEEKER APPLICATIONS

Add:

POST /api/v1/job-seeker/jobs/{jobId}/applications
GET /api/v1/job-seeker/applications
GET /api/v1/job-seeker/applications/{applicationId}
POST /api/v1/job-seeker/applications/{applicationId}/withdraw

Rules:
- Resolve the seeker from JWT.
- Never accept jobSeekerProfileId.
- Only PUBLISHED, non-expired jobs accept applications.
- Prevent duplicate applications for one seeker and job.
- Validate that the optional resume belongs to the seeker.
- The submitted resume may be PRIVATE.
- Add unique(job_post_id, job_seeker_profile_id).
- Recruiters receive no access to these records.

PHASE 2 — APPLICATION-LINKED AI INTERVIEW

Add:

POST /api/v1/job-seeker/applications/{applicationId}/ai-interviews

Rules:
- Application must belong to the authenticated seeker.
- Derive the job from the application.
- Set ai_interview_sessions.application_id.
- Prevent multiple active sessions for one application.
- Keep practice sessions with applicationId null working.
- Starting an application interview updates the application to
  AI_INTERVIEW_IN_PROGRESS.
- Successful AI completion updates the application to
  MODERATOR_REVIEW_PENDING.
- AI failure must not expose partial data to recruiters.

PHASE 3 — MODERATOR REVIEW

Add:

GET /api/v1/moderator/candidate-applications
GET /api/v1/moderator/candidate-applications/{applicationId}

The list supports status filtering and pagination.

The moderator detail may include:
- application
- candidate profile
- submitted resume
- cover letter
- AI result
- questions, answers, scores and feedback
- human interview information
- project assignment information

Never expose provider prompts, Gemini metadata, credentials or stack traces.

Add a dedicated CandidateApplicationReview entity/table with:
- application
- moderator
- review status
- decision note
- reviewedAt
- approvedAt
- forwardedAt

Use review statuses:
PENDING,
IN_REVIEW,
HUMAN_INTERVIEW_SCHEDULED,
DECISION_PENDING,
APPROVED,
REJECTED,
FORWARDED.

PHASE 4 — HUMAN INTERVIEW

Add:

POST /api/v1/moderator/candidate-applications/{applicationId}/human-interviews
PATCH /api/v1/moderator/human-interviews/{interviewId}/reschedule
POST /api/v1/moderator/human-interviews/{interviewId}/complete
POST /api/v1/moderator/human-interviews/{interviewId}/cancel

Use an external meetingUrl. Do not implement video infrastructure.

PHASE 5 — MODERATOR DECISION AND FORWARDING

Add:

POST /api/v1/moderator/candidate-applications/{applicationId}/approve
POST /api/v1/moderator/candidate-applications/{applicationId}/reject
POST /api/v1/moderator/candidate-applications/{applicationId}/forward

Rules:
- Approval requires completed AI and human interviews.
- Rejected or withdrawn applications cannot be approved.
- Forwarding requires APPROVED.
- Approval does not automatically expose the application.
- Forwarding changes the review status to FORWARDED.

PHASE 6 — RECRUITER FORWARDED APPLICATIONS

Add:

GET /api/v1/recruiter/forwarded-applications
GET /api/v1/recruiter/forwarded-applications/{applicationId}

Rules:
- Return only FORWARDED applications.
- Recruiter must own the application’s job post.
- Public talent discovery remains separate.
- Do not expose private grading rubrics, expected answers, provider metadata
  or internal moderator notes.

TESTS

- Cannot apply twice.
- Cannot apply to draft, paused, closed or expired job.
- Cannot submit another seeker's resume.
- Private resume can be submitted by its owner.
- Another seeker cannot access the application.
- Application AI session links correctly.
- AI completion moves the application to moderator review.
- Recruiter cannot see an unforwarded application.
- Moderator can access the review queue.
- Approval requires completed interviews.
- Forwarding requires approval.
- Recruiter can access only forwarded applications for their own jobs.
- Public talent APIs never expose application or AI data.

Preserve unrelated worktree changes.
Do not implement frontend, chat, notifications, finance, Vapi or unrelated
refactors.

Run:
./gradlew compileJava
./gradlew test

Report changed files, migrations, endpoints, state transitions, sample
Postman requests, security rules, test results and known limitations.