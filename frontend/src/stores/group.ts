import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { groupApi } from '@/api'
import type { Group } from '@/types'

export const useGroupStore = defineStore('group', () => {
  // State
  const groups = ref<Group[]>([])
  const currentGroup = ref<Group | null>(null)
  const loading = ref(false)
  
  // Getters
  const myGroups = computed(() => groups.value)
  
  // Actions
  const fetchMyGroups = async () => {
    loading.value = true
    try {
      const data = await groupApi.getMyGroups()
      groups.value = data
      return data
    } finally {
      loading.value = false
    }
  }
  
  const fetchGroup = async (groupId: string) => {
    loading.value = true
    try {
      const data = await groupApi.getGroup(groupId)
      currentGroup.value = data
      return data
    } finally {
      loading.value = false
    }
  }
  
  const createGroup = async (name: string, description?: string) => {
    const data = await groupApi.createGroup({ name, description })
    groups.value.push(data)
    return data
  }
  
  const joinGroup = async (groupId: string) => {
    await groupApi.joinGroup(groupId)
    await fetchMyGroups()
  }
  
  const leaveGroup = async (groupId: string) => {
    await groupApi.leaveGroup(groupId)
    groups.value = groups.value.filter(g => g.id !== groupId)
  }
  
  const setCurrentGroup = (group: Group | null) => {
    currentGroup.value = group
  }
  
  return {
    groups,
    currentGroup,
    loading,
    myGroups,
    fetchMyGroups,
    fetchGroup,
    createGroup,
    joinGroup,
    leaveGroup,
    setCurrentGroup
  }
})
