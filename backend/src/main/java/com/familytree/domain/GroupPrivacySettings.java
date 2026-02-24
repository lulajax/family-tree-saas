package com.familytree.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_privacy_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPrivacySettings {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", unique = true, nullable = false)
    private UUID groupId;

    @Column(name = "is_searchable")
    private Boolean isSearchable;

    @Column(name = "public_ancestors_level")
    private Integer publicAncestorsLevel;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (isSearchable == null) {
            isSearchable = false;
        }
        if (publicAncestorsLevel == null) {
            publicAncestorsLevel = 0;
        }
    }
}
