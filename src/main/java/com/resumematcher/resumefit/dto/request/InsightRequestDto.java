package com.resumematcher.resumefit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightRequestDto {
    private String seniority;
    private String education;
    private List<String> currentRoles;
    private List<String> currentSkills;
    private List<String> resumeSkillsMatched;
    private List<MissingSkillContextDto> missingSkills;
    private String currentSalaryAvg;
    private String postSkillSalaryAvg;
    private String upliftLpa;
    private List<String> topJobTitles;
    private int totalJobsAnalyzed;
}