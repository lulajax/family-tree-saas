package com.familytree.infrastructure.repository;

import com.familytree.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    
    List<Photo> findByPersonId(UUID personId);
    
    List<Photo> findByPersonIdOrderByCreatedAtDesc(UUID personId);
    
    Optional<Photo> findByPersonIdAndIsPrimaryTrue(UUID personId);
    
    List<Photo> findByUploaderId(UUID uploaderId);
    
    long countByPersonId(UUID personId);
}
