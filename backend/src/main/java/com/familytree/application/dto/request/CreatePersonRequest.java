package com.familytree.application.dto.request;

import com.familytree.domain.Person;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreatePersonRequest {
    @NotNull(message = "家族ID不能为空")
    private UUID groupId;
    
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50个字符")
    private String firstName;
    
    @Size(max = 50, message = "姓氏不能超过50个字符")
    private String lastName;
    
    private Person.Gender gender;
    
    private LocalDate birthDate;
    
    private LocalDate deathDate;
    
    @Size(max = 100, message = "出生地不能超过100个字符")
    private String birthPlace;
    
    private UUID currentSpouseId;
}
