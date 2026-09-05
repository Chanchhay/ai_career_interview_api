package co.istad.ai_interview_app.features.interview.guest.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewAnswerRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewResultResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewSessionResponse;
import co.istad.ai_interview_app.features.interview.ai.repository.AiInterviewSessionRepository;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewService;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewAvailabilityResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewSettingsResponse;
import co.istad.ai_interview_app.features.interview.guest.dto.GuestInterviewStartResponse;
import co.istad.ai_interview_app.features.interview.vapi.dto.VapiCallBindingRequest;
import co.istad.ai_interview_app.features.interview.vapi.dto.VoiceTranscriptRequest;
import co.istad.ai_interview_app.shared.enums.interview.GuestQuestionSource;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Who may take a guest interview, how many, and out of which questions.
 *
 * <p>Holds only the policy. The interview itself — composing the questions,
 * running the state machine, scoring the answers — is the same code a
 * signed-in candidate goes through, so a guest is shown the real thing rather
 * than a demonstration of it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestInterviewServiceImpl implements GuestInterviewService {

    /**
     * Salt for the stored IP hashes. Fixed rather than random per-restart: the
     * daily count has to survive a deploy, and this is a counting key, not a
     * password. It exists so the table cannot be read as a list of visitor
     * addresses.
     */
    private static final String IP_HASH_SALT = "guest-interview-ip";

    private static final Duration IP_WINDOW = Duration.ofDays(1);

    private final GuestInterviewSettingsService settingsService;
    private final AiInterviewService aiInterviewService;
    private final AiInterviewSessionRepository sessionRepository;

    @Override
    @Transactional(readOnly = true)
    public GuestInterviewAvailabilityResponse availability(String guestToken) {
        GuestInterviewSettingsResponse settings = settingsService.getSettings();
        int used = attemptsUsed(guestToken);
        int allowed = settings.maxAttemptsPerGuest();

        String blocked = null;
        if (!settings.enabled()) {
            blocked = "Practice interviews are not open at the moment.";
        } else if (used >= allowed) {
            blocked = allowed == 0
                    ? "Practice interviews are not open at the moment."
                    : "You have used all " + allowed + " of your practice interviews. Create an account to keep going.";
        }

        return new GuestInterviewAvailabilityResponse(
                settings.enabled(),
                used,
                allowed,
                blocked == null,
                blocked
        );
    }

    @Override
    public GuestInterviewStartResponse start(Long jobId, String guestToken, String clientIp) {
        GuestInterviewSettingsResponse settings = settingsService.getSettings();

        if (!settings.enabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Practice interviews are not open at the moment.");
        }

        /*
         * A visitor with no token yet is issued one now rather than on the way
         * out: the token is what the new session is filed under, so it has to
         * exist before the session does.
         */
        String token = guestToken == null || guestToken.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : guestToken;

        int used = attemptsUsed(token);
        if (used >= settings.maxAttemptsPerGuest()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    settings.maxAttemptsPerGuest() == 0
                            ? "Practice interviews are not open at the moment."
                            : "You have used all " + settings.maxAttemptsPerGuest()
                                    + " of your practice interviews. Create an account to keep going."
            );
        }

        /*
         * The network cap is the backstop for the fact that clearing a browser
         * produces a brand-new guest. Without it the per-guest limit is a
         * courtesy and the AI bill is unbounded.
         */
        String ipHash = hashIp(clientIp);
        if (ipHash != null && settings.maxAttemptsPerIpPerDay() > 0) {
            long fromThisNetwork = sessionRepository.countByGuestIpHashAndCreatedAtAfter(
                    ipHash,
                    Instant.now().minus(IP_WINDOW)
            );

            if (fromThisNetwork >= settings.maxAttemptsPerIpPerDay()) {
                log.info("Guest interview refused: network hash {} has started {} today", ipHash, fromThisNetwork);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many practice interviews have been started from this network today. Try again tomorrow."
                );
            }
        }

        AiInterviewSessionResponse session = aiInterviewService.createGuestInterview(
                jobId,
                token,
                ipHash,
                modeOverride(settings.questionSource())
        );

        return new GuestInterviewStartResponse(
                token,
                used + 1,
                settings.maxAttemptsPerGuest(),
                session
        );
    }

    @Override
    public AiInterviewSessionResponse get(Long sessionId, String guestToken) {
        return aiInterviewService.getGuestInterview(sessionId, guestToken);
    }

    @Override
    public AiInterviewSessionResponse begin(Long sessionId, String guestToken) {
        return aiInterviewService.startGuestInterview(sessionId, guestToken);
    }

    @Override
    public AiInterviewSessionResponse answer(
            Long sessionId,
            Long questionId,
            String guestToken,
            AiInterviewAnswerRequest request
    ) {
        return aiInterviewService.submitGuestAnswer(sessionId, questionId, guestToken, request);
    }

    @Override
    public AiInterviewResultResponse complete(Long sessionId, String guestToken) {
        return aiInterviewService.completeGuestInterview(sessionId, guestToken);
    }

    @Override
    public AiInterviewResultResponse result(Long sessionId, String guestToken) {
        return aiInterviewService.getGuestResult(sessionId, guestToken);
    }

    @Override
    public AiInterviewSessionResponse bindVoiceCall(
            Long sessionId,
            String guestToken,
            VapiCallBindingRequest request
    ) {
        return aiInterviewService.bindGuestVapiCall(sessionId, guestToken, request);
    }

    /**
     * A voice interview is not a second attempt.
     *
     * <p>The limit is counted when the interview is created, so speaking it
     * rather than typing it costs the guest nothing extra — the same interview,
     * answered a different way.
     */
    @Override
    public AiInterviewSessionResponse submitVoiceTranscript(
            Long sessionId,
            String guestToken,
            VoiceTranscriptRequest request
    ) {
        return aiInterviewService.submitGuestVoiceTranscript(sessionId, guestToken, request);
    }

    private int attemptsUsed(String guestToken) {
        if (guestToken == null || guestToken.isBlank()) return 0;

        return (int) sessionRepository.countByGuestToken(guestToken);
    }

    /**
     * Null lets each job keep its own manual/AI setting; anything else overrides
     * every job for guests, which is what an administrator chose when they set
     * the source to something other than "follow the job".
     */
    private ManualQuestionMode modeOverride(GuestQuestionSource source) {
        return switch (source) {
            case FOLLOW_JOB -> null;
            case WRITTEN_ONLY -> ManualQuestionMode.MANUAL_ONLY;
            case ALWAYS_GENERATE -> ManualQuestionMode.MANUAL_PLUS_AI;
        };
    }

    /**
     * A salted SHA-256 of the caller's address, or null when there is no usable
     * address. Null disables the network cap for that request rather than
     * refusing it: a proxy that hides the address is not the visitor's fault.
     */
    private String hashIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((IP_HASH_SALT + clientIp.trim()).getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 is required of every JVM; if it is missing, counting by
            // network is the least of the problems.
            log.warn("Cannot hash guest IP, network cap disabled for this request", exception);
            return null;
        }
    }
}
