<template>
  <div class="merge-request-list-page">
    <van-nav-bar
      title="合并请求"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    />
    
    <van-tabs v-model:active="activeTab">
      <van-tab title="待审核" :badge="pendingCount">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-empty v-if="pendingList.length === 0" description="暂无待审核请求" />
          
          <van-cell-group v-for="mr in pendingList" :key="mr.id" inset style="margin-bottom: 10px;">
            <van-cell :title="mr.title" :label="mr.description || '暂无描述'">
              <template #value>
                <van-tag type="warning">待审核</van-tag>
              </template>
            </van-cell>
            <van-cell :title="`变更数: ${mr.changeCount}`" :label="formatDate(mr.createdAt)">
              <template #value>
                <van-button
                  v-if="isGroupAdmin(mr.groupId)"
                  type="primary"
                  size="small"
                  @click="approveMergeRequest(mr.id)"
                >
                  批准
                </van-button>
                <van-button
                  v-if="isGroupAdmin(mr.groupId)"
                  type="danger"
                  size="small"
                  plain
                  style="margin-left: 8px;"
                  @click="rejectMergeRequest(mr.id)"
                >
                  拒绝
                </van-button>
              </template>
            </van-cell>
          </van-cell-group>
        </van-pull-refresh>
      </van-tab>
      
      <van-tab title="已处理">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-empty v-if="processedList.length === 0" description="暂无已处理请求" />
          
          <van-cell-group v-for="mr in processedList" :key="mr.id" inset style="margin-bottom: 10px;">
            <van-cell :title="mr.title" :label="mr.description || '暂无描述'">
              <template #value>
                <van-tag :type="mr.status === 'APPROVED' ? 'success' : 'danger'">
                  {{ mr.status === 'APPROVED' ? '已批准' : '已拒绝' }}
                </van-tag>
              </template>
            </van-cell>
            <van-cell :title="`变更数: ${mr.changeCount}`" :label="formatDate(mr.createdAt)">
              <template #value>
                <span v-if="mr.reviewedByName">审核人: {{ mr.reviewedByName }}</span>
              </template>
            </van-cell>
          </van-cell-group>
        </van-pull-refresh>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { showToast, showLoadingToast, closeToast, showDialog } from 'vant'
import { mergeRequestApi, groupApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { MergeRequest } from '@/types'

const userStore = useUserStore()

const activeTab = ref(0)
const mergeRequests = ref<MergeRequest[]>([])
const refreshing = ref(false)
const adminGroupIds = ref<string[]>([])

const pendingList = computed(() => mergeRequests.value.filter(mr => mr.status === 'PENDING'))
const processedList = computed(() => mergeRequests.value.filter(mr => mr.status !== 'PENDING'))
const pendingCount = computed(() => pendingList.value.length)

const fetchMergeRequests = async () => {
  try {
    // 获取用户参与的所有家族的合并请求
    const groups = await groupApi.getMyGroups()
    adminGroupIds.value = groups.filter(g => g.adminId === userStore.userId).map(g => g.id)
    
    const allRequests: MergeRequest[] = []
    for (const group of groups) {
      const requests = await mergeRequestApi.getGroupMergeRequests(group.id)
      allRequests.push(...requests)
    }
    
    mergeRequests.value = allRequests
  } catch (error) {
    showToast('获取合并请求失败')
  }
}

const onRefresh = async () => {
  refreshing.value = true
  await fetchMergeRequests()
  refreshing.value = false
}

const isGroupAdmin = (groupId: string) => {
  return adminGroupIds.value.includes(groupId)
}

const approveMergeRequest = async (mergeRequestId: string) => {
  showLoadingToast({ message: '处理中...', forbidClick: true })
  
  try {
    const result = await mergeRequestApi.approveMergeRequest(mergeRequestId)
    
    if (result.success) {
      closeToast()
      showToast('已批准')
      await fetchMergeRequests()
    } else if (result.conflicts && result.conflicts.length > 0) {
      closeToast()
      showDialog({
        title: '存在冲突',
        message: '该合并请求存在冲突，需要提交者解决后重新提交。'
      })
    }
  } catch (error) {
    closeToast()
    showToast('操作失败')
  }
}

const rejectMergeRequest = async (mergeRequestId: string) => {
  try {
    await mergeRequestApi.rejectMergeRequest(mergeRequestId, '不符合合并要求')
    showToast('已拒绝')
    await fetchMergeRequests()
  } catch (error) {
    showToast('操作失败')
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  fetchMergeRequests()
})
</script>

<style scoped>
.merge-request-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}
</style>
