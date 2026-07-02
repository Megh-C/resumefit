package com.resumematcher.resumefit.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "unknown_skills")
@Getter
@Setter
@NoArgsConstructor //for making object by jpa at startup
@AllArgsConstructor//needed by builder internally
@Builder
public class UnknownSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="skill_name", unique = true, nullable = false)
    private String skillName;

    @Column(name = "frequency")
    private Integer frequency = 1;

    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @PrePersist
    public void prePersist() {
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }
}
