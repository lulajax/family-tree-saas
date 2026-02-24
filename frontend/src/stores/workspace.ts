import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { workspaceApi } from '@/api'
import type { Workspace, ChangeSet, MergeRequest } from '@/types'
import type { CreatePersonRequest, UpdatePersonRequest, CommitChangesRequest } from '@/api'

export const useWorkspaceStore = defineStore('workspace', () => {
  // State
  const currentWorkspace = ref<Workspace | null>(null)
  const changes = ref<ChangeSet[]>([])
  const loading = ref(false)
  
  // Getters
  const isDirty = computed(() => changes.value.length > 0)
  const changeCount = computed(() => changes.value.length)
  const isEditable = computed(() => currentWorkspace.value?.isEditable ?? false)
  
  // Actions
  const createOrGetWorkspace = async (groupId: string) => {
    loading.value = true
    try {
      const data = await workspaceApi.createOrGetWorkspace(groupId)
      currentWorkspace.value = data
      if (data.id) {
        await fetchChanges(data.id)
      }
      return data
    } finally {
      loading.value = false
    }
  }
  
  const fetchWorkspace = async (workspaceId: string) => {
    loading.value = true
    try {
      const data = await workspaceApi.getWorkspace(workspaceId)
      currentWorkspace.value = data
      return data
    } finally {
      loading.value = false
    }
  }
  
  const fetchChanges = async (workspaceId: string) => {
    const data = await workspaceApi.getChanges(workspaceId)
    changes.value = data
    return data
  }
  
  const addPersonChange = async (data: CreatePersonRequest) => {
    if (!currentWorkspace.value) throw new Error('No active workspace')
    
    const change = await workspaceApi.addPersonChange(currentWorkspace.value.id, data)
    changes.value.push(change)
    return change
  }
  
  const updatePersonChange = async (personId: string, data: UpdatePersonRequest) => {
    if (!currentWorkspace.value) throw new Error('No active workspace')
    
    const change = await workspaceApi.updatePersonChange(currentWorkspace.value.id, personId, data)
    changes.value.push(change)
    return change
  }
  
  const deletePersonChange = async (personId: string) => {
    if (!currentWorkspace.value) throw new Error('No active workspace')
    
    const change = await workspaceApi.deletePersonChange(currentWorkspace.value.id, personId)
    changes.value.push(change)
    return change
  }
  
  const commitChanges = async (data: CommitChangesRequest) => {
    if (!currentWorkspace.value) throw new Error('No active workspace')
    
    const mergeRequest = await workspaceApi.commitChanges(currentWorkspace.value.id, data)
    currentWorkspace.value.status = 'SUBMITTED'
    return mergeRequest
  }
  
  const resetWorkspace = async () => {
    if (!currentWorkspace.value) throw new Error('No active workspace')
    
    await workspaceApi.resetWorkspace(currentWorkspace.value.id)
    changes.value = []
    if (currentWorkspace.value) {
      currentWorkspace.value.status = 'EDITING'
    }
  }
  
  const clearWorkspace = () => {
    currentWorkspace.value = null
    changes.value = []
  }
  
  return {
    currentWorkspace,
    changes,
    loading,
    isDirty,
    changeCount,
    isEditable,
    createOrGetWorkspace,
    fetchWorkspace,
    fetchChanges,
    addPersonChange,
    updatePersonChange,
    deletePersonChange,
    commitChanges,
    resetWorkspace,
    clearWorkspace
  }
})
