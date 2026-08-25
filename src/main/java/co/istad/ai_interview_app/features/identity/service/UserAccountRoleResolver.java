package co.istad.ai_interview_app.features.identity.service;

import co.istad.ai_interview_app.features.identity.repository.CurrentUserAdminProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserFinanceProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserJobSeekerProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserModeratorProfileRepository;
import co.istad.ai_interview_app.features.identity.repository.CurrentUserRecruiterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Which kind of account this is, judged by which profile row it owns.
 *
 * <p>Profiles rather than Keycloak realm roles: this runs inside request and
 * listener code where a per-account admin-API call would be far too expensive,
 * and every account that can do anything on this platform has a profile.
 *
 * <p>An account can in principle own more than one profile. The order below is
 * the answer's priority, and it is deliberate — a moderator who also once
 * registered as a seeker should be treated as a moderator.
 */
@Component
@RequiredArgsConstructor
public class UserAccountRoleResolver {

    public enum AccountRole {
        MODERATOR,
        ADMIN,
        FINANCE,
        RECRUITER,
        SEEKER,
        UNKNOWN
    }

    private final CurrentUserJobSeekerProfileRepository jobSeekerProfileRepository;
    private final CurrentUserRecruiterProfileRepository recruiterProfileRepository;
    private final CurrentUserModeratorProfileRepository moderatorProfileRepository;
    private final CurrentUserAdminProfileRepository adminProfileRepository;
    private final CurrentUserFinanceProfileRepository financeProfileRepository;

    public AccountRole resolve(Long userAccountId) {
        if (userAccountId == null) return AccountRole.UNKNOWN;

        if (moderatorProfileRepository.findByUserAccount_Id(userAccountId).isPresent()) {
            return AccountRole.MODERATOR;
        }
        if (adminProfileRepository.findByUserAccount_Id(userAccountId).isPresent()) {
            return AccountRole.ADMIN;
        }
        if (financeProfileRepository.findByUserAccount_Id(userAccountId).isPresent()) {
            return AccountRole.FINANCE;
        }
        if (recruiterProfileRepository.findByUserAccount_Id(userAccountId).isPresent()) {
            return AccountRole.RECRUITER;
        }
        if (jobSeekerProfileRepository.findByUserAccount_Id(userAccountId).isPresent()) {
            return AccountRole.SEEKER;
        }

        return AccountRole.UNKNOWN;
    }

    /**
     * Where this account reads its inbox.
     *
     * <p>The three front ends mount messaging under different prefixes, and a
     * notification carries one URL, so the path has to be chosen for the
     * recipient rather than left for the client to rewrite.
     */
    public String messagesPath(Long userAccountId, Long conversationId) {
        return switch (resolve(userAccountId)) {
            case SEEKER -> "/job-seeker/messages/" + conversationId;
            case RECRUITER -> "/recruiter/messages/" + conversationId;
            default -> "/messages/" + conversationId;
        };
    }
}
