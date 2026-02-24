package com.familytree.interfaces.controller;

import com.familytree.application.dto.PersonNodeDTO;
import com.familytree.application.dto.TreeViewDTO;
import com.familytree.application.service.FamilyTreeService;
import com.familytree.application.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/tree")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "家谱图谱", description = "家谱树形结构相关接口")
public class FamilyTreeController {
    
    private final FamilyTreeService familyTreeService;
    
    @GetMapping
    @Operation(summary = "获取家谱树视图")
    public ApiResponse<TreeViewDTO> getTreeView(
            @PathVariable UUID groupId,
            @RequestParam(required = false) UUID focusPersonId,
            @RequestParam(defaultValue = "3") int depth) {
        return ApiResponse.success(familyTreeService.getTreeView(groupId, focusPersonId, depth));
    }
    
    @GetMapping("/persons/{personId}/ancestors")
    @Operation(summary = "获取祖先")
    public ApiResponse<List<PersonNodeDTO>> getAncestors(
            @PathVariable UUID groupId,
            @PathVariable UUID personId,
            @RequestParam(defaultValue = "5") int generations) {
        return ApiResponse.success(familyTreeService.getAncestors(personId, generations));
    }
    
    @GetMapping("/persons/{personId}/descendants")
    @Operation(summary = "获取后代")
    public ApiResponse<List<PersonNodeDTO>> getDescendants(
            @PathVariable UUID groupId,
            @PathVariable UUID personId,
            @RequestParam(defaultValue = "3") int generations) {
        return ApiResponse.success(familyTreeService.getDescendants(personId, generations));
    }
}
