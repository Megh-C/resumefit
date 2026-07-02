package com.resumematcher.resumefit.service;

import com.resumematcher.resumefit.dto.response.ParsedResumeDto;
import com.resumematcher.resumefit.dto.response.JobMatchDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FitScorerService {

    // Education hierarchy — higher index = higher qualification
    private static final Map<String, Integer> EDUCATION_RANK = Map.of(
            "BCA", 1,
            "B.Tech/B.E.", 2,
            "MCA", 3,
            "M.Tech/M.E.", 4,
            "PhD", 5
    );

    // Experience hierarchy — higher index = more experience required
    private static final Map<String, Integer> EXPERIENCE_RANK = Map.of(
            "Fresher (0-1 yr)", 1,
            "Junior (1-3 yrs)", 2,
            "Mid (3-6 yrs)", 3,
            "Senior (6-10 yrs)", 4,
            "Lead (10+ yrs)", 5
    );

    public List<JobMatchDto> filterAndScore(List<JobMatchDto> candidates, ParsedResumeDto resume) {
        String resumeEducation = resume.getEducation();
        String resumeSeniority = resume.getSeniority();

        log.info("Filtering {} candidates for resume: education={}, seniority={}",
                candidates.size(), resumeEducation, resumeSeniority);

        List<JobMatchDto> qualified = candidates.stream()
                .filter(job -> isEducationQualified(resumeEducation, job.getEducationRequired()))
                .filter(job -> isExperienceQualified(resumeSeniority, job.getExperienceLevel()))
                .map(job -> {
                    double fitScore = computeFitScore(job, resume);
                    job.setFitScore(fitScore);
                    return job;
                })
                .sorted((a, b) -> Double.compare(
                        b.getFitScore() != null ? b.getFitScore() : 0.0,
                        a.getFitScore() != null ? a.getFitScore() : 0.0))
                .limit(10)
                .collect(Collectors.toList());

        log.info("After qualification filter: {} jobs remain from {} candidates",
                qualified.size(), candidates.size());

        return qualified;
    }

    private boolean isEducationQualified(String resumeEducation, String jobEducation) {
        if (jobEducation == null || jobEducation.isBlank()) return true;
        if (resumeEducation == null || resumeEducation.isBlank()) return true;

        int resumeRank = EDUCATION_RANK.getOrDefault(resumeEducation.trim(), 2);
        int jobRank = EDUCATION_RANK.getOrDefault(jobEducation.trim(), 2);

        boolean qualified = resumeRank >= jobRank;
        if (!qualified) {
            log.debug("Education disqualified: resume={} (rank {}) < job={} (rank {})",
                    resumeEducation, resumeRank, jobEducation, jobRank);
        }
        return qualified;
    }

    private boolean isExperienceQualified(String resumeSeniority, String jobExperienceLevel) {
        if (jobExperienceLevel == null || jobExperienceLevel.isBlank()) return true;
        if (resumeSeniority == null || resumeSeniority.isBlank()) return true;

        // Map Gemini's seniority label to the dataset's bracket format
        String mappedSeniority = mapSeniorityToDatasetFormat(resumeSeniority);

        int resumeRank = EXPERIENCE_RANK.getOrDefault(mappedSeniority, 1);
        int jobRank = EXPERIENCE_RANK.getOrDefault(jobExperienceLevel.trim(), 1);

        boolean qualified = resumeRank >= jobRank;
        if (!qualified) {
            log.debug("Experience disqualified: resume={} (rank {}) < job={} (rank {})",
                    mappedSeniority, resumeRank, jobExperienceLevel, jobRank);
        }
        return qualified;
    }

    private String mapSeniorityToDatasetFormat(String seniority) {
        if (seniority == null) return "Fresher (0-1 yr)";
        return switch (seniority.trim().toLowerCase()) {
            case "fresher" -> "Fresher (0-1 yr)";
            case "junior"  -> "Junior (1-3 yrs)";
            case "mid"     -> "Mid (3-6 yrs)";
            case "senior"  -> "Senior (6-10 yrs)";
            case "lead"    -> "Lead (10+ yrs)";
            default        -> "Fresher (0-1 yr)";
        };
    }

    private double computeFitScore(JobMatchDto job, ParsedResumeDto resume) {
        double baseScore = job.getMatchScore() != null ? job.getMatchScore() : 0.0;

        // Skill overlap bonus
        List<String> resumeSkills = resume.getSkills();
        List<String> jobSkills = job.getSkillsRequired();

        double skillOverlap = 0.0;
        if (jobSkills != null && !jobSkills.isEmpty() && resumeSkills != null) {
            long matchingSkills = jobSkills.stream()
                    .filter(skill -> resumeSkills.stream()
                            .anyMatch(rs -> rs.equalsIgnoreCase(skill)))
                    .count();
            skillOverlap = (double) matchingSkills / jobSkills.size();
        }

        // fitScore = 70% cosine similarity + 30% skill overlap
        double fitScore = (baseScore * 0.70) + (skillOverlap * 0.30);

        log.debug("Job {}: matchScore={}, skillOverlap={}, fitScore={}",
                job.getJobId(), baseScore, skillOverlap, fitScore);

        return fitScore;
    }
}