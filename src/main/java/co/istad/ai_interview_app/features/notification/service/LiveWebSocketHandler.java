package co.istad.ai_interview_app.features.notification.service;

import co.istad.ai_interview_app.features.identity.repository.IdentityUserAccountRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Account-scoped live events. Database writes remain on the authorized REST API.
 * Single-instance transport; use a shared event broker before scaling horizontally. */
@Component
public class LiveWebSocketHandler extends TextWebSocketHandler {
    private record Connection(UUID accountId, Instant expiresAt, WebSocketSession session) {}
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final IdentityUserAccountRepository accounts;
    private final ObjectMapper mapper;

    public LiveWebSocketHandler(IdentityUserAccountRepository accounts, ObjectMapper mapper) {
        this.accounts = accounts;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!(session.getPrincipal() instanceof JwtAuthenticationToken auth)
                || !auth.isAuthenticated() || auth.getToken().getExpiresAt() == null
                || !auth.getToken().getExpiresAt().isAfter(Instant.now())) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        var account = accounts.findByKeycloakUserId(auth.getToken().getSubject());
        if (account.isEmpty()) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        var connection = new Connection(account.get().getId(), auth.getToken().getExpiresAt(),
                new ConcurrentWebSocketSessionDecorator(session, 5000, 65536));
        connections.put(session.getId(), connection);
        send(connection, "connected", Map.of());
    }

    public void push(UUID accountId, String type, Object data) {
        connections.values().stream().filter(c -> c.accountId().equals(accountId))
                .forEach(c -> send(c, type, data));
    }

    @Scheduled(fixedDelay = 25000)
    public void heartbeat() {
        connections.values().forEach(c -> send(c, "heartbeat", Map.of()));
    }

    private void send(Connection connection, String type, Object data) {
        try {
            if (!connection.expiresAt().isAfter(Instant.now())) {
                drop(connection.session(), CloseStatus.POLICY_VIOLATION);
                return;
            }
            connection.session().sendMessage(new TextMessage(
                    mapper.writeValueAsString(Map.of("type", type, "data", data))));
        } catch (Exception exception) {
            drop(connection.session(), CloseStatus.SERVER_ERROR);
        }
    }

    private void drop(WebSocketSession session, CloseStatus status) {
        connections.remove(session.getId());
        try { session.close(status); } catch (Exception ignored) { }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connections.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        drop(session, CloseStatus.SERVER_ERROR);
    }
}
