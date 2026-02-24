package com.familytree.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDTO {
    private UUID id;
    private String name;
    private String description;
    private UUID adminId;
    private String adminName;
    private Integer memberCount;
    private Integer personCount;
    private LocalDateTime createdAt;
    private Integer version;
}
