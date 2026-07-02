package com.resumematcher.resumefit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryAnalysisDto {

    private SalaryRangeDto currentRange;
    private SalaryRangeDto postSkillRange;
    private List<String> skillsUsedForUplift;
    private BigDecimal upliftLpa; //post skill range average - current range avg
    private String upliftNote;

}
