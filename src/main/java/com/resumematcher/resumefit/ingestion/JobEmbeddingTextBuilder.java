package com.resumematcher.resumefit.ingestion;

import com.resumematcher.resumefit.entity.JobPosting;
import org.springframework.stereotype.Component;

@Component
public class JobEmbeddingTextBuilder {

    public String buildEmbeddingText(JobPosting job) {
        return String.format(
                "Job Title: %s. Skills: %s. Experience Level: %s. Education Required: %s.",
                safe(job.getJobTitle()),
                safe(job.getSkillsRequired()),
                safe(job.getExperienceLevel()),
                safe(job.getEducationRequired())
        );
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "Not specified" : value.trim();
    }

}