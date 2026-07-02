package com.resumematcher.resumefit.ingestion;

import com.resumematcher.resumefit.entity.MasterVocabulary;
import com.resumematcher.resumefit.repository.MasterVocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularySkillEmbeddingService {

    private static final int BATCH_SIZE = 50;

    private final VectorStore vectorStore;
    private final MasterVocabularyRepository masterVocabularyRepository;

    public void embedAllSkills() {
        List<MasterVocabulary> allSkills = masterVocabularyRepository.findAll();
        log.info("Starting embedding of {} vocabulary skills", allSkills.size());

        List<Document> batch = new ArrayList<>();
        int totalEmbedded = 0;

        for (MasterVocabulary skill : allSkills) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("skill_name", skill.getSkillName());
            metadata.put("type", "vocabulary_skill");

            Document document = new Document(skill.getSkillName(), metadata);
            batch.add(document);

            if (batch.size() >= BATCH_SIZE) {
                vectorStore.add(batch);
                totalEmbedded += batch.size();
                log.info("Embedded {}/{} vocabulary skills", totalEmbedded, allSkills.size());
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            vectorStore.add(batch);
            totalEmbedded += batch.size();
            log.info("Embedded {}/{} vocabulary skills", totalEmbedded, allSkills.size());
        }

        log.info("Vocabulary skill embedding complete. Total embedded: {}", totalEmbedded);
    }
}