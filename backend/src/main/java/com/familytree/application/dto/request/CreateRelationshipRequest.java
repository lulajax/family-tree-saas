package com.familytree.application.dto.request;

import com.familytree.domain.Relationship;
import jakarta.validation.constraints.NotNull;
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
public class CreateRelationshipRequest {
    @NotNull(message = "来源人物ID不能为空")
    private UUID fromPersonId;
    
    @NotNull(message = "目标人物ID不能为空")
    private UUID toPersonId;
    
    @NotNull(message = "关系类型不能为空")
    private Relationship.RelationshipType type;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
}
