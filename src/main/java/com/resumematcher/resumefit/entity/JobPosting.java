package com.resumematcher.resumefit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "job_postings")
@NoArgsConstructor //needed for entities jpa requires it hibernate instantiates entities using no arg constructors
@AllArgsConstructor //needed by @Builer internally
@Builder
@Getter
@Setter
public class JobPosting{

    @Id
    @Column(name = "job_id")
    private String jobId;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "company")
    private String company;

    @Column(name = "company_type")
    private String companyType;

    @Column(name = "industry")
    private String industry;

    @Column(name = "city")
    private String city;

    @Column(name = "location_tier")
    private String locationTier;

    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "work_mode")
    private String workMode;

    @Column(name = "salary_lpa")
    private BigDecimal salaryLpa;

    @Column(name = "skills_required", columnDefinition = "TEXT")
    private String skillsRequired;

    @Column(name = "education_required")
    private String educationRequired;

    @Column(name = "openings")
    private Integer openings;

    @Column(name = "applicants")
    private Integer applicants;

    @Column(name = "company_rating")
    private BigDecimal companyRating;

    @Column(name = "date_posted")
    private LocalDate datePosted;

    // Helper method — returns skills as a clean list
    public java.util.List<String> getSkillsAsList() {
        if (skillsRequired == null || skillsRequired.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(skillsRequired.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

}
