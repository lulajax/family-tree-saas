<template>
  <div class="person-edit-page">
    <van-nav-bar
      title="编辑人物"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    >
      <template #right>
        <van-icon name="success" size="20" @click="onSubmit" />
      </template>
    </van-nav-bar>
    
    <div v-if="loading" class="loading-state">
      <van-skeleton title :row="8" />
    </div>
    
    <van-form v-else ref="formRef" class="edit-form">
      <van-cell-group inset>
        <van-field
          v-model="form.firstName"
          name="firstName"
          label="姓名"
          placeholder="请输入姓名"
          :rules="[{ required: true, message: '请输入姓名' }]"
        />
        <van-field
          v-model="form.lastName"
          name="lastName"
          label="姓氏"
          placeholder="请输入姓氏"
        />
        <van-field name="gender" label="性别">
          <template #input>
            <van-radio-group v-model="form.gender" direction="horizontal">
              <van-radio name="MALE">男</van-radio>
              <van-radio name="FEMALE">女</van-radio>
              <van-radio name="UNKNOWN">未知</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field
          v-model="form.birthDate"
          name="birthDate"
          label="出生日期"
          placeholder="选择日期"
          readonly
          @click="showBirthPicker = true"
        />
        <van-field
          v-model="form.deathDate"
          name="deathDate"
          label="逝世日期"
          placeholder="选择日期（选填）"
          readonly
          @click="showDeathPicker = true"
        />
        <van-field
          v-model="form.birthPlace"
          name="birthPlace"
          label="出生地"
          placeholder="请输入出生地"
        />
      </van-cell-group>
    </van-form>
    
    <!-- 日期选择器 -->
    <van-popup v-model:show="showBirthPicker" position="bottom">
      <van-date-picker
        v-model="birthDatePicker"
        title="选择出生日期"
        @confirm="onBirthDateConfirm"
        @cancel="showBirthPicker = false"
      />
    </van-popup>
    
    <van-popup v-model:show="showDeathPicker" position="bottom">
      <van-date-picker
        v-model="deathDatePicker"
        title="选择逝世日期"
        @confirm="onDeathDateConfirm"
        @cancel="showDeathPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { personApi } from '@/api'
import type { Person } from '@/types'

const route = useRoute()
const router = useRouter()
const personId = route.params.id as string

const loading = ref(false)
const formRef = ref()
const person = ref<Person | null>(null)

const showBirthPicker = ref(false)
const showDeathPicker = ref(false)

const form = reactive({
  firstName: '',
  lastName: '',
  gender: 'MALE' as 'MALE' | 'FEMALE' | 'UNKNOWN',
  birthDate: '',
  deathDate: '',
  birthPlace: ''
})

const birthDatePicker = ref(['1990', '01', '01'])
const deathDatePicker = ref(['2020', '01', '01'])

const fetchPerson = async () => {
  loading.value = true
  try {
    const data = await personApi.getPerson(personId)
    person.value = data
    
    // 填充表单
    form.firstName = data.firstName
    form.lastName = data.lastName || ''
    form.gender = data.gender || 'UNKNOWN'
    form.birthDate = data.birthDate ? data.birthDate.split('T')[0] : ''
    form.deathDate = data.deathDate ? data.deathDate.split('T')[0] : ''
    form.birthPlace = data.birthPlace || ''
    
    if (data.birthDate) {
      const parts = form.birthDate.split('-')
      birthDatePicker.value = parts
    }
    if (data.deathDate) {
      const parts = form.deathDate.split('-')
      deathDatePicker.value = parts
    }
  } catch (error) {
    showToast('获取人物信息失败')
  } finally {
    loading.value = false
  }
}

const onSubmit = async () => {
  if (!form.firstName.trim()) {
    showToast('请输入姓名')
    return
  }
  
  showLoadingToast({ message: '保存中...', forbidClick: true })
  
  try {
    await personApi.updatePerson(personId, {
      firstName: form.firstName,
      lastName: form.lastName || undefined,
      gender: form.gender,
      birthDate: form.birthDate || undefined,
      deathDate: form.deathDate || undefined,
      birthPlace: form.birthPlace || undefined
    })
    
    closeToast()
    showToast('保存成功')
    router.back()
  } catch (error) {
    closeToast()
    showToast('保存失败')
  }
}

const onBirthDateConfirm = ({ selectedValues }: { selectedValues: string[] }) => {
  form.birthDate = selectedValues.join('-')
  showBirthPicker.value = false
}

const onDeathDateConfirm = ({ selectedValues }: { selectedValues: string[] }) => {
  form.deathDate = selectedValues.join('-')
  showDeathPicker.value = false
}

onMounted(() => {
  fetchPerson()
})
</script>

<style scoped>
.person-edit-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.loading-state {
  padding: 20px;
}

.edit-form {
  padding-top: 20px;
}
</style>
