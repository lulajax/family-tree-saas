<template>
  <div class="tree-view-page">
    <van-nav-bar
      :title="viewMode === 'list' ? '家族成员' : '家谱图谱'"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    >
      <template #right>
        <van-icon name="search" size="20" @click="showSearch = true" />
      </template>
    </van-nav-bar>

    <!-- 列表模式 -->
    <PersonListView
      v-if="viewMode === 'list'"
      :persons="treeData.nodes"
      :focus-person-id="treeData.focusPersonId"
      :loading="loading"
      @person-click="onPersonClick"
      @focus-me="onFocusMe"
      @add-person="onAddPerson"
      @toggle-view="toggleViewMode"
      @set-focus="onSetFocus"
      @add-relation="onAddRelationFromList"
      @delete-person="onDeleteFromList"
      @show-search="showSearch = true"
    />

    <!-- 图谱模式 -->
    <div v-else class="tree-container" ref="treeContainer">
      <div v-if="loading" class="loading-overlay">
        <van-loading type="spinner" size="40px" />
        <p>加载中...</p>
      </div>

      <div v-else-if="treeData.nodes.length === 0" class="empty-tree">
        <van-empty description="暂无家谱数据">
          <van-button type="primary" @click="goToWorkspace">
            开始编辑
          </van-button>
        </van-empty>
      </div>

      <canvas
        v-else
        ref="canvas"
        class="tree-canvas"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="handleTouchEnd"
        @click="handleCanvasClick"
      />

      <!-- 图谱模式底部按钮 -->
      <div v-if="viewMode === 'graph' && treeData.nodes.length > 0" class="graph-actions">
        <van-button round icon="aim" size="small" @click="onFocusMe">回到我</van-button>
        <van-button round type="primary" icon="plus" size="small" @click="onAddPerson">添加成员</van-button>
        <van-button round icon="list-switching" size="small" @click="toggleViewMode">列表视图</van-button>
      </div>
    </div>

    <!-- 人员详情抽屉 -->
    <PersonDetailDrawer
      v-model:show="showDetailDrawer"
      :person-id="selectedPersonId"
      :current-user-id="treeData.focusPersonId"
      :loading="detailLoading"
      :relations="personRelations"
      @relation-click="onRelationClick"
      @add-relation="onAddRelation"
      @set-focus="onSetFocusFromDrawer"
      @view-full="goToPersonDetail"
      @edit="goToEditPerson"
    @delete="onDeletePerson"
    />

    <!-- 添加人员向导 -->
    <AddPersonWizard
      v-model:show="showAddWizard"
      :target-person-id="wizardTargetPersonId"
      :target-person-name="wizardTargetPersonName"
      :default-relation-type="wizardDefaultRelation"
      :parent-count="parentCount"
      @submit="onWizardSubmit"
    />

    <!-- 搜索弹窗 -->
    <van-popup
      v-model:show="showSearch"
      position="top"
      :style="{ height: '80%' }"
    >
      <van-nav-bar
        title="搜索人物"
        left-arrow
        @click-left="showSearch = false"
      />
      <van-search
        v-model="searchKeyword"
        placeholder="请输入姓名搜索"
        @search="onSearch"
      />
      <van-list>
        <van-cell
          v-for="person in searchResults"
          :key="person.id"
          :title="person.fullName"
          @click="selectPerson(person)"
        />
      </van-list>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast, showDialog } from 'vant'
import { treeApi, personApi, groupApi } from '@/api'
import type { TreeView, Person, PersonNode, PersonRelations } from '@/types'
import PersonListView from '@/components/PersonListView.vue'
import PersonDetailDrawer from '@/components/PersonDetailDrawer.vue'
import AddPersonWizard from '@/components/AddPersonWizard.vue'

const route = useRoute()
const router = useRouter()
const groupId = route.params.id as string

// 视图模式: 'list' | 'graph'
const viewMode = ref<'list' | 'graph'>('list')

// 数据
const treeContainer = ref<HTMLDivElement>()
const canvas = ref<HTMLCanvasElement>()
const loading = ref(false)
const detailLoading = ref(false)
const treeData = ref<TreeView>({ focusPersonId: '', focusPersonName: '', depth: 3, nodes: [], edges: [] })

// 弹窗控制
const showDetailDrawer = ref(false)
const showAddWizard = ref(false)
const showSearch = ref(false)

// 选中人物
const selectedPersonId = ref('')
const personRelations = ref<PersonRelations>({
  personId: '',
  personName: '',
  parents: [],
  spouses: [],
  children: [],
  siblings: []
})

// 向导参数
const wizardTargetPersonId = ref('')
const wizardTargetPersonName = ref('')
const wizardDefaultRelation = ref('')

// 计算属性：父母数量
const parentCount = computed(() => personRelations.value.parents?.length || 0)

// 搜索
const searchKeyword = ref('')
const searchResults = ref<Person[]>([])

// 画布状态
let ctx: CanvasRenderingContext2D | null = null
let scale = 1
let offsetX = 0
let offsetY = 0
let isDragging = false
let lastTouchX = 0
let lastTouchY = 0

// 获取家谱数据
const fetchTreeData = async () => {
  loading.value = true
  try {
    const data = await treeApi.getTreeView(groupId, undefined, 3)
    treeData.value = data
    if (viewMode.value === 'graph') {
      nextTick(() => {
        initCanvas()
        renderTree()
        setTimeout(() => {
          initCanvas()
          renderTree()
        }, 100)
      })
    }
  } catch (error) {
    showToast('获取家谱数据失败')
  } finally {
    loading.value = false
  }
}

// 切换视图模式
const toggleViewMode = () => {
  viewMode.value = viewMode.value === 'list' ? 'graph' : 'list'
  if (viewMode.value === 'graph') {
    nextTick(() => {
      initCanvas()
      renderTree()
    })
  }
}

// 列表模式 - 点击人物
const onPersonClick = async (person: PersonNode) => {
  selectedPersonId.value = person.id
  showDetailDrawer.value = true
  await loadPersonRelations(person.id)
}

// 加载人员关系详情
const loadPersonRelations = async (personId: string) => {
  detailLoading.value = true
  try {
    const data = await personApi.getPersonRelations(personId)
    personRelations.value = data
  } catch (error) {
    showToast('获取关系详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 回到焦点人物
const onFocusMe = () => {
  if (treeData.value.focusPersonId) {
    onSetFocus(treeData.value.focusPersonId)
  }
}

// 设置焦点人物
const onSetFocus = async (personId: string) => {
  loading.value = true
  try {
    const data = await treeApi.getTreeView(groupId, personId, 3)
    treeData.value = data
    if (viewMode.value === 'graph') {
      renderTree()
    }
    showToast('已切换焦点')
  } catch (error) {
    showToast('切换焦点失败')
  } finally {
    loading.value = false
  }
}

// 从抽屉设置焦点
const onSetFocusFromDrawer = () => {
  onSetFocus(selectedPersonId.value)
  showDetailDrawer.value = false
}

// 打开添加向导
const onAddPerson = () => {
  wizardTargetPersonId.value = treeData.value.focusPersonId
  wizardTargetPersonName.value = treeData.value.focusPersonName
  wizardDefaultRelation.value = ''
  showAddWizard.value = true
}

// 从列表添加关系
const onAddRelationFromList = (person: PersonNode, relationType: string) => {
  wizardTargetPersonId.value = person.id
  wizardTargetPersonName.value = person.fullName
  wizardDefaultRelation.value = relationType
  showAddWizard.value = true
}

// 从列表删除人物
const onDeleteFromList = (person: PersonNode) => {
  selectedPersonId.value = person.id
  onDeletePerson()
}

// 从抽屉添加关系
const onAddRelation = (type: string) => {
  showDetailDrawer.value = false
  wizardTargetPersonId.value = selectedPersonId.value
  wizardTargetPersonName.value = personRelations.value.personName
  wizardDefaultRelation.value = type
  showAddWizard.value = true
}

// 向导提交
const onWizardSubmit = async (data: any) => {
  showLoadingToast({ message: '添加中...', forbidClick: true })
  try {
    // 1. 创建人物
    const person = await personApi.createPerson({
      groupId,
      ...data.person
    })

    // 2. 创建关系
    const relationType = data.relation.type
    const targetId = data.relation.targetPersonId

    // 统一按“目标人物(targetId)的关系”提交：
    // PARENT: person 是 targetId 的父母
    // CHILD:  person 是 targetId 的子女
    // SPOUSE/SIBLING: person 是 targetId 的配偶/兄弟姐妹
    // 后端会将父子关系归一化为 from=父母, to=子女
    const fromId = targetId
    const toId = person.id

    await groupApi.createRelationship(groupId, {
      fromPersonId: fromId,
      toPersonId: toId,
      type: relationType
    })

    closeToast()
    showToast('添加成功')
    fetchTreeData()
  } catch (error) {
    closeToast()
    showToast('添加失败')
  }
}

// 关系点击
const onRelationClick = (personId: string) => {
  selectedPersonId.value = personId
  loadPersonRelations(personId)
}

// 查看完整详情
const goToPersonDetail = () => {
  router.push(`/person/${selectedPersonId.value}`)
}

// 编辑人物
const goToEditPerson = () => {
  router.push(`/person/${selectedPersonId.value}/edit`)
}

// 删除人物
const onDeletePerson = async () => {
  showDialog({
    title: '确认删除',
    message: '删除后将无法恢复，是否继续？',
    showCancelButton: true,
    confirmButtonText: '删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      showLoadingToast({ message: '删除中...', forbidClick: true })
      await personApi.deletePerson(selectedPersonId.value)
      closeToast()
      showToast('删除成功')
      showDetailDrawer.value = false
      fetchTreeData()
    } catch (error) {
      closeToast()
      showToast('删除失败')
    }
  }).catch(() => {
    // 用户取消，不做任何操作
  })
}

// 搜索
const onSearch = async () => {
  if (!searchKeyword.value.trim()) return
  try {
    const results = await personApi.searchPersons(groupId, searchKeyword.value)
    searchResults.value = results
  } catch (error) {
    showToast('搜索失败')
  }
}

const selectPerson = (person: Person) => {
  const personNode: PersonNode = {
    id: person.id,
    firstName: person.firstName,
    lastName: person.lastName,
    fullName: person.fullName,
    gender: person.gender,
    birthDate: person.birthDate,
    deathDate: person.deathDate,
    primaryPhotoUrl: person.primaryPhotoUrl,
    generation: 0
  }
  onPersonClick(personNode)
  showSearch.value = false
}

const goToWorkspace = () => {
  router.push(`/workspace/${groupId}`)
}

// 图谱渲染相关代码
const initCanvas = () => {
  if (!canvas.value || !treeContainer.value) return
  const container = treeContainer.value
  const cvs = canvas.value

  let width = container.clientWidth
  let height = container.clientHeight

  if (width === 0 || height === 0) {
    const rect = container.getBoundingClientRect()
    width = rect.width
    height = rect.height
  }
  if (width === 0 || height === 0) {
    width = window.innerWidth
    height = window.innerHeight - 100
  }

  cvs.width = width
  cvs.height = height
  ctx = cvs.getContext('2d')
  offsetX = cvs.width / 2
  offsetY = cvs.height / 2
}

const renderTree = () => {
  if (!ctx || !canvas.value) return
  const cvs = canvas.value
  ctx.clearRect(0, 0, cvs.width, cvs.height)
  ctx.save()
  ctx.translate(offsetX, offsetY)
  ctx.scale(scale, scale)

  ctx.strokeStyle = '#999'
  ctx.lineWidth = 2
  treeData.value.edges.forEach(edge => {
    const fromNode = treeData.value.nodes.find(n => n.id === edge.fromPersonId)
    const toNode = treeData.value.nodes.find(n => n.id === edge.toPersonId)
    if (fromNode && toNode) {
      ctx!.beginPath()
      ctx!.moveTo(fromNode.x || 0, fromNode.y || 0)
      ctx!.lineTo(toNode.x || 0, toNode.y || 0)
      ctx!.stroke()
    }
  })

  treeData.value.nodes.forEach(node => {
    const x = node.x ?? 0
    const y = node.y ?? 0
    ctx!.beginPath()
    ctx!.arc(x, y, 30, 0, Math.PI * 2)
    ctx!.fillStyle = node.gender === 'MALE' ? '#e3f2fd' : '#fce4ec'
    ctx!.fill()
    ctx!.strokeStyle = node.gender === 'MALE' ? '#2196f3' : '#e91e63'
    ctx!.lineWidth = 3
    ctx!.stroke()
    ctx!.fillStyle = '#333'
    ctx!.font = '14px sans-serif'
    ctx!.textAlign = 'center'
    ctx!.fillText(node.fullName, x, y + 50)
  })

  ctx.restore()
}

const handleTouchStart = (e: TouchEvent) => {
  if (e.touches.length === 1) {
    isDragging = true
    lastTouchX = e.touches[0].clientX
    lastTouchY = e.touches[0].clientY
  }
}

const handleTouchMove = (e: TouchEvent) => {
  if (!isDragging || e.touches.length !== 1) return
  e.preventDefault()
  const touchX = e.touches[0].clientX
  const touchY = e.touches[0].clientY
  offsetX += touchX - lastTouchX
  offsetY += touchY - lastTouchY
  lastTouchX = touchX
  lastTouchY = touchY
  renderTree()
}

const handleTouchEnd = () => {
  isDragging = false
}

const handleCanvasClick = (e: MouseEvent) => {
  if (!canvas.value) return
  const rect = canvas.value.getBoundingClientRect()
  const clickX = (e.clientX - rect.left - offsetX) / scale
  const clickY = (e.clientY - rect.top - offsetY) / scale

  const clickedNode = treeData.value.nodes.find(node => {
    const x = node.x || 0
    const y = node.y || 0
    const distance = Math.sqrt((clickX - x) ** 2 + (clickY - y) ** 2)
    return distance <= 30
  })

  if (clickedNode) {
    onPersonClick(clickedNode)
  }
}

const handleResize = () => {
  if (viewMode.value === 'graph') {
    initCanvas()
    renderTree()
  }
}

onMounted(() => {
  fetchTreeData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.tree-view-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.tree-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: white;
}

.loading-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #999;
}

.empty-tree {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.tree-canvas {
  width: 100%;
  height: 100%;
  touch-action: none;
}

.graph-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background: white;
  display: flex;
  justify-content: space-around;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
  z-index: 100;
}
</style>
