package com.familytree.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.familytree.domain.ChangeSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSetDTO {
    private UUID id;
    private UUID workspaceId;
    private ChangeSet.ActionType actionType;
    private ChangeSet.EntityType entityType;
    private UUID entityId;
    private String entityName;
    private Map<String, Object> payload;
    private Map<String, Object> previousPayload;
    private Map<String, DiffValue> diff;
    private Integer sequenceNumber;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffValue {
        private Object old;
        @JsonProperty("new")
        private Object newValue;
    }
}
