package com.familytree.application.dto;

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
    private UUID focusPersonId;
    private String focusPersonName;
    private Integer depth;
    private List<PersonNodeDTO> nodes;
    private List<RelationshipEdgeDTO> edges;
}
