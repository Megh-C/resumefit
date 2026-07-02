package com.resumematcher.resumefit.repository;

import com.resumematcher.resumefit.entity.UnknownSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnknownSkillRepository extends JpaRepository<UnknownSkill,Long> {
    Optional<UnknownSkill> findBySkillName(String skillName);
}
