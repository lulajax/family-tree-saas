package com.familytree.application.service;

import com.familytree.application.dto.ChangeSetDTO;
import com.familytree.application.dto.WorkspaceDTO;
import com.familytree.application.dto.request.CommitChangesRequest;
import com.familytree.application.dto.request.CreatePersonRequest;
import com.familytree.application.dto.request.UpdatePersonRequest;
import com.familytree.domain.*;
import com.familytree.infrastructure.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {
    
    private final WorkspaceRepository workspaceRepository;
    private final ChangeSetRepository changeSetRepository;
    private final MergeRequestRepository mergeRequestRepository;
    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public WorkspaceDTO createOrGetWorkspace(UUID userId, UUID groupId) {
        // 检查是否已有活跃工作区
        Optional<Workspace> existingWorkspace = workspaceRepository.findActiveWorkspace(groupId, userId);
        
        if (existingWorkspace.isPresent()) {
            return toDTO(existingWorkspace.get());
        }
        
        // 获取群组当前版本
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("家族不存在"));
        
        Workspace workspace = Workspace.builder()
            .groupId(groupId)
            .userId(userId)
            .baseVersion(group.getVersion())
            .status(Workspace.Status.EDITING)
            .build();
        
        workspace = workspaceRepository.save(workspace);
        return toDTO(workspace);
    }
    
    @Transactional(readOnly = true)
    public WorkspaceDTO getWorkspace(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new RuntimeException("工作区不存在"));
        return toDTO(workspace);
    }
    
    @Transactional(readOnly = true)
    public List<ChangeSetDTO> getChanges(UUID workspaceId) {
        List<ChangeSet> changes = changeSetRepository.findByWorkspaceIdOrderBySequenceNumberAsc(workspaceId);
        return changes.stream()
            .map(this::toChangeSetDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ChangeSetDTO addPersonChange(UUID workspaceId, UUID userId, CreatePersonRequest request) {
        Workspace workspace = validateWorkspace(workspaceId, userId);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", request.getGroupId());
        payload.put("firstName", request.getFirstName());
        payload.put("lastName", request.getLastName());
        payload.put("gender", request.getGender());
        payload.put("birthDate", request.getBirthDate() != null ? request.getBirthDate().toString() : null);
        payload.put("deathDate", request.getDeathDate() != null ? request.getDeathDate().toString() : null);
        payload.put("birthPlace", request.getBirthPlace());
        payload.put("currentSpouseId", request.getCurrentSpouseId());
        
        ChangeSet changeSet = ChangeSet.builder()
            .workspaceId(workspaceId)
            .actionType(ChangeSet.ActionType.CREATE)
            .entityType(ChangeSet.EntityType.PERSON)
            .entityId(UUID.randomUUID()) // 临时ID
            .payload(payload)
            .sequenceNumber(getNextSequenceNumber(workspaceId))
            .build();
        
        changeSet = changeSetRepository.save(changeSet);
        return toChangeSetDTO(changeSet);
    }
    
    @Transactional
    public ChangeSetDTO updatePersonChange(UUID workspaceId, UUID userId, UUID personId, UpdatePersonRequest request) {
        Workspace workspace = validateWorkspace(workspaceId, userId);
        
        // 获取原始数据
        Person original = getOriginalPerson(workspace, personId);
        
        Map<String, Object> previousPayload = toMap(original);
        
        Map<String, Object> payload = new HashMap<>();
        if (request.getFirstName() != null) payload.put("firstName", request.getFirstName());
        if (request.getLastName() != null) payload.put("lastName", request.getLastName());
        if (request.getGender() != null) payload.put("gender", request.getGender());
        if (request.getBirthDate() != null) payload.put("birthDate", request.getBirthDate().toString());
        if (request.getDeathDate() != null) payload.put("deathDate", request.getDeathDate().toString());
        if (request.getBirthPlace() != null) payload.put("birthPlace", request.getBirthPlace());
        if (request.getCurrentSpouseId() != null) payload.put("currentSpouseId", request.getCurrentSpouseId());
        
        ChangeSet changeSet = ChangeSet.builder()
            .workspaceId(workspaceId)
            .actionType(ChangeSet.ActionType.UPDATE)
            .entityType(ChangeSet.EntityType.PERSON)
            .entityId(personId)
            .payload(payload)
            .previousPayload(previousPayload)
            .sequenceNumber(getNextSequenceNumber(workspaceId))
            .build();
        
        changeSet = changeSetRepository.save(changeSet);
        return toChangeSetDTO(changeSet);
    }
    
    @Transactional
    public ChangeSetDTO deletePersonChange(UUID workspaceId, UUID userId, UUID personId) {
        Workspace workspace = validateWorkspace(workspaceId, userId);
        
        Person original = getOriginalPerson(workspace, personId);
        
        ChangeSet changeSet = ChangeSet.builder()
            .workspaceId(workspaceId)
            .actionType(ChangeSet.ActionType.DELETE)
            .entityType(ChangeSet.EntityType.PERSON)
            .entityId(personId)
            .payload(new HashMap<>())
            .previousPayload(toMap(original))
            .sequenceNumber(getNextSequenceNumber(workspaceId))
            .build();
        
        changeSet = changeSetRepository.save(changeSet);
        return toChangeSetDTO(changeSet);
    }
    
    @Transactional
    public void resetWorkspace(UUID workspaceId, UUID userId) {
        Workspace workspace = validateWorkspace(workspaceId, userId);
        
        // 删除所有变更
        changeSetRepository.deleteByWorkspaceId(workspaceId);
        
        // 重置工作区状态
        workspace.setStatus(Workspace.Status.EDITING);
        workspaceRepository.save(workspace);
    }
    
    @Transactional
    public MergeRequest commitChanges(UUID workspaceId, UUID userId, CommitChangesRequest request) {
        Workspace workspace = validateWorkspace(workspaceId, userId);
        
        List<ChangeSet> changes = changeSetRepository.findByWorkspaceIdOrderBySequenceNumberAsc(workspaceId);
        
        if (changes.isEmpty()) {
            throw new RuntimeException("没有变更需要提交");
        }
        
        // 验证变更合法性
        validateChanges(changes);
        
        // 创建合并请求
        MergeRequest mergeRequest = MergeRequest.builder()
            .workspaceId(workspaceId)
            .groupId(workspace.getGroupId())
            .title(request.getTitle())
            .description(request.getDescription())
            .status(MergeRequest.Status.PENDING)
            .createdBy(userId)
            .build();
        
        mergeRequest = mergeRequestRepository.save(mergeRequest);
        
        // 更新工作区状态
        workspace.setStatus(Workspace.Status.SUBMITTED);
        workspaceRepository.save(workspace);
        
        return mergeRequest;
    }
    
    private Workspace validateWorkspace(UUID workspaceId, UUID userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new RuntimeException("工作区不存在"));
        
        if (!workspace.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此工作区");
        }
        
        if (!workspace.isEditable()) {
            throw new RuntimeException("工作区不可编辑");
        }
        
        return workspace;
    }
    
    private Person getOriginalPerson(Workspace workspace, UUID personId) {
        // 先查找工作区内的变更
        List<ChangeSet> changes = changeSetRepository.findByWorkspaceIdAndEntityIdOrderBySequenceDesc(workspace.getId(), personId);
        
        for (ChangeSet change : changes) {
            if (change.getActionType() == ChangeSet.ActionType.CREATE) {
                // 如果是新建的，从payload重建
                return fromMap(change.getPayload(), personId);
            } else if (change.getActionType() == ChangeSet.ActionType.UPDATE) {
                // 如果是更新的，获取基础数据并应用变更
                Person base = personRepository.findById(personId).orElse(null);
                if (base != null) {
                    return applyChanges(base, change.getPayload());
                }
            }
        }
        
        // 从主数据库获取
        return personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("人物不存在"));
    }
    
    private Integer getNextSequenceNumber(UUID workspaceId) {
        return changeSetRepository.findMaxSequenceNumberByWorkspaceId(workspaceId)
            .map(n -> n + 1)
            .orElse(1);
    }
    
    private void validateChanges(List<ChangeSet> changes) {
        // 检查是否存在删除后修改同一实体的情况
        Set<UUID> deletedEntities = changes.stream()
            .filter(c -> c.getActionType() == ChangeSet.ActionType.DELETE)
            .map(ChangeSet::getEntityId)
            .collect(Collectors.toSet());
        
        for (ChangeSet change : changes) {
            if (deletedEntities.contains(change.getEntityId()) && 
                change.getActionType() != ChangeSet.ActionType.DELETE) {
                throw new RuntimeException("无法修改已删除的实体");
            }
        }
    }
    
    @SneakyThrows
    private Map<String, Object> toMap(Object obj) {
        return objectMapper.convertValue(obj, Map.class);
    }
    
    @SneakyThrows
    private Person fromMap(Map<String, Object> map, UUID id) {
        Person person = objectMapper.convertValue(map, Person.class);
        person.setId(id);
        return person;
    }
    
    private Person applyChanges(Person base, Map<String, Object> changes) {
        // 简化实现：直接修改字段
        if (changes.containsKey("firstName")) {
            base.setFirstName((String) changes.get("firstName"));
        }
        if (changes.containsKey("lastName")) {
            base.setLastName((String) changes.get("lastName"));
        }
        // ... 其他字段
        return base;
    }
    
    private WorkspaceDTO toDTO(Workspace workspace) {
        long changeCount = changeSetRepository.countByWorkspaceId(workspace.getId());
        
        String groupName = groupRepository.findById(workspace.getGroupId())
            .map(Group::getName)
            .orElse(null);
        
        return WorkspaceDTO.builder()
            .id(workspace.getId())
            .groupId(workspace.getGroupId())
            .groupName(groupName)
            .userId(workspace.getUserId())
            .baseVersion(workspace.getBaseVersion())
            .status(workspace.getStatus())
            .changeCount((int) changeCount)
            .createdAt(workspace.getCreatedAt())
            .updatedAt(workspace.getUpdatedAt())
            .isEditable(workspace.isEditable())
            .build();
    }
    
    private ChangeSetDTO toChangeSetDTO(ChangeSet changeSet) {
        return ChangeSetDTO.builder()
            .id(changeSet.getId())
            .workspaceId(changeSet.getWorkspaceId())
            .actionType(changeSet.getActionType())
            .entityType(changeSet.getEntityType())
            .entityId(changeSet.getEntityId())
            .payload(changeSet.getPayload())
            .previousPayload(changeSet.getPreviousPayload())
            .sequenceNumber(changeSet.getSequenceNumber())
            .createdAt(changeSet.getCreatedAt())
            .build();
    }
}
