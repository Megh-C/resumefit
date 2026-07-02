package com.resumematcher.resumefit.service;

import com.resumematcher.resumefit.dto.response.GapAnalysisDto;
import com.resumematcher.resumefit.dto.response.ParsedResumeDto;
import com.resumematcher.resumefit.dto.response.SalaryAnalysisDto;
import com.resumematcher.resumefit.dto.response.SalaryRangeDto;
import com.resumematcher.resumefit.dto.response.SkillGapDto;
import com.resumematcher.resumefit.entity.JobPosting;
import com.resumematcher.resumefit.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryInferenceService {

    private final JobPostingRepository jobPostingRepository;

    @Value("${app.salary.skill-overlap-threshold}")
    private double overlapThreshold;

    @Value("${app.salary.top-missing-skills-count}")
    private int topMissingSkillsCount;

    public SalaryAnalysisDto analyze(ParsedResumeDto resume, GapAnalysisDto gapAnalysis) {
        String experienceLevel = mapSeniorityToDatasetFormat(resume.getSeniority());
        List<String> resumeSkills = resume.getSkills();

        log.info("Starting salary inference for experience={}, skills={}, threshold={}",
                experienceLevel, resumeSkills.size(), overlapThreshold);

        List<String> topMissingSkills = gapAnalysis.getMissingSkills().stream()
                .limit(topMissingSkillsCount)
                .map(SkillGapDto::getSkill)
                .collect(Collectors.toList());

        log.info("Top {} missing skills for uplift: {}", topMissingSkillsCount, topMissingSkills);

        // CURRENT salary range
        List<JobPosting> currentCandidates = fetchCandidateJobs(
                experienceLevel, resumeSkills, gapAnalysis.getResumeSkillsMatched());
        log.info("Fetched {} candidate jobs for current salary analysis", currentCandidates.size());

        List<JobPosting> currentFiltered = filterByOverlap(currentCandidates, resumeSkills);
        log.info("After {}% overlap filter: {} jobs for current range",
                (int)(overlapThreshold * 100), currentFiltered.size());

        SalaryRangeDto currentRange = computeRange(currentFiltered);

        // POST-SKILL salary range
        List<String> expandedSkills = new ArrayList<>(resumeSkills);
        expandedSkills.addAll(topMissingSkills);

        List<JobPosting> postCandidates = fetchCandidateJobs(
                experienceLevel, expandedSkills, topMissingSkills);
        log.info("Fetched {} candidate jobs for post-skill salary analysis", postCandidates.size());

        List<JobPosting> postFiltered = filterByOverlap(postCandidates, expandedSkills);
        log.info("After {}% overlap filter: {} jobs for post-skill range",
                (int)(overlapThreshold * 100), postFiltered.size());

        SalaryRangeDto postSkillRange = computeRange(postFiltered);

        // uplift — capped at 0, never negative
        BigDecimal uplift = BigDecimal.ZERO;
        String upliftNote;

        if (currentRange.getAvg() != null && postSkillRange.getAvg() != null) {
            BigDecimal delta = postSkillRange.getAvg()
                    .subtract(currentRange.getAvg())
                    .setScale(2, RoundingMode.HALF_UP);

            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                uplift = delta;
                upliftNote = "Adding " + String.join(", ", topMissingSkills)
                        + " could increase your average expected salary by "
                        + uplift + " LPA.";
            } else {
                upliftNote = "These skills broaden your job opportunities significantly. "
                        + "At " + experienceLevel + " level, the salary impact is marginal "
                        + "but eligibility for these roles increases substantially.";
            }
        } else {
            upliftNote = "Salary data unavailable for one or both scenarios.";
        }

        log.info("Salary analysis complete. Current avg: {} LPA, Post-skill avg: {} LPA, Uplift: {} LPA",
                currentRange.getAvg(), postSkillRange.getAvg(), uplift);

        return SalaryAnalysisDto.builder()
                .currentRange(currentRange)
                .postSkillRange(postSkillRange)
                .skillsUsedForUplift(topMissingSkills)
                .upliftLpa(uplift)
                .upliftNote(upliftNote)
                .build();
    }

    private List<JobPosting> fetchCandidateJobs(String experienceLevel,
                                                List<String> resumeSkills,
                                                List<String> filterSkills) {
        List<String> padded = new ArrayList<>(filterSkills);
        while (padded.size() < 5) padded.add("NOMATCH_PLACEHOLDER_XYZ");

        log.info("SQL filter skills (first 5): {}", padded.subList(0, 5));

        return jobPostingRepository.findByExperienceAndAnySkill(
                experienceLevel,
                padded.get(0),
                padded.get(1),
                padded.get(2),
                padded.get(3),
                padded.get(4)
        );
    }

    private List<JobPosting> filterByOverlap(List<JobPosting> jobs, List<String> resumeSkills) {
        Set<String> resumeSkillSet = resumeSkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return jobs.stream()
                .filter(job -> {
                    List<String> jobSkills = job.getSkillsAsList();
                    if (jobSkills.isEmpty()) return false;

                    long matching = jobSkills.stream()
                            .filter(s -> resumeSkillSet.contains(s.toLowerCase()))
                            .count();

                    double overlap = (double) matching / jobSkills.size();
                    return overlap >= overlapThreshold;
                })
                .collect(Collectors.toList());
    }

    private SalaryRangeDto computeRange(List<JobPosting> jobs) {
        if (jobs.isEmpty()) {
            log.warn("No jobs survived overlap filter — returning null salary range");
            return SalaryRangeDto.builder()
                    .min(null).avg(null).max(null)
                    .jobsAnalyzed(0)
                    .build();
        }

        List<BigDecimal> salaries = jobs.stream()
                .map(JobPosting::getSalaryLpa)
                .filter(s -> s != null)
                .collect(Collectors.toList());

        if (salaries.isEmpty()) {
            return SalaryRangeDto.builder()
                    .min(null).avg(null).max(null)
                    .jobsAnalyzed(jobs.size())
                    .build();
        }

        BigDecimal min = salaries.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = salaries.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal sum = salaries.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(salaries.size()), 2, RoundingMode.HALF_UP);

        log.info("Salary range computed from {} jobs: min={}, avg={}, max={}",
                salaries.size(), min, avg, max);

        return SalaryRangeDto.builder()
                .min(min).avg(avg).max(max)
                .jobsAnalyzed(salaries.size())
                .build();
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
}