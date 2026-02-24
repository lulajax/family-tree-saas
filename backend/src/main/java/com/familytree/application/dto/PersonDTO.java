package com.familytree.application.dto;

import com.familytree.domain.Person;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonDTO {
    private UUID id;
    private UUID groupId;
    private String firstName;
    private String lastName;
    private String fullName;
    private Person.Gender gender;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String birthPlace;
    private UUID currentSpouseId;
    private String currentSpouseName;
    private String primaryPhotoUrl;
    private List<PhotoDTO> photos;
    private List<CustomAttributeDTO> customAttributes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
    
    // 家族关系字段（用于树形展示）
    private List<UUID> parentIds;
    private List<UUID> childrenIds;
    private List<UUID> spouseIds;
    private List<UUID> siblingIds;
}
