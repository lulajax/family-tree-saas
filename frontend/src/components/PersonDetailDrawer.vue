<template>
  <van-popup
    v-model:show="visible"
    position="bottom"
    :style="{ height: '78%' }"
    round
    closeable
    close-icon="close"
    @closed="onClosed"
  >
    <div class="person-detail-drawer">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <van-loading type="spinner" size="40px" />
        <p>加载中...</p>
      </div>

      <!-- 内容 -->
      <template v-else-if="relations.personId">
        <!-- 头部信息卡片 -->
        <div class="header-card">
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <img
                :src="relations.primaryPhotoUrl || getDefaultAvatar()"
                :alt="relations.personName"
              />
              <div v-if="isCurrentUser" class="identity-badge">
                <van-icon name="aim" />
              </div>
            </div>
          </div>

          <div class="basic-info">
            <h2 class="name">{{ relations.personName }}</h2>
            <div class="action-btns">
              <van-button
                v-if="isEditable"
                round
                size="small"
                icon="edit"
                @click="onEdit"
              >
                编辑
              </van-button>
              <van-button
                round
                size="small"
                type="primary"
                icon="aim"
                @click="onSetFocus"
              >
                设为焦点
              </van-button>
            </div>
          </div>
        </div>

        <!-- 关系统计 -->
        <div class="relation-stats">
          <div class="stat-item" :class="{ active: activeTab === 0 }" @click="activeTab = 0">
            <span class="number">{{ relations.parents?.length || 0 }}</span>
            <span class="label">父母</span>
          </div>
          <div class="stat-item" :class="{ active: activeTab === 1 }" @click="activeTab = 1">
            <span class="number">{{ relations.spouses?.length || 0 }}</span>
            <span class="label">配偶</span>
          </div>
          <div class="stat-item" :class="{ active: activeTab === 2 }" @click="activeTab = 2">
            <span class="number">{{ relations.children?.length || 0 }}</span>
            <span class="label">子女</span>
          </div>
          <div class="stat-item" :class="{ active: activeTab === 3 }" @click="activeTab = 3">
            <span class="number">{{ relations.siblings?.length || 0 }}</span>
            <span class="label">兄弟姐妹</span>
          </div>
        </div>

        <!-- 关系列表 -->
        <div class="relation-content">
          <!-- 父母 -->
          <div v-show="activeTab === 0" class="relation-panel">
            <div
              v-for="p in relations.parents"
              :key="p.id"
              class="relation-card"
              @click="onRelationClick(p)"
            >
              <div class="card-avatar" :class="p.gender?.toLowerCase()">
                <img :src="p.primaryPhotoUrl || getDefaultAvatar(p.gender)" />
              </div>
              <div class="card-info">
                <span class="card-name">{{ p.fullName }}</span>
                <span class="card-meta">{{ formatLifeInfo(p) }}</span>
              </div>
              <van-icon name="arrow" class="card-arrow" />
            </div>

            <van-button
              v-if="(relations.parents?.length || 0) < 2"
              block
              round
              type="primary"
              icon="plus"
              class="add-relation-btn"
              @click="onAddRelation('PARENT')"
            >
              添加父母
            </van-button>

            <van-empty v-if="!relations.parents?.length" image="default" description="暂无父母信息">
              <van-button round type="primary" size="small" @click="onAddRelation('PARENT')">
                添加父母
              </van-button>
            </van-empty>
          </div>

          <!-- 配偶 -->
          <div v-show="activeTab === 1" class="relation-panel">
            <div
              v-for="p in relations.spouses"
              :key="p.id"
              class="relation-card"
              @click="onRelationClick(p)"
            >
              <div class="card-avatar" :class="p.gender?.toLowerCase()">
                <img :src="p.primaryPhotoUrl || getDefaultAvatar(p.gender)" />
              </div>
              <div class="card-info">
                <span class="card-name">{{ p.fullName }}</span>
                <span class="card-meta">{{ formatLifeInfo(p) }}</span>
              </div>
              <van-icon name="arrow" class="card-arrow" />
            </div>

            <van-button
              block
              round
              type="primary"
              icon="plus"
              class="add-relation-btn"
              @click="onAddRelation('SPOUSE')"
            >
              添加配偶
            </van-button>

            <van-empty v-if="!relations.spouses?.length" image="default" description="暂无配偶信息">
              <van-button round type="primary" size="small" @click="onAddRelation('SPOUSE')">
                添加配偶
              </van-button>
            </van-empty>
          </div>

          <!-- 子女 -->
          <div v-show="activeTab === 2" class="relation-panel">
            <div
              v-for="p in relations.children"
              :key="p.id"
              class="relation-card"
              @click="onRelationClick(p)"
            >
              <div class="card-avatar" :class="p.gender?.toLowerCase()">
                <img :src="p.primaryPhotoUrl || getDefaultAvatar(p.gender)" />
              </div>
              <div class="card-info">
                <span class="card-name">{{ p.fullName }}</span>
                <span class="card-meta">{{ formatLifeInfo(p) }}</span>
              </div>
              <van-icon name="arrow" class="card-arrow" />
            </div>

            <van-button
              block
              round
              type="primary"
              icon="plus"
              class="add-relation-btn"
              @click="onAddRelation('CHILD')"
            >
              添加子女
            </van-button>

            <van-empty v-if="!relations.children?.length" image="default" description="暂无子女信息">
              <van-button round type="primary" size="small" @click="onAddRelation('CHILD')">
                添加子女
              </van-button>
            </van-empty>
          </div>

          <!-- 兄弟姐妹 -->
          <div v-show="activeTab === 3" class="relation-panel">
            <div
              v-for="p in relations.siblings"
              :key="p.id"
              class="relation-card"
              @click="onRelationClick(p)"
            >
              <div class="card-avatar" :class="p.gender?.toLowerCase()">
                <img :src="p.primaryPhotoUrl || getDefaultAvatar(p.gender)" />
              </div>
              <div class="card-info">
                <span class="card-name">{{ p.fullName }}</span>
                <span class="card-meta">{{ formatLifeInfo(p) }}</span>
              </div>
              <van-icon name="arrow" class="card-arrow" />
            </div>

            <van-button
              v-if="isEditable"
              block
              round
              type="primary"
              icon="plus"
              class="add-relation-btn"
              @click="onAddRelation('SIBLING')"
            >
              添加兄弟姐妹
            </van-button>

            <van-empty v-if="!relations.siblings?.length" image="default" description="暂无兄弟姐妹信息">
              <van-button v-if="isEditable" round type="primary" size="small" @click="onAddRelation('SIBLING')">
                添加兄弟姐妹
              </van-button>
            </van-empty>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="drawer-footer">
          <van-cell-group inset>
            <van-cell title="查看完整资料" is-link @click="onViewFull">
              <template #icon>
                <van-icon name="description" class="cell-icon" />
              </template>
            </van-cell>
            <van-cell
              v-if="isEditable"
              title="删除人物"
              is-link
              class="delete-cell"
              @click="onDelete"
            >
              <template #icon>
                <van-icon name="delete-o" class="cell-icon delete-icon" />
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </template>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { PersonRelations } from '@/types'

interface Props {
  show: boolean
  personId?: string
  currentUserId?: string
  loading?: boolean
  relations: PersonRelations
  isEditable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  isEditable: true,
  relations: () => ({
    personId: '',
    personName: '',
    primaryPhotoUrl: undefined,
    parents: [],
    spouses: [],
    children: [],
    siblings: []
  })
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  'relation-click': [personId: string]
  'add-relation': [type: string]
  'set-focus': []
  'view-full': []
  'edit': []
  'delete': []
  'closed': []
}>()

// 状态
const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const activeTab = ref(0)

// 计算属性
const isCurrentUser = computed(() => props.personId === props.currentUserId)

// 方法
const getDefaultAvatar = (gender?: string) => {
  if (gender === 'MALE') {
    return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTAiIGhlaWdodD0iNTAiIHZpZXdCb3g9IjAgMCA1MCA1MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjUiIGN5PSIyNSIgcj0iMjUiIGZpbGw9IiNFM0YyRkQiLz4KPHN2ZyB4PSIxMyIgeT0iMTAiIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjMjE5NkYzIj4KPHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyUzYuNDggMjIgMTIgMjIgMjIgMTcuNTIgMjIgMTIgMTcuNTIgMiAxMiAyem0wIDNjMS42NiAwIDMgMS4zNCAzIDNzLTEuMzQgMy0zIDMtMy0xLjM0LTMtMyAxLjM0LTMgMy0zem0wIDE0LjJjLTIuNSAwLTQuNzEtMS4yOC02LTMuMjJDOC45OSAxMy4wNSAxMiAxMSAxMiAxMXMzLjAxIDIuMDUgNiAyLjc4Yy0xLjI5IDEuOTQtMy41IDMuMjItNiAzLjIyeiIvPgo8L3N2Zz4KPC9zdmc+'
  }
  if (gender === 'FEMALE') {
    return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTAiIGhlaWdodD0iNTAiIHZpZXdCb3g9IjAgMCA1MCA1MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjUiIGN5PSIyNSIgcj0iMjUiIGZpbGw9IiNGQ0U0RUMiLz4KPHN2ZyB4PSIxMyIgeT0iMTAiIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjRTkxRTYzIj4KPHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyUzYuNDggMjIgMTIgMjIgMjIgMTcuNTIgMjIgMTIgMTcuNTIgMiAxMiAyem0wIDNjMS42NiAwIDMgMS4zNCAzIDNzLTEuMzQgMy0zIDMtMy0xLjM0LTMtMyAxLjM0LTMgMy0zem0wIDE0LjJjLTIuNSAwLTQuNzEtMS4yOC02LTMuMjJDOC45OSAxMy4wNSAxMiAxMSAxMiAxMXMzLjAxIDIuMDUgNiAyLjc4Yy0xLjI5IDEuOTQtMy41IDMuMjItNiAzLjIyeiIvPgo8L3N2Zz4KPC9zdmc+'
  }
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTAiIGhlaWdodD0iNTAiIHZpZXdCb3g9IjAgMCA1MCA1MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjUiIGN5PSIyNSIgcj0iMjUiIGZpbGw9IiNGNUY1RjUiLz4KPHN2ZyB4PSIxMyIgeT0iMTAiIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjOTk5Ij4KPHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyUzYuNDggMjIgMTIgMjIgMjIgMTcuNTIgMjIgMTIgMTcuNTIgMiAxMiAyem0wIDNjMS42NiAwIDMgMS4zNCAzIDNzLTEuMzQgMy0zIDMtMy0xLjM0LTMtMyAxLjM0LTMgMy0zem0wIDE0LjJjLTIuNSAwLTQuNzEtMS4yOC02LTMuMjJDOC45OSAxMy4wNSAxMiAxMSAxMiAxMXMzLjAxIDIuMDUgNiAyLjc4Yy0xLjI5IDEuOTQtMy41IDMuMjItNiAzLjIyeiIvPgo8L3N2Zz4KPC9zdmc+'
}

const formatLifeInfo = (p: any) => {
  if (!p.birthDate) return '出生日期未知'
  const birthYear = p.birthDate.split('-')[0]
  if (p.deathDate) {
    const deathYear = p.deathDate.split('-')[0]
    return `${birthYear}-${deathYear} · 享年${Number(deathYear) - Number(birthYear)}岁`
  }
  const currentYear = new Date().getFullYear()
  const age = currentYear - Number(birthYear)
  return `${p.birthDate} · ${age}岁`
}

const onRelationClick = (p: any) => {
  emit('relation-click', p.id)
}

const onAddRelation = (type: string) => {
  emit('add-relation', type)
}

const onSetFocus = () => {
  emit('set-focus')
  visible.value = false
}

const onViewFull = () => {
  emit('view-full')
}

const onEdit = () => {
  emit('edit')
}

const onDelete = () => {
  emit('delete')
}

const onClosed = () => {
  activeTab.value = 0
  emit('closed')
}

// 监听 personId 变化
watch(() => props.personId, () => {
  activeTab.value = 0
})
</script>

<style scoped>
.person-detail-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  gap: 12px;
}

/* 头部卡片 */
.header-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 20px;
  color: white;
  position: relative;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.avatar-wrapper {
  position: relative;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid rgba(255, 255, 255, 0.3);
  background: white;
}

.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.identity-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #ffd700, #ffb700);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  border: 3px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.basic-info {
  text-align: center;
}

.name {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.action-btns {
  display: flex;
  justify-content: center;
  gap: 12px;
}

:deep(.action-btns .van-button) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

:deep(.action-btns .van-button--primary) {
  background: white;
  color: #667eea;
  border-color: white;
}

/* 关系统计 */
.relation-stats {
  display: flex;
  background: white;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.stat-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 24px;
  background: #f0f0f0;
}

.stat-item.active {
  background: linear-gradient(180deg, transparent 0%, rgba(102, 126, 234, 0.05) 100%);
}

.stat-item.active .number {
  color: #667eea;
  transform: scale(1.1);
}

.stat-item .number {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  transition: all 0.2s;
}

.stat-item .label {
  font-size: 12px;
  color: #999;
}

/* 关系内容 */
.relation-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.relation-panel {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.relation-card {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background: white;
  border-radius: 14px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}

.relation-card:active {
  transform: scale(0.98);
  background: #f8f9fa;
}

.card-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 14px;
  border: 3px solid transparent;
}

.card-avatar.male {
  border-color: #bbdefb;
  background: #e3f2fd;
}

.card-avatar.female {
  border-color: #f8bbd9;
  background: #fce4ec;
}

.card-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.card-meta {
  font-size: 13px;
  color: #999;
}

.card-arrow {
  color: #ddd;
  font-size: 16px;
}

.add-relation-btn {
  margin-top: 8px;
  height: 44px;
  font-size: 15px;
}

/* 底部 */
.drawer-footer {
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: white;
  border-top: 1px solid #f0f0f0;
}

.cell-icon {
  margin-right: 8px;
  font-size: 18px;
  color: #667eea;
}

.cell-icon.delete-icon {
  color: #ee0a24;
}

:deep(.delete-cell .van-cell__title) {
  color: #ee0a24;
}

:deep(.delete-cell) {
  background: #fff5f5;
}
</style>
