package com.resumematcher.resumefit.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "master_vocabulary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_name", unique = true, nullable = false)
    private String skillName;
}
