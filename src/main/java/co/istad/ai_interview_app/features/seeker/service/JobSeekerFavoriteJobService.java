package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.FavoriteJobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface JobSeekerFavoriteJobService {

    Page<FavoriteJobResponse> findFavoriteJobs(Pageable pageable);

    FavoriteJobResponse saveFavoriteJob(UUID jobId);

    void removeFavoriteJob(UUID jobId);
}
