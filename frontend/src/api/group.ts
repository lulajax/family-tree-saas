import request from './request'
import type { Group } from '@/types'

export interface CreateGroupRequest {
  name: string
  description?: string
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
  }
}
