package com.resumematcher.resumefit.ingestion;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.resumematcher.resumefit.entity.JobPosting;
import com.resumematcher.resumefit.entity.MasterVocabulary;
import com.resumematcher.resumefit.repository.JobPostingRepository;
import com.resumematcher.resumefit.repository.MasterVocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import org.springframework.ai.document.Document;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobIngestionRunner implements CommandLineRunner {

    private final JobPostingRepository jobPostingRepository;
    private final JobCsvMapper jobCsvMapper;
    private final MasterVocabularyRepository masterVocabularyRepository;
    private final VocabularyService vocabularyService;
    private final JobEmbeddingService jobEmbeddingService;
    private final VectorStore vectorStore;
    private final VocabularySkillEmbeddingService vocabularySkillEmbeddingService;

    @Value("${app.dataset.path}")
    private String datasetPath;

    @Value("${app.ingestion.enabled}")
    private boolean ingestionEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!ingestionEnabled) {
            log.info("Ingestion is disabled via app.ingestion.enabled=false. Skipping.");
            return;
        }

        long existingCount = jobPostingRepository.countBy();

        if (existingCount > 0) {
            log.info("job_postings table already has {} rows. Skipping CSV ingestion.", existingCount);
        } else {
            log.info("Starting CSV ingestion from path: {}", datasetPath);

            List<JobCsvRowDto> rows = readCsv();
            log.info("Read {} rows from CSV", rows.size());

            int savedCount = 0;
            int failedCount = 0;

            for (JobCsvRowDto row : rows) {
                JobPosting jobPosting = jobCsvMapper.mapToEntity(row);
                if (jobPosting == null) {
                    failedCount++;
                    continue;
                }
                jobPostingRepository.save(jobPosting);
                savedCount++;
            }

            log.info("Ingestion complete. Saved: {}, Failed: {}", savedCount, failedCount);
        }

        buildAndPersistVocabulary();
        embedJobPostingsIfNeeded();
        embedVocabularySkillsIfNeeded();
    }

    private void buildAndPersistVocabulary() {
        long existingVocabCount = masterVocabularyRepository.countBy();

        if (existingVocabCount > 0) {
            log.info("master_vocabulary already has {} entries. Skipping vocabulary persistence.", existingVocabCount);
            return;
        }

        List<JobPosting> allJobs = jobPostingRepository.findAll();
        Set<String> vocabulary = vocabularyService.buildVocabulary(allJobs);

        List<MasterVocabulary> vocabEntities = vocabulary.stream()
                .map(skill -> MasterVocabulary.builder().skillName(skill).build())
                .toList();

        masterVocabularyRepository.saveAll(vocabEntities);
        log.info("Persisted {} skills to master_vocabulary table", vocabEntities.size());
    }

    private List<JobCsvRowDto> readCsv() throws Exception {
        try (Reader reader = new FileReader(datasetPath)) {
            CsvToBean<JobCsvRowDto> csvToBean = new CsvToBeanBuilder<JobCsvRowDto>(reader)
                    .withType(JobCsvRowDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            return csvToBean.parse();
        }
    }
    private void embedJobPostingsIfNeeded() {
        // We use vector count as a proxy check since VectorStore doesn't expose a clean count method
        // A simple similarity search with a dummy query tells us if anything exists
        List<org.springframework.ai.document.Document> probe = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query("test")
                        .topK(1)
                        .build()
        );

        if (!probe.isEmpty()) {
            log.info("Vector store already contains embeddings. Skipping job embedding step.");
            return;
        }

        jobEmbeddingService.embedAllJobs();
    }
    private void embedVocabularySkillsIfNeeded() {
        List<Document> probe = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Java")
                        .topK(5)
                        .build()
        );

        boolean alreadyHasVocabVectors = probe.stream()
                .anyMatch(doc -> "vocabulary_skill".equals(doc.getMetadata().get("type")));

        if (alreadyHasVocabVectors) {
            log.info("Vector store already contains vocabulary skill embeddings. Skipping.");
            return;
        }

        vocabularySkillEmbeddingService.embedAllSkills();
    }
}