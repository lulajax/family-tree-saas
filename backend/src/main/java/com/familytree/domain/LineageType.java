package com.familytree.domain;

/**
 * 血统线类型
 * 用于区分父系和母系亲属关系
 */
public enum LineageType {
    FATHER_LINE,   // 父系（通过父亲路径可达的亲属）
    MOTHER_LINE,   // 母系（通过母亲路径可达的亲属）
    SELF,          // 本人（焦点人物）
    UNKNOWN        // 未知（无法判定血统线）
}
