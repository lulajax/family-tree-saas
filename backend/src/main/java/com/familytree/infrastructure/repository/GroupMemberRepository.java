package com.familytree.infrastructure.repository;

import com.familytree.domain.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    List<GroupMember> findByGroupId(UUID groupId);
    
    List<GroupMember> findByUserId(UUID userId);
    
    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);
    
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
    
    boolean existsByGroupIdAndUserIdAndRole(UUID groupId, UUID userId, GroupMember.Role role);
    
    long countByGroupId(UUID groupId);
}
