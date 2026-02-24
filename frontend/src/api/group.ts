import request from './request'
import type { Group, Relationship } from '@/types'

export interface CreateGroupRequest {
  name: string
  description?: string
}

export interface CreateRelationshipRequest {
  fromPersonId: string
  toPersonId: string
  type: 'PARENT' | 'CHILD' | 'SPOUSE' | 'SIBLING'
  startDate?: string
  endDate?: string
}

export const groupApi = {
  getMyGroups: (): Promise<Group[]> => {
    return request.get('/groups')
  },

  getGroup: (groupId: string): Promise<Group> => {
    return request.get(`/groups/${groupId}`)
  },

  createGroup: (data: CreateGroupRequest): Promise<Group> => {
    return request.post('/groups', data)
  },

  joinGroup: (groupId: string): Promise<void> => {
    return request.post(`/groups/${groupId}/join`)
  },

  leaveGroup: (groupId: string): Promise<void> => {
    return request.post(`/groups/${groupId}/leave`)
  },

  // 关系管理
  createRelationship: (groupId: string, data: CreateRelationshipRequest): Promise<Relationship> => {
    return request.post(`/groups/${groupId}/relationships`, data)
  },

  deleteRelationship: (groupId: string, relationshipId: string): Promise<void> => {
    return request.delete(`/groups/${groupId}/relationships/${relationshipId}`)
  }
}
