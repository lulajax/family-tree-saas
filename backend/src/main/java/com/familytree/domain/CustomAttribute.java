package com.familytree.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "custom_attributes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "attr_key", nullable = false, length = 50)
    private String key;

    @Column(name = "attr_value", columnDefinition = "TEXT")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 20)
    private DataType dataType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum DataType {
        STRING, NUMBER, DATE, BOOLEAN
    }
}
