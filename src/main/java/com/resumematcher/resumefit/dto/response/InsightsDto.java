package com.resumematcher.resumefit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightsDto {

    // Tab 1 — Overview (shown by default)
    private String overallAssessment;
    private String roleFitAssessment;
    private List<String> recommendedLearningOrder;

    // Tab 2 — Skill Gaps
    private List<SkillInsightDto> skillInsights;

    // Tab 3 — Salary
    private String salaryContext;

    // Tab 4 — Resume Tips
    private List<String> resumeSuggestions;
}