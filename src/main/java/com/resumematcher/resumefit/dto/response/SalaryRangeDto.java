package com.resumematcher.resumefit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryRangeDto {

    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal avg;
    private int jobsAnalyzed;

}
