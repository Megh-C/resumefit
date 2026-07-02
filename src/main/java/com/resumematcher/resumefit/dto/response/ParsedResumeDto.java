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
public class ParsedResumeDto {

    private List<String> skills;
    private Integer yearsExperience;
    private List<String> currentRoles;
    private String seniority;
    private String education;

}
