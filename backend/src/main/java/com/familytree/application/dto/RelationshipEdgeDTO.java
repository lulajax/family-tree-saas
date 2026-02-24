package com.familytree.application.dto;

import com.familytree.domain.Relationship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipEdgeDTO {
    private UUID id;
    private UUID fromPersonId;
    private UUID toPersonId;
    private Relationship.RelationshipType type;
}
