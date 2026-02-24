<template>
  <div class="login-page">
    <div class="login-header">
      <h1>家族树</h1>
      <p>记录家族历史，传承家族文化</p>
    </div>
    
    <van-form @submit="onSubmit" class="login-form">
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
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>
      
      <div class="login-actions">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
        >
          登录
        </van-button>
        
        <div class="login-links">
          <router-link to="/register">注册账号</router-link>
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
  password: ''
})

const onSubmit = async () => {
  loading.value = true
  try {
    const response = await authApi.login({
      phone: form.phone,
      password: form.password
    })
    
    userStore.setAuth(response)
    showToast('登录成功')
    router.push('/groups')
  } catch (error) {
    console.error('Login failed:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  padding: 40px 20px;
}

.login-header {
  text-align: center;
  color: white;
  margin-bottom: 60px;
  margin-top: 60px;
}

.login-header h1 {
  font-size: 36px;
  margin-bottom: 10px;
}

.login-header p {
  font-size: 16px;
  opacity: 0.9;
}

.login-form {
  flex: 1;
}

.login-actions {
  margin: 30px 16px;
}

.login-links {
  text-align: center;
  margin-top: 20px;
}

.login-links a {
  color: white;
  text-decoration: none;
  font-size: 14px;
}
</style>
