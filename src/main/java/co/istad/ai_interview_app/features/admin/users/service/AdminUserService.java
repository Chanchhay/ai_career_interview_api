package co.istad.ai_interview_app.features.admin.users.service;

import co.istad.ai_interview_app.features.admin.users.dto.AdminUserCreateRequest;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserResponse;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserRolesRequest;
import co.istad.ai_interview_app.features.admin.users.dto.AdminUserStatusRequest;
import co.istad.ai_interview_app.shared.enums.account.AccountStatus;
import co.istad.ai_interview_app.shared.enums.account.ManageableRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResponse> findUsers(
            String search,
            ManageableRole role,
            AccountStatus status,
            Pageable pageable
    );

    AdminUserResponse getUser(String keycloakUserId);

    AdminUserResponse createUser(AdminUserCreateRequest request);

    AdminUserResponse suspendUser(String keycloakUserId, AdminUserStatusRequest request);

    AdminUserResponse reactivateUser(String keycloakUserId, AdminUserStatusRequest request);

    AdminUserResponse updateRoles(String keycloakUserId, AdminUserRolesRequest request);
}
