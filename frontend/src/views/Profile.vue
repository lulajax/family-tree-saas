<template>
  <div class="profile-page">
    <van-nav-bar title="我的" fixed placeholder />
    
    <div class="profile-header">
      <div class="user-info">
        <van-image
          round
          width="80"
          height="80"
          :src="user?.avatarUrl || '/default-avatar.png'"
        />
        <div class="user-meta">
          <h3>{{ user?.nickname || user?.phone }}</h3>
          <p>{{ user?.phone }}</p>
        </div>
      </div>
    </div>
    
    <van-cell-group inset class="profile-menu">
      <van-cell title="我的家族" icon="friends-o" is-link to="/groups" />
      <van-cell title="合并请求" icon="records" is-link to="/merge-requests">
        <template #value>
          <van-badge v-if="pendingCount > 0" :content="pendingCount" />
        </template>
      </van-cell>
      <van-cell title="编辑资料" icon="edit" is-link @click="showEditProfile = true" />
    </van-cell-group>
    
    <van-cell-group inset class="profile-menu">
      <van-cell title="关于我们" icon="info-o" is-link />
      <van-cell title="意见反馈" icon="comment-o" is-link />
    </van-cell-group>
    
    <div class="logout-button">
      <van-button block type="danger" plain @click="logout">
        退出登录
      </van-button>
    </div>
    
    <!-- 底部导航 -->
    <van-tabbar v-model="activeTab" route>
      <van-tabbar-item icon="friends-o" to="/groups">家族</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
    
    <!-- 编辑资料弹窗 -->
    <van-popup
      v-model:show="showEditProfile"
      position="bottom"
      :style="{ height: '50%' }"
      round
    >
      <van-nav-bar
        title="编辑资料"
        left-arrow
        @click-left="showEditProfile = false"
      />
      <van-form @submit="updateProfile">
        <van-cell-group inset>
          <van-field
            v-model="editForm.nickname"
            name="nickname"
            label="昵称"
            placeholder="请输入昵称"
          />
        </van-cell-group>
        <div style="margin: 16px;">
          <van-button round block type="primary" native-type="submit">
            保存
          </van-button>
        </div>
      </van-form>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores/user'
import { useGroupStore } from '@/stores/group'
import { mergeRequestApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const groupStore = useGroupStore()

const activeTab = ref(1)
const showEditProfile = ref(false)
const pendingCount = ref(0)

const user = computed(() => userStore.user)

const editForm = ref({
  nickname: user.value?.nickname || ''
})

const fetchPendingCount = async () => {
  try {
    const groups = await groupStore.fetchMyGroups()
    let count = 0
    for (const group of groups) {
      const requests = await mergeRequestApi.getGroupMergeRequests(group.id)
      count += requests.filter(r => r.status === 'PENDING').length
    }
    pendingCount.value = count
  } catch (error) {
    console.error('Failed to fetch pending count:', error)
  }
}

const updateProfile = async () => {
  userStore.updateUser({ nickname: editForm.value.nickname })
  showToast('保存成功')
  showEditProfile.value = false
}

const logout = async () => {
  try {
    await showConfirmDialog({
      title: '确认退出',
      message: '确定要退出登录吗？'
    })
    
    userStore.logout()
    router.push('/login')
  } catch (error) {
    // 用户取消
  }
}

onMounted(() => {
  fetchPendingCount()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 60px;
}

.profile-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.user-info {
  display: flex;
  align-items: center;
  color: white;
}

.user-meta {
  margin-left: 16px;
}

.user-meta h3 {
  font-size: 20px;
  margin: 0 0 4px 0;
}

.user-meta p {
  font-size: 14px;
  opacity: 0.8;
  margin: 0;
}

.profile-menu {
  margin: 10px;
}

.logout-button {
  margin: 30px 16px;
}
</style>
