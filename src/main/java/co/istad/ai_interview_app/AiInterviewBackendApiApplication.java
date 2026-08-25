package co.istad.ai_interview_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Drives the SSE heartbeat that keeps notification streams from being closed
// by an idle-connection timeout.
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AiInterviewBackendApiApplication {

    static void main(String[] args) {
        SpringApplication.run(AiInterviewBackendApiApplication.class, args);
    }
}
