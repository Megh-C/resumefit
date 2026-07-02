package com.resumematcher.resumefit.ingestion;


import com.networknt.schema.format.DateTimeFormat;
import com.resumematcher.resumefit.entity.JobPosting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.math.BigDecimal;
import java.time.LocalDate;


@Slf4j
@Component
public class JobCsvMapper {

    private static final DateTimeFormatter US_FORMAT = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-M-d");

    public JobPosting mapToEntity(JobCsvRowDto row){
        log.debug("Mapping CSV row with Job_ID={}", row.getJobId());
        try{
            return JobPosting.builder()
                    .jobId(row.getJobId())
                    .jobTitle(row.getJobTitle())
                    .company(row.getCompany())
                    .companyType(row.getCompanyType())
                    .industry(row.getIndustry())
                    .city(row.getCity())
                    .locationTier(row.getLocationTier())
                    .experienceLevel(row.getExperienceLevel())
                    .jobType(row.getJobType())
                    .workMode(row.getWorkMode())
                    .salaryLpa(parseBigDecimal(row.getSalaryLpa()))
                    .skillsRequired(row.getSkillsRequired())
                    .educationRequired(row.getEducationRequired())
                    .openings(parseInteger(row.getOpenings()))
                    .applicants(parseInteger(row.getApplicants()))
                    .companyRating(parseBigDecimal(row.getCompanyRating()))
                    .datePosted(parseDate(row.getDatePosted()))
                    .build();
        }catch(Exception e){
            log.warn("Failed to map CSV row with Job_ID={}: {}", row.getJobId(), e.getMessage());
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse BigDecimal value: '{}'", value);
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse Integer value: '{}'", value);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;

        String trimmed = value.trim();

        try {
            return LocalDate.parse(trimmed, US_FORMAT);
        } catch (DateTimeParseException ignored) {
            // fall through to try ISO format
        }

        try {
            return LocalDate.parse(trimmed, ISO_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse Date value in any known format: '{}'", value);
            return null;
        }
    }

}
