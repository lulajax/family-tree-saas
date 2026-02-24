package com.familytree.infrastructure.repository;

import com.familytree.domain.GroupPrivacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupPrivacySettingsRepository extends JpaRepository<GroupPrivacySettings, UUID> {
    
    Optional<GroupPrivacySettings> findByGroupId(UUID groupId);
    
    boolean existsByGroupId(UUID groupId);
}
