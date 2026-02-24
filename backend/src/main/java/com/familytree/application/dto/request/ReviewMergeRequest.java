package com.familytree.application.dto.request;

import com.familytree.domain.MergeRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMergeRequest {
    @NotNull(message = "审核状态不能为空")
    private MergeRequest.Status status;
    
    @Size(max = 1000, message = "审核意见不能超过1000个字符")
    private String comment;
}
