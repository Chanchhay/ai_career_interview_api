Read the existing job post, job seeker profile, authentication, repository,
service, controller, DTO and mapper patterns before changing code.

Implement a backend-only, text-first AI interview MVP.

Scope:
1. A JOB_SEEKER starts an interview for a PUBLISHED job.
2. Resolve the authenticated job seeker profile from the security principal.
3. Create an AI interview session with PREPARING status.
4. Generate 7 structured questions from the job title, description, sections,
   experience level and job skills through an AiInterviewQuestionGenerator
   interface.
5. Save questions and mark the session READY.
6. Allow the session owner to start the session and submit one text answer
   per question.
7. Complete the session only when all questions are answered.
8. Evaluate all answers in one AI request through an AiInterviewEvaluator
   interface.
9. Save per-answer score/feedback and one overall feedback record.
10. Mark the session COMPLETED and expose a result endpoint.

Do not implement yet:
- adaptive follow-up questions
- job applications
- recruiter result views
- moderator verification
- unrelated refactors

Critical rules:
- applicationId remains null for this MVP.
- Only PUBLISHED jobs can start interviews.
- Never accept jobSeekerProfileId from the request.
- Never expose expected answers or rubrics in API responses.
- Never keep a DB transaction open during an external AI request.
- Make completion idempotent.
- Validate all model JSON before persistence.
- Use fake AI implementations in unit/integration tests.
- Preserve unrelated existing worktree changes.

Endpoints:
POST /api/v1/job-seeker/jobs/{jobId}/ai-interviews
GET /api/v1/job-seeker/ai-interviews
GET /api/v1/job-seeker/ai-interviews/{sessionId}
POST /api/v1/job-seeker/ai-interviews/{sessionId}/start
PUT /api/v1/job-seeker/ai-interviews/{sessionId}/questions/{questionId}/answer
POST /api/v1/job-seeker/ai-interviews/{sessionId}/complete
GET /api/v1/job-seeker/ai-interviews/{sessionId}/result

Run:
./gradlew compileJava
./gradlew test

Report changed files, endpoint examples, state transitions, test results and
known limitations.