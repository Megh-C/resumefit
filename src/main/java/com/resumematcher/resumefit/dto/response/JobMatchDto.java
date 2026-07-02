package com.resumematcher.resumefit.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatchDto {

    private String jobId;
    private String jobTitle;
    private String company;
    private String companyType;
    private String industry;
    private String city;
    private String workMode;
    private String experienceLevel;
    private String educationRequired;
    private BigDecimal salaryLpa;
    private List<String> skillsRequired;
    private LocalDate datePosted;
    private Double matchScore;
    private Double fitScore;

}
