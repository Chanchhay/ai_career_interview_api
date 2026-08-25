package co.istad.ai_interview_app.features.communication;

import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.communication.dto.ConversationResponse;
import co.istad.ai_interview_app.features.communication.dto.CreateConversationRequest;
import co.istad.ai_interview_app.features.communication.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Opening and closing threads — moderators only, via the existing
 * {@code /api/v1/moderator/**} rule in SecurityConfig.
 *
 * <p>Keeping creation here rather than on the shared controller is what makes
 * the messaging boundary hold: a recruiter cannot conjure a thread with a
 * candidate, because the only endpoint that creates one is closed to them.
 */
@RestController
@RequestMapping("/api/v1/moderator/conversations")
@RequiredArgsConstructor
public class ModeratorConversationController {

    private final ConversationService conversationService;

    /**
     * Idempotent in practice: naming an application or a person who already has
     * an open thread returns that thread, appending the message rather than
     * splitting the history in two.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return ApiResponse.success(conversationService.createConversation(request));
    }

    @PostMapping("/{conversationId}/close")
    public ApiResponse<ConversationResponse> closeConversation(
            @PathVariable Long conversationId
    ) {
        return ApiResponse.success(conversationService.closeConversation(conversationId));
    }
}
