import request from './request'
import type { Person } from '@/types'

export interface CreatePersonRequest {
  groupId: string
  firstName: string
  lastName?: string
  gender?: 'MALE' | 'FEMALE' | 'UNKNOWN'
  birthDate?: string
  deathDate?: string
  birthPlace?: string
  currentSpouseId?: string
}

export interface UpdatePersonRequest {
  firstName?: string
  lastName?: string
  gender?: 'MALE' | 'FEMALE' | 'UNKNOWN'
  birthDate?: string
  deathDate?: string
  birthPlace?: string
  currentSpouseId?: string
}

export const personApi = {
  getGroupPersons: (groupId: string): Promise<Person[]> => {
    return request.get(`/persons/group/${groupId}`)
  },
  
  getPerson: (personId: string): Promise<Person> => {
    return request.get(`/persons/${personId}`)
  },
  
  createPerson: (data: CreatePersonRequest): Promise<Person> => {
    return request.post('/persons', data)
  },
  
  updatePerson: (personId: string, data: UpdatePersonRequest): Promise<Person> => {
    return request.put(`/persons/${personId}`, data)
  },
  
  deletePerson: (personId: string): Promise<void> => {
    return request.delete(`/persons/${personId}`)
  },
  
  searchPersons: (groupId: string, keyword: string): Promise<Person[]> => {
    return request.get(`/persons/group/${groupId}/search`, { params: { keyword } })
  }
}
