package com.resumematcher.resumefit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapDto {
    private String skill;
    private int appearsInJobs;
    private int outOf;
}