package co.istad.ai_interview_app.features.common.audit;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@EnableJpaAuditing
public class JpaAuditorAware implements AuditorAware<String> {

    @NullMarked
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of("SYSTEM");
    }
}
