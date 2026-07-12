package co.istad.ai_interview_app.features.interview.ai.repository;

import co.istad.ai_interview_app.features.interview.ai.entity.AiInterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiInterviewAnswerRepository extends JpaRepository<AiInterviewAnswer, Long> {

    Optional<AiInterviewAnswer> findByQuestion_Id(Long questionId);

    List<AiInterviewAnswer> findAllByQuestion_Session_Id(Long sessionId);
}
