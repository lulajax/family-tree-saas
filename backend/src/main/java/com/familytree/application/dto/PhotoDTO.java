package com.familytree.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDTO {
    private UUID id;
    private UUID personId;
    private String url;
    private String description;
    private LocalDate takenAt;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}
