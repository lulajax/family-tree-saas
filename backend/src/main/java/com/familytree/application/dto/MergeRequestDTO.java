package com.familytree.application.dto;

import com.familytree.domain.MergeRequest;
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
public class MergeRequestDTO {
    private UUID id;
    private UUID workspaceId;
    private UUID groupId;
    private String groupName;
    private String title;
    private String description;
    private MergeRequest.Status status;
    private UUID createdBy;
    private String createdByName;
    private UUID reviewedBy;
    private String reviewedByName;
    private String reviewComment;
    private Integer changeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChangeSetDTO> changes;
}
