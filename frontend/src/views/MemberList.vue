<template>
  <div class="member-list-page">
    <van-nav-bar
      title="人物列表"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    />
    
    <van-search
      v-model="searchKeyword"
      placeholder="搜索人物姓名"
      @search="onSearch"
    />
    
    <van-list
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多了"
      @load="onLoad"
    >
      <van-empty v-if="persons.length === 0 && !loading" description="暂无人物" />
      
      <van-cell-group>
        <van-cell
          v-for="person in persons"
          :key="person.id"
          :title="person.fullName"
          :label="personLabel(person)"
          center
          is-link
          @click="goToPerson(person.id)"
        >
          <template #icon>
            <van-image
              round
              width="44"
              height="44"
              :src="person.primaryPhotoUrl || '/default-avatar.png'"
              style="margin-right: 12px;"
            />
          </template>
          <template #value>
            <van-tag :type="person.gender === 'MALE' ? 'primary' : 'danger'">
              {{ person.gender === 'MALE' ? '男' : person.gender === 'FEMALE' ? '女' : '未知' }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>
    </van-list>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { personApi } from '@/api'
import type { Person } from '@/types'

const route = useRoute()
const router = useRouter()
const groupId = route.params.id as string

const persons = ref<Person[]>([])
const loading = ref(false)
const finished = ref(true)
const searchKeyword = ref('')

const fetchPersons = async () => {
  loading.value = true
  try {
    const data = await personApi.getGroupPersons(groupId)
    persons.value = data
  } catch (error) {
    showToast('获取人物列表失败')
  } finally {
    loading.value = false
  }
}

const onLoad = () => {
  fetchPersons()
}

const onSearch = async () => {
  if (!searchKeyword.value.trim()) {
    fetchPersons()
    return
  }
  
  loading.value = true
  try {
    const data = await personApi.searchPersons(groupId, searchKeyword.value)
    persons.value = data
  } catch (error) {
    showToast('搜索失败')
  } finally {
    loading.value = false
  }
}

const personLabel = (person: Person) => {
  const parts = []
  if (person.birthDate) {
    parts.push(`生于 ${person.birthDate.split('T')[0]}`)
  }
  if (person.birthPlace) {
    parts.push(person.birthPlace)
  }
  return parts.join(' · ') || '暂无信息'
}

const goToPerson = (personId: string) => {
  router.push(`/person/${personId}`)
}

onMounted(() => {
  fetchPersons()
})
</script>

<style scoped>
.member-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}
</style>
