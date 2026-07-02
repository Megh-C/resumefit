package com.resumematcher.resumefit.ingestion;

import com.resumematcher.resumefit.entity.JobPosting;
import com.resumematcher.resumefit.repository.JobPostingRepository;
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
public class JobEmbeddingService {

    private static final int BATCH_SIZE = 50;

    private final VectorStore vectorStore;
    private final JobEmbeddingTextBuilder embeddingTextBuilder;
    private final JobPostingRepository jobPostingRepository;

    public void embedAllJobs() {
        List<JobPosting> allJobs = jobPostingRepository.findAll();
        log.info("Starting embedding of {} job postings", allJobs.size());

        List<Document> batch = new ArrayList<>();
        int totalEmbedded = 0;

        for (JobPosting job : allJobs) {
            String embeddingText = embeddingTextBuilder.buildEmbeddingText(job);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("job_id", job.getJobId());
            metadata.put("type", "job_posting");

            Document document = new Document(embeddingText, metadata);
            batch.add(document);

            if (batch.size() >= BATCH_SIZE) {
                vectorStore.add(batch);
                totalEmbedded += batch.size();
                log.info("Embedded {}/{} job postings", totalEmbedded, allJobs.size());
                batch.clear();
            }
        }

        // Flush any remaining documents smaller than a full batch
        if (!batch.isEmpty()) {
            vectorStore.add(batch);
            totalEmbedded += batch.size();
            log.info("Embedded {}/{} job postings", totalEmbedded, allJobs.size());
        }

        log.info("Job embedding complete. Total embedded: {}", totalEmbedded);
    }
}