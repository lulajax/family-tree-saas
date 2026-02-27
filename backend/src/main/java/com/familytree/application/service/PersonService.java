package com.familytree.application.service;

import com.familytree.application.dto.PersonDTO;
import com.familytree.application.dto.PersonRelationsDTO;
import com.familytree.application.dto.PhotoDTO;
import com.familytree.application.dto.request.CreatePersonRequest;
import com.familytree.application.dto.request.UpdatePersonRequest;
import com.familytree.domain.Person;
import com.familytree.domain.Photo;
import com.familytree.domain.Relationship;
import com.familytree.infrastructure.repository.GroupMemberRepository;
import com.familytree.infrastructure.repository.PersonRepository;
import com.familytree.infrastructure.repository.PhotoRepository;
import com.familytree.infrastructure.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonService {
    
    private final PersonRepository personRepository;
    private final PhotoRepository photoRepository;
    private final RelationshipRepository relationshipRepository;
    private final GroupMemberRepository groupMemberRepository;
    
    @Transactional
    public PersonDTO createPerson(UUID userId, CreatePersonRequest request) {
        Person person = Person.builder()
            .groupId(request.getGroupId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .gender(request.getGender())
            .birthDate(request.getBirthDate())
            .deathDate(request.getDeathDate())
            .birthPlace(request.getBirthPlace())
            .currentSpouseId(request.getCurrentSpouseId())
            .createdBy(userId)
            .version(0)
            .build();
        
        person = personRepository.save(person);
        return toDTO(person);
    }
    
    @Transactional(readOnly = true)
    public PersonDTO getPerson(UUID personId) {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("人物不存在"));
        return toDTO(person);
    }
    
    @Transactional(readOnly = true)
    public List<PersonDTO> getGroupPersons(UUID groupId) {
        List<Person> persons = personRepository.findByGroupId(groupId);
        return persons.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public PersonDTO updatePerson(UUID personId, UpdatePersonRequest request) {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("人物不存在"));
        
        if (request.getFirstName() != null) {
            person.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            person.setLastName(request.getLastName());
        }
        if (request.getGender() != null) {
            person.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            person.setBirthDate(request.getBirthDate());
        }
        if (request.getDeathDate() != null) {
            person.setDeathDate(request.getDeathDate());
        }
        if (request.getBirthPlace() != null) {
            person.setBirthPlace(request.getBirthPlace());
        }
        if (request.getCurrentSpouseId() != null) {
            person.setCurrentSpouseId(request.getCurrentSpouseId());
        }
        
        person = personRepository.save(person);
        return toDTO(person);
    }
    
    @Transactional
    public void deletePerson(UUID personId) {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("人物不存在"));
        
        // 删除相关关系
        List<Relationship> relationships = relationshipRepository.findByPersonId(person.getGroupId(), personId);
        relationshipRepository.deleteAll(relationships);
        
        // 删除相关照片
        List<Photo> photos = photoRepository.findByPersonId(personId);
        photoRepository.deleteAll(photos);
        
        personRepository.delete(person);
    }
    
    @Transactional(readOnly = true)
    public List<PersonDTO> searchPersons(UUID groupId, String keyword) {
        List<Person> persons = personRepository.searchByGroupIdAndName(groupId, keyword);
        return persons.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    private PersonDTO toDTO(Person person) {
        Photo primaryPhoto = photoRepository.findByPersonIdAndIsPrimaryTrue(person.getId()).orElse(null);
        
        List<PhotoDTO> photos = photoRepository.findByPersonId(person.getId()).stream()
            .map(this::toPhotoDTO)
            .collect(Collectors.toList());
        
        // 获取关系
        List<Relationship> relationships = relationshipRepository.findByPersonId(person.getGroupId(), person.getId());
        
        List<UUID> parentIds = relationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.PARENT && r.getToPersonId().equals(person.getId()))
            .map(Relationship::getFromPersonId)
            .distinct()
            .collect(Collectors.toList());
        
        List<UUID> childrenIds = relationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.PARENT && r.getFromPersonId().equals(person.getId()))
            .map(Relationship::getToPersonId)
            .distinct()
            .collect(Collectors.toList());
        
        List<UUID> spouseIds = relationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.SPOUSE)
            .map(r -> r.getFromPersonId().equals(person.getId()) ? r.getToPersonId() : r.getFromPersonId())
            .distinct()
            .collect(Collectors.toList());

        List<UUID> siblingIds = resolveSiblingIds(person.getGroupId(), person.getId(), parentIds, relationships);
        
        String currentSpouseName = null;
        if (person.getCurrentSpouseId() != null) {
            currentSpouseName = personRepository.findById(person.getCurrentSpouseId())
                .map(Person::getFullName)
                .orElse(null);
        }
        
        return PersonDTO.builder()
            .id(person.getId())
            .groupId(person.getGroupId())
            .firstName(person.getFirstName())
            .lastName(person.getLastName())
            .fullName(person.getFullName())
            .gender(person.getGender())
            .birthDate(person.getBirthDate())
            .deathDate(person.getDeathDate())
            .birthPlace(person.getBirthPlace())
            .currentSpouseId(person.getCurrentSpouseId())
            .currentSpouseName(currentSpouseName)
            .primaryPhotoUrl(primaryPhoto != null ? primaryPhoto.getUrl() : null)
            .photos(photos)
            .parentIds(parentIds)
            .childrenIds(childrenIds)
            .spouseIds(spouseIds)
            .siblingIds(siblingIds)
            .createdAt(person.getCreatedAt())
            .updatedAt(person.getUpdatedAt())
            .version(person.getVersion())
            .build();
    }
    
    private PhotoDTO toPhotoDTO(Photo photo) {
        return PhotoDTO.builder()
            .id(photo.getId())
            .personId(photo.getPersonId())
            .url(photo.getUrl())
            .description(photo.getDescription())
            .takenAt(photo.getTakenAt())
            .isPrimary(photo.getIsPrimary())
            .createdAt(photo.getCreatedAt())
            .build();
    }

    /**
     * 获取人员关系详情
     * 返回某人的父母、配偶、子女、兄弟姐妹列表（包含完整信息）
     */
    @Transactional(readOnly = true)
    public PersonRelationsDTO getPersonRelations(UUID personId, UUID currentUserId) {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("人物不存在"));

        // 检查用户是否有权限查看该家族的人员（必须是家族成员）
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(person.getGroupId(), currentUserId);
        if (!isMember) {
            throw new RuntimeException("您没有权限查看此人员信息");
        }

        // 获取该人物的所有关系
        List<Relationship> relationships = relationshipRepository.findByPersonId(person.getGroupId(), personId);

        // 获取父母ID
        List<UUID> parentIds = relationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.PARENT && r.getToPersonId().equals(personId))
            .map(Relationship::getFromPersonId)
            .distinct()
            .collect(Collectors.toList());

        // 获取子女ID
        List<UUID> childrenIds = relationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.PARENT && r.getFromPersonId().equals(personId))
            .map(Relationship::getToPersonId)
            .distinct()
            .collect(Collectors.toList());

        // 获取配偶ID
        List<UUID> spouseIds = relationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.SPOUSE)
            .map(r -> r.getFromPersonId().equals(personId) ? r.getToPersonId() : r.getFromPersonId())
            .distinct()
            .collect(Collectors.toList());

        List<UUID> siblingIds = resolveSiblingIds(person.getGroupId(), personId, parentIds, relationships);

        // 构建返回DTO
        Photo primaryPhoto = photoRepository.findByPersonIdAndIsPrimaryTrue(personId).orElse(null);

        return PersonRelationsDTO.builder()
            .personId(personId)
            .personName(person.getFullName())
            .primaryPhotoUrl(primaryPhoto != null ? primaryPhoto.getUrl() : null)
            .parents(toSummaryList(parentIds, "PARENT"))
            .spouses(toSummaryList(spouseIds, "SPOUSE"))
            .children(toSummaryList(childrenIds, "CHILD"))
            .siblings(toSummaryList(siblingIds, "SIBLING"))
            .build();
    }

    /**
     * 将人员ID列表转换为摘要DTO列表
     */
    private List<PersonRelationsDTO.PersonSummaryDTO> toSummaryList(List<UUID> personIds, String relationType) {
        return personIds.stream()
            .map(id -> personRepository.findById(id).orElse(null))
            .filter(p -> p != null)
            .map(p -> {
                Photo photo = photoRepository.findByPersonIdAndIsPrimaryTrue(p.getId()).orElse(null);
                return PersonRelationsDTO.PersonSummaryDTO.builder()
                    .id(p.getId())
                    .fullName(p.getFullName())
                    .firstName(p.getFirstName())
                    .lastName(p.getLastName())
                    .gender(p.getGender() != null ? p.getGender().name() : "UNKNOWN")
                    .birthDate(p.getBirthDate() != null ? p.getBirthDate().toString() : null)
                    .deathDate(p.getDeathDate() != null ? p.getDeathDate().toString() : null)
                    .primaryPhotoUrl(photo != null ? photo.getUrl() : null)
                    .relationType(relationType)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private List<UUID> resolveSiblingIds(
            UUID groupId,
            UUID personId,
            List<UUID> parentIds,
            List<Relationship> directRelationships) {

        Set<UUID> siblingIds = new LinkedHashSet<>();

        // 显式兄弟姐妹关系
        directRelationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.SIBLING)
            .map(r -> r.getFromPersonId().equals(personId) ? r.getToPersonId() : r.getFromPersonId())
            .filter(id -> !id.equals(personId))
            .forEach(siblingIds::add);

        // 通过共同父母推导兄弟姐妹
        for (UUID parentId : parentIds) {
            relationshipRepository.findByGroupIdAndFromPersonIdAndType(
                    groupId, parentId, Relationship.RelationshipType.PARENT)
                .stream()
                .map(Relationship::getToPersonId)
                .filter(id -> !id.equals(personId))
                .forEach(siblingIds::add);
        }

        return new ArrayList<>(siblingIds);
    }
}
