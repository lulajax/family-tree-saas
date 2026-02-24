import request from './request'
import type { LoginRequest, RegisterRequest, LoginResponse } from '@/types'

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> => {
    return request.post('/auth/login', data)
  },
  
  register: (data: RegisterRequest): Promise<LoginResponse> => {
    return request.post('/auth/register', data)
  }
}
