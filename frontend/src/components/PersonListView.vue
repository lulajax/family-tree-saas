<template>
  <div class="person-list-view">
    <!-- 顶部导航栏替换 -->
    <div class="custom-header">
      <div class="header-title">
        <h1>家族成员</h1>
        <span class="member-count">{{ persons.length }}人</span>
      </div>
      <van-icon name="search" size="22" @click="$emit('show-search')" />
    </div>

    <!-- 代际筛选标签 -->
    <div class="generation-tabs">
      <van-tabs
        v-model:active="activeGeneration"
        swipeable
        background="#f8f9fa"
        title-active-color="#2196f3"
        title-inactive-color="#666"
      >
        <van-tab title="全部" name="all" />
        <van-tab title="同辈" name="same" />
        <van-tab title="下一代" name="next" />
        <van-tab
          v-for="gen in extraGenerations"
          :key="gen"
          :title="getGenerationLabel(gen)"
          :name="gen"
        />
      </van-tabs>
    </div>

    <!-- 人员列表 -->
    <div class="person-list">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh" success-text="刷新成功">
        <van-list
          :loading="loading"
          :finished="finished"
          finished-text=""
          @load="onLoad"
        >
          <!-- 代际分组标题 -->
          <template v-for="(group, gen) in groupedPersons" :key="gen">
            <div class="generation-title">
              <div class="title-line"></div>
              <span>{{ getGenerationTitle(Number(gen)) }}</span>
              <div class="title-line"></div>
            </div>

            <div
              v-for="person in group"
              :key="person.id"
              class="person-card"
              :class="{
                'is-focus': person.id === focusPersonId,
                'is-male': person.gender === 'MALE',
                'is-female': person.gender === 'FEMALE'
              }"
              @click="onPersonClick(person)"
              @touchstart="onTouchStart($event, person)"
              @touchend="onTouchEnd"
              @touchmove="onTouchMove"
            >
              <!-- 头像 -->
              <div class="person-avatar">
                <div class="avatar-wrapper" :class="person.gender?.toLowerCase()">
                  <img
                    :src="person.primaryPhotoUrl || getDefaultAvatar(person.gender)"
                    :alt="person.fullName"
                  />
                </div>
                <div v-if="person.id === focusPersonId" class="focus-badge">
                  <van-icon name="location-o" />
                </div>
              </div>

              <!-- 基本信息 -->
              <div class="person-info">
                <div class="name-row">
                  <span class="name">{{ person.fullName }}</span>
                  <div
                    class="gender-badge"
                    :class="person.gender?.toLowerCase() || 'unknown'"
                  >
                    {{ getGenderText(person.gender) }}
                  </div>
                </div>

                <div class="meta-row">
                  <van-icon name="calendar-o" class="meta-icon" />
                  <span v-if="person.birthDate" class="birth-date">
                    {{ formatBirthInfo(person) }}
                  </span>
                  <span v-else class="birth-date unknown">出生日期未知</span>
                </div>

                <!-- 关系标签 -->
                <div class="relation-tags">
                  <span v-if="person.id === focusPersonId" class="tag focus-tag">
                    <van-icon name="aim" /> 当前焦点
                  </span>
                  <span v-if="person.lineageType === 'FATHER_LINE'" class="tag father-line-tag">
                    <van-icon name="guide-o" /> 父系
                  </span>
                  <span v-if="person.lineageType === 'MOTHER_LINE'" class="tag mother-line-tag">
                    <van-icon name="guide-o" /> 母系
                  </span>
                </div>
              </div>

              <!-- 右箭头 -->
              <div class="arrow-area">
                <van-icon name="arrow" class="arrow-icon" />
              </div>
            </div>
          </template>
        </van-list>

        <!-- 空状态 -->
        <van-empty v-if="filteredPersons.length === 0 && !loading" description="暂无成员">
          <van-button round type="primary" size="small" @click="$emit('add-person')">
            添加第一个成员
          </van-button>
        </van-empty>
      </van-pull-refresh>
    </div>

    <!-- 底部浮动操作栏 -->
    <div class="bottom-actions">
      <div class="action-btn" @click="$emit('focus-me')">
        <div class="btn-icon primary">
          <van-icon name="aim" />
        </div>
        <span>回到我</span>
      </div>

      <div class="action-btn main" @click="$emit('add-person')">
        <div class="btn-icon highlight">
          <van-icon name="plus" />
        </div>
        <span>添加成员</span>
      </div>

      <div class="action-btn" @click="$emit('toggle-view')">
        <div class="btn-icon primary">
          <van-icon name="cluster-o" />
        </div>
        <span>图谱视图</span>
      </div>
    </div>

    <!-- 长按菜单 -->
    <van-action-sheet
      v-model:show="showActionSheet"
      :title="selectedPerson?.fullName"
      :actions="actionSheetActions"
      cancel-text="取消"
      close-on-click-action
      @select="onActionSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { PersonNode } from '@/types'

interface Props {
  persons: PersonNode[]
  focusPersonId: string
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  'person-click': [person: PersonNode]
  'focus-me': []
  'add-person': []
  'toggle-view': []
  'set-focus': [personId: string]
  'add-relation': [person: PersonNode, relationType: string]
  'delete-person': [person: PersonNode]
  'show-search': []
}>()

// 状态
const ALL_GENERATIONS = 'all'
const SAME_GENERATION = 'same'
const NEXT_GENERATION = 'next'
const activeGeneration = ref<number | string>(ALL_GENERATIONS)
const refreshing = ref(false)
const finished = ref(true)
const showActionSheet = ref(false)
const selectedPerson = ref<PersonNode | null>(null)

// 长按相关
const longPressTimer = ref<number | null>(null)
const isLongPress = ref(false)
const LONG_PRESS_DURATION = 500

// 按代际分组的人员
const groupedPersons = computed(() => {
  const filtered = filteredPersons.value
  const groups: Record<number, PersonNode[]> = {}

  filtered.forEach(person => {
    const gen = person.generation || 0
    if (!groups[gen]) {
      groups[gen] = []
    }
    groups[gen].push(person)
  })

  // 对每个组内按年龄排序
  Object.keys(groups).forEach(key => {
    const gen = Number(key)
    groups[gen].sort((a, b) => {
      if (a.birthDate && b.birthDate) {
        return new Date(a.birthDate).getTime() - new Date(b.birthDate).getTime()
      }
      return a.fullName.localeCompare(b.fullName)
    })
  })

  return groups
})

// 可用代际
const availableGenerations = computed(() => {
  const gens = new Set(props.persons.map(p => p.generation || 0))
  return Array.from(gens).sort((a, b) => a - b)
})

const extraGenerations = computed(() =>
  availableGenerations.value.filter(gen => gen !== 0 && gen !== 1)
)

const selectedGeneration = computed<number | null>(() => {
  if (activeGeneration.value === ALL_GENERATIONS) {
    return null
  }
  if (activeGeneration.value === SAME_GENERATION) {
    return 0
  }
  if (activeGeneration.value === NEXT_GENERATION) {
    return 1
  }
  if (typeof activeGeneration.value === 'number') {
    return activeGeneration.value
  }
  return Number(activeGeneration.value)
})

// 筛选后的人员列表
const filteredPersons = computed(() => {
  let list = [...props.persons]

  // 按代际筛选
  if (selectedGeneration.value !== null) {
    list = list.filter(p => (p.generation || 0) === selectedGeneration.value)
  }

  // 按代际排序
  list.sort((a, b) => (a.generation || 0) - (b.generation || 0))

  return list
})

// 操作菜单
const actionSheetActions = computed(() => [
  { name: '查看详情', value: 'detail', icon: 'user-o' },
  { name: '设为焦点', value: 'focus', icon: 'aim' },
  { name: '添加父母', value: 'add-parent', icon: 'friends-o' },
  { name: '添加配偶', value: 'add-spouse', icon: 'like-o' },
  { name: '添加子女', value: 'add-child', icon: 'smile-o' },
  { name: '删除人物', value: 'delete', icon: 'delete-o', color: '#ee0a24' }
])

// 方法
const getGenerationLabel = (gen: number) => {
  if (gen === 0) return '同辈'
  if (gen < 0) return `上${Math.abs(gen)}代`
  return `下${gen}代`
}

const getGenerationTitle = (gen: number) => {
  if (gen === 0) return '同辈成员'
  if (gen === -1) return '父母辈'
  if (gen === 1) return '子女辈'
  if (gen < 0) return `上${Math.abs(gen)}代 · 祖辈`
  return `下${gen}代 · 子孙辈`
}

// 焦点切换后若当前代际不存在，自动回到“全部”
watch([availableGenerations, activeGeneration], () => {
  if (activeGeneration.value === ALL_GENERATIONS
    || activeGeneration.value === SAME_GENERATION
    || activeGeneration.value === NEXT_GENERATION) {
    return
  }
  if (typeof activeGeneration.value !== 'number') {
    return
  }
  if (!extraGenerations.value.includes(activeGeneration.value)) {
    activeGeneration.value = ALL_GENERATIONS
  }
})

const getGenderText = (gender?: string) => {
  const map: Record<string, string> = {
    MALE: '男',
    FEMALE: '女',
    UNKNOWN: '未知'
  }
  return map[gender || 'UNKNOWN'] || '未知'
}

const getDefaultAvatar = (gender?: string) => {
  // 使用在线默认头像
  if (gender === 'MALE') {
    return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTYiIGhlaWdodD0iNTYiIHZpZXdCb3g9IjAgMCA1NiA1NiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjgiIGN5PSIyOCIgcj0iMjgiIGZpbGw9IiNFM0YyRkQiLz4KPHN2ZyB4PSIxNiIgeT0iMTIiIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjMjE5NkYzIj4KPHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyUzYuNDggMjIgMTIgMjIgMjIgMTcuNTIgMjIgMTIgMTcuNTIgMiAxMiAyem0wIDNjMS42NiAwIDMgMS4zNCAzIDNzLTEuMzQgMy0zIDMtMy0xLjM0LTMtMyAxLjM0LTMgMy0zem0wIDE0LjJjLTIuNSAwLTQuNzEtMS4yOC02LTMuMjJDOC45OSAxMy4wNSAxMiAxMSAxMiAxMXMzLjAxIDIuMDUgNiAyLjc4Yy0xLjI5IDEuOTQtMy41IDMuMjItNiAzLjIyeiIvPgo8L3N2Zz4KPC9zdmc+'
  }
  if (gender === 'FEMALE') {
    return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTYiIGhlaWdodD0iNTYiIHZpZXdCb3g9IjAgMCA1NiA1NiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjgiIGN5PSIyOCIgcj0iMjgiIGZpbGw9IiNGQ0U0RUMiLz4KPHN2ZyB4PSIxNiIgeT0iMTIiIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjRTkxRTYzIj4KPHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyUzYuNDggMjIgMTIgMjIgMjIgMTcuNTIgMjIgMTIgMTcuNTIgMiAxMiAyem0wIDNjMS42NiAwIDMgMS4zNCAzIDNzLTEuMzQgMy0zIDMtMy0xLjM0LTMtMyAxLjM0LTMgMy0zem0wIDE0LjJjLTIuNSAwLTQuNzEtMS4yOC02LTMuMjJDOC45OSAxMy4wNSAxMiAxMSAxMiAxMXMzLjAxIDIuMDUgNiAyLjc4Yy0xLjI5IDEuOTQtMy41IDMuMjItNiAzLjIyeiIvPgo8L3N2Zz4KPC9zdmc+'
  }
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTYiIGhlaWdodD0iNTYiIHZpZXdCb3g9IjAgMCA1NiA1NiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjgiIGN5PSIyOCIgcj0iMjgiIGZpbGw9IiNGNUY1RjUiLz4KPHN2ZyB4PSIxNiIgeT0iMTIiIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSIjOTk5Ij4KPHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyUzYuNDggMjIgMTIgMjIgMjIgMTcuNTIgMjIgMTIgMTcuNTIgMiAxMiAyem0wIDNjMS42NiAwIDMgMS4zNCAzIDNzLTEuMzQgMy0zIDMtMy0xLjM0LTMtMyAxLjM0LTMgMy0zem0wIDE0LjJjLTIuNSAwLTQuNzEtMS4yOC02LTMuMjJDOC45OSAxMy4wNSAxMiAxMSAxMiAxMXMzLjAxIDIuMDUgNiAyLjc4Yy0xLjI5IDEuOTQtMy41IDMuMjItNiAzLjIyeiIvPgo8L3N2Zz4KPC9zdmc+'
}

const formatBirthInfo = (person: PersonNode) => {
  if (!person.birthDate) return ''
  const birth = person.birthDate.split('T')[0]
  if (person.deathDate) {
    const birthYear = new Date(person.birthDate).getFullYear()
    const deathYear = new Date(person.deathDate).getFullYear()
    return `${birthYear}-${deathYear} · 享年${deathYear - birthYear}岁`
  }
  // 计算年龄
  const birthYear = new Date(person.birthDate).getFullYear()
  const currentYear = new Date().getFullYear()
  const age = currentYear - birthYear
  return `${birth} · ${age}岁`
}

// 点击事件
const onPersonClick = (person: PersonNode) => {
  if (isLongPress.value) return
  emit('person-click', person)
}

// 长按事件
const onTouchStart = (_e: TouchEvent, person: PersonNode) => {
  isLongPress.value = false
  longPressTimer.value = window.setTimeout(() => {
    isLongPress.value = true
    selectedPerson.value = person
    showActionSheet.value = true
  }, LONG_PRESS_DURATION)
}

const onTouchEnd = () => {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
  setTimeout(() => {
    isLongPress.value = false
  }, 50)
}

const onTouchMove = () => {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
}

// 操作菜单选择
const onActionSelect = (action: { name: string; value: string }) => {
  if (!selectedPerson.value) return

  switch (action.value) {
    case 'detail':
      emit('person-click', selectedPerson.value)
      break
    case 'focus':
      emit('set-focus', selectedPerson.value.id)
      break
    case 'add-parent':
      emit('add-relation', selectedPerson.value, 'PARENT')
      break
    case 'add-spouse':
      emit('add-relation', selectedPerson.value, 'SPOUSE')
      break
    case 'add-child':
      emit('add-relation', selectedPerson.value, 'CHILD')
      break
    case 'delete':
      emit('delete-person', selectedPerson.value)
      break
  }
}

// 刷新和加载
const onRefresh = () => {
  refreshing.value = false
}

const onLoad = () => {
  finished.value = true
}
</script>

<style scoped>
.person-list-view {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f9fa 0%, #f0f2f5 100%);
  padding-bottom: 100px;
}

/* 自定义头部 */
.custom-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.header-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.header-title h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.member-count {
  font-size: 13px;
  color: #999;
}

/* 代际筛选 */
.generation-tabs {
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.van-tabs__nav) {
  background: #f8f9fa;
}

/* 列表区域 */
.person-list {
  padding: 12px;
}

/* 代际分组标题 */
.generation-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0 12px;
  color: #999;
  font-size: 13px;
}

.generation-title:first-child {
  margin-top: 8px;
}

.generation-title .title-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, #ddd, transparent);
}

/* 人员卡片 */
.person-card {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 10px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.person-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: transparent;
  transition: all 0.2s;
}

.person-card.is-male::before {
  background: linear-gradient(180deg, #2196f3, #64b5f6);
}

.person-card.is-female::before {
  background: linear-gradient(180deg, #e91e63, #f06292);
}

.person-card:active {
  transform: scale(0.98);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.person-card.is-focus {
  background: linear-gradient(135deg, #e3f2fd 0%, #ffffff 100%);
  border: 1px solid #bbdefb;
}

/* 头像 */
.person-avatar {
  position: relative;
  flex-shrink: 0;
  margin-right: 14px;
}

.avatar-wrapper {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid transparent;
  transition: all 0.2s;
}

.avatar-wrapper.male {
  border-color: #bbdefb;
  background: #e3f2fd;
}

.avatar-wrapper.female {
  border-color: #f8bbd9;
  background: #fce4ec;
}

.avatar-wrapper.unknown {
  border-color: #e0e0e0;
  background: #f5f5f5;
}

.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.focus-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 22px;
  height: 22px;
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: white;
  font-size: 12px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(33, 150, 243, 0.3);
}

/* 信息区域 */
.person-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.gender-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.gender-badge.male {
  background: #e3f2fd;
  color: #2196f3;
}

.gender-badge.female {
  background: #fce4ec;
  color: #e91e63;
}

.gender-badge.unknown {
  background: #f5f5f5;
  color: #999;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.meta-icon {
  font-size: 14px;
  color: #bbb;
}

.meta-row .unknown {
  color: #bbb;
}

/* 关系标签 */
.relation-tags {
  margin-top: 6px;
  display: flex;
  gap: 6px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}

.focus-tag {
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: white;
}

.father-line-tag {
  background: #dbeafe;
  color: #1e40af;
}

.mother-line-tag {
  background: #fce7f3;
  color: #be185d;
}

/* 箭头 */
.arrow-area {
  padding-left: 12px;
  margin-left: auto;
}

.arrow-icon {
  color: #ddd;
  font-size: 16px;
}

/* 底部操作栏 */
.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 24px calc(16px + env(safe-area-inset-bottom));
  background: white;
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  z-index: 100;
  border-radius: 20px 20px 0 0;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.action-btn span {
  font-size: 12px;
  color: #666;
}

.action-btn.main span {
  color: #2196f3;
  font-weight: 500;
}

.btn-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  transition: all 0.2s;
}

.btn-icon.primary {
  background: #f5f5f5;
  color: #666;
}

.btn-icon.highlight {
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: white;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.action-btn:active .btn-icon {
  transform: scale(0.95);
}
</style>
