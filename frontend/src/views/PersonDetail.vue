<template>
  <div class="person-detail-page">
    <van-nav-bar
      title="人物详情"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    >
      <template #right>
        <van-icon name="edit" size="20" @click="goToEdit" />
      </template>
    </van-nav-bar>
    
    <div v-if="loading" class="loading-state">
      <van-skeleton title avatar :row="5" />
    </div>
    
    <template v-else-if="person">
      <!-- 人物头部信息 -->
      <div class="person-header">
        <div class="person-avatar-large">
          <img
            :src="person.primaryPhotoUrl || '/default-avatar.png'"
            :alt="person.fullName"
          />
        </div>
        <h2 class="person-name">{{ person.fullName }}</h2>
        <div class="person-tags">
          <van-tag :type="person.gender === 'MALE' ? 'primary' : 'danger'" size="large">
            {{ person.gender === 'MALE' ? '男' : person.gender === 'FEMALE' ? '女' : '未知' }}
          </van-tag>
          <van-tag v-if="isDeceased" type="default" size="large">已故</van-tag>
        </div>
      </div>
      
      <!-- 基本信息 -->
      <van-cell-group inset title="基本信息">
        <van-cell v-if="person.birthDate" title="出生日期" :value="formatDate(person.birthDate)" />
        <van-cell v-if="person.deathDate" title="逝世日期" :value="formatDate(person.deathDate)" />
        <van-cell v-if="person.birthPlace" title="出生地" :value="person.birthPlace" />
        <van-cell v-if="person.currentSpouseName" title="配偶" :value="person.currentSpouseName" />
      </van-cell-group>
      
      <!-- 家族关系 -->
      <van-cell-group inset title="家族关系">
        <van-cell
          v-if="person.parentIds && person.parentIds.length > 0"
          title="父母"
          :value="parentNames"
        />
        <van-cell
          v-if="person.childrenIds && person.childrenIds.length > 0"
          title="子女"
          :value="childrenNames"
        />
        <van-cell
          v-if="person.spouseIds && person.spouseIds.length > 0"
          title="配偶"
          :value="spouseNames"
        />
      </van-cell-group>
      
      <!-- 照片墙 -->
      <van-cell-group inset title="照片">
        <div v-if="person.photos && person.photos.length > 0" class="photo-grid">
          <van-image
            v-for="photo in person.photos"
            :key="photo.id"
            width="100"
            height="100"
            :src="photo.url"
            fit="cover"
            radius="8"
            @click="previewPhoto(photo.url)"
          />
        </div>
        <van-cell v-else title="暂无照片" />
      </van-cell-group>

      <!-- 删除操作 -->
      <div class="delete-section">
        <van-button
          block
          round
          type="danger"
          icon="delete-o"
          @click="onDeletePerson"
        >
          删除人物
        </van-button>
        <p class="delete-tip">删除后将无法恢复，请谨慎操作</p>
      </div>
    </template>
    
    <van-empty v-else description="人物不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showImagePreview, showDialog, showLoadingToast, closeToast } from 'vant'
import { personApi } from '@/api'
import type { Person } from '@/types'

const route = useRoute()
const router = useRouter()
const personId = route.params.id as string

const person = ref<Person | null>(null)
const loading = ref(false)

const isDeceased = computed(() => !!person.value?.deathDate)

const parentNames = computed(() => {
  // 简化实现，实际需要查询
  return `${person.value?.parentIds?.length || 0} 人`
})

const childrenNames = computed(() => {
  return `${person.value?.childrenIds?.length || 0} 人`
})

const spouseNames = computed(() => {
  return `${person.value?.spouseIds?.length || 0} 人`
})

const fetchPerson = async () => {
  loading.value = true
  try {
    const data = await personApi.getPerson(personId)
    person.value = data
  } catch (error) {
    showToast('获取人物信息失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (date: string) => {
  return date.split('T')[0]
}

const goToEdit = () => {
  router.push(`/person/${personId}/edit`)
}

const previewPhoto = (url: string) => {
  showImagePreview([url])
}

const onDeletePerson = () => {
  showDialog({
    title: '确认删除',
    message: `确定要删除 ${person.value?.fullName || '该人物'} 吗？删除后将无法恢复。`,
    showCancelButton: true,
    confirmButtonText: '删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      showLoadingToast({ message: '删除中...', forbidClick: true })
      await personApi.deletePerson(personId)
      closeToast()
      showToast('删除成功')
      // 返回上一页
      router.back()
    } catch (error) {
      closeToast()
      showToast('删除失败')
    }
  }).catch(() => {
    // 用户取消，不做任何操作
  })
}

onMounted(() => {
  fetchPerson()
})
</script>

<style scoped>
.person-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.loading-state {
  padding: 20px;
}

.person-header {
  background: white;
  padding: 30px 20px;
  text-align: center;
  margin-bottom: 10px;
}

.person-avatar-large {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto 16px;
  border: 4px solid #e0e0e0;
}

.person-avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.person-name {
  font-size: 24px;
  margin: 0 0 12px 0;
}

.person-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
  padding: 12px;
}

.delete-section {
  margin: 24px 16px;
  padding-bottom: 24px;
}

.delete-tip {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>
