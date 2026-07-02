package com.resumematcher.resumefit.ingestion;

import com.opencsv.bean.CsvBindAndJoinByName;
import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class JobCsvRowDto {

    @CsvBindByName(column = "Job_ID")
    private String jobId;

    @CsvBindByName(column = "Job_Title")
    private String jobTitle;

    @CsvBindByName(column = "Company")
    private String company;

    @CsvBindByName(column = "Company_Type")
    private String companyType;

    @CsvBindByName(column = "Industry")
    private String industry;

    @CsvBindByName(column = "City")
    private String city;

    @CsvBindByName(column = "Location_Tier")
    private String locationTier;

    @CsvBindByName(column = "Experience_Level")
    private String experienceLevel;

    @CsvBindByName(column = "Job_Type")
    private String jobType;

    @CsvBindByName(column = "Work_Mode")
    private String workMode;

    @CsvBindByName(column = "Salary_LPA")
    private String salaryLpa;

    @CsvBindByName(column = "Skills_Required")
    private String skillsRequired;

    @CsvBindByName(column = "Education_Required")
    private String educationRequired;

    @CsvBindByName(column = "Openings")
    private String openings;

    @CsvBindByName(column = "Applicants")
    private String applicants;

    @CsvBindByName(column = "Company_Rating")
    private String companyRating;

    @CsvBindByName(column = "Date_Posted")
    private String datePosted;

}
