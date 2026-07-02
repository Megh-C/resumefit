package com.resumematcher.resumefit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillInsightDto {
    private String skill;
    private String explanation;
    private Boolean isQuickWin;      // true = you likely have adjacent knowledge, add to resume
    private String quickWinReason;   // only populated if isQuickWin = true
}