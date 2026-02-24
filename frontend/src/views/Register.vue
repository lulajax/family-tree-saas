<template>
  <div class="register-page">
    <van-nav-bar
      title="注册账号"
      left-arrow
      @click-left="$router.back()"
    />
    
    <van-form @submit="onSubmit" class="register-form">
      <van-cell-group inset>
        <van-field
          v-model="form.phone"
          name="phone"
          label="手机号"
          placeholder="请输入手机号"
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
          ]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码（6-20位）"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 6, max: 20, message: '密码长度6-20位' }
          ]"
        />
        <van-field
          v-model="form.nickname"
          name="nickname"
          label="昵称"
          placeholder="请输入昵称（选填）"
        />
      </van-cell-group>
      
      <div class="register-actions">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
        >
          注册
        </van-button>
        
        <div class="register-links">
          <router-link to="/login">已有账号？去登录</router-link>
        </div>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
  nickname: ''
})

const onSubmit = async () => {
  loading.value = true
  try {
    const response = await authApi.register({
      phone: form.phone,
      password: form.password,
      nickname: form.nickname
    })
    
    userStore.setAuth(response)
    showToast('注册成功')
    router.push('/groups')
  } catch (error) {
    console.error('Register failed:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.register-form {
  padding-top: 20px;
}

.register-actions {
  margin: 30px 16px;
}

.register-links {
  text-align: center;
  margin-top: 20px;
}

.register-links a {
  color: #1989fa;
  text-decoration: none;
  font-size: 14px;
}
</style>
