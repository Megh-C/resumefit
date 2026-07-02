package com.resumematcher.resumefit.service;

import com.resumematcher.resumefit.entity.UnknownSkill;
import com.resumematcher.resumefit.repository.UnknownSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillNormalizationService {

    private final VectorStore vectorStore;
    private final UnknownSkillRepository unknownSkillRepository;

    @Value("${app.skill.normalization-threshold}")
    private double normalizationThreshold;

    public List<String> normalizeSkills(List<String> rawSkills) {
        List<String> normalized = new ArrayList<>();

        for (String rawSkill : rawSkills) {
            String mapped = normalizeSingleSkill(rawSkill);
            normalized.add(mapped);
        }

        log.info("Normalized {} raw skills into {} canonical skills", rawSkills.size(), normalized.size());
        return normalized;
    }

    private String normalizeSingleSkill(String rawSkill) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(rawSkill)
                        .topK(1)
                        .filterExpression(b.eq("type", "vocabulary_skill").build())
                        .build()
        );

        if (results.isEmpty()) {
            log.info("UNMATCHED (no vocabulary candidates at all): '{}'", rawSkill);
            logUnknownSkill(rawSkill);
            return rawSkill;
        }

        Document bestMatch = results.get(0);
        Double score = bestMatch.getScore();
        String closestCandidate = (String) bestMatch.getMetadata().get("skill_name");

        if (score == null || score < normalizationThreshold) {
            log.info("BELOW THRESHOLD: '{}' -> closest candidate '{}' (score: {}, threshold: {})",
                    rawSkill, closestCandidate, score, normalizationThreshold);
            logUnknownSkill(rawSkill);
            return rawSkill;
        }

        log.info("MATCHED: '{}' -> '{}' (score: {})", rawSkill, closestCandidate, score);
        return closestCandidate;
    }

    private void logUnknownSkill(String skillName) {
        unknownSkillRepository.findBySkillName(skillName).ifPresentOrElse(
                existing -> {
                    existing.setFrequency(existing.getFrequency() + 1);
                    existing.setLastSeen(LocalDateTime.now());
                    unknownSkillRepository.save(existing);
                },
                () -> {
                    UnknownSkill newUnknown = UnknownSkill.builder()
                            .skillName(skillName)
                            .frequency(1)
                            .build();
                    unknownSkillRepository.save(newUnknown);
                    log.info("Logged new unknown skill: {}", skillName);
                }
        );
    }
}