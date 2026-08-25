package co.istad.ai_interview_app.features.interview.question.repository;

import co.istad.ai_interview_app.features.interview.question.entity.JobInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobInterviewQuestionRepository extends JpaRepository<JobInterviewQuestion, Long> {

    /** A job's questions in the order candidates are asked them. */
    List<JobInterviewQuestion> findAllByJobPost_IdOrderByDisplayOrderAsc(Long jobPostId);

    boolean existsByJobPost_Id(Long jobPostId);
}
