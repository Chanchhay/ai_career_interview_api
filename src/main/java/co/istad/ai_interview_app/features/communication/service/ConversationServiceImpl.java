package co.istad.ai_interview_app.features.communication.service;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.application.repository.JobApplicationRepository;
import co.istad.ai_interview_app.features.communication.dto.ConversationParticipantResponse;
import co.istad.ai_interview_app.features.communication.dto.ConversationResponse;
import co.istad.ai_interview_app.features.communication.dto.CreateConversationRequest;
import co.istad.ai_interview_app.features.communication.dto.MessageResponse;
import co.istad.ai_interview_app.features.communication.dto.OpenSupportRequest;
import co.istad.ai_interview_app.features.communication.dto.SendMessageRequest;
import co.istad.ai_interview_app.features.communication.entity.Conversation;
import co.istad.ai_interview_app.features.communication.entity.ConversationParticipant;
import co.istad.ai_interview_app.features.communication.entity.Message;
import co.istad.ai_interview_app.features.communication.repository.ConversationParticipantRepository;
import co.istad.ai_interview_app.features.communication.repository.ConversationRepository;
import co.istad.ai_interview_app.features.communication.repository.MessageRepository;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserJobSeekerProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserRecruiterProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.identity.service.UserAccountRoleResolver;
import co.istad.ai_interview_app.features.moderator.repository.ModeratorProfileRepository;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.features.notification.event.NotificationEvents;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationStatus;
import co.istad.ai_interview_app.shared.enums.conversation.ConversationType;
import co.istad.ai_interview_app.shared.enums.conversation.MessageStatus;
import co.istad.ai_interview_app.shared.enums.conversation.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Moderator-mediated messaging.
 *
 * <p>Only a moderator may open a thread, and a thread holds exactly two people:
 * the moderator and one counterpart. There is deliberately no recruiter-to-
 * candidate channel — the platform keeps those two apart until a moderator
 * forwards the candidate, and a chat that ignored that would undo the boundary
 * the application endpoints, the talent search, and the notification rules all
 * enforce.
 *
 * <p>Every read and write resolves the caller's own participant row first. That
 * single lookup is the authorization check: not being in the thread is
 * indistinguishable from the thread not existing, so probing ids leaks nothing.
 */
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final IdentityUserAccountRepository userAccountRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CompanyRepository companyRepository;
    private final CurrentUserJobSeekerProfileRepository jobSeekerProfileRepository;
    private final CurrentUserRecruiterProfileRepository recruiterProfileRepository;
    private final UserAccountRoleResolver roleResolver;
    private final ModeratorProfileRepository moderatorProfileRepository;
    private final ApplicationEventPublisher events;

    /* ------------------------------------------------------- participant --- */

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> findMyConversations(Pageable pageable) {
        UserAccount me = currentUserAccount();
        Page<Conversation> page = conversationRepository.findAllForParticipant(me.getId(), pageable);

        List<Long> conversationIds = page.getContent().stream().map(Conversation::getId).toList();

        // Both maps are one query each, so an inbox of any size costs three
        // round trips rather than three per row.
        Map<Long, Message> latestByConversation = latestMessages(conversationIds);
        Map<Long, Long> unreadByConversation = unreadCounts(me.getId(), conversationIds);

        return page.map(conversation -> toResponse(
                conversation,
                me,
                latestByConversation.get(conversation.getId()),
                unreadByConversation.getOrDefault(conversation.getId(), 0L)
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId) {
        UserAccount me = currentUserAccount();
        Conversation conversation = requireMyConversation(conversationId, me);

        return toResponse(
                conversation,
                me,
                latestMessages(List.of(conversationId)).get(conversationId),
                unreadCounts(me.getId(), List.of(conversationId)).getOrDefault(conversationId, 0L)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> findMessages(Long conversationId, Pageable pageable) {
        UserAccount me = currentUserAccount();
        requireMyConversation(conversationId, me);

        return messageRepository
                .findAllByConversation_IdOrderBySentAtDesc(conversationId, pageable)
                .map(message -> toResponse(message, me));
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request) {
        UserAccount me = currentUserAccount();
        Conversation conversation = requireMyConversation(conversationId, me);

        if (conversation.getStatus() != ConversationStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This conversation is %s and cannot receive new messages"
                            .formatted(conversation.getStatus().name().toLowerCase())
            );
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderUserAccount(me);
        message.setContent(request.content().trim());
        message.setMessageType(MessageType.TEXT);
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(Instant.now());

        Message saved = messageRepository.save(message);

        // Sending is also reading: nothing you just wrote should count as
        // unread against you.
        participantRepository
                .findByConversation_IdAndUserAccount_Id(conversationId, me.getId())
                .ifPresent(participant -> participant.setLastReadAt(saved.getSentAt()));

        events.publishEvent(new NotificationEvents.MessageReceived(saved.getId()));

        return toResponse(saved, me);
    }

    @Override
    @Transactional
    public ConversationResponse markAsRead(Long conversationId) {
        UserAccount me = currentUserAccount();
        requireMyConversation(conversationId, me);

        participantRepository
                .findByConversation_IdAndUserAccount_Id(conversationId, me.getId())
                .ifPresent(participant -> participant.setLastReadAt(Instant.now()));

        return getConversation(conversationId);
    }

    /**
     * Soft-deletes one of the caller's own messages.
     *
     * <p>The row stays and the content is cleared. Removing it outright would
     * renumber a thread the other person has already read, and a moderated
     * channel is exactly where the record of what was said needs to survive
     * someone's second thoughts.
     */
    @Override
    @Transactional
    public void deleteMessage(Long conversationId, Long messageId) {
        UserAccount me = currentUserAccount();
        requireMyConversation(conversationId, me);

        Message message = messageRepository
                .findByIdAndConversation_Id(messageId, conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message was not found"));

        if (!message.getSenderUserAccount().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own messages");
        }

        if (message.getStatus() == MessageStatus.DELETED) return;

        message.setStatus(MessageStatus.DELETED);
        message.setContent(null);
        message.setDeletedAt(Instant.now());
    }

    /**
     * Opens the caller's support thread, or continues the one they already have.
     *
     * <p>Addressed to the whole active moderator team rather than to one
     * person: support is a queue, and whoever is free should be able to answer
     * without the thread having been aimed at them.
     *
     * <p>Participants are fixed at creation, so a moderator who joins the
     * platform later will not appear in threads opened before them. Acceptable
     * while the team is small; if it stops being true, the fix is to resolve
     * moderator recipients at read time rather than to backfill rows.
     */
    @Override
    @Transactional
    public ConversationResponse openSupportConversation(OpenSupportRequest request) {
        UserAccount me = currentUserAccount();

        Conversation existing = conversationRepository
                .findOpenSupportConversations(me.getId())
                .stream()
                .findFirst()
                .orElse(null);

        if (existing != null) {
            // Continuing rather than creating: the caller gets one history, and
            // repeated submissions cannot fan out into a queue of duplicates.
            appendOpeningMessage(existing, me, request.message());
            return getConversation(existing.getId());
        }

        List<UserAccount> moderators = moderatorRecipients(me);

        if (moderators.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No moderator is available to receive support requests right now"
            );
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.SUPPORT);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setTitle(resolveSupportTitle(request.subject()));
        Conversation saved = conversationRepository.save(conversation);

        addParticipant(saved, me);
        moderators.forEach(moderator -> addParticipant(saved, moderator));

        appendOpeningMessage(saved, me, request.message());

        return getConversation(saved.getId());
    }

    /**
     * Active moderators, minus the caller.
     *
     * <p>The subtraction matters: a moderator who opens a support thread would
     * otherwise be added twice and trip the participant uniqueness constraint.
     */
    private List<UserAccount> moderatorRecipients(UserAccount caller) {
        List<Long> moderatorAccountIds = moderatorProfileRepository
                .findUserAccountIdsByStatus(ProfileStatus.ACTIVE)
                .stream()
                .filter(id -> !id.equals(caller.getId()))
                .toList();

        return moderatorAccountIds.isEmpty()
                ? List.of()
                : userAccountRepository.findAllById(moderatorAccountIds);
    }

    private String resolveSupportTitle(String subject) {
        String title = normalizeBlankToNull(subject);
        return title == null ? "Support request" : title;
    }

    /* ---------------------------------------------------------- moderator --- */

    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        UserAccount moderator = currentUserAccount();
        Target target = resolveTarget(request);

        if (target.recipient().getId().equals(moderator.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot open a conversation with yourself"
            );
        }

        Conversation existing = findReusableConversation(target, moderator);

        if (existing != null) {
            appendOpeningMessage(existing, moderator, request.message());
            return getConversation(existing.getId());
        }

        Conversation conversation = new Conversation();
        conversation.setType(target.type());
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setApplication(target.application());
        conversation.setTitle(resolveTitle(request.title(), target));
        Conversation saved = conversationRepository.save(conversation);

        addParticipant(saved, moderator);
        addParticipant(saved, target.recipient());

        appendOpeningMessage(saved, moderator, request.message());

        return getConversation(saved.getId());
    }

    @Override
    @Transactional
    public ConversationResponse closeConversation(Long conversationId) {
        UserAccount me = currentUserAccount();
        Conversation conversation = requireMyConversation(conversationId, me);

        conversation.setStatus(ConversationStatus.CLOSED);

        return getConversation(conversationId);
    }

    /* ------------------------------------------------------------ target --- */

    private record Target(
            UserAccount recipient,
            ConversationType type,
            JobApplication application,
            String subject
    ) {
    }

    /**
     * Exactly one of the three addressing fields must be set. Accepting more
     * than one would make the recipient depend on an evaluation order the
     * caller cannot see.
     */
    private Target resolveTarget(CreateConversationRequest request) {
        int provided = 0;
        if (request.applicationId() != null) provided++;
        if (request.companyId() != null) provided++;
        if (hasText(request.recipientKeycloakUserId())) provided++;

        if (provided != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name exactly one of applicationId, companyId, or recipientKeycloakUserId"
            );
        }

        if (request.applicationId() != null) {
            JobApplication application = jobApplicationRepository.findById(request.applicationId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Job application was not found"
                    ));

            UserAccount candidate = Optional.ofNullable(application.getJobSeekerProfile())
                    .map(profile -> profile.getUserAccount())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "This application has no candidate account to message"
                    ));

            return new Target(
                    candidate,
                    ConversationType.APPLICATION,
                    application,
                    application.getJobPost().getTitle()
            );
        }

        if (request.companyId() != null) {
            Company company = companyRepository.findById(request.companyId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company was not found"));

            UserAccount recruiter = Optional.ofNullable(company.getRecruiterProfile())
                    .map(profile -> profile.getUserAccount())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "This company has no recruiter account to message"
                    ));

            return new Target(recruiter, ConversationType.GENERAL, null, company.getName());
        }

        UserAccount recipient = userAccountRepository
                .findByKeycloakUserId(request.recipientKeycloakUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient was not found"));

        return new Target(recipient, ConversationType.GENERAL, null, null);
    }

    private Conversation findReusableConversation(Target target, UserAccount moderator) {
        if (target.application() != null) {
            return conversationRepository
                    .findFirstByApplication_IdAndTypeAndStatus(
                            target.application().getId(),
                            ConversationType.APPLICATION,
                            ConversationStatus.OPEN
                    )
                    .orElse(null);
        }

        return participantRepository
                .findSharedOpenConversationIds(moderator.getId(), target.recipient().getId(), target.type())
                .stream()
                .findFirst()
                .flatMap(conversationRepository::findById)
                .orElse(null);
    }

    private String resolveTitle(String requested, Target target) {
        String title = normalizeBlankToNull(requested);
        if (title != null) return title;

        return target.subject() == null ? "Conversation" : target.subject();
    }

    private void addParticipant(Conversation conversation, UserAccount userAccount) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(conversation);
        participant.setUserAccount(userAccount);
        participant.setJoinedAt(Instant.now());
        participantRepository.save(participant);
    }

    private void appendOpeningMessage(Conversation conversation, UserAccount sender, String content) {
        String text = normalizeBlankToNull(content);
        if (text == null) return;

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderUserAccount(sender);
        message.setContent(text.trim());
        message.setMessageType(MessageType.TEXT);
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(Instant.now());

        Message saved = messageRepository.save(message);
        events.publishEvent(new NotificationEvents.MessageReceived(saved.getId()));
    }

    /* ----------------------------------------------------------- helpers --- */

    private UserAccount currentUserAccount() {
        return userAccountRepository.findByKeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account was not found for authenticated user"
                ));
    }

    /**
     * Resolves a conversation the caller is actually in.
     *
     * <p>404 rather than 403 when they are not: telling an outsider that a
     * thread exists is itself a disclosure, and here it would confirm that two
     * particular people are talking.
     */
    private Conversation requireMyConversation(Long conversationId, UserAccount me) {
        participantRepository
                .findByConversation_IdAndUserAccount_Id(conversationId, me.getId())
                .filter(participant -> participant.getLeftAt() == null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation was not found"
                ));

        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation was not found"
                ));
    }

    private Map<Long, Message> latestMessages(List<Long> conversationIds) {
        if (conversationIds.isEmpty()) return Map.of();

        Map<Long, Message> latest = new HashMap<>();

        for (Message message : messageRepository.findLatestPerConversation(conversationIds)) {
            latest.merge(
                    message.getConversation().getId(),
                    message,
                    (first, second) -> first.getId() >= second.getId() ? first : second
            );
        }

        return latest;
    }

    private Map<Long, Long> unreadCounts(Long userAccountId, List<Long> conversationIds) {
        if (conversationIds.isEmpty()) return Map.of();

        Map<Long, Long> counts = new HashMap<>();

        for (Object[] row : messageRepository.countUnreadPerConversation(userAccountId, conversationIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }

        return counts;
    }

    private ConversationResponse toResponse(
            Conversation conversation,
            UserAccount me,
            Message lastMessage,
            long unreadCount
    ) {
        List<ConversationParticipantResponse> participants =
                participantRepository.findAllByConversation_Id(conversation.getId()).stream()
                        .sorted(Comparator.comparing(ConversationParticipant::getId))
                        .map(participant -> toResponse(participant, me))
                        .toList();

        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getType(),
                conversation.getStatus(),
                conversation.getApplication() == null ? null : conversation.getApplication().getId(),
                participants,
                lastMessage == null ? null : toResponse(lastMessage, me),
                unreadCount,
                conversation.getCreatedAt()
        );
    }

    private ConversationParticipantResponse toResponse(ConversationParticipant participant, UserAccount me) {
        Long accountId = participant.getUserAccount().getId();
        UserAccountRoleResolver.AccountRole role = roleResolver.resolve(accountId);

        // Only these two profiles carry something worth showing as a label; a
        // moderator is identified by being the moderator.
        String label = null;
        String avatarUrl = null;

        if (role == UserAccountRoleResolver.AccountRole.SEEKER) {
            var seeker = jobSeekerProfileRepository.findByUserAccount_Id(accountId);
            label = seeker.map(profile -> profile.getHeadline()).orElse(null);
            avatarUrl = seeker.map(profile -> profile.getAvatarUrl()).orElse(null);
        } else if (role == UserAccountRoleResolver.AccountRole.RECRUITER) {
            var recruiter = recruiterProfileRepository.findByUserAccount_Id(accountId);
            label = recruiter.map(profile -> profile.getPosition()).orElse(null);
            avatarUrl = recruiter.map(profile -> profile.getAvatarUrl()).orElse(null);
        }

        return new ConversationParticipantResponse(
                accountId,
                role.name(),
                hasText(label) ? label : humanize(role.name()),
                avatarUrl,
                accountId.equals(me.getId()),
                participant.getLastReadAt()
        );
    }

    private MessageResponse toResponse(Message message, UserAccount me) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderUserAccount().getId(),
                message.getSenderUserAccount().getId().equals(me.getId()),
                message.getStatus() == MessageStatus.DELETED ? null : message.getContent(),
                message.getMessageType(),
                message.getStatus(),
                message.getSentAt(),
                message.getDeletedAt()
        );
    }

    private String humanize(String role) {
        String lower = role.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
