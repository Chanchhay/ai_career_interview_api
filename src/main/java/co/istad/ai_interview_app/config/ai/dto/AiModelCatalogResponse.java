package co.istad.ai_interview_app.config.ai.dto;

import java.util.List;

/**
 * The models the console lets an administrator choose between.
 *
 * <p>{@code live} says where the list came from. Asking the provider is the only
 * way to know what a given key may actually call, but a key that is missing or
 * rejected must still leave a usable dropdown, so a known-good list stands in
 * and says so rather than leaving the field empty.
 */
public record AiModelCatalogResponse(
        boolean live,
        String message,
        List<AiModelOption> models
) {
}
