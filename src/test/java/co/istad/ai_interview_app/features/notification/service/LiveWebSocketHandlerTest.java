package co.istad.ai_interview_app.features.notification.service;

import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.*;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LiveWebSocketHandlerTest {
    private IdentityUserAccountRepository accounts;
    private LiveWebSocketHandler handler;

    @BeforeEach
    void setup() {
        accounts = mock(IdentityUserAccountRepository.class);
        handler = new LiveWebSocketHandler(accounts, new ObjectMapper());
    }

    @Test
    void eventsReachEveryTabOfOnlyTheAddressedAccount() throws Exception {
        UUID owner = UUID.randomUUID();
        var first = session(owner, "first", Instant.now().plusSeconds(300));
        var second = session(owner, "second", Instant.now().plusSeconds(300));
        var stranger = session(UUID.randomUUID(), "stranger", Instant.now().plusSeconds(300));
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);
        handler.afterConnectionEstablished(stranger);
        verify(first).sendMessage(argThat(m -> m.getPayload().toString().contains("connected")));
        clearInvocations(first, second, stranger);

        handler.push(owner, "message", Map.of("conversationId", "thread"));
        verify(first).sendMessage(argThat(m -> m.getPayload().toString().contains("thread")));
        verify(second).sendMessage(any(TextMessage.class));
        verify(stranger, never()).sendMessage(any());
    }

    @Test
    void anonymousAndExpiredConnectionsAreRejected() throws Exception {
        var anonymous = mock(WebSocketSession.class);
        handler.afterConnectionEstablished(anonymous);
        verify(anonymous).close(CloseStatus.POLICY_VIOLATION);
        var expired = session(UUID.randomUUID(), "expired", Instant.now().minusSeconds(1));
        handler.afterConnectionEstablished(expired);
        verify(expired).close(CloseStatus.POLICY_VIOLATION);
        verify(expired, never()).sendMessage(any());
    }

    @Test
    void authenticatedIdentityWithoutAPlatformAccountIsRejected() throws Exception {
        UUID id = UUID.randomUUID();
        var session = session(id, "unknown", Instant.now().plusSeconds(300));
        when(accounts.findByKeycloakUserId(id.toString())).thenReturn(Optional.empty());
        handler.afterConnectionEstablished(session);
        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(session, never()).sendMessage(any());
    }

    @Test
    void disconnectedTabIsRemovedWithoutAffectingOtherTabs() throws Exception {
        UUID id = UUID.randomUUID();
        var first = session(id, "closed", Instant.now().plusSeconds(300));
        var second = session(id, "open", Instant.now().plusSeconds(300));
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);
        handler.afterConnectionClosed(first, CloseStatus.NORMAL);
        clearInvocations(first, second);
        handler.push(id, "message", Map.of());
        verify(first, never()).sendMessage(any());
        verify(second).sendMessage(any(TextMessage.class));
    }

    @Test
    void failedSocketIsClosedAndDoesNotPreventDeliveryToOtherTabs() throws Exception {
        UUID id = UUID.randomUUID();
        var broken = session(id, "broken", Instant.now().plusSeconds(300));
        var healthy = session(id, "healthy", Instant.now().plusSeconds(300));
        handler.afterConnectionEstablished(broken);
        handler.afterConnectionEstablished(healthy);
        clearInvocations(broken, healthy);
        doThrow(new IOException("Disconnected")).when(broken).sendMessage(any());
        handler.push(id, "message", Map.of());
        verify(broken).close(CloseStatus.SERVER_ERROR);
        verify(healthy).sendMessage(any(TextMessage.class));
        clearInvocations(broken);
        handler.heartbeat();
        verify(broken, never()).sendMessage(any());
    }

    private WebSocketSession session(UUID accountId, String id, Instant expiresAt) {
        UserAccount account = new UserAccount();
        account.setId(accountId);
        when(accounts.findByKeycloakUserId(accountId.toString())).thenReturn(Optional.of(account));
        var jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject(accountId.toString())
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiresAt).build();
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        when(session.getPrincipal()).thenReturn(new JwtAuthenticationToken(jwt, List.of()));
        return session;
    }
}
