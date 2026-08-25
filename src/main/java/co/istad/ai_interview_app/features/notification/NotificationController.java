package co.istad.ai_interview_app.features.notification;

import co.istad.ai_interview_app.config.security.AuthUtils;
import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import co.istad.ai_interview_app.features.notification.dto.NotificationResponse;
import co.istad.ai_interview_app.features.notification.dto.UnreadCountResponse;
import co.istad.ai_interview_app.features.notification.service.NotificationService;
import co.istad.ai_interview_app.features.notification.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * A signed-in account's own notifications. Not role-scoped: recruiters,
 * seekers, moderators and administrators all read their inbox here, and every
 * query is filtered to the caller's own account inside the service.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;
    private final IdentityUserAccountRepository userAccountRepository;

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> findMine(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be less than or equal to " + MAX_PAGE_SIZE
            );
        }

        return ApiResponse.success(notificationService.findMine(unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.success(notificationService.unreadCount());
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable Long notificationId
    ) {
        return ApiResponse.success(notificationService.markAsRead(notificationId));
    }

    @PostMapping("/read-all")
    public ApiResponse<UnreadCountResponse> markAllAsRead() {
        return ApiResponse.success(notificationService.markAllAsRead());
    }

    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long notificationId
    ) {
        notificationService.delete(notificationId);
    }

    /**
     * Live notification stream.
     *
     * <p>Returns the raw {@link SseEmitter} rather than the usual
     * {@code ApiResponse} envelope: this is an event stream, and each event
     * already carries a complete {@code NotificationResponse}.
     *
     * <p>The browser's {@code EventSource} cannot set an Authorization header.
     * That works here only because the gateway holds the session and attaches
     * the token to forwarded requests — a client calling this backend directly
     * would need a different mechanism.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Long userAccountId = userAccountRepository
                .findByKeycloakUserId(AuthUtils.extractUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account was not found for authenticated user"
                ))
                .getId();

        return notificationStreamService.subscribe(userAccountId);
    }
}
