package co.istad.ai_interview_app.features.recruiter.talent.dto;

public record PublicResumeDownloadResponse(
        Long resumeId,
        String downloadUrl
) {
}
