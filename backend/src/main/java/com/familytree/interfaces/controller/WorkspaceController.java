package com.familytree.interfaces.controller;

import com.familytree.application.dto.ChangeSetDTO;
import com.familytree.application.dto.WorkspaceDTO;
import com.familytree.application.dto.request.CommitChangesRequest;
import com.familytree.application.dto.request.CreatePersonRequest;
import com.familytree.application.dto.request.UpdatePersonRequest;
import com.familytree.application.dto.response.ApiResponse;
import com.familytree.application.service.WorkspaceService;
import com.familytree.domain.MergeRequest;
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
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "工作区管理", description = "编辑工作区相关接口")
public class WorkspaceController {
    
    private final WorkspaceService workspaceService;
    
    @PostMapping("/{groupId}")
    @Operation(summary = "创建或获取工作区")
    public ApiResponse<WorkspaceDTO> createOrGetWorkspace(
            @CurrentUser UUID userId,
            @PathVariable UUID groupId) {
        return ApiResponse.success(workspaceService.createOrGetWorkspace(userId, groupId));
    }
    
    @GetMapping("/{workspaceId}")
    @Operation(summary = "获取工作区详情")
    public ApiResponse<WorkspaceDTO> getWorkspace(@PathVariable UUID workspaceId) {
        return ApiResponse.success(workspaceService.getWorkspace(workspaceId));
    }
    
    @GetMapping("/{workspaceId}/changes")
    @Operation(summary = "获取工作区变更列表")
    public ApiResponse<List<ChangeSetDTO>> getChanges(@PathVariable UUID workspaceId) {
        return ApiResponse.success(workspaceService.getChanges(workspaceId));
    }
    
    @PostMapping("/{workspaceId}/persons")
    @Operation(summary = "添加人物变更")
    public ApiResponse<ChangeSetDTO> addPersonChange(
            @CurrentUser UUID userId,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreatePersonRequest request) {
        return ApiResponse.success(workspaceService.addPersonChange(workspaceId, userId, request));
    }
    
    @PutMapping("/{workspaceId}/persons/{personId}")
    @Operation(summary = "更新人物变更")
    public ApiResponse<ChangeSetDTO> updatePersonChange(
            @CurrentUser UUID userId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID personId,
            @Valid @RequestBody UpdatePersonRequest request) {
        return ApiResponse.success(workspaceService.updatePersonChange(workspaceId, userId, personId, request));
    }
    
    @DeleteMapping("/{workspaceId}/persons/{personId}")
    @Operation(summary = "删除人物变更")
    public ApiResponse<ChangeSetDTO> deletePersonChange(
            @CurrentUser UUID userId,
            @PathVariable UUID workspaceId,
            @PathVariable UUID personId) {
        return ApiResponse.success(workspaceService.deletePersonChange(workspaceId, userId, personId));
    }
    
    @PostMapping("/{workspaceId}/commit")
    @Operation(summary = "提交变更")
    public ApiResponse<MergeRequest> commitChanges(
            @CurrentUser UUID userId,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CommitChangesRequest request) {
        return ApiResponse.success(workspaceService.commitChanges(workspaceId, userId, request));
    }
    
    @PostMapping("/{workspaceId}/reset")
    @Operation(summary = "重置工作区")
    public ApiResponse<Void> resetWorkspace(
            @CurrentUser UUID userId,
            @PathVariable UUID workspaceId) {
        workspaceService.resetWorkspace(workspaceId, userId);
        return ApiResponse.success();
    }
}
