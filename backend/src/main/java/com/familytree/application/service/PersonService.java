package com.familytree.application.service;

import com.familytree.application.dto.PersonDTO;
import com.familytree.application.dto.PhotoDTO;
import com.familytree.application.dto.request.CreatePersonRequest;
import com.familytree.application.dto.request.UpdatePersonRequest;
import com.familytree.domain.Person;
import com.familytree.domain.Photo;
import com.familytree.domain.Relationship;
import com.familytree.infrastructure.repository.PersonRepository;
import com.familytree.infrastructure.repository.PhotoRepository;
import com.familytree.infrastructure.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonService {
    
    private final PersonRepository personRepository;
    private final PhotoRepository photoRepository;
    private final RelationshipRepository relationshipRepository;
    
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
            .collect(Collectors.toList());
        
        List<UUID> childrenIds = relationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.PARENT && r.getFromPersonId().equals(person.getId()))
            .map(Relationship::getToPersonId)
            .collect(Collectors.toList());
        
        List<UUID> spouseIds = relationships.stream()
            .filter(r -> r.getType() == Relationship.RelationshipType.SPOUSE)
            .map(r -> r.getFromPersonId().equals(person.getId()) ? r.getToPersonId() : r.getFromPersonId())
            .collect(Collectors.toList());
        
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
}
