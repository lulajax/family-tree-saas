<template>
  <div class="group-detail-page">
    <van-nav-bar
      :title="group?.name || '家族详情'"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    />
    
    <div v-if="loading" class="loading-state">
      <van-skeleton title :row="5" />
    </div>
    
    <template v-else-if="group">
      <!-- 家族信息卡片 -->
      <div class="group-info-card">
        <div class="group-header">
          <div class="group-avatar-large">
            {{ group.name.charAt(0) }}
          </div>
          <div class="group-meta">
            <h2>{{ group.name }}</h2>
            <p v-if="group.description">{{ group.description }}</p>
          </div>
        </div>
        
        <van-grid :column-num="3" :border="false">
          <van-grid-item>
            <div class="stat-number">{{ group.memberCount }}</div>
            <div class="stat-label">成员</div>
          </van-grid-item>
          <van-grid-item>
            <div class="stat-number">{{ group.personCount }}</div>
            <div class="stat-label">人物</div>
          </van-grid-item>
          <van-grid-item>
            <div class="stat-number">{{ group.version }}</div>
            <div class="stat-label">版本</div>
          </van-grid-item>
        </van-grid>
      </div>
      
      <!-- 功能入口 -->
      <van-cell-group inset class="feature-list">
        <van-cell
          title="家谱图谱"
          icon="cluster-o"
          is-link
          @click="goToTree"
        />
        <van-cell
          title="人物列表"
          icon="friends-o"
          is-link
          @click="goToMembers"
        />
        <van-cell
          title="编辑家谱"
          icon="edit"
          is-link
          @click="goToWorkspace"
        />
        <van-cell
          title="合并请求"
          icon="records"
          is-link
          @click="goToMergeRequests"
        >
          <template #value>
            <van-badge v-if="pendingMergeCount > 0" :content="pendingMergeCount" />
          </template>
        </van-cell>
      </van-cell-group>
      
      <!-- 最近更新 -->
      <van-cell-group inset title="最近更新" class="recent-updates">
        <van-cell v-if="recentPersons.length === 0" title="暂无更新" />
        <van-cell
          v-for="person in recentPersons"
          :key="person.id"
          :title="person.fullName"
          :label="formatDate(person.updatedAt)"
          center
          @click="goToPerson(person.id)"
        >
          <template #icon>
            <van-image
              round
              width="40"
              height="40"
              :src="person.primaryPhotoUrl || '/default-avatar.png'"
              style="margin-right: 12px;"
            />
          </template>
        </van-cell>
      </van-cell-group>
    </template>
    
    <van-empty v-else description="家族不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import dayjs from 'dayjs'
import { useGroupStore } from '@/stores/group'
import { usePersonStore } from '@/stores/person'
import { groupApi, mergeRequestApi } from '@/api'
import type { Group, Person } from '@/types'

const route = useRoute()
const router = useRouter()
const groupStore = useGroupStore()
const personStore = usePersonStore()

const groupId = route.params.id as string
const group = ref<Group | null>(null)
const loading = ref(false)
const recentPersons = ref<Person[]>([])
const pendingMergeCount = ref(0)

const fetchGroupDetail = async () => {
  loading.value = true
  try {
    const data = await groupApi.getGroup(groupId)
    group.value = data
    groupStore.setCurrentGroup(data)
    
    // 获取人物列表
    const persons = await personStore.fetchGroupPersons(groupId)
    recentPersons.value = persons.slice(0, 5)
    
    // 获取待处理合并请求数量
    const mergeRequests = await mergeRequestApi.getGroupMergeRequests(groupId)
    pendingMergeCount.value = mergeRequests.filter(mr => mr.status === 'PENDING').length
  } catch (error) {
    showToast('获取家族信息失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const goToTree = () => {
  router.push(`/groups/${groupId}/tree`)
}

const goToMembers = () => {
  router.push(`/groups/${groupId}/members`)
}

const goToWorkspace = () => {
  router.push(`/workspace/${groupId}`)
}

const goToMergeRequests = () => {
  router.push('/merge-requests')
}

const goToPerson = (personId: string) => {
  router.push(`/person/${personId}`)
}

onMounted(() => {
  fetchGroupDetail()
})
</script>

<style scoped>
.group-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.loading-state {
  padding: 20px;
}

.group-info-card {
  background: white;
  margin: 10px;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.group-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.group-avatar-large {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: bold;
  margin-right: 16px;
}

.group-meta h2 {
  font-size: 20px;
  margin: 0 0 4px 0;
}

.group-meta p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.feature-list {
  margin: 10px;
}

.recent-updates {
  margin: 10px;
}
</style>
