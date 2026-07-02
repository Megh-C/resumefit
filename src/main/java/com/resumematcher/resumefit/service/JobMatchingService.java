package com.resumematcher.resumefit.service;


import com.resumematcher.resumefit.dto.response.ParsedResumeDto;
import com.resumematcher.resumefit.dto.response.JobMatchDto;
import com.resumematcher.resumefit.entity.JobPosting;
import com.resumematcher.resumefit.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchingService {

    private final VectorStore vectorStore;
    private final JobPostingRepository jobPostingRepository;
    private final ResumeEmbeddingTextBuilder resumeEmbeddingTextBuilder;
    private final FitScorerService fitScorerService;

    @Value("${app.matching.top-k}")
    private int topK;

    public List<JobMatchDto> findTopMatches(ParsedResumeDto resume){
        // Step 1 — build embedding text from resume
        String embeddingText = resumeEmbeddingTextBuilder.buildEmbeddingText(resume);
        log.info("Resume embedding text: {}", embeddingText);

        // Step 2 — cosine similarity search against job posting vectors only
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        List<Document> searchResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(embeddingText)
                        .topK(topK)
                        .filterExpression(b.eq("type", "job_posting").build())
                        .build()
        );
        log.info("Vector search returned {} matches", searchResults.size());

        if (searchResults.isEmpty()) {
            log.warn("No matches found for resume. Check that job posting vectors exist in vector_store.");
            return new ArrayList<>();
        }


        // Step 3 — extract job IDs and scores from search results
        List<String> jobIds = searchResults.stream()
                .map(doc -> (String) doc.getMetadata().get("job_id"))
                .collect(Collectors.toList());

        Map<String, Double> scoresByJobId = searchResults.stream()
                .collect(Collectors.toMap(
                        doc -> (String) doc.getMetadata().get("job_id"),
                        doc -> doc.getScore() != null ? doc.getScore() : 0.0
                ));

        log.info("Top {} matched job IDs: {}", topK, jobIds);

        // Step 4 — fetch full job details from job_postings table
        List<JobPosting> jobPostings = jobPostingRepository.findByJobIdIn(jobIds);

        log.info("Fetched {} job postings from DB", jobPostings.size());

        // Step 5 — assemble JobMatchDto list, preserving cosine score
        // fitScore is null for now — we add the logistic regression scorer next
        List<JobMatchDto> matches = jobPostings.stream()
                .map(job -> JobMatchDto.builder()
                        .jobId(job.getJobId())
                        .jobTitle(job.getJobTitle())
                        .company(job.getCompany())
                        .companyType(job.getCompanyType())
                        .industry(job.getIndustry())
                        .city(job.getCity())
                        .workMode(job.getWorkMode())
                        .experienceLevel(job.getExperienceLevel())
                        .educationRequired(job.getEducationRequired())
                        .salaryLpa(job.getSalaryLpa())
                        .skillsRequired(job.getSkillsAsList())
                        .datePosted(job.getDatePosted())
                        .matchScore(scoresByJobId.get(job.getJobId()))
                        .fitScore(null)
                        .build())
                .sorted((a, b2) -> Double.compare(
                        b2.getMatchScore() != null ? b2.getMatchScore() : 0.0,
                        a.getMatchScore() != null ? a.getMatchScore() : 0.0))
                .collect(Collectors.toList());

        log.info("Assembled {} job matches. Top match: {} at score {}",
                matches.size(),
                matches.isEmpty() ? "none" : matches.get(0).getJobTitle(),
                matches.isEmpty() ? 0 : matches.get(0).getMatchScore());

        List<JobMatchDto> scoredAndFiltered = fitScorerService.filterAndScore(matches, resume);

        log.info("After fit scoring and qualification filter: {} jobs returned",
                scoredAndFiltered.size());

        return scoredAndFiltered;


    }



}
