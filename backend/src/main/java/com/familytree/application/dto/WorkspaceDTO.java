package com.familytree.application.dto;

import com.familytree.domain.Workspace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDTO {
    private UUID id;
    private UUID groupId;
    private String groupName;
    private UUID userId;
    private Integer baseVersion;
    private Workspace.Status status;
    private Integer changeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChangeSetDTO> changes;
    private Boolean isEditable;
}
