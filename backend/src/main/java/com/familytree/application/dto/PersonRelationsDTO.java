package com.familytree.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 人员关系详情 DTO
 * 用于展示某人的完整家族关系网络
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonRelationsDTO {

    private UUID personId;
    private String personName;
    private String primaryPhotoUrl;

    // 父母列表（包含完整信息）
    private List<PersonSummaryDTO> parents;

    // 配偶列表（包含完整信息）
    private List<PersonSummaryDTO> spouses;

    // 子女列表（包含完整信息）
    private List<PersonSummaryDTO> children;

    // 兄弟姐妹列表（包含完整信息）
    private List<PersonSummaryDTO> siblings;

    /**
     * 人员摘要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonSummaryDTO {
        private UUID id;
        private String fullName;
        private String firstName;
        private String lastName;
        private String gender;
        private String birthDate;
        private String deathDate;
        private String primaryPhotoUrl;
        // 与目标人物的关系类型（用于展示）
        private String relationType;
    }
}
