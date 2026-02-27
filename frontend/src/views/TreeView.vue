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

      <!-- 血统线筛选栏 -->
      <div v-if="viewMode === 'graph' && treeData.nodes.length > 0" class="lineage-filter-bar">
        <div class="lineage-tabs">
          <button
            v-for="filter in LINEAGE_FILTERS"
            :key="filter.type"
            class="lineage-tab"
            :class="{ active: lineageFilter === filter.type }"
            :style="{
              backgroundColor: lineageFilter === filter.type ? filter.borderColor : '#f3f4f6',
              color: lineageFilter === filter.type ? '#fff' : filter.color,
              borderColor: filter.borderColor
            }"
            @click="onLineageChange(filter.type)"
          >
            <span class="lineage-dot" :style="{ backgroundColor: lineageFilter === filter.type ? '#fff' : filter.borderColor }"></span>
            {{ filter.label }}
          </button>
        </div>
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
import type { TreeView, Person, PersonNode, PersonRelations, RelationshipEdge, LineageType } from '@/types'
import { LINEAGE_FILTERS } from '@/types'
import PersonListView from '@/components/PersonListView.vue'
import PersonDetailDrawer from '@/components/PersonDetailDrawer.vue'
import AddPersonWizard from '@/components/AddPersonWizard.vue'

const route = useRoute()
const router = useRouter()
const groupId = route.params.id as string

// 视图模式: 'list' | 'graph'
const viewMode = ref<'list' | 'graph'>('list')

// 血统线筛选: 'BOTH' | 'FATHER_LINE' | 'MOTHER_LINE'
const lineageFilter = ref<LineageType | 'BOTH'>('BOTH')

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
const NODE_RADIUS = 30

type PositionedPersonNode = PersonNode & { x: number; y: number }

const drawSegment = (
  canvasCtx: CanvasRenderingContext2D,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  style: string,
  width: number,
  dash: number[] = []
) => {
  canvasCtx.save()
  canvasCtx.strokeStyle = style
  canvasCtx.lineWidth = width
  canvasCtx.setLineDash(dash)
  canvasCtx.beginPath()
  canvasCtx.moveTo(x1, y1)
  canvasCtx.lineTo(x2, y2)
  canvasCtx.stroke()
  canvasCtx.restore()
}

const drawBundledParentEdges = (
  canvasCtx: CanvasRenderingContext2D,
  nodeMap: Map<string, PositionedPersonNode>,
  parentLinks: Array<{ parentId: string; childId: string }>
) => {
  if (parentLinks.length === 0) return

  const childParentMap = new Map<string, Set<string>>()
  parentLinks.forEach(({ parentId, childId }) => {
    if (!nodeMap.has(parentId) || !nodeMap.has(childId) || parentId === childId) return
    if (!childParentMap.has(childId)) {
      childParentMap.set(childId, new Set())
    }
    childParentMap.get(childId)!.add(parentId)
  })

  const familyGroups = new Map<string, { parentIds: string[]; childIds: string[] }>()
  childParentMap.forEach((parentSet, childId) => {
    const parentIds = Array.from(parentSet).sort()
    const key = parentIds.join('|')
    if (!familyGroups.has(key)) {
      familyGroups.set(key, { parentIds, childIds: [] })
    }
    familyGroups.get(key)!.childIds.push(childId)
  })

  familyGroups.forEach(group => {
    const parents = group.parentIds
      .map(id => nodeMap.get(id))
      .filter((node): node is PositionedPersonNode => Boolean(node))
      .sort((a, b) => a.x - b.x)
    const children = group.childIds
      .map(id => nodeMap.get(id))
      .filter((node): node is PositionedPersonNode => Boolean(node))
      .sort((a, b) => a.x - b.x)

    if (parents.length === 0 || children.length === 0) return

    const avgParentY = parents.reduce((sum, p) => sum + p.y, 0) / parents.length
    const avgChildY = children.reduce((sum, c) => sum + c.y, 0) / children.length
    const direction = avgChildY >= avgParentY ? 1 : -1

    const parentAnchorY = direction === 1
      ? Math.max(...parents.map(p => p.y)) + NODE_RADIUS
      : Math.min(...parents.map(p => p.y)) - NODE_RADIUS
    const childAnchorY = direction === 1
      ? Math.min(...children.map(c => c.y)) - NODE_RADIUS
      : Math.max(...children.map(c => c.y)) + NODE_RADIUS

    let parentBusY = parentAnchorY + direction * 20
    let childBusY = childAnchorY - direction * 20
    const gap = direction * (childBusY - parentBusY)
    if (gap < 12) {
      const midY = (parentAnchorY + childAnchorY) / 2
      parentBusY = midY - direction * 6
      childBusY = midY + direction * 6
    }

    const minParentX = Math.min(...parents.map(p => p.x))
    const maxParentX = Math.max(...parents.map(p => p.x))
    const parentCenterX = parents.reduce((sum, p) => sum + p.x, 0) / parents.length

    parents.forEach(parent => {
      drawSegment(
        canvasCtx,
        parent.x,
        parent.y + direction * NODE_RADIUS,
        parent.x,
        parentBusY,
        '#9e9e9e',
        2
      )
    })

    if (parents.length > 1) {
      drawSegment(canvasCtx, minParentX, parentBusY, maxParentX, parentBusY, '#9e9e9e', 2)
    }

    drawSegment(canvasCtx, parentCenterX, parentBusY, parentCenterX, childBusY, '#9e9e9e', 2.2)

    const minChildX = Math.min(...children.map(c => c.x))
    const maxChildX = Math.max(...children.map(c => c.x))
    if (children.length > 1) {
      drawSegment(canvasCtx, minChildX, childBusY, maxChildX, childBusY, '#9e9e9e', 2)
    }

    children.forEach(child => {
      if (children.length === 1 && Math.abs(child.x - parentCenterX) > 1) {
        drawSegment(canvasCtx, parentCenterX, childBusY, child.x, childBusY, '#9e9e9e', 2)
      }
      drawSegment(
        canvasCtx,
        child.x,
        childBusY,
        child.x,
        child.y - direction * NODE_RADIUS,
        '#9e9e9e',
        2
      )
    })
  })
}

const drawNonParentEdges = (
  canvasCtx: CanvasRenderingContext2D,
  nodeMap: Map<string, PositionedPersonNode>,
  edges: RelationshipEdge[]
) => {
  edges.forEach(edge => {
    const fromNode = nodeMap.get(edge.fromPersonId)
    const toNode = nodeMap.get(edge.toPersonId)
    if (!fromNode || !toNode) return

    if (edge.type === 'SPOUSE') {
      drawSegment(canvasCtx, fromNode.x, fromNode.y, toNode.x, toNode.y, '#e28cb5', 2.2)
      return
    }
    if (edge.type === 'SIBLING') {
      drawSegment(canvasCtx, fromNode.x, fromNode.y, toNode.x, toNode.y, '#90a4c8', 1.4, [6, 4])
      return
    }
    drawSegment(canvasCtx, fromNode.x, fromNode.y, toNode.x, toNode.y, '#b0b0b0', 1.6)
  })
}

// 获取家谱数据
const fetchTreeData = async () => {
  loading.value = true
  try {
    const lineageParam = lineageFilter.value === 'BOTH' ? undefined : lineageFilter.value
    const data = await treeApi.getTreeView(groupId, undefined, 3, lineageParam)
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
    const lineageParam = lineageFilter.value === 'BOTH' ? undefined : lineageFilter.value
    const data = await treeApi.getTreeView(groupId, personId, 3, lineageParam)
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

// 切换血统线筛选
const onLineageChange = async (lineage: LineageType | 'BOTH') => {
  lineageFilter.value = lineage
  await fetchTreeData()
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
    generation: 0,
    lineageType: 'SELF'
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

  const positionedNodes: PositionedPersonNode[] = treeData.value.nodes.map(node => ({
    ...node,
    x: node.x ?? 0,
    y: node.y ?? 0
  }))
  const nodeMap = new Map(positionedNodes.map(node => [node.id, node]))
  const parentLinks: Array<{ parentId: string; childId: string }> = []
  const nonParentEdges: RelationshipEdge[] = []

  treeData.value.edges.forEach(edge => {
    if (edge.type === 'PARENT') {
      parentLinks.push({ parentId: edge.fromPersonId, childId: edge.toPersonId })
      return
    }
    if (edge.type === 'CHILD') {
      parentLinks.push({ parentId: edge.toPersonId, childId: edge.fromPersonId })
      return
    }
    nonParentEdges.push(edge)
  })

  drawBundledParentEdges(ctx, nodeMap, parentLinks)
  drawNonParentEdges(ctx, nodeMap, nonParentEdges)

  positionedNodes.forEach(node => {
    const x = node.x
    const y = node.y

    // 根据血统线确定颜色
    const getNodeColors = (lineageType: LineageType) => {
      switch (lineageType) {
        case 'FATHER_LINE':
          return { fill: '#dbeafe', stroke: '#2563eb', text: '#1e40af' }
        case 'MOTHER_LINE':
          return { fill: '#fce7f3', stroke: '#ec4899', text: '#be185d' }
        case 'SELF':
          return { fill: '#fef3c7', stroke: '#f59e0b', text: '#92400e' }
        default:
          return { fill: '#f3f4f6', stroke: '#9ca3af', text: '#4b5563' }
      }
    }

    const colors = getNodeColors(node.lineageType)

    ctx!.beginPath()
    ctx!.arc(x, y, NODE_RADIUS, 0, Math.PI * 2)
    ctx!.fillStyle = colors.fill
    ctx!.fill()
    ctx!.strokeStyle = colors.stroke
    ctx!.lineWidth = 3
    ctx!.stroke()

    // 绘制性别图标（小圆点）
    ctx!.beginPath()
    ctx!.arc(x + NODE_RADIUS - 8, y - NODE_RADIUS + 8, 6, 0, Math.PI * 2)
    ctx!.fillStyle = node.gender === 'MALE' ? '#3b82f6' : (node.gender === 'FEMALE' ? '#ec4899' : '#9ca3af')
    ctx!.fill()
    ctx!.strokeStyle = '#fff'
    ctx!.lineWidth = 1.5
    ctx!.stroke()

    // 绘制名字
    ctx!.fillStyle = colors.text
    ctx!.font = 'bold 13px sans-serif'
    ctx!.textAlign = 'center'
    ctx!.fillText(node.fullName, x, y + 50)

    // 绘制血统线标签（小字）
    if (node.lineageType !== 'SELF' && node.lineageType !== 'UNKNOWN') {
      ctx!.fillStyle = colors.stroke
      ctx!.font = '10px sans-serif'
      const label = node.lineageType === 'FATHER_LINE' ? '父系' : '母系'
      ctx!.fillText(label, x, y + 64)
    }
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
  bottom: 56px;
  left: 0;
  right: 0;
  padding: 8px 16px;
  background: white;
  display: flex;
  justify-content: space-around;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.lineage-filter-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 16px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  background: white;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.12);
  z-index: 101;
}

.lineage-tabs {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.lineage-tab {
  flex: 1;
  max-width: 100px;
  padding: 8px 12px;
  border: 1px solid;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #6b7280;
}

.lineage-tab.active {
  font-weight: 600;
}

.lineage-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
</style>
