package co.istad.ai_interview_app.features.communication;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.communication.dto.ConversationResponse;
import co.istad.ai_interview_app.features.communication.dto.MessageResponse;
import co.istad.ai_interview_app.features.communication.dto.OpenSupportRequest;
import co.istad.ai_interview_app.features.communication.dto.SendMessageRequest;
import co.istad.ai_interview_app.features.communication.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reading and replying, for whoever is in the thread.
 *
 * <p>Not role-scoped, because a thread's two sides hold different roles and
 * both need the same operations. Membership is the permission, and the service
 * checks it on every call.
 *
 * <p>Starting a thread is not here — see {@code ModeratorConversationController}.
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationService conversationService;

    @GetMapping
    public ApiResponse<Page<ConversationResponse>> findMyConversations(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        validate(pageable);
        return ApiResponse.success(conversationService.findMyConversations(pageable));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> getConversation(
            @PathVariable Long conversationId
    ) {
        return ApiResponse.success(conversationService.getConversation(conversationId));
    }

    /** Newest first, so opening a thread costs one page rather than its whole history. */
    @GetMapping("/{conversationId}/messages")
    public ApiResponse<Page<MessageResponse>> findMessages(
            @PathVariable Long conversationId,
            @PageableDefault(size = 30) Pageable pageable
    ) {
        validate(pageable);
        return ApiResponse.success(conversationService.findMessages(conversationId, pageable));
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return ApiResponse.success(conversationService.sendMessage(conversationId, request));
    }

    /**
     * The one creation path open to a non-moderator.
     *
     * <p>Safe to leave here rather than behind a role rule precisely because
     * the caller cannot choose who receives it — the recipients are the
     * moderator team, resolved server-side.
     */
    @PostMapping("/support")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> openSupportConversation(
            @Valid @RequestBody OpenSupportRequest request
    ) {
        return ApiResponse.success(conversationService.openSupportConversation(request));
    }

    @PostMapping("/{conversationId}/read")
    public ApiResponse<ConversationResponse> markAsRead(
            @PathVariable Long conversationId
    ) {
        return ApiResponse.success(conversationService.markAsRead(conversationId));
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
            @PathVariable Long conversationId,
            @PathVariable Long messageId
    ) {
        conversationService.deleteMessage(conversationId, messageId);
    }

    private void validate(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be less than or equal to " + MAX_PAGE_SIZE
            );
        }
    }
}
