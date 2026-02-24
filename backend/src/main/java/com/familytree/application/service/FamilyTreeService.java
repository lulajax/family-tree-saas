package com.familytree.application.service;

import com.familytree.application.dto.PersonNodeDTO;
import com.familytree.application.dto.RelationshipEdgeDTO;
import com.familytree.application.dto.TreeViewDTO;
import com.familytree.domain.Person;
import com.familytree.domain.Relationship;
import com.familytree.infrastructure.repository.PersonRepository;
import com.familytree.infrastructure.repository.PhotoRepository;
import com.familytree.infrastructure.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyTreeService {
    
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final PhotoRepository photoRepository;
    
    @Transactional(readOnly = true)
    public TreeViewDTO getTreeView(UUID groupId, UUID focusPersonId, int depth) {
        // 如果没有指定焦点人物，获取该家族第一个人物
        if (focusPersonId == null) {
            List<Person> persons = personRepository.findByGroupId(groupId);
            if (persons.isEmpty()) {
                return TreeViewDTO.builder()
                    .groupId(groupId)
                    .nodes(new ArrayList<>())
                    .edges(new ArrayList<>())
                    .build();
            }
            focusPersonId = persons.get(0).getId();
        }
        
        Person focusPerson = personRepository.findById(focusPersonId)
            .orElseThrow(() -> new RuntimeException("焦点人物不存在"));
        
        // 获取所有相关人物和关系
        Set<UUID> personIds = new HashSet<>();
        personIds.add(focusPersonId);
        
        // 获取所有关系
        List<Relationship> allRelationships = relationshipRepository.findByGroupId(groupId);
        
        // BFS获取指定深度内的人物
        Map<UUID, Integer> personDepth = new HashMap<>();
        personDepth.put(focusPersonId, 0);
        
        Queue<UUID> queue = new LinkedList<>();
        queue.offer(focusPersonId);
        
        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();
            int currentDepth = personDepth.get(currentId);
            
            if (currentDepth >= depth) {
                continue;
            }
            
            // 找到所有相关关系
            for (Relationship rel : allRelationships) {
                UUID relatedId = null;
                if (rel.getFromPersonId().equals(currentId)) {
                    relatedId = rel.getToPersonId();
                } else if (rel.getToPersonId().equals(currentId)) {
                    relatedId = rel.getFromPersonId();
                }
                
                if (relatedId != null && !personDepth.containsKey(relatedId)) {
                    personDepth.put(relatedId, currentDepth + 1);
                    personIds.add(relatedId);
                    queue.offer(relatedId);
                }
            }
        }
        
        // 获取所有人物信息
        List<Person> persons = personRepository.findAllById(personIds);
        Map<UUID, Person> personMap = persons.stream()
            .collect(Collectors.toMap(Person::getId, p -> p));
        
        // 构建节点
        List<PersonNodeDTO> nodes = persons.stream()
            .map(p -> toNodeDTO(p, personDepth.getOrDefault(p.getId(), 0)))
            .collect(Collectors.toList());
        
        // 构建边（只包含在人物集合内的关系）
        List<RelationshipEdgeDTO> edges = allRelationships.stream()
            .filter(r -> personIds.contains(r.getFromPersonId()) && personIds.contains(r.getToPersonId()))
            .map(this::toEdgeDTO)
            .collect(Collectors.toList());
        
        return TreeViewDTO.builder()
            .focusPersonId(focusPersonId)
            .focusPersonName(focusPerson.getFullName())
            .depth(depth)
            .nodes(nodes)
            .edges(edges)
            .build();
    }
    
    @Transactional(readOnly = true)
    public List<PersonNodeDTO> getAncestors(UUID personId, int generations) {
        List<Person> ancestors = personRepository.findAncestorsWithPath(personId);
        
        return ancestors.stream()
            .map(p -> toNodeDTO(p, 0))
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PersonNodeDTO> getDescendants(UUID personId, int generations) {
        List<Person> descendants = personRepository.findDescendants(personId, generations);
        
        return descendants.stream()
            .map(p -> toNodeDTO(p, 0))
            .collect(Collectors.toList());
    }
    
    private PersonNodeDTO toNodeDTO(Person person, int generation) {
        String primaryPhotoUrl = photoRepository.findByPersonIdAndIsPrimaryTrue(person.getId())
            .map(Photo::getUrl)
            .orElse(null);
        
        return PersonNodeDTO.builder()
            .id(person.getId())
            .firstName(person.getFirstName())
            .lastName(person.getLastName())
            .fullName(person.getFullName())
            .gender(person.getGender())
            .birthDate(person.getBirthDate())
            .deathDate(person.getDeathDate())
            .primaryPhotoUrl(primaryPhotoUrl)
            .generation(generation)
            .build();
    }
    
    private RelationshipEdgeDTO toEdgeDTO(Relationship relationship) {
        return RelationshipEdgeDTO.builder()
            .id(relationship.getId())
            .fromPersonId(relationship.getFromPersonId())
            .toPersonId(relationship.getToPersonId())
            .type(relationship.getType())
            .build();
    }
}
