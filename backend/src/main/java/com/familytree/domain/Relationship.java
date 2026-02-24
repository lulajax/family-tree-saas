package com.familytree.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "relationships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "from_person_id", "to_person_id", "type"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Relationship {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "from_person_id", nullable = false)
    private UUID fromPersonId;

    @Column(name = "to_person_id", nullable = false)
    private UUID toPersonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private RelationshipType type;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum RelationshipType {
        PARENT, CHILD, SPOUSE, SIBLING
    }
}
