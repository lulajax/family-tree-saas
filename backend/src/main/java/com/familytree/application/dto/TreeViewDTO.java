package com.familytree.application.dto;

import com.familytree.domain.LineageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeViewDTO {
    private UUID groupId;
    private UUID focusPersonId;
    private String focusPersonName;
    private Integer depth;
    private LineageType filterLineageType; // 当前筛选的血统线类型
    private List<PersonNodeDTO> nodes;
    private List<RelationshipEdgeDTO> edges;
}
