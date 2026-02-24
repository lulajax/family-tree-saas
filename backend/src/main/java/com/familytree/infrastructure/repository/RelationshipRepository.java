package com.familytree.infrastructure.repository;

import com.familytree.domain.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, UUID> {
    
    List<Relationship> findByGroupId(UUID groupId);
    
    List<Relationship> findByFromPersonId(UUID fromPersonId);
    
    List<Relationship> findByToPersonId(UUID toPersonId);
    
    @Query("SELECT r FROM Relationship r WHERE r.groupId = :groupId AND " +
           "((r.fromPersonId = :person1Id AND r.toPersonId = :person2Id) OR " +
           "(r.fromPersonId = :person2Id AND r.toPersonId = :person1Id))")
    List<Relationship> findBetweenPersons(@Param("groupId") UUID groupId, 
                                          @Param("person1Id") UUID person1Id, 
                                          @Param("person2Id") UUID person2Id);
    
    @Query("SELECT r FROM Relationship r WHERE r.groupId = :groupId AND " +
           "(r.fromPersonId = :personId OR r.toPersonId = :personId)")
    List<Relationship> findByPersonId(@Param("groupId") UUID groupId, @Param("personId") UUID personId);
    
    Optional<Relationship> findByGroupIdAndFromPersonIdAndToPersonIdAndType(
        UUID groupId, UUID fromPersonId, UUID toPersonId, Relationship.RelationshipType type);
    
    long countByGroupId(UUID groupId);
}
