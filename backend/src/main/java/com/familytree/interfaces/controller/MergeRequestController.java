package com.familytree.interfaces.controller;

import com.familytree.application.dto.response.ApiResponse;
import com.familytree.application.dto.request.ReviewMergeRequest;
import com.familytree.application.service.MergeService;
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
@RequestMapping("/api/merge-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "合并请求", description = "变更合并审核相关接口")
public class MergeRequestController {
    
    private final MergeService mergeService;
    
    @GetMapping("/group/{groupId}")
    @Operation(summary = "获取家族的合并请求列表")
    public ApiResponse<List<MergeRequest>> getGroupMergeRequests(@PathVariable UUID groupId) {
        return ApiResponse.success(mergeService.getPendingMergeRequests(groupId));
    }
    
    @GetMapping("/{mergeRequestId}")
    @Operation(summary = "获取合并请求详情")
    public ApiResponse<MergeRequest> getMergeRequest(@PathVariable UUID mergeRequestId) {
        return ApiResponse.success(mergeService.getMergeRequest(mergeRequestId));
    }
    
    @PostMapping("/{mergeRequestId}/approve")
    @Operation(summary = "批准合并请求")
    public ApiResponse<MergeService.MergeResult> approveMergeRequest(
            @CurrentUser UUID userId,
            @PathVariable UUID mergeRequestId) {
        return ApiResponse.success(mergeService.approveMergeRequest(mergeRequestId, userId));
    }
    
    @PostMapping("/{mergeRequestId}/reject")
    @Operation(summary = "拒绝合并请求")
    public ApiResponse<Void> rejectMergeRequest(
            @CurrentUser UUID userId,
            @PathVariable UUID mergeRequestId,
            @Valid @RequestBody ReviewMergeRequest request) {
        mergeService.rejectMergeRequest(mergeRequestId, userId, request.getComment());
        return ApiResponse.success();
    }
}
