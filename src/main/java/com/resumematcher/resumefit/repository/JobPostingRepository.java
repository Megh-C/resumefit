package com.resumematcher.resumefit.repository;

import com.resumematcher.resumefit.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, String> {

    // Idempotency check — has ingestion already run?
    long countBy();

    // Fetch full job details after vector search returns job IDs
    List<JobPosting> findByJobIdIn(List<String> jobIds);

    // Salary inference — fetch jobs matching experience level with at least one skill match
    // % wildcards are passed from Java so Spring Data JPA handles them correctly
    @Query(value = """
    SELECT * FROM job_postings
    WHERE experience_level = :experienceLevel
    AND (
        POSITION(LOWER(:skill1) IN LOWER(skills_required)) > 0
        OR POSITION(LOWER(:skill2) IN LOWER(skills_required)) > 0
        OR POSITION(LOWER(:skill3) IN LOWER(skills_required)) > 0
        OR POSITION(LOWER(:skill4) IN LOWER(skills_required)) > 0
        OR POSITION(LOWER(:skill5) IN LOWER(skills_required)) > 0
    )
    """, nativeQuery = true)
    List<JobPosting> findByExperienceAndAnySkill(
            @Param("experienceLevel") String experienceLevel,
            @Param("skill1") String skill1,
            @Param("skill2") String skill2,
            @Param("skill3") String skill3,
            @Param("skill4") String skill4,
            @Param("skill5") String skill5
    );
}