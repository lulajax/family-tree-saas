package com.familytree.application.service;

import com.familytree.application.dto.GroupDTO;
import com.familytree.application.dto.request.CreateGroupRequest;
import com.familytree.domain.*;
import com.familytree.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {
    private static final int MAX_PARENT_COUNT = 2;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPrivacySettingsRepository privacySettingsRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final RelationshipRepository relationshipRepository;
    
    @Transactional
    public GroupDTO createGroup(UUID userId, CreateGroupRequest request) {
        Group group = Group.builder()
            .name(request.getName())
            .description(request.getDescription())
            .adminId(userId)
            .version(0)
            .build();
        
        group = groupRepository.save(group);
        
        // 添加创建者为管理员
        GroupMember member = GroupMember.builder()
            .groupId(group.getId())
            .userId(userId)
            .role(GroupMember.Role.ADMIN)
            .build();
        groupMemberRepository.save(member);
        
        // 创建默认隐私设置
        GroupPrivacySettings privacySettings = GroupPrivacySettings.builder()
            .groupId(group.getId())
            .isSearchable(false)
            .publicAncestorsLevel(0)
            .build();
        privacySettingsRepository.save(privacySettings);
        
        return toDTO(group);
    }
    
    @Transactional(readOnly = true)
    public List<GroupDTO> getUserGroups(UUID userId) {
        List<Group> groups = groupRepository.findByMemberUserId(userId);
        return groups.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public GroupDTO getGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("家族不存在"));
        return toDTO(group);
    }
    
    @Transactional
    public void joinGroup(UUID userId, UUID groupId) {
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("您已是该家族成员");
        }
        
        GroupMember member = GroupMember.builder()
            .groupId(groupId)
            .userId(userId)
            .role(GroupMember.Role.MEMBER)
            .build();
        groupMemberRepository.save(member);
    }
    
    @Transactional
    public void leaveGroup(UUID userId, UUID groupId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new RuntimeException("您不是该家族成员"));
        
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("家族不存在"));
        
        // 管理员不能退出，需要先转让管理员权限
        if (group.getAdminId().equals(userId)) {
            throw new RuntimeException("管理员无法退出家族，请先转让管理员权限");
        }
        
        groupMemberRepository.delete(member);
    }
    
    @Transactional(readOnly = true)
    public boolean isGroupMember(UUID groupId, UUID userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }
    
    @Transactional(readOnly = true)
    public boolean isGroupAdmin(UUID groupId, UUID userId) {
        return groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, GroupMember.Role.ADMIN);
    }

    @Transactional
    public Relationship createRelationship(UUID groupId, UUID userId,
            com.familytree.application.dto.request.CreateRelationshipRequest request) {
        // 验证用户是家族成员
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("您没有权限操作该家族");
        }

        // 验证家族存在
        groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("家族不存在"));

        // 验证人物存在且属于该家族
        Person fromPerson = personRepository.findById(request.getFromPersonId())
                .orElseThrow(() -> new RuntimeException("来源人物不存在"));
        Person toPerson = personRepository.findById(request.getToPersonId())
                .orElseThrow(() -> new RuntimeException("目标人物不存在"));

        if (!fromPerson.getGroupId().equals(groupId) || !toPerson.getGroupId().equals(groupId)) {
            throw new RuntimeException("人物不属于该家族");
        }

        UUID normalizedFromPersonId = request.getFromPersonId();
        UUID normalizedToPersonId = request.getToPersonId();
        Relationship.RelationshipType normalizedType = request.getType();

        // 统一存储规则：父子关系统一使用 PARENT（from=父母, to=子女）
        if (request.getType() == Relationship.RelationshipType.PARENT) {
            normalizedFromPersonId = request.getToPersonId();
            normalizedToPersonId = request.getFromPersonId();
            normalizedType = Relationship.RelationshipType.PARENT;
        } else if (request.getType() == Relationship.RelationshipType.CHILD) {
            normalizedType = Relationship.RelationshipType.PARENT;
        }

        if (normalizedFromPersonId.equals(normalizedToPersonId)) {
            throw new RuntimeException("不能给自己创建关系");
        }

        Relationship.RelationshipType finalType = normalizedType;
        boolean exists;
        if (finalType == Relationship.RelationshipType.SPOUSE
                || finalType == Relationship.RelationshipType.SIBLING) {
            exists = relationshipRepository.findBetweenPersons(groupId, normalizedFromPersonId, normalizedToPersonId)
                    .stream()
                    .anyMatch(r -> r.getType() == finalType);
        } else {
            exists = relationshipRepository
                    .findByGroupIdAndFromPersonIdAndToPersonIdAndType(
                            groupId, normalizedFromPersonId, normalizedToPersonId, finalType)
                    .isPresent();
        }
        if (exists) {
            throw new RuntimeException("关系已存在");
        }

        validateConsistencyBeforeCreate(groupId, normalizedFromPersonId, normalizedToPersonId, finalType);

        // 创建关系
        Relationship relationship = Relationship.builder()
                .groupId(groupId)
                .fromPersonId(normalizedFromPersonId)
                .toPersonId(normalizedToPersonId)
                .type(finalType)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Relationship savedRelationship = relationshipRepository.save(relationship);

        // 兄弟姐妹关系建立后，自动补齐双方已知父母关系，保证父节点可关联到新兄弟节点
        if (finalType == Relationship.RelationshipType.SIBLING) {
            synchronizeSiblingParents(groupId, normalizedFromPersonId, normalizedToPersonId);
        } else if (finalType == Relationship.RelationshipType.PARENT) {
            // 新增父母关系后，若子女已有兄弟姐妹关系，则同步该父母到兄弟姐妹
            synchronizeParentToSiblings(groupId, normalizedFromPersonId, normalizedToPersonId);
        }

        return savedRelationship;
    }

    private void validateConsistencyBeforeCreate(
            UUID groupId,
            UUID normalizedFromPersonId,
            UUID normalizedToPersonId,
            Relationship.RelationshipType finalType) {
        if (finalType == Relationship.RelationshipType.SIBLING) {
            validateSiblingParentCompatibility(groupId, normalizedFromPersonId, normalizedToPersonId);
        } else if (finalType == Relationship.RelationshipType.PARENT) {
            validateParentLimitForChildAndSiblings(groupId, normalizedFromPersonId, normalizedToPersonId);
        }
    }

    private void validateSiblingParentCompatibility(UUID groupId, UUID personAId, UUID personBId) {
        Set<UUID> personAParents = loadParentIds(groupId, personAId);
        Set<UUID> personBParents = loadParentIds(groupId, personBId);

        Set<UUID> mergedParents = new HashSet<>(personAParents);
        mergedParents.addAll(personBParents);
        if (mergedParents.size() > MAX_PARENT_COUNT) {
            throw new RuntimeException("无法建立兄弟姐妹关系：双方父母信息合并后超过2位");
        }
    }

    private void validateParentLimitForChildAndSiblings(UUID groupId, UUID parentId, UUID childId) {
        ensureParentCapacity(groupId, childId, parentId);

        Set<UUID> siblingIds = loadSiblingIds(groupId, childId);
        for (UUID siblingId : siblingIds) {
            ensureParentCapacity(groupId, siblingId, parentId);
        }
    }

    private void ensureParentCapacity(UUID groupId, UUID childId, UUID parentId) {
        Set<UUID> parentIds = loadParentIds(groupId, childId);
        if (!parentIds.contains(parentId) && parentIds.size() >= MAX_PARENT_COUNT) {
            throw new RuntimeException("父母数量不能超过2位，关系同步失败");
        }
    }

    private Set<UUID> loadParentIds(UUID groupId, UUID childId) {
        return relationshipRepository
            .findByGroupIdAndToPersonIdAndType(groupId, childId, Relationship.RelationshipType.PARENT)
            .stream()
            .map(Relationship::getFromPersonId)
            .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<UUID> loadSiblingIds(UUID groupId, UUID personId) {
        return relationshipRepository.findByPersonId(groupId, personId).stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.SIBLING)
            .map(r -> r.getFromPersonId().equals(personId) ? r.getToPersonId() : r.getFromPersonId())
            .collect(Collectors.toCollection(HashSet::new));
    }

    private void synchronizeSiblingParents(UUID groupId, UUID personAId, UUID personBId) {
        Set<UUID> personAParents = loadParentIds(groupId, personAId);
        Set<UUID> personBParents = loadParentIds(groupId, personBId);

        createMissingParentLinks(groupId, personAParents, personBId);
        createMissingParentLinks(groupId, personBParents, personAId);
    }

    private void createMissingParentLinks(UUID groupId, Set<UUID> parentIds, UUID childId) {
        for (UUID parentId : parentIds) {
            if (parentId.equals(childId)) {
                continue;
            }

            ensureParentCapacity(groupId, childId, parentId);

            boolean exists = relationshipRepository
                .findByGroupIdAndFromPersonIdAndToPersonIdAndType(
                    groupId, parentId, childId, Relationship.RelationshipType.PARENT)
                .isPresent();
            if (exists) {
                continue;
            }

            relationshipRepository.save(Relationship.builder()
                .groupId(groupId)
                .fromPersonId(parentId)
                .toPersonId(childId)
                .type(Relationship.RelationshipType.PARENT)
                .build());
        }
    }

    private void synchronizeParentToSiblings(UUID groupId, UUID parentId, UUID childId) {
        Set<UUID> siblingIds = loadSiblingIds(groupId, childId);

        for (UUID siblingId : siblingIds) {
            createMissingParentLinks(groupId, Set.of(parentId), siblingId);
        }
    }

    private GroupDTO toDTO(Group group) {
        long memberCount = groupMemberRepository.countByGroupId(group.getId());
        long personCount = personRepository.countByGroupId(group.getId());
        
        String adminName = userRepository.findById(group.getAdminId())
            .map(User::getNickname)
            .orElse(null);
        
        return GroupDTO.builder()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription())
            .adminId(group.getAdminId())
            .adminName(adminName)
            .memberCount((int) memberCount)
            .personCount((int) personCount)
            .createdAt(group.getCreatedAt())
            .version(group.getVersion())
            .build();
    }
}
