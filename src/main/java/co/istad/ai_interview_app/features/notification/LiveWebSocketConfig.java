package co.istad.ai_interview_app.features.notification;

import co.istad.ai_interview_app.features.notification.service.LiveWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class LiveWebSocketConfig implements WebSocketConfigurer {
    private final LiveWebSocketHandler handler;
    private final String[] origins;

    public LiveWebSocketConfig(LiveWebSocketHandler handler,
            @Value("${app.websocket.allowed-origins:http://localhost:8090}") String[] origins) {
        this.handler = handler;
        this.origins = origins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/notifications/ws").setAllowedOrigins(origins);
    }
}
