package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job_seeker.domain.ai.AiInterviewQuestion;
import co.istad.ai_interview_app.shared.enums.InterviewQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface AiInterviewQuestionRepository extends JpaRepository<AiInterviewQuestion, Long> {

    List<AiInterviewQuestion> findBySession_IdOrderByDisplayOrderAsc(Long sessionId);

    List<AiInterviewQuestion> findBySession_IdAndQuestionType(
            Long sessionId,
            InterviewQuestionType questionType
    );
}