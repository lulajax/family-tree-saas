package com.familytree.application.dto;

import com.familytree.domain.Relationship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipDTO {
    private UUID id;
    private UUID groupId;
    private UUID fromPersonId;
    private String fromPersonName;
    private UUID toPersonId;
    private String toPersonName;
    private Relationship.RelationshipType type;
    private LocalDate startDate;
    private LocalDate endDate;
}
