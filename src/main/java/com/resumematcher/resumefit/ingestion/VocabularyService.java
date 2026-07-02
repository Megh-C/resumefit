package com.resumematcher.resumefit.ingestion;


import com.resumematcher.resumefit.entity.JobPosting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
@Service
public class VocabularyService {

    public Set<String> buildVocabulary(List<JobPosting> jobPostings){
        Set<String> vocabulary = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for(JobPosting job : jobPostings){
            vocabulary.addAll(job.getSkillsAsList());
        }

        log.info("Built master vocabulary with {} unique skills",vocabulary.size());
        return vocabulary;
    }

}
