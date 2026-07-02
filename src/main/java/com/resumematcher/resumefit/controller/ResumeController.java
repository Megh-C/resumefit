package com.resumematcher.resumefit.controller;

import com.resumematcher.resumefit.dto.response.*;
import com.resumematcher.resumefit.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeParsingService resumeParsingService;
    private final ResumeExtractionService resumeExtractionService;
    private final SkillNormalizationService skillNormalizationService;
    private final JobMatchingService jobMatchingService;
    private final GapAnalysisService gapAnalysisService;
    private final SalaryInferenceService salaryInferenceService;
    private final InsightGenerationService insightGenerationService;
    private final SessionCacheService sessionCacheService;

    @PostMapping("/analyze")
    public ResponseEntity<ResumeAnalysisResponseDto> analyzeResume(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        try {
            // Cache check — if session ID provided and result exists, return immediately
            if (sessionId != null && !sessionId.isBlank()) {
                Optional<ResumeAnalysisResponseDto> cached = sessionCacheService.get(sessionId);
                if (cached.isPresent()) {
                    log.info("Returning cached result for session {}", sessionId);
                    return ResponseEntity.ok()
                            .header("X-Session-Id", sessionId)
                            .header("X-Cache", "HIT")
                            .body(cached.get());
                }
                log.info("Cache MISS for session {} — reprocessing", sessionId);
            }

            log.info("Starting full resume analysis pipeline for file: {}",
                    file.getOriginalFilename());

            // Full pipeline
            String rawText = resumeParsingService.extractText(file);
            ParsedResumeDto parsed = resumeExtractionService.extractStructuredData(rawText);

            List<String> normalizedSkills = skillNormalizationService.normalizeSkills(parsed.getSkills());
            parsed.setSkills(normalizedSkills);

            List<JobMatchDto> matches = jobMatchingService.findTopMatches(parsed);
            GapAnalysisDto gapAnalysis = gapAnalysisService.analyze(matches, parsed.getSkills());
            SalaryAnalysisDto salaryAnalysis = salaryInferenceService.analyze(parsed, gapAnalysis);
            InsightsDto insights = insightGenerationService.generateInsights(
                    parsed, gapAnalysis, salaryAnalysis, matches);

            String matchNote = matches.size() < 10
                    ? "Only " + matches.size() + " qualified matches found. " +
                    "This reflects your current eligibility based on experience " +
                    "and education requirements in the market."
                    : "Showing top 10 qualified matches.";

            ResumeAnalysisResponseDto response = ResumeAnalysisResponseDto.builder()
                    .resumeSummary(parsed)
                    .topMatches(matches)
                    .qualifiedMatchCount(matches.size())
                    .qualifiedMatchNote(matchNote)
                    .gapAnalysis(gapAnalysis)
                    .salaryAnalysis(salaryAnalysis)
                    .insights(insights)
                    .build();

            // Generate new session ID and cache the result
            String newSessionId = UUID.randomUUID().toString();
            sessionCacheService.save(newSessionId, response);

            log.info("Analysis complete. Session ID {} cached for {} minutes.",
                    newSessionId, 30);

            return ResponseEntity.ok()
                    .header("X-Session-Id", newSessionId)
                    .header("X-Cache", "MISS")
                    .body(response);

        } catch (Exception e) {
            log.error("Resume analysis pipeline failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}