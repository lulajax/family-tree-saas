import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginResponse } from '@/types'

export const useUserStore = defineStore(
  'user',
  () => {
    // State
    const token = ref<string>('')
    const user = ref<User | null>(null)
    
    // Getters
    const isLoggedIn = computed(() => !!token.value)
    const userId = computed(() => user.value?.id)
    
    // Actions
    const setAuth = (data: LoginResponse) => {
      token.value = data.token
      user.value = data.user
    }
    
    const logout = () => {
      token.value = ''
      user.value = null
    }
    
    const updateUser = (data: Partial<User>) => {
      if (user.value) {
        user.value = { ...user.value, ...data }
      }
    }
    
    return {
      token,
      user,
      isLoggedIn,
      userId,
      setAuth,
      logout,
      updateUser
    }
  },
  {
    persist: true
  }
)
