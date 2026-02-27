package com.familytree.application.service;

import com.familytree.domain.LineageType;
import com.familytree.domain.Person;
import com.familytree.domain.Relationship;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 血统线计算器
 * 用于计算家族成员相对于焦点人物的血统线（父系/母系）
 */
@Component
public class LineageCalculator {

    /**
     * 计算每个人物相对于焦点人物的血统线
     *
     * @param focusPersonId 焦点人物ID
     * @param relationships 家族内所有关系
     * @param genderMap     人物ID到性别的映射
     * @return 人物ID到血统线类型的映射
     */
    public Map<UUID, LineageType> calculateLineageMap(
            UUID focusPersonId,
            List<Relationship> relationships,
            Map<UUID, Person.Gender> genderMap) {

        Map<UUID, LineageType> lineageMap = new HashMap<>();
        lineageMap.put(focusPersonId, LineageType.SELF);

        // 找出焦点人物的父亲和母亲
        UUID fatherId = findFatherId(focusPersonId, relationships, genderMap);
        UUID motherId = findMotherId(focusPersonId, relationships, genderMap);

        // BFS 标记父系
        if (fatherId != null) {
            markLineageBFS(fatherId, LineageType.FATHER_LINE, relationships, lineageMap);
        }

        // BFS 标记母系
        if (motherId != null) {
            markLineageBFS(motherId, LineageType.MOTHER_LINE, relationships, lineageMap);
        }

        return lineageMap;
    }

    /**
     * BFS 遍历标记血统线
     */
    private void markLineageBFS(
            UUID startPersonId,
            LineageType lineageType,
            List<Relationship> relationships,
            Map<UUID, LineageType> lineageMap) {

        Queue<UUID> queue = new LinkedList<>();
        queue.offer(startPersonId);
        lineageMap.put(startPersonId, lineageType);

        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();

            // 找到所有与该人物相关的关系
            for (Relationship rel : relationships) {
                UUID relatedId = null;

                if (rel.getFromPersonId().equals(currentId)) {
                    relatedId = rel.getToPersonId();
                } else if (rel.getToPersonId().equals(currentId)) {
                    relatedId = rel.getFromPersonId();
                }

                if (relatedId != null && !lineageMap.containsKey(relatedId)) {
                    lineageMap.put(relatedId, lineageType);
                    queue.offer(relatedId);
                }
            }
        }
    }

    /**
     * 查找父亲ID（通过性别判断）
     *
     * @param personId      人物ID
     * @param relationships 关系列表
     * @param genderMap     性别映射
     * @return 父亲ID，未找到返回null
     */
    public UUID findFatherId(UUID personId, List<Relationship> relationships, Map<UUID, Person.Gender> genderMap) {
        return findParentByGender(personId, relationships, genderMap, Person.Gender.MALE);
    }

    /**
     * 查找母亲ID（通过性别判断）
     *
     * @param personId      人物ID
     * @param relationships 关系列表
     * @param genderMap     性别映射
     * @return 母亲ID，未找到返回null
     */
    public UUID findMotherId(UUID personId, List<Relationship> relationships, Map<UUID, Person.Gender> genderMap) {
        return findParentByGender(personId, relationships, genderMap, Person.Gender.FEMALE);
    }

    /**
     * 根据性别查找父母
     */
    private UUID findParentByGender(
            UUID personId,
            List<Relationship> relationships,
            Map<UUID, Person.Gender> genderMap,
            Person.Gender targetGender) {

        for (Relationship rel : relationships) {
            // PARENT 关系：from = 父母, to = 子女
            if (rel.getType() == Relationship.RelationshipType.PARENT &&
                rel.getToPersonId().equals(personId)) {

                UUID parentId = rel.getFromPersonId();
                Person.Gender parentGender = genderMap.get(parentId);

                if (parentGender == targetGender) {
                    return parentId;
                }
            }
            // CHILD 关系：from = 子女, to = 父母（兼容历史数据）
            else if (rel.getType() == Relationship.RelationshipType.CHILD &&
                     rel.getFromPersonId().equals(personId)) {

                UUID parentId = rel.getToPersonId();
                Person.Gender parentGender = genderMap.get(parentId);

                if (parentGender == targetGender) {
                    return parentId;
                }
            }
        }
        return null;
    }

    /**
     * 按血统线过滤人物ID集合
     *
     * @param personIds     原人物ID集合
     * @param lineageMap    血统线映射
     * @param filterType    筛选类型（null或BOTH表示不过滤）
     * @return 过滤后的人物ID集合
     */
    public Set<UUID> filterByLineage(
            Set<UUID> personIds,
            Map<UUID, LineageType> lineageMap,
            LineageType filterType) {

        if (filterType == null) {
            return personIds;
        }

        return personIds.stream()
            .filter(id -> {
                LineageType lineage = lineageMap.getOrDefault(id, LineageType.UNKNOWN);
                return lineage == filterType || lineage == LineageType.SELF;
            })
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 判断一条边是否应该显示（两端节点都在筛选结果中）
     *
     * @param relationship 关系
     * @param filteredIds  过滤后的人物ID集合
     * @return 是否应该显示
     */
    public boolean shouldShowEdge(Relationship relationship, Set<UUID> filteredIds) {
        return filteredIds.contains(relationship.getFromPersonId()) &&
               filteredIds.contains(relationship.getToPersonId());
    }

    /**
     * 获取关系的血统线类型
     * 如果两端同属一个血统线，返回该血统线；否则返回null
     *
     * @param relationship 关系
     * @param lineageMap   血统线映射
     * @return 关系的血统线类型
     */
    public LineageType getEdgeLineageType(Relationship relationship, Map<UUID, LineageType> lineageMap) {
        LineageType fromLineage = lineageMap.getOrDefault(relationship.getFromPersonId(), LineageType.UNKNOWN);
        LineageType toLineage = lineageMap.getOrDefault(relationship.getToPersonId(), LineageType.UNKNOWN);

        // 如果两端都是同一条血统线，返回该血统线
        if (fromLineage == toLineage && fromLineage != LineageType.UNKNOWN) {
            return fromLineage;
        }

        // 如果一端是本人，另一端有血统线，返回另一端的血统线
        if (fromLineage == LineageType.SELF && toLineage != LineageType.UNKNOWN) {
            return toLineage;
        }
        if (toLineage == LineageType.SELF && fromLineage != LineageType.UNKNOWN) {
            return fromLineage;
        }

        // 其他情况（跨血统线的关系）返回null
        return null;
    }
}
