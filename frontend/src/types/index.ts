export interface User {
  id: string
  phone: string
  nickname?: string
  avatarUrl?: string
  createdAt: string
}

export interface Group {
  id: string
  name: string
  description?: string
  adminId: string
  adminName?: string
  memberCount: number
  personCount: number
  createdAt: string
  version: number
}

export interface Person {
  id: string
  groupId: string
  firstName: string
  lastName?: string
  fullName: string
  gender: 'MALE' | 'FEMALE' | 'UNKNOWN'
  birthDate?: string
  deathDate?: string
  birthPlace?: string
  currentSpouseId?: string
  currentSpouseName?: string
  primaryPhotoUrl?: string
  photos?: Photo[]
  customAttributes?: CustomAttribute[]
  parentIds?: string[]
  childrenIds?: string[]
  spouseIds?: string[]
  siblingIds?: string[]
  createdAt: string
  updatedAt: string
  version: number
}

export interface Photo {
  id: string
  personId: string
  url: string
  description?: string
  takenAt?: string
  isPrimary: boolean
  createdAt: string
}

export interface CustomAttribute {
  id: string
  personId: string
  key: string
  value: string
  dataType: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN'
}

export interface Relationship {
  id: string
  groupId: string
  fromPersonId: string
  fromPersonName?: string
  toPersonId: string
  toPersonName?: string
  type: 'PARENT' | 'CHILD' | 'SPOUSE' | 'SIBLING'
  startDate?: string
  endDate?: string
}

export interface Workspace {
  id: string
  groupId: string
  groupName?: string
  userId: string
  baseVersion: number
  status: 'EDITING' | 'SUBMITTED' | 'MERGED' | 'CONFLICT'
  changeCount: number
  createdAt: string
  updatedAt: string
  isEditable: boolean
}

export interface ChangeSet {
  id: string
  workspaceId: string
  actionType: 'CREATE' | 'UPDATE' | 'DELETE'
  entityType: 'PERSON' | 'RELATIONSHIP' | 'PHOTO'
  entityId: string
  entityName?: string
  payload: Record<string, any>
  previousPayload?: Record<string, any>
  diff?: Record<string, { old: any; new: any }>
  sequenceNumber: number
  createdAt: string
}

export interface MergeRequest {
  id: string
  workspaceId: string
  groupId: string
  groupName?: string
  title: string
  description?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CONFLICT'
  createdBy: string
  createdByName?: string
  reviewedBy?: string
  reviewedByName?: string
  reviewComment?: string
  changeCount: number
  createdAt: string
  updatedAt: string
}

export interface TreeView {
  focusPersonId: string
  focusPersonName: string
  depth: number
  nodes: PersonNode[]
  edges: RelationshipEdge[]
}

export interface PersonNode {
  id: string
  firstName: string
  lastName?: string
  fullName: string
  gender: 'MALE' | 'FEMALE' | 'UNKNOWN'
  birthDate?: string
  deathDate?: string
  primaryPhotoUrl?: string
  generation: number
  x?: number
  y?: number
}

export interface RelationshipEdge {
  id: string
  fromPersonId: string
  toPersonId: string
  type: 'PARENT' | 'CHILD' | 'SPOUSE' | 'SIBLING'
}

export interface PersonRelations {
  personId: string
  personName: string
  primaryPhotoUrl?: string
  parents?: PersonSummary[]
  spouses?: PersonSummary[]
  children?: PersonSummary[]
  siblings?: PersonSummary[]
}

export interface PersonSummary {
  id: string
  fullName: string
  firstName?: string
  lastName?: string
  gender?: string
  birthDate?: string
  deathDate?: string
  primaryPhotoUrl?: string
  relationType?: string
}

export interface LoginRequest {
  phone: string
  password: string
}

export interface RegisterRequest {
  phone: string
  password: string
  nickname?: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}
