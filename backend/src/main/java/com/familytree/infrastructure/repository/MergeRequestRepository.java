package com.familytree.infrastructure.repository;

import com.familytree.domain.MergeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MergeRequestRepository extends JpaRepository<MergeRequest, UUID> {
    
    List<MergeRequest> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
    
    List<MergeRequest> findByGroupIdAndStatus(UUID groupId, MergeRequest.Status status);
    
    List<MergeRequest> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);
    
    Optional<MergeRequest> findByWorkspaceId(UUID workspaceId);
    
    @Query("SELECT mr FROM MergeRequest mr WHERE mr.groupId = :groupId AND mr.status = 'PENDING'")
    List<MergeRequest> findPendingByGroupId(@Param("groupId") UUID groupId);
    
    long countByGroupIdAndStatus(UUID groupId, MergeRequest.Status status);
}
