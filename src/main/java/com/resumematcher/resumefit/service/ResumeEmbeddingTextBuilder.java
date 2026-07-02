package com.resumematcher.resumefit.service;

import com.resumematcher.resumefit.dto.response.ParsedResumeDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResumeEmbeddingTextBuilder {

    public String buildEmbeddingText(ParsedResumeDto resume) {
        return String.format(
                "Job Title: %s. Skills: %s. Experience Level: %s. Education Required: %s.",
                safeJoin(resume.getCurrentRoles()),
                safeJoin(resume.getSkills()),
                formatExperienceLevel(resume.getSeniority()),
                safe(resume.getEducation())
        );
    }

    private String safeJoin(List<String> values){
        if (values == null || values.isEmpty()) return "Not specified";
        return String.join(", ", values);
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "Not specified" : value.trim();
    }

    private String formatExperienceLevel(String seniority) {
        if (seniority == null || seniority.isBlank()) return "Not specified";

        return switch (seniority.trim().toLowerCase()) {
            case "fresher" -> "Fresher (0-1 yr)";
            case "junior"  -> "Junior (1-3 yrs)";
            case "mid"     -> "Mid (3-6 yrs)";
            case "senior"  -> "Senior (6-10 yrs)";
            case "lead"    -> "Lead (10+ yrs)";
            default        -> seniority;
        };
    }
}
