import { defineStore } from 'pinia'
import { ref } from 'vue'
import { personApi } from '@/api'
import type { Person } from '@/types'

export const usePersonStore = defineStore('person', () => {
  // State
  const persons = ref<Person[]>([])
  const currentPerson = ref<Person | null>(null)
  const loading = ref(false)
  
  // Actions
  const fetchGroupPersons = async (groupId: string) => {
    loading.value = true
    try {
      const data = await personApi.getGroupPersons(groupId)
      persons.value = data
      return data
    } finally {
      loading.value = false
    }
  }
  
  const fetchPerson = async (personId: string) => {
    loading.value = true
    try {
      const data = await personApi.getPerson(personId)
      currentPerson.value = data
      return data
    } finally {
      loading.value = false
    }
  }
  
  const searchPersons = async (groupId: string, keyword: string) => {
    loading.value = true
    try {
      const data = await personApi.searchPersons(groupId, keyword)
      return data
    } finally {
      loading.value = false
    }
  }
  
  const setCurrentPerson = (person: Person | null) => {
    currentPerson.value = person
  }
  
  return {
    persons,
    currentPerson,
    loading,
    fetchGroupPersons,
    fetchPerson,
    searchPersons,
    setCurrentPerson
  }
})
