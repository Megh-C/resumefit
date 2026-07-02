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
public class GapAnalysisDto {
    private List<SkillGapDto> missingSkills;      // sorted by frequency, raw counts
    private List<String> resumeSkillsMatched;      // skills you already have that jobs want
    private int totalJobsAnalyzed;                 // so the user knows what X/Y means
}