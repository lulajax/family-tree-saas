package com.familytree.interfaces.controller;

import com.familytree.application.dto.GroupDTO;
import com.familytree.application.dto.request.CreateGroupRequest;
import com.familytree.application.dto.response.ApiResponse;
import com.familytree.application.service.GroupService;
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
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "家族管理", description = "家族群组相关接口")
public class GroupController {
    
    private final GroupService groupService;
    
    @PostMapping
    @Operation(summary = "创建家族")
    public ApiResponse<GroupDTO> createGroup(
            @CurrentUser UUID userId,
            @Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.success(groupService.createGroup(userId, request));
    }
    
    @GetMapping
    @Operation(summary = "获取我的家族列表")
    public ApiResponse<List<GroupDTO>> getMyGroups(@CurrentUser UUID userId) {
        return ApiResponse.success(groupService.getUserGroups(userId));
    }
    
    @GetMapping("/{groupId}")
    @Operation(summary = "获取家族详情")
    public ApiResponse<GroupDTO> getGroup(@PathVariable UUID groupId) {
        return ApiResponse.success(groupService.getGroup(groupId));
    }
    
    @PostMapping("/{groupId}/join")
    @Operation(summary = "加入家族")
    public ApiResponse<Void> joinGroup(
            @CurrentUser UUID userId,
            @PathVariable UUID groupId) {
        groupService.joinGroup(userId, groupId);
        return ApiResponse.success();
    }
    
    @PostMapping("/{groupId}/leave")
    @Operation(summary = "退出家族")
    public ApiResponse<Void> leaveGroup(
            @CurrentUser UUID userId,
            @PathVariable UUID groupId) {
        groupService.leaveGroup(userId, groupId);
        return ApiResponse.success();
    }
}
