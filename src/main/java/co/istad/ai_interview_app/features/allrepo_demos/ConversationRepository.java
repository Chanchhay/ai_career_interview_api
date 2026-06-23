package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.communication.entity.Conversation;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationStatus;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByApplication_Id(Long applicationId);

    List<Conversation> findByType(ConversationType type);

    List<Conversation> findByStatus(ConversationStatus status);

    List<Conversation> findDistinctByParticipants_UserAccount_Id(Long userAccountId);
}