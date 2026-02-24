import request from './request'
import type { MergeRequest } from '@/types'

export interface ReviewMergeRequest {
  status: 'APPROVED' | 'REJECTED'
  comment?: string
}

export const mergeRequestApi = {
  getGroupMergeRequests: (groupId: string): Promise<MergeRequest[]> => {
    return request.get(`/merge-requests/group/${groupId}`)
  },
  
  getMergeRequest: (mergeRequestId: string): Promise<MergeRequest> => {
    return request.get(`/merge-requests/${mergeRequestId}`)
  },
  
  approveMergeRequest: (mergeRequestId: string): Promise<{ success: boolean; conflicts?: any[] }> => {
    return request.post(`/merge-requests/${mergeRequestId}/approve`)
  },
  
  rejectMergeRequest: (mergeRequestId: string, comment?: string): Promise<void> => {
    return request.post(`/merge-requests/${mergeRequestId}/reject`, { comment })
  }
}
