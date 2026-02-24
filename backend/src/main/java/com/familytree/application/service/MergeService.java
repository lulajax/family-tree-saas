package com.familytree.application.service;

import com.familytree.domain.*;
import com.familytree.infrastructure.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MergeService {
    
    private final MergeRequestRepository mergeRequestRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ChangeSetRepository changeSetRepository;
    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional(readOnly = true)
    public List<MergeRequest> getPendingMergeRequests(UUID groupId) {
        return mergeRequestRepository.findPendingByGroupId(groupId);
    }
    
    @Transactional(readOnly = true)
    public MergeRequest getMergeRequest(UUID mergeRequestId) {
        return mergeRequestRepository.findById(mergeRequestId)
            .orElseThrow(() -> new RuntimeException("合并请求不存在"));
    }
    
    @Transactional
    public MergeResult approveMergeRequest(UUID mergeRequestId, UUID reviewerId) {
        MergeRequest mergeRequest = mergeRequestRepository.findById(mergeRequestId)
            .orElseThrow(() -> new RuntimeException("合并请求不存在"));
        
        if (!mergeRequest.isPending()) {
            throw new RuntimeException("合并请求状态不正确");
        }
        
        Workspace workspace = workspaceRepository.findById(mergeRequest.getWorkspaceId())
            .orElseThrow(() -> new RuntimeException("工作区不存在"));
        
        Group group = groupRepository.findById(mergeRequest.getGroupId())
            .orElseThrow(() -> new RuntimeException("家族不存在"));
        
        // 检查版本是否落后
        if (!group.getVersion().equals(workspace.getBaseVersion())) {
            // 检测冲突
            List<Conflict> conflicts = detectConflicts(workspace, group);
            if (!conflicts.isEmpty()) {
                mergeRequest.setStatus(MergeRequest.Status.CONFLICT);
                mergeRequestRepository.save(mergeRequest);
                
                workspace.setStatus(Workspace.Status.CONFLICT);
                workspaceRepository.save(workspace);
                
                return MergeResult.conflict(conflicts);
            }
        }
        
        // 应用所有变更
        List<ChangeSet> changes = changeSetRepository.findByWorkspaceIdOrderBySequenceNumberAsc(workspace.getId());
        
        for (ChangeSet change : changes) {
            applyChange(change, group.getId());
        }
        
        // 更新群组版本
        group.setVersion(group.getVersion() + 1);
        groupRepository.save(group);
        
        // 更新合并请求状态
        mergeRequest.setStatus(MergeRequest.Status.APPROVED);
        mergeRequest.setReviewedBy(reviewerId);
        mergeRequestRepository.save(mergeRequest);
        
        // 更新工作区状态
        workspace.setStatus(Workspace.Status.MERGED);
        workspaceRepository.save(workspace);
        
        return MergeResult.success();
    }
    
    @Transactional
    public void rejectMergeRequest(UUID mergeRequestId, UUID reviewerId, String comment) {
        MergeRequest mergeRequest = mergeRequestRepository.findById(mergeRequestId)
            .orElseThrow(() -> new RuntimeException("合并请求不存在"));
        
        if (!mergeRequest.isPending()) {
            throw new RuntimeException("合并请求状态不正确");
        }
        
        mergeRequest.setStatus(MergeRequest.Status.REJECTED);
        mergeRequest.setReviewedBy(reviewerId);
        mergeRequest.setReviewComment(comment);
        mergeRequestRepository.save(mergeRequest);
        
        // 更新工作区状态为可编辑
        Workspace workspace = workspaceRepository.findById(mergeRequest.getWorkspaceId())
            .orElseThrow(() -> new RuntimeException("工作区不存在"));
        workspace.setStatus(Workspace.Status.EDITING);
        workspaceRepository.save(workspace);
    }
    
    private List<Conflict> detectConflicts(Workspace workspace, Group group) {
        List<Conflict> conflicts = new ArrayList<>();
        
        // 获取工作区内的所有变更
        List<ChangeSet> changes = changeSetRepository.findByWorkspaceIdOrderBySequenceNumberAsc(workspace.getId());
        
        // 检查每个变更的实体是否被其他人修改过
        for (ChangeSet change : changes) {
            if (change.getActionType() == ChangeSet.ActionType.UPDATE) {
                Person person = personRepository.findById(change.getEntityId()).orElse(null);
                if (person != null && person.getVersion() > workspace.getBaseVersion()) {
                    conflicts.add(Conflict.builder()
                        .entityId(change.getEntityId())
                        .entityType("PERSON")
                        .message("该人物在您编辑期间已被其他人修改")
                        .build());
                }
            }
        }
        
        return conflicts;
    }
    
    @SneakyThrows
    private void applyChange(ChangeSet change, UUID groupId) {
        switch (change.getActionType()) {
            case CREATE:
                applyCreate(change, groupId);
                break;
            case UPDATE:
                applyUpdate(change);
                break;
            case DELETE:
                applyDelete(change);
                break;
        }
    }
    
    @SneakyThrows
    private void applyCreate(ChangeSet change, UUID groupId) {
        if (change.getEntityType() == ChangeSet.EntityType.PERSON) {
            Map<String, Object> payload = change.getPayload();
            
            Person person = Person.builder()
                .id(change.getEntityId())
                .groupId(groupId)
                .firstName((String) payload.get("firstName"))
                .lastName((String) payload.get("lastName"))
                .gender(payload.get("gender") != null ? Person.Gender.valueOf((String) payload.get("gender")) : null)
                .birthDate(parseDate((String) payload.get("birthDate")))
                .deathDate(parseDate((String) payload.get("deathDate")))
                .birthPlace((String) payload.get("birthPlace"))
                .currentSpouseId(payload.get("currentSpouseId") != null ? UUID.fromString((String) payload.get("currentSpouseId")) : null)
                .version(0)
                .build();
            
            personRepository.save(person);
        }
    }
    
    @SneakyThrows
    private void applyUpdate(ChangeSet change) {
        if (change.getEntityType() == ChangeSet.EntityType.PERSON) {
            Person person = personRepository.findById(change.getEntityId())
                .orElseThrow(() -> new RuntimeException("人物不存在"));
            
            Map<String, Object> payload = change.getPayload();
            
            if (payload.containsKey("firstName")) {
                person.setFirstName((String) payload.get("firstName"));
            }
            if (payload.containsKey("lastName")) {
                person.setLastName((String) payload.get("lastName"));
            }
            if (payload.containsKey("gender")) {
                person.setGender(Person.Gender.valueOf((String) payload.get("gender")));
            }
            if (payload.containsKey("birthDate")) {
                person.setBirthDate(parseDate((String) payload.get("birthDate")));
            }
            if (payload.containsKey("deathDate")) {
                person.setDeathDate(parseDate((String) payload.get("deathDate")));
            }
            if (payload.containsKey("birthPlace")) {
                person.setBirthPlace((String) payload.get("birthPlace"));
            }
            if (payload.containsKey("currentSpouseId")) {
                String spouseId = (String) payload.get("currentSpouseId");
                person.setCurrentSpouseId(spouseId != null ? UUID.fromString(spouseId) : null);
            }
            
            personRepository.save(person);
        }
    }
    
    private void applyDelete(ChangeSet change) {
        if (change.getEntityType() == ChangeSet.EntityType.PERSON) {
            personRepository.deleteById(change.getEntityId());
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr);
    }
    
    // 合并结果
    public static class MergeResult {
        private boolean success;
        private List<Conflict> conflicts;
        
        public static MergeResult success() {
            MergeResult result = new MergeResult();
            result.success = true;
            return result;
        }
        
        public static MergeResult conflict(List<Conflict> conflicts) {
            MergeResult result = new MergeResult();
            result.success = false;
            result.conflicts = conflicts;
            return result;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public List<Conflict> getConflicts() {
            return conflicts;
        }
    }
    
    // 冲突信息
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Conflict {
        private UUID entityId;
        private String entityType;
        private String message;
    }
}
