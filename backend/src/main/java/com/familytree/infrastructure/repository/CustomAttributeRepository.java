package com.familytree.infrastructure.repository;

import com.familytree.domain.CustomAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomAttributeRepository extends JpaRepository<CustomAttribute, UUID> {
    
    List<CustomAttribute> findByPersonId(UUID personId);
    
    Optional<CustomAttribute> findByPersonIdAndKey(UUID personId, String key);
    
    void deleteByPersonId(UUID personId);
}
