package co.istad.ai_interview_app.features.job.repository;

import co.istad.ai_interview_app.features.job.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findAllByOrderByNameAsc();

    /**
     * Looks skills up by name without caring about case, for matching the names
     * an AI lifted out of an uploaded job description.
     *
     * @param lowercaseNames names already lowercased by the caller
     */
    @Query("SELECT s FROM Skill s WHERE LOWER(s.name) IN :lowercaseNames")
    List<Skill> findAllByLowercaseNameIn(
            @Param("lowercaseNames") Collection<String> lowercaseNames
    );

    Optional<Skill> findFirstByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
