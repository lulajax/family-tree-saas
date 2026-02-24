<template>
  <div class="group-list-page">
    <van-nav-bar title="我的家族" fixed placeholder />
    
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div v-if="groups.length === 0 && !loading" class="empty-state">
          <van-empty description="您还没有加入任何家族">
            <van-button round type="primary" @click="showCreateDialog = true">
              创建家族
            </van-button>
          </van-empty>
        </div>
        
        <div v-else class="group-list">
          <van-cell-group inset v-for="group in groups" :key="group.id">
            <van-cell
              :title="group.name"
              :label="group.description || '暂无描述'"
              is-link
              @click="goToGroup(group.id)"
            >
              <template #icon>
                <div class="group-avatar">
                  {{ group.name.charAt(0) }}
                </div>
              </template>
              <template #value>
                <div class="group-stats">
                  <van-tag type="primary" size="small">
                    {{ group.memberCount }} 成员
                  </van-tag>
                  <van-tag type="success" size="small" style="margin-left: 4px;">
                    {{ group.personCount }} 人物
                  </van-tag>
                </div>
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </van-list>
    </van-pull-refresh>
    
    <!-- 底部导航 -->
    <van-tabbar v-model="activeTab" route>
      <van-tabbar-item icon="friends-o" to="/groups">家族</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
    
    <!-- 创建家族弹窗 -->
    <van-dialog
      v-model:show="showCreateDialog"
      title="创建家族"
      show-cancel-button
      @confirm="createGroup"
    >
      <van-field
        v-model="newGroupName"
        label="家族名称"
        placeholder="请输入家族名称"
        :rules="[{ required: true }]"
      />
      <van-field
        v-model="newGroupDescription"
        label="家族描述"
        type="textarea"
        placeholder="请输入家族描述（选填）"
        rows="2"
      />
    </van-dialog>
    
    <!-- 悬浮按钮 -->
    <van-floating-bubble
      v-if="groups.length > 0"
      axis="xy"
      magnetic="x"
      icon="plus"
      @click="showCreateDialog = true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useGroupStore } from '@/stores/group'
import { groupApi } from '@/api'
import type { Group } from '@/types'

const router = useRouter()
const groupStore = useGroupStore()

const activeTab = ref(0)
const loading = ref(false)
const finished = ref(true)
const refreshing = ref(false)
const groups = ref<Group[]>([])

const showCreateDialog = ref(false)
const newGroupName = ref('')
const newGroupDescription = ref('')

const fetchGroups = async () => {
  try {
    const data = await groupStore.fetchMyGroups()
    groups.value = data
  } catch (error) {
    showToast('获取家族列表失败')
  }
}

const onLoad = async () => {
  loading.value = true
  await fetchGroups()
  loading.value = false
  finished.value = true
}

const onRefresh = async () => {
  refreshing.value = true
  await fetchGroups()
  refreshing.value = false
}

const goToGroup = (groupId: string) => {
  router.push(`/groups/${groupId}`)
}

const createGroup = async () => {
  if (!newGroupName.value.trim()) {
    showToast('请输入家族名称')
    return
  }
  
  showLoadingToast({ message: '创建中...', forbidClick: true })
  
  try {
    await groupStore.createGroup(newGroupName.value, newGroupDescription.value)
    closeToast()
    showToast('创建成功')
    newGroupName.value = ''
    newGroupDescription.value = ''
    await fetchGroups()
  } catch (error) {
    closeToast()
    showToast('创建失败')
  }
}

onMounted(() => {
  fetchGroups()
})
</script>

<style scoped>
.group-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 60px;
}

.empty-state {
  padding: 60px 20px;
}

.group-list {
  padding: 10px;
}

.group-avatar {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  margin-right: 12px;
}

.group-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
</style>
