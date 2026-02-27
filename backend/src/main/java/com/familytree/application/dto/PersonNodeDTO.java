package com.familytree.application.dto;

import com.familytree.domain.LineageType;
import com.familytree.domain.Person;
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
public class PersonNodeDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String fullName;
    private Person.Gender gender;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String primaryPhotoUrl;
    private Integer generation; // 相对于焦点人物的代数
    private Double x; // 用于布局的坐标
    private Double y;
    private LineageType lineageType; // 相对于焦点人物的血统线类型
}
