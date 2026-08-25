package co.istad.ai_interview_app.features.admin.users;

import co.istad.ai_interview_app.features.admin.users.dto.AdminUserCreateRequest;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserResponse;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserRolesRequest;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserStatusRequest;
import co.istad.ai_interview_app.features.admin.users.service.AdminUserService;
import co.istad.ai_interview_app.features.common.response.ApiResponse;
import co.istad.ai_interview_app.shared.enums.account.AccountStatus;
import co.istad.ai_interview_app.shared.enums.account.ManageableRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * SUPER_ADMIN only. The rule sits in SecurityConfig ahead of the broader
 * {@code /api/v1/admin/**} line, which admits moderators for taxonomy work —
 * these operations must not.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<Page<AdminUserResponse>> findUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ManageableRole role,
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be less than or equal to " + MAX_PAGE_SIZE
            );
        }

        return ApiResponse.success(adminUserService.findUsers(search, role, status, pageable));
    }

    @GetMapping("/{keycloakUserId}")
    public ApiResponse<AdminUserResponse> getUser(
            @PathVariable String keycloakUserId
    ) {
        return ApiResponse.success(adminUserService.getUser(keycloakUserId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminUserResponse> createUser(
            @Valid @RequestBody AdminUserCreateRequest request
    ) {
        return ApiResponse.success(adminUserService.createUser(request));
    }

    @PostMapping("/{keycloakUserId}/suspend")
    public ApiResponse<AdminUserResponse> suspendUser(
            @PathVariable String keycloakUserId,
            @Valid @RequestBody(required = false) AdminUserStatusRequest request
    ) {
        return ApiResponse.success(adminUserService.suspendUser(keycloakUserId, request));
    }

    @PostMapping("/{keycloakUserId}/reactivate")
    public ApiResponse<AdminUserResponse> reactivateUser(
            @PathVariable String keycloakUserId,
            @Valid @RequestBody(required = false) AdminUserStatusRequest request
    ) {
        return ApiResponse.success(adminUserService.reactivateUser(keycloakUserId, request));
    }

    /**
     * Replaces the role set outright rather than adding or removing one role,
     * so the result does not depend on what another administrator did between
     * the page loading and the form being submitted.
     */
    @PutMapping("/{keycloakUserId}/roles")
    public ApiResponse<AdminUserResponse> updateRoles(
            @PathVariable String keycloakUserId,
            @Valid @RequestBody AdminUserRolesRequest request
    ) {
        return ApiResponse.success(adminUserService.updateRoles(keycloakUserId, request));
    }
}
