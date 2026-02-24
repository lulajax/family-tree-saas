package com.familytree.infrastructure.repository;

import com.familytree.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    
    Optional<Workspace> findByGroupIdAndUserId(UUID groupId, UUID userId);
    
    List<Workspace> findByGroupId(UUID groupId);
    
    List<Workspace> findByUserId(UUID userId);
    
    @Query("SELECT w FROM Workspace w WHERE w.groupId = :groupId AND w.status = :status")
    List<Workspace> findByGroupIdAndStatus(@Param("groupId") UUID groupId, @Param("status") Workspace.Status status);
    
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
    
    @Query("SELECT w FROM Workspace w WHERE w.groupId = :groupId AND w.userId = :userId AND w.status IN ('EDITING', 'CONFLICT')")
    Optional<Workspace> findActiveWorkspace(@Param("groupId") UUID groupId, @Param("userId") UUID userId);
}
