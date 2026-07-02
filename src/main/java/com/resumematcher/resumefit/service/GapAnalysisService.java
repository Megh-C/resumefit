package com.resumematcher.resumefit.service;

import com.resumematcher.resumefit.dto.response.GapAnalysisDto;
import com.resumematcher.resumefit.dto.response.JobMatchDto;
import com.resumematcher.resumefit.dto.response.SkillGapDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GapAnalysisService {

    public GapAnalysisDto analyze(List<JobMatchDto> topMatches, List<String> resumeSkills) {
        int totalJobs = topMatches.size();
        log.info("Running gap analysis across {} matched jobs for resume with {} skills",
                totalJobs, resumeSkills.size());

        Set<String> resumeSkillSet = resumeSkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Map<String, Integer> missingSkillFrequency = new LinkedHashMap<>();
        Set<String> matchedSkills = new LinkedHashSet<>();

        for (JobMatchDto job : topMatches) {
            List<String> jobSkills = job.getSkillsRequired();
            if (jobSkills == null || jobSkills.isEmpty()) continue;

            for (String jobSkill : jobSkills) {
                if (resumeSkillSet.contains(jobSkill.toLowerCase())) {
                    matchedSkills.add(jobSkill);
                } else {
                    missingSkillFrequency.merge(jobSkill, 1, Integer::sum);
                }
            }
        }

        log.info("Found {} unique missing skills. Resume matched {} skills across jobs.",
                missingSkillFrequency.size(), matchedSkills.size());

        List<SkillGapDto> missingSkills = missingSkillFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> SkillGapDto.builder()
                        .skill(e.getKey())
                        .appearsInJobs(e.getValue())
                        .outOf(totalJobs)
                        .build())
                .toList();

        log.info("Top missing skills: {}",
                missingSkills.stream()
                        .limit(5)
                        .map(s -> s.getSkill() + "(" + s.getAppearsInJobs() + "/" + totalJobs + ")")
                        .toList());

        return GapAnalysisDto.builder()
                .missingSkills(missingSkills)
                .resumeSkillsMatched(new ArrayList<>(matchedSkills))
                .totalJobsAnalyzed(totalJobs)
                .build();
    }
}