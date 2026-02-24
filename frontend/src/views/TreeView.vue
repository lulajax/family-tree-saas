<template>
  <div class="tree-view-page">
    <van-nav-bar
      title="家谱图谱"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    >
      <template #right>
        <van-icon name="search" size="20" @click="showSearch = true" />
      </template>
    </van-nav-bar>
    
    <div class="tree-container" ref="treeContainer">
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
    </div>
    
    <!-- 底部人物详情抽屉 -->
    <van-popup
      v-model:show="showDrawer"
      position="bottom"
      :style="{ height: '45%' }"
      round
    >
      <div class="person-drawer" v-if="selectedPerson">
        <div class="drawer-header">
          <div class="person-avatar">
            <img
              :src="selectedPerson.primaryPhotoUrl || '/default-avatar.png'"
              :alt="selectedPerson.fullName"
            />
          </div>
          <div class="person-info">
            <h3>{{ selectedPerson.fullName }}</h3>
            <p v-if="selectedPerson.birthDate">
              {{ formatDate(selectedPerson.birthDate) }}
              <span v-if="selectedPerson.deathDate"> - {{ formatDate(selectedPerson.deathDate) }}</span>
            </p>
            <van-tag :type="selectedPerson.gender === 'MALE' ? 'primary' : 'danger'">
              {{ selectedPerson.gender === 'MALE' ? '男' : selectedPerson.gender === 'FEMALE' ? '女' : '未知' }}
            </van-tag>
          </div>
        </div>
        
        <van-cell-group>
          <van-cell title="查看详情" is-link @click="goToPersonDetail" />
          <van-cell title="设为焦点" is-link @click="setAsFocus" />
          <van-cell title="查看祖先" is-link @click="showAncestors" />
          <van-cell title="查看后代" is-link @click="showDescendants" />
        </van-cell-group>
      </div>
    </van-popup>
    
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
    
    <!-- 悬浮操作按钮 -->
    <div class="fab-menu">
      <van-button
        round
        type="primary"
        icon="plus"
        @click="goToWorkspace"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { treeApi, personApi } from '@/api'
import type { TreeView, PersonNode, Person } from '@/types'

const route = useRoute()
const router = useRouter()
const groupId = route.params.id as string

const treeContainer = ref<HTMLDivElement>()
const canvas = ref<HTMLCanvasElement>()
const loading = ref(false)
const treeData = ref<TreeView>({ focusPersonId: '', focusPersonName: '', depth: 3, nodes: [], edges: [] })
const showDrawer = ref(false)
const selectedPerson = ref<Person | null>(null)
const showSearch = ref(false)
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

const fetchTreeData = async () => {
  loading.value = true
  try {
    const data = await treeApi.getTreeView(groupId, undefined, 3)
    treeData.value = data
    nextTick(() => {
      initCanvas()
      renderTree()
    })
  } catch (error) {
    showToast('获取家谱数据失败')
  } finally {
    loading.value = false
  }
}

const initCanvas = () => {
  if (!canvas.value || !treeContainer.value) return
  
  const container = treeContainer.value
  const cvs = canvas.value
  
  cvs.width = container.clientWidth
  cvs.height = container.clientHeight
  
  ctx = cvs.getContext('2d')
  
  // 初始居中
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
  
  // 绘制边（关系线）
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
  
  // 绘制节点（人物）
  treeData.value.nodes.forEach(node => {
    const x = node.x || 0
    const y = node.y || 0
    
    // 绘制圆形头像背景
    ctx!.beginPath()
    ctx!.arc(x, y, 30, 0, Math.PI * 2)
    ctx!.fillStyle = node.gender === 'MALE' ? '#e3f2fd' : '#fce4ec'
    ctx!.fill()
    ctx!.strokeStyle = node.gender === 'MALE' ? '#2196f3' : '#e91e63'
    ctx!.lineWidth = 3
    ctx!.stroke()
    
    // 绘制姓名
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
  
  // 查找点击的节点
  const clickedNode = treeData.value.nodes.find(node => {
    const x = node.x || 0
    const y = node.y || 0
    const distance = Math.sqrt((clickX - x) ** 2 + (clickY - y) ** 2)
    return distance <= 30
  })
  
  if (clickedNode) {
    selectPersonById(clickedNode.id)
  }
}

const selectPersonById = async (personId: string) => {
  try {
    const person = await personApi.getPerson(personId)
    selectedPerson.value = person
    showDrawer.value = true
  } catch (error) {
    showToast('获取人物信息失败')
  }
}

const formatDate = (date: string) => {
  return date.split('T')[0]
}

const goToPersonDetail = () => {
  if (selectedPerson.value) {
    router.push(`/person/${selectedPerson.value.id}`)
  }
}

const setAsFocus = async () => {
  if (!selectedPerson.value) return
  
  loading.value = true
  try {
    const data = await treeApi.getTreeView(groupId, selectedPerson.value.id, 3)
    treeData.value = data
    showDrawer.value = false
    renderTree()
  } catch (error) {
    showToast('切换焦点失败')
  } finally {
    loading.value = false
  }
}

const showAncestors = async () => {
  if (!selectedPerson.value) return
  
  try {
    const ancestors = await treeApi.getAncestors(groupId, selectedPerson.value.id, 5)
    showToast(`找到 ${ancestors.length} 位祖先`)
  } catch (error) {
    showToast('获取祖先失败')
  }
}

const showDescendants = async () => {
  if (!selectedPerson.value) return
  
  try {
    const descendants = await treeApi.getDescendants(groupId, selectedPerson.value.id, 3)
    showToast(`找到 ${descendants.length} 位后代`)
  } catch (error) {
    showToast('获取后代失败')
  }
}

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
  selectPersonById(person.id)
  showSearch.value = false
}

const goToWorkspace = () => {
  router.push(`/workspace/${groupId}`)
}

onMounted(() => {
  fetchTreeData()
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

.person-drawer {
  padding: 20px;
}

.drawer-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.person-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 16px;
  border: 3px solid #e0e0e0;
}

.person-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.person-info h3 {
  font-size: 20px;
  margin: 0 0 8px 0;
}

.person-info p {
  font-size: 14px;
  color: #666;
  margin: 0 0 8px 0;
}

.fab-menu {
  position: fixed;
  right: 20px;
  bottom: 100px;
  z-index: 100;
}
</style>
