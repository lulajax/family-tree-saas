-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 1. 用户与认证
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    nickname VARCHAR(50),
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 群组（家族）
CREATE TABLE IF NOT EXISTS groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    admin_id UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

-- 3. 群组成员
CREATE TABLE IF NOT EXISTS group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    role VARCHAR(20) CHECK (role IN ('ADMIN', 'MEMBER')),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, user_id)
);

-- 4. 人物节点（主表 - 当前状态）
CREATE TABLE IF NOT EXISTS persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50),
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'UNKNOWN')),
    birth_date DATE,
    death_date DATE,
    birth_place VARCHAR(100),
    current_spouse_id UUID REFERENCES persons(id),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

-- 5. 关系边（图结构）
CREATE TABLE IF NOT EXISTS relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    from_person_id UUID REFERENCES persons(id) ON DELETE CASCADE,
    to_person_id UUID REFERENCES persons(id) ON DELETE CASCADE,
    type VARCHAR(20) CHECK (type IN ('PARENT', 'CHILD', 'SPOUSE', 'SIBLING')),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, from_person_id, to_person_id, type)
);

-- 6. 工作区（每个用户每群组一个工作区）
CREATE TABLE IF NOT EXISTS workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    base_version INTEGER NOT NULL,
    status VARCHAR(20) CHECK (status IN ('EDITING', 'SUBMITTED', 'MERGED', 'CONFLICT')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, user_id)
);

-- 7. 变更集（ChangeSet）- 核心表
CREATE TABLE IF NOT EXISTS changesets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    action_type VARCHAR(20) CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE')),
    entity_type VARCHAR(20) CHECK (entity_type IN ('PERSON', 'RELATIONSHIP', 'PHOTO')),
    entity_id UUID,
    payload JSONB NOT NULL,
    previous_payload JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sequence_number INTEGER
);

-- 8. 合并请求（Merge Request）
CREATE TABLE IF NOT EXISTS merge_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID REFERENCES workspaces(id),
    group_id UUID REFERENCES groups(id),
    title VARCHAR(200),
    description TEXT,
    status VARCHAR(20) CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CONFLICT')),
    created_by UUID REFERENCES users(id),
    reviewed_by UUID REFERENCES users(id),
    review_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. 照片墙
CREATE TABLE IF NOT EXISTS photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id UUID REFERENCES persons(id) ON DELETE CASCADE,
    uploader_id UUID REFERENCES users(id),
    url TEXT NOT NULL,
    description TEXT,
    taken_at DATE,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10. 自定义属性
CREATE TABLE IF NOT EXISTS custom_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id UUID REFERENCES persons(id) ON DELETE CASCADE,
    attr_key VARCHAR(50) NOT NULL,
    attr_value TEXT,
    data_type VARCHAR(20) CHECK (data_type IN ('STRING', 'NUMBER', 'DATE', 'BOOLEAN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 11. 群组隐私设置
CREATE TABLE IF NOT EXISTS group_privacy_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    is_searchable BOOLEAN DEFAULT false,
    public_ancestors_level INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id)
);

-- 索引优化
CREATE INDEX IF NOT EXISTS idx_persons_group ON persons(group_id);
CREATE INDEX IF NOT EXISTS idx_persons_name ON persons USING gin(first_name gin_trgm_ops, last_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_relationships_group ON relationships(group_id);
CREATE INDEX IF NOT EXISTS idx_relationships_from ON relationships(from_person_id);
CREATE INDEX IF NOT EXISTS idx_relationships_to ON relationships(to_person_id);
CREATE INDEX IF NOT EXISTS idx_changesets_workspace ON changesets(workspace_id);
CREATE INDEX IF NOT EXISTS idx_changesets_entity ON changesets(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_photos_person ON photos(person_id);
CREATE INDEX IF NOT EXISTS idx_group_members_group ON group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user ON group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_workspaces_group ON workspaces(group_id);
CREATE INDEX IF NOT EXISTS idx_workspaces_user ON workspaces(user_id);
CREATE INDEX IF NOT EXISTS idx_merge_requests_group ON merge_requests(group_id);
CREATE INDEX IF NOT EXISTS idx_merge_requests_workspace ON merge_requests(workspace_id);

-- GIN 索引用于 JSONB 查询
CREATE INDEX IF NOT EXISTS idx_changesets_payload ON changesets USING GIN (payload);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为需要自动更新时间的表创建触发器
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_persons_updated_at BEFORE UPDATE ON persons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_workspaces_updated_at BEFORE UPDATE ON workspaces
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merge_requests_updated_at BEFORE UPDATE ON merge_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 创建相似度匹配函数（用于寻亲功能）
CREATE OR REPLACE FUNCTION find_potential_matches(
    target_group_id UUID,
    similarity_threshold FLOAT DEFAULT 0.8
)
RETURNS TABLE (
    matched_group_id UUID,
    matched_person_id UUID,
    person_id UUID,
    similarity_score FLOAT,
    match_reason TEXT
) AS $$
BEGIN
    RETURN QUERY
    WITH target_ancestors AS (
        SELECT p.id, p.first_name, p.last_name, p.birth_date, p.birth_place
        FROM persons p
        JOIN group_privacy_settings gps ON p.group_id = gps.group_id
        WHERE p.group_id = target_group_id
        AND gps.is_searchable = true
        AND p.birth_date < CURRENT_DATE - INTERVAL '100 years'
    ),
    other_ancestors AS (
        SELECT p.id, p.first_name, p.last_name, p.birth_date, p.birth_place, p.group_id
        FROM persons p
        JOIN group_privacy_settings gps ON p.group_id = gps.group_id
        WHERE p.group_id != target_group_id
        AND gps.is_searchable = true
        AND gps.public_ancestors_level >= 1
    )
    SELECT 
        o.group_id as matched_group_id,
        o.id as matched_person_id,
        t.id as person_id,
        similarity(t.first_name, o.first_name) * 0.4 +
        similarity(t.last_name, o.last_name) * 0.4 +
        (CASE WHEN t.birth_date = o.birth_date THEN 0.2 ELSE 0 END) as similarity_score,
        '姓名和生年匹配' as match_reason
    FROM target_ancestors t
    CROSS JOIN other_ancestors o
    WHERE similarity(t.first_name, o.first_name) > 0.7
    AND similarity(t.last_name, o.last_name) > 0.7
    HAVING similarity(t.first_name, o.first_name) * 0.4 +
           similarity(t.last_name, o.last_name) * 0.4 > similarity_threshold;
END;
$$ LANGUAGE plpgsql;
