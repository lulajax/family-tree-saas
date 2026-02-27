import request from './request'
import type { TreeView, PersonNode, LineageType } from '@/types'

export const treeApi = {
  getTreeView: (groupId: string, focusPersonId?: string, depth: number = 3, lineage?: LineageType): Promise<TreeView> => {
    return request.get(`/groups/${groupId}/tree`, {
      params: { focusPersonId, depth, lineage }
    })
  },
  
  getAncestors: (groupId: string, personId: string, generations: number = 5): Promise<PersonNode[]> => {
    return request.get(`/groups/${groupId}/tree/persons/${personId}/ancestors`, {
      params: { generations }
    })
  },
  
  getDescendants: (groupId: string, personId: string, generations: number = 3): Promise<PersonNode[]> => {
    return request.get(`/groups/${groupId}/tree/persons/${personId}/descendants`, {
      params: { generations }
    })
  }
}
