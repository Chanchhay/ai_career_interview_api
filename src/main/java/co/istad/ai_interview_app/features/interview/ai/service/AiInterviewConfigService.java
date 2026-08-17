package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewConfigRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewConfigResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;

/**
 * Reads and writes the admin-owned settings that decide what an AI interview
 * looks like: how many questions it has, of which types, and how they score.
 */
public interface AiInterviewConfigService {

    AiInterviewConfigResponse getConfig();

    AiInterviewConfigResponse updateConfig(AiInterviewConfigRequest request);

    /** The same settings in the shape the generator and scorer consume. */
    AiInterviewGenerationConfig currentGenerationConfig();
}
