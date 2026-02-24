package com.familytree.interfaces.controller;

import com.familytree.application.dto.PersonDTO;
import com.familytree.application.dto.request.CreatePersonRequest;
import com.familytree.application.dto.request.UpdatePersonRequest;
import com.familytree.application.dto.response.ApiResponse;
import com.familytree.application.service.PersonService;
import com.familytree.infrastructure.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "人物管理", description = "家族人物相关接口")
public class PersonController {
    
    private final PersonService personService;
    
    @PostMapping
    @Operation(summary = "创建人物")
    public ApiResponse<PersonDTO> createPerson(
            @CurrentUser UUID userId,
            @Valid @RequestBody CreatePersonRequest request) {
        return ApiResponse.success(personService.createPerson(userId, request));
    }
    
    @GetMapping("/{personId}")
    @Operation(summary = "获取人物详情")
    public ApiResponse<PersonDTO> getPerson(@PathVariable UUID personId) {
        return ApiResponse.success(personService.getPerson(personId));
    }
    
    @GetMapping("/group/{groupId}")
    @Operation(summary = "获取家族人物列表")
    public ApiResponse<List<PersonDTO>> getGroupPersons(@PathVariable UUID groupId) {
        return ApiResponse.success(personService.getGroupPersons(groupId));
    }
    
    @PutMapping("/{personId}")
    @Operation(summary = "更新人物")
    public ApiResponse<PersonDTO> updatePerson(
            @PathVariable UUID personId,
            @Valid @RequestBody UpdatePersonRequest request) {
        return ApiResponse.success(personService.updatePerson(personId, request));
    }
    
    @DeleteMapping("/{personId}")
    @Operation(summary = "删除人物")
    public ApiResponse<Void> deletePerson(@PathVariable UUID personId) {
        personService.deletePerson(personId);
        return ApiResponse.success();
    }
    
    @GetMapping("/group/{groupId}/search")
    @Operation(summary = "搜索人物")
    public ApiResponse<List<PersonDTO>> searchPersons(
            @PathVariable UUID groupId,
            @RequestParam String keyword) {
        return ApiResponse.success(personService.searchPersons(groupId, keyword));
    }
}
