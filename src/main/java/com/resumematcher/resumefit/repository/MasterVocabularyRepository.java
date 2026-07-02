package com.resumematcher.resumefit.repository;

import com.resumematcher.resumefit.entity.MasterVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MasterVocabularyRepository extends JpaRepository<MasterVocabulary,Long> {

    Optional<MasterVocabulary> findBySkillName(String skillName);
    long countBy();

}
