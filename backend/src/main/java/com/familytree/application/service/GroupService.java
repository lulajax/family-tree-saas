package com.familytree.application.service;

import com.familytree.application.dto.GroupDTO;
import com.familytree.application.dto.request.CreateGroupRequest;
import com.familytree.domain.*;
import com.familytree.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {
    
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPrivacySettingsRepository privacySettingsRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    
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
