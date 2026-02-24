package com.familytree.application.service;

import com.familytree.application.dto.PersonNodeDTO;
import com.familytree.application.dto.RelationshipEdgeDTO;
import com.familytree.application.dto.TreeViewDTO;
import com.familytree.domain.Person;
import com.familytree.domain.Photo;
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

        // BFS获取指定深度内的人物（distance 用于截断，generation 用于代际显示）
        Map<UUID, Integer> personDistance = new HashMap<>();
        Map<UUID, Integer> personGeneration = new HashMap<>();
        personDistance.put(focusPersonId, 0);
        personGeneration.put(focusPersonId, 0);

        Queue<UUID> queue = new LinkedList<>();
        queue.offer(focusPersonId);

        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();
            int currentDistance = personDistance.get(currentId);
            int currentGeneration = personGeneration.getOrDefault(currentId, 0);

            if (currentDistance >= depth) {
                continue;
            }

            // 找到所有相关关系
            for (Relationship rel : allRelationships) {
                RelationStep step = resolveRelationStep(currentId, rel);
                if (step == null) {
                    continue;
                }

                UUID relatedId = step.relatedPersonId();
                if (!personDistance.containsKey(relatedId)) {
                    personDistance.put(relatedId, currentDistance + 1);
                    personGeneration.put(relatedId, currentGeneration + step.generationDelta());
                    personIds.add(relatedId);
                    queue.offer(relatedId);
                }
            }
        }

        // 获取所有人物信息
        List<Person> persons = personRepository.findAllById(personIds);

        // 按代分组并排序（用于布局计算）
        Map<Integer, List<Person>> personsByGeneration = persons.stream()
            .collect(Collectors.groupingBy(p -> personGeneration.getOrDefault(p.getId(), 0)));

        // 计算节点坐标
        Map<UUID, double[]> coordinates = calculateTreeLayout(personsByGeneration, personGeneration, allRelationships);

        // 构建节点（包含坐标）
        List<PersonNodeDTO> nodes = persons.stream()
            .map(p -> toNodeDTO(p, personGeneration.getOrDefault(p.getId(), 0), coordinates.getOrDefault(p.getId(), new double[]{0, 0})))
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

    /**
     * 计算树形布局坐标
     * 使用分层布局：祖先在上，后代在下
     *
     * @param personsByGeneration 按代分组的人物
     * @param personGeneration    人物代际映射
     * @param relationships       所有关系
     * @return 人物ID到坐标的映射 [x, y]
     */
    private Map<UUID, double[]> calculateTreeLayout(
            Map<Integer, List<Person>> personsByGeneration,
            Map<UUID, Integer> personGeneration,
            List<Relationship> relationships) {

        Map<UUID, double[]> coordinates = new HashMap<>();

        // 水平间距和垂直间距
        final double HORIZONTAL_SPACING = 120.0;
        final double VERTICAL_SPACING = 150.0;

        // 找到最大深度
        int maxDepth = personGeneration.values().stream().max(Integer::compare).orElse(0);
        int minDepth = personGeneration.values().stream().min(Integer::compare).orElse(0);

        // 构建关系映射（用于查找父子关系）
        Map<UUID, List<UUID>> parentToChildren = new HashMap<>();
        Map<UUID, List<UUID>> childToParents = new HashMap<>();

        for (Relationship rel : relationships) {
            if (rel.getType() == Relationship.RelationshipType.PARENT) {
                // PARENT 关系：from = 父母, to = 子女
                parentToChildren.computeIfAbsent(rel.getFromPersonId(), k -> new ArrayList<>())
                    .add(rel.getToPersonId());
                childToParents.computeIfAbsent(rel.getToPersonId(), k -> new ArrayList<>())
                    .add(rel.getFromPersonId());
            }
        }

        // 从最深的一代开始向上计算位置
        for (int gen = maxDepth; gen >= minDepth; gen--) {
            List<Person> generationPersons = personsByGeneration.getOrDefault(gen, new ArrayList<>());

            if (generationPersons.isEmpty()) {
                continue;
            }

            // 按出生日期排序（年龄大的在左）
            generationPersons.sort(Comparator
                .comparing(Person::getBirthDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Person::getId));

            int count = generationPersons.size();

            for (int i = 0; i < count; i++) {
                Person person = generationPersons.get(i);
                double x, y;

                // 计算Y坐标（基于generation，焦点人物generation=0）
                y = gen * VERTICAL_SPACING;

                // 计算X坐标
                List<UUID> children = parentToChildren.getOrDefault(person.getId(), new ArrayList<>());
                List<UUID> parents = childToParents.getOrDefault(person.getId(), new ArrayList<>());

                if (!children.isEmpty()) {
                    // 有子女：X坐标为子女们的中点
                    double childrenXSum = 0;
                    int validChildren = 0;
                    for (UUID childId : children) {
                        if (coordinates.containsKey(childId)) {
                            childrenXSum += coordinates.get(childId)[0];
                            validChildren++;
                        }
                    }
                    if (validChildren > 0) {
                        x = childrenXSum / validChildren;
                    } else {
                        x = (i - (count - 1) / 2.0) * HORIZONTAL_SPACING;
                    }
                } else if (!parents.isEmpty() && coordinates.containsKey(parents.get(0))) {
                    // 有父母但没有子女：在父母附近偏移
                    double parentX = coordinates.get(parents.get(0))[0];
                    // 根据在父母列表中的索引决定左右偏移
                    int parentIndex = parents.indexOf(person.getId());
                    double offset = (parentIndex % 2 == 0 ? 1 : -1) * (parentIndex + 1) * HORIZONTAL_SPACING / 2;
                    x = parentX + offset;
                } else {
                    // 没有关联关系：均匀分布
                    x = (i - (count - 1) / 2.0) * HORIZONTAL_SPACING;
                }

                coordinates.put(person.getId(), new double[]{x, y});
            }

            // 处理同代人物之间的重叠（简单的防重叠调整）
            adjustOverlappingNodes(generationPersons, coordinates, HORIZONTAL_SPACING);
        }

        return coordinates;
    }

    /**
     * 调整同代人物之间的重叠
     */
    private void adjustOverlappingNodes(List<Person> persons, Map<UUID, double[]> coordinates, double minSpacing) {
        if (persons.size() <= 1) return;

        // 按X坐标排序
        List<Person> sorted = new ArrayList<>(persons);
        sorted.sort(Comparator.comparingDouble(p -> coordinates.get(p.getId())[0]));

        // 检测并调整重叠
        for (int i = 1; i < sorted.size(); i++) {
            Person prev = sorted.get(i - 1);
            Person curr = sorted.get(i);

            double prevX = coordinates.get(prev.getId())[0];
            double currX = coordinates.get(curr.getId())[0];

            if (currX - prevX < minSpacing) {
                double adjust = (minSpacing - (currX - prevX)) / 2 + 5; // 5px 额外间隔
                coordinates.get(prev.getId())[0] -= adjust;
                coordinates.get(curr.getId())[0] += adjust;
            }
        }
    }
    
    @Transactional(readOnly = true)
    public List<PersonNodeDTO> getAncestors(UUID personId, int generations) {
        List<Person> ancestors = personRepository.findAncestorsWithPath(personId);

        return ancestors.stream()
            .map(p -> toNodeDTO(p, 0, new double[]{0, 0}))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonNodeDTO> getDescendants(UUID personId, int generations) {
        List<Person> descendants = personRepository.findDescendants(personId, generations);

        return descendants.stream()
            .map(p -> toNodeDTO(p, 0, new double[]{0, 0}))
            .collect(Collectors.toList());
    }
    
    private PersonNodeDTO toNodeDTO(Person person, int generation, double[] coords) {
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
            .x(coords[0])
            .y(coords[1])
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

    /**
     * 解析 currentId 在一条关系边上的“相邻节点 + 代际变化”。
     * 代际定义：焦点人物为 0，父母 -1，子女 +1，配偶/兄弟姐妹 0。
     */
    private RelationStep resolveRelationStep(UUID currentId, Relationship rel) {
        boolean isFrom = rel.getFromPersonId().equals(currentId);
        boolean isTo = rel.getToPersonId().equals(currentId);
        if (!isFrom && !isTo) {
            return null;
        }

        UUID relatedId = isFrom ? rel.getToPersonId() : rel.getFromPersonId();
        int delta = 0;

        if (rel.getType() == Relationship.RelationshipType.PARENT) {
            // PARENT: from=父母, to=子女
            delta = isFrom ? 1 : -1;
        } else if (rel.getType() == Relationship.RelationshipType.CHILD) {
            // CHILD: from=子女, to=父母（兼容历史数据）
            delta = isFrom ? -1 : 1;
        }

        return new RelationStep(relatedId, delta);
    }

    private record RelationStep(UUID relatedPersonId, int generationDelta) {}
}
