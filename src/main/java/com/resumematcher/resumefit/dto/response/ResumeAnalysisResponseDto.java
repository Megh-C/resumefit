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
public class ResumeAnalysisResponseDto {
    private ParsedResumeDto resumeSummary;
    private List<JobMatchDto> topMatches;
    private int qualifiedMatchCount;
    private String qualifiedMatchNote;
    private GapAnalysisDto gapAnalysis;
    private SalaryAnalysisDto salaryAnalysis;
    private InsightsDto insights;
}