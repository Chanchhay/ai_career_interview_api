package co.istad.ai_interview_app.features.seeker.service;

import co.istad.ai_interview_app.features.seeker.dto.FavoriteJobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobSeekerFavoriteJobService {

    Page<FavoriteJobResponse> findFavoriteJobs(Pageable pageable);

    FavoriteJobResponse saveFavoriteJob(Long jobId);

    void removeFavoriteJob(Long jobId);
}
