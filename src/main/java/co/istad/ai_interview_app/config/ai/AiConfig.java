package co.istad.ai_interview_app.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient geminiChatClient(
            ChatClient.Builder builder
    ) {
        return builder.build();
    }
}
