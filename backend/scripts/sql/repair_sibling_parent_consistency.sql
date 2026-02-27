-- 一次性修复脚本：补齐兄弟姐妹关系缺失的父母关联
-- 适用数据库：PostgreSQL
-- 使用方式：
--   psql "$DATABASE_URL" -f scripts/sql/repair_sibling_parent_consistency.sql

BEGIN;

DO $$
DECLARE
    sibling_rel RECORD;
    parent_id UUID;
    added_count INTEGER := 0;
    skipped_limit_count INTEGER := 0;
BEGIN
    FOR sibling_rel IN
        SELECT r.group_id,
               r.from_person_id AS person_a_id,
               r.to_person_id AS person_b_id
        FROM relationships r
        WHERE r.type = 'SIBLING'
    LOOP
        -- A 的父母同步到 B
        FOR parent_id IN
            SELECT DISTINCT p.from_person_id
            FROM relationships p
            WHERE p.group_id = sibling_rel.group_id
              AND p.type = 'PARENT'
              AND p.to_person_id = sibling_rel.person_a_id
        LOOP
            IF NOT EXISTS (
                SELECT 1
                FROM relationships e
                WHERE e.group_id = sibling_rel.group_id
                  AND e.type = 'PARENT'
                  AND e.from_person_id = parent_id
                  AND e.to_person_id = sibling_rel.person_b_id
            ) THEN
                IF (
                    SELECT COUNT(DISTINCT c.from_person_id)
                    FROM relationships c
                    WHERE c.group_id = sibling_rel.group_id
                      AND c.type = 'PARENT'
                      AND c.to_person_id = sibling_rel.person_b_id
                ) < 2 THEN
                    INSERT INTO relationships (id, group_id, from_person_id, to_person_id, type, created_at)
                    VALUES (
                        md5(random()::text || clock_timestamp()::text || parent_id::text || sibling_rel.person_b_id::text)::uuid,
                        sibling_rel.group_id,
                        parent_id,
                        sibling_rel.person_b_id,
                        'PARENT',
                        NOW()
                    );
                    added_count := added_count + 1;
                ELSE
                    skipped_limit_count := skipped_limit_count + 1;
                    RAISE NOTICE '跳过同步：group=%, parent=% -> child=%，原因=父母数已达上限(2)',
                        sibling_rel.group_id, parent_id, sibling_rel.person_b_id;
                END IF;
            END IF;
        END LOOP;

        -- B 的父母同步到 A
        FOR parent_id IN
            SELECT DISTINCT p.from_person_id
            FROM relationships p
            WHERE p.group_id = sibling_rel.group_id
              AND p.type = 'PARENT'
              AND p.to_person_id = sibling_rel.person_b_id
        LOOP
            IF NOT EXISTS (
                SELECT 1
                FROM relationships e
                WHERE e.group_id = sibling_rel.group_id
                  AND e.type = 'PARENT'
                  AND e.from_person_id = parent_id
                  AND e.to_person_id = sibling_rel.person_a_id
            ) THEN
                IF (
                    SELECT COUNT(DISTINCT c.from_person_id)
                    FROM relationships c
                    WHERE c.group_id = sibling_rel.group_id
                      AND c.type = 'PARENT'
                      AND c.to_person_id = sibling_rel.person_a_id
                ) < 2 THEN
                    INSERT INTO relationships (id, group_id, from_person_id, to_person_id, type, created_at)
                    VALUES (
                        md5(random()::text || clock_timestamp()::text || parent_id::text || sibling_rel.person_a_id::text)::uuid,
                        sibling_rel.group_id,
                        parent_id,
                        sibling_rel.person_a_id,
                        'PARENT',
                        NOW()
                    );
                    added_count := added_count + 1;
                ELSE
                    skipped_limit_count := skipped_limit_count + 1;
                    RAISE NOTICE '跳过同步：group=%, parent=% -> child=%，原因=父母数已达上限(2)',
                        sibling_rel.group_id, parent_id, sibling_rel.person_a_id;
                END IF;
            END IF;
        END LOOP;
    END LOOP;

    RAISE NOTICE '修复完成：新增父母关系=%，因父母上限跳过=%', added_count, skipped_limit_count;
END $$;

COMMIT;

-- 可选核查：检查是否存在超过 2 位父母的人物（理论上应为空）
SELECT group_id,
       to_person_id AS person_id,
       COUNT(DISTINCT from_person_id) AS parent_count
FROM relationships
WHERE type = 'PARENT'
GROUP BY group_id, to_person_id
HAVING COUNT(DISTINCT from_person_id) > 2;
