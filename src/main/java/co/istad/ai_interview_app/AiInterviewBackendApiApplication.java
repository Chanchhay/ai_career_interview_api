package co.istad.ai_interview_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AiInterviewBackendApiApplication {

    static void main(String[] args) {
        SpringApplication.run(AiInterviewBackendApiApplication.class, args);
    }
}
