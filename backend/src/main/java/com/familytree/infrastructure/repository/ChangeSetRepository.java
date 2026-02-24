package com.familytree.infrastructure.repository;

import com.familytree.domain.ChangeSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeSetRepository extends JpaRepository<ChangeSet, UUID> {
    
    List<ChangeSet> findByWorkspaceIdOrderBySequenceNumberAsc(UUID workspaceId);
    
    List<ChangeSet> findByWorkspaceIdOrderBySequenceNumberDesc(UUID workspaceId);
    
    @Query("SELECT c FROM ChangeSet c WHERE c.workspaceId = :workspaceId AND c.entityId = :entityId ORDER BY c.sequenceNumber DESC")
    List<ChangeSet> findByWorkspaceIdAndEntityIdOrderBySequenceDesc(
        @Param("workspaceId") UUID workspaceId, 
        @Param("entityId") UUID entityId);
    
    @Query("SELECT MAX(c.sequenceNumber) FROM ChangeSet c WHERE c.workspaceId = :workspaceId")
    Optional<Integer> findMaxSequenceNumberByWorkspaceId(@Param("workspaceId") UUID workspaceId);
    
    @Query("SELECT c FROM ChangeSet c WHERE c.workspaceId = :workspaceId AND c.entityType = :entityType")
    List<ChangeSet> findByWorkspaceIdAndEntityType(
        @Param("workspaceId") UUID workspaceId, 
        @Param("entityType") ChangeSet.EntityType entityType);
    
    long countByWorkspaceId(UUID workspaceId);
    
    void deleteByWorkspaceId(UUID workspaceId);
}
