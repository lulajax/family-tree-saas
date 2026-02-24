import request from './request'
import type { Workspace, ChangeSet, MergeRequest } from '@/types'
import type { CreatePersonRequest, UpdatePersonRequest } from './person'

export interface CommitChangesRequest {
  title: string
  description?: string
}

export const workspaceApi = {
  createOrGetWorkspace: (groupId: string): Promise<Workspace> => {
    return request.post(`/workspaces/${groupId}`)
  },
  
  getWorkspace: (workspaceId: string): Promise<Workspace> => {
    return request.get(`/workspaces/${workspaceId}`)
  },
  
  getChanges: (workspaceId: string): Promise<ChangeSet[]> => {
    return request.get(`/workspaces/${workspaceId}/changes`)
  },
  
  addPersonChange: (workspaceId: string, data: CreatePersonRequest): Promise<ChangeSet> => {
    return request.post(`/workspaces/${workspaceId}/persons`, data)
  },
  
  updatePersonChange: (workspaceId: string, personId: string, data: UpdatePersonRequest): Promise<ChangeSet> => {
    return request.put(`/workspaces/${workspaceId}/persons/${personId}`, data)
  },
  
  deletePersonChange: (workspaceId: string, personId: string): Promise<ChangeSet> => {
    return request.delete(`/workspaces/${workspaceId}/persons/${personId}`)
  },
  
  commitChanges: (workspaceId: string, data: CommitChangesRequest): Promise<MergeRequest> => {
    return request.post(`/workspaces/${workspaceId}/commit`, data)
  },
  
  resetWorkspace: (workspaceId: string): Promise<void> => {
    return request.post(`/workspaces/${workspaceId}/reset`)
  }
}
