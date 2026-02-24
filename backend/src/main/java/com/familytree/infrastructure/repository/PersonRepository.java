package com.familytree.infrastructure.repository;

import com.familytree.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {
    
    List<Person> findByGroupId(UUID groupId);
    
    @Query("SELECT p FROM Person p WHERE p.groupId = :groupId AND (p.firstName ILIKE %:keyword% OR p.lastName ILIKE %:keyword%)")
    List<Person> searchByGroupIdAndName(@Param("groupId") UUID groupId, @Param("keyword") String keyword);
    
    @Query(value = """
        WITH RECURSIVE ancestors AS (
            SELECT p.*, 0 as depth, ARRAY[p.id] as path
            FROM persons p
            WHERE p.id = :personId
            UNION ALL
            SELECT p.*, a.depth + 1, a.path || p.id
            FROM persons p
            JOIN relationships r ON p.id = r.from_person_id
            JOIN ancestors a ON r.to_person_id = a.id
            WHERE r.type = 'PARENT'
            AND NOT p.id = ANY(a.path)
        )
        SELECT * FROM ancestors ORDER BY depth
        """, nativeQuery = true)
    List<Person> findAncestorsWithPath(@Param("personId") UUID personId);
    
    @Query(value = """
        WITH RECURSIVE descendants AS (
            SELECT p.*, 0 as depth
            FROM persons p
            WHERE p.id = :personId
            UNION ALL
            SELECT p.*, d.depth + 1
            FROM persons p
            JOIN relationships r ON p.id = r.to_person_id
            JOIN descendants d ON r.from_person_id = d.id
            WHERE r.type = 'PARENT'
            AND d.depth < :maxDepth
        )
        SELECT * FROM descendants WHERE depth > 0
        """, nativeQuery = true)
    List<Person> findDescendants(@Param("personId") UUID personId, @Param("maxDepth") int maxDepth);
    
    long countByGroupId(UUID groupId);
}
