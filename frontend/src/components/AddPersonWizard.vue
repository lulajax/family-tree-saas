<template>
  <van-popup
    v-model:show="visible"
    position="bottom"
    :style="{ height: '85%' }"
    round
    closeable
    close-icon="close"
    @closed="onClosed"
  >
    <div class="add-person-wizard">
      <!-- 步骤指示器 -->
      <div class="wizard-header">
        <h3 class="wizard-title">{{ stepTitle }}</h3>
        <div class="step-indicator">
          <div
            v-for="(_, index) in steps"
            :key="index"
            class="step-dot"
            :class="{ active: currentStep >= index, current: currentStep === index }"
          >
            <van-icon v-if="currentStep > index" name="success" />
            <span v-else>{{ index + 1 }}</span>
          </div>
          <div class="step-line">
            <div class="step-progress" :style="{ width: `${(currentStep / (steps.length - 1)) * 100}%` }" />
          </div>
        </div>
      </div>

      <!-- 步骤 1: 选择关系类型 -->
      <div v-if="currentStep === 0" class="wizard-body">
        <div class="step-intro">
          <p class="intro-text">
            为 <strong>{{ targetPersonName }}</strong> 添加关系
          </p>
        </div>

        <div class="relation-grid">
          <div
            v-for="option in relationOptions"
            :key="option.value"
            class="relation-card"
            :class="{ selected: selectedRelation === option.value, disabled: isOptionDisabled(option.value) }"
            @click="!isOptionDisabled(option.value) && (selectedRelation = option.value)"
          >
            <div class="card-icon" :style="{ background: option.color }">
              <van-icon :name="option.icon" />
            </div>
            <div class="card-content">
              <span class="card-title">{{ option.label }}</span>
              <span class="card-desc">{{ option.desc }}</span>
            </div>
            <div class="card-check" v-if="selectedRelation === option.value">
              <van-icon name="success" />
            </div>
            <div class="card-disabled" v-if="isOptionDisabled(option.value)">
              <span>已达上限</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤 2: 填写基本信息 -->
      <div v-if="currentStep === 1" class="wizard-body">
        <div class="step-intro">
          <p class="intro-text">填写{{ relationLabel }}的信息</p>
        </div>

        <div class="form-container">
          <!-- 姓名 -->
          <div class="form-group">
            <label class="form-label required">姓名</label>
            <div class="name-inputs">
              <van-field
                v-model="formData.lastName"
                placeholder="姓"
                class="name-field last-name"
              />
              <van-field
                v-model="formData.firstName"
                placeholder="名"
                class="name-field first-name"
                :error-message="nameError"
              />
            </div>
          </div>

          <!-- 性别 -->
          <div class="form-group">
            <label class="form-label required">性别</label>
            <div class="gender-options">
              <div
                class="gender-card"
                :class="{ selected: formData.gender === 'MALE' }"
                @click="formData.gender = 'MALE'"
              >
                <div class="gender-icon male">
                  <van-icon name="manager-o" />
                </div>
                <span>男</span>
              </div>
              <div
                class="gender-card"
                :class="{ selected: formData.gender === 'FEMALE' }"
                @click="formData.gender = 'FEMALE'"
              >
                <div class="gender-icon female">
                  <van-icon name="manager-o" />
                </div>
                <span>女</span>
              </div>
            </div>
          </div>

          <!-- 出生日期 -->
          <div class="form-group">
            <label class="form-label">出生日期</label>
            <van-field
              v-model="formData.birthDate"
              placeholder="请选择出生日期"
              readonly
              right-icon="calendar-o"
              @click="showBirthPicker = true"
            />
          </div>

          <!-- 逝世日期 -->
          <div class="form-group">
            <label class="form-label">
              逝世日期
              <span class="optional">（选填）</span>
            </label>
            <van-field
              v-model="formData.deathDate"
              placeholder="如已去世，请选择日期"
              readonly
              right-icon="calendar-o"
              @click="showDeathPicker = true"
            />
          </div>

          <!-- 出生地 -->
          <div class="form-group">
            <label class="form-label">
              出生地
              <span class="optional">（选填）</span>
            </label>
            <van-field
              v-model="formData.birthPlace"
              placeholder="请输入出生地"
            />
          </div>
        </div>

        <!-- 日期选择器 -->
        <van-popup v-model:show="showBirthPicker" position="bottom" round>
          <van-date-picker
            v-model="birthDateValue"
            title="选择出生日期"
            :min-date="new Date(1900, 0, 1)"
            :max-date="new Date()"
            @confirm="onBirthConfirm"
            @cancel="showBirthPicker = false"
          />
        </van-popup>

        <van-popup v-model:show="showDeathPicker" position="bottom" round>
          <van-date-picker
            v-model="deathDateValue"
            title="选择逝世日期"
            :min-date="new Date(1900, 0, 1)"
            :max-date="new Date()"
            @confirm="onDeathConfirm"
            @cancel="showDeathPicker = false"
          />
        </van-popup>
      </div>

      <!-- 步骤 3: 确认 -->
      <div v-if="currentStep === 2" class="wizard-body">
        <div class="confirm-section">
          <div class="confirm-avatar" :class="formData.gender.toLowerCase()">
            <van-icon :name="formData.gender === 'MALE' ? 'manager-o' : 'manager-o'" />
          </div>

          <h4 class="confirm-name">{{ fullName }}</h4>
          <p class="confirm-relation">{{ targetPersonName }} 的 {{ relationLabel }}</p>

          <div class="info-list">
            <div class="info-item">
              <span class="label">性别</span>
              <span class="value">{{ formData.gender === 'MALE' ? '男' : '女' }}</span>
            </div>
            <div v-if="formData.birthDate" class="info-item">
              <span class="label">出生日期</span>
              <span class="value">{{ formData.birthDate }}</span>
            </div>
            <div v-if="formData.deathDate" class="info-item">
              <span class="label">逝世日期</span>
              <span class="value">{{ formData.deathDate }}</span>
            </div>
            <div v-if="formData.birthPlace" class="info-item">
              <span class="label">出生地</span>
              <span class="value">{{ formData.birthPlace }}</span>
            </div>
          </div>

          <div class="confirm-tip">
            <van-icon name="info-o" />
            <span>确认后将创建新人物并建立关系</span>
          </div>
        </div>
      </div>

      <!-- 底部操作按钮 -->
      <div class="wizard-footer">
        <van-button
          v-if="currentStep > 0"
          round
          class="prev-btn"
          @click="onPrev"
        >
          上一步
        </van-button>
        <van-button
          v-if="currentStep < steps.length - 1"
          round
          type="primary"
          class="next-btn"
          :disabled="!canProceed"
          @click="onNext"
        >
          下一步
        </van-button>
        <van-button
          v-else
          round
          type="primary"
          class="next-btn"
          :loading="submitting"
          @click="onSubmit"
        >
          确认添加
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { showToast } from 'vant'

interface Props {
  show: boolean
  targetPersonId?: string
  targetPersonName?: string
  defaultRelationType?: string
  parentCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  targetPersonName: '',
  defaultRelationType: '',
  parentCount: 0
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  'submit': [data: {
    person: {
      firstName: string
      lastName: string
      gender: string
      birthDate?: string
      deathDate?: string
      birthPlace?: string
    }
    relation: {
      type: string
      targetPersonId: string
    }
  }]
  'closed': []
}>()

// 状态
const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const currentStep = ref(0)
const selectedRelation = ref('')
const submitting = ref(false)
const showBirthPicker = ref(false)
const showDeathPicker = ref(false)
const nameError = ref('')

// 日期选择器值
const birthDateValue = ref<string[]>([])
const deathDateValue = ref<string[]>([])

// 步骤定义
const steps = ['选择关系', '填写信息', '确认']

// 表单数据
const formData = ref({
  firstName: '',
  lastName: '',
  gender: 'MALE' as 'MALE' | 'FEMALE',
  birthDate: '',
  deathDate: '',
  birthPlace: ''
})

// 关系选项（基础定义，disabled 在模板中动态判断）
const relationOptions = [
  {
    value: 'PARENT',
    label: '父母',
    icon: 'friends-o',
    desc: '父亲或母亲',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    value: 'SPOUSE',
    label: '配偶',
    icon: 'like-o',
    desc: '丈夫或妻子',
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    value: 'CHILD',
    label: '子女',
    icon: 'smile-o',
    desc: '儿子或女儿',
    color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    value: 'SIBLING',
    label: '兄弟姐妹',
    icon: 'user-o',
    desc: '哥哥、弟弟、姐姐、妹妹',
    color: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
  }
]

// 检查是否禁用某个关系选项
const isOptionDisabled = (value: string) => {
  if (value === 'PARENT') {
    return props.parentCount >= 2
  }
  return false
}

// 计算属性
const stepTitle = computed(() => steps[currentStep.value])

const relationLabel = computed(() => {
  const option = relationOptions.find(o => o.value === selectedRelation.value)
  return option?.label || ''
})

const fullName = computed(() => {
  if (formData.value.lastName) {
    return formData.value.lastName + formData.value.firstName
  }
  return formData.value.firstName
})

const canProceed = computed(() => {
  if (currentStep.value === 0) {
    return !!selectedRelation.value
  }
  if (currentStep.value === 1) {
    return !!formData.value.firstName.trim()
  }
  return true
})

// 方法
const onNext = () => {
  if (currentStep.value === 1) {
    if (!formData.value.firstName.trim()) {
      nameError.value = '请输入名字'
      return
    }
    nameError.value = ''
  }
  if (currentStep.value < steps.length - 1) {
    currentStep.value++
  }
}

const onPrev = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const onBirthConfirm = ({ selectedValues }: { selectedValues: string[] }) => {
  formData.value.birthDate = selectedValues.join('-')
  showBirthPicker.value = false
}

const onDeathConfirm = ({ selectedValues }: { selectedValues: string[] }) => {
  formData.value.deathDate = selectedValues.join('-')
  showDeathPicker.value = false
}

const onSubmit = () => {
  if (!props.targetPersonId) {
    showToast('目标人物ID不存在')
    return
  }

  submitting.value = true

  emit('submit', {
    person: {
      firstName: formData.value.firstName.trim(),
      lastName: formData.value.lastName.trim(),
      gender: formData.value.gender,
      birthDate: formData.value.birthDate || undefined,
      deathDate: formData.value.deathDate || undefined,
      birthPlace: formData.value.birthPlace.trim() || undefined
    },
    relation: {
      type: selectedRelation.value,
      targetPersonId: props.targetPersonId
    }
  })

  submitting.value = false
  visible.value = false
}

const onClosed = () => {
  currentStep.value = 0
  selectedRelation.value = ''
  nameError.value = ''
  formData.value = {
    firstName: '',
    lastName: '',
    gender: 'MALE',
    birthDate: '',
    deathDate: '',
    birthPlace: ''
  }
  emit('closed')
}

// 监听 show 变化
watch(() => props.show, (newVal) => {
  if (newVal && props.defaultRelationType) {
    selectedRelation.value = props.defaultRelationType
  }
})
</script>

<style scoped>
.add-person-wizard {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}

/* 头部 */
.wizard-header {
  padding: 20px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.wizard-title {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  color: #333;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  gap: 8px;
}

.step-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #999;
  position: relative;
  z-index: 2;
  transition: all 0.3s;
}

.step-dot.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.step-dot.current {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.step-line {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 120px;
  height: 2px;
  background: #f0f0f0;
  z-index: 1;
}

.step-progress {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s;
}

/* 内容区域 */
.wizard-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.step-intro {
  margin-bottom: 20px;
}

.intro-text {
  margin: 0;
  font-size: 15px;
  color: #666;
  text-align: center;
}

.intro-text strong {
  color: #333;
  font-weight: 600;
}

/* 关系选择卡片 */
.relation-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.relation-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}

.relation-card:active {
  transform: scale(0.98);
}

.relation-card.selected {
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
  border: 2px solid #667eea;
}

.relation-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  margin-right: 16px;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.card-desc {
  font-size: 13px;
  color: #999;
}

.card-check {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.card-disabled {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-disabled span {
  background: #999;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

/* 表单 */
.form-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-label.required::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.form-label .optional {
  color: #999;
  font-weight: normal;
  font-size: 13px;
}

.name-inputs {
  display: flex;
  gap: 12px;
}

.name-field {
  flex: 1;
}

.name-field.last-name {
  flex: 0.4;
}

.name-field.first-name {
  flex: 0.6;
}

:deep(.name-field .van-field__control) {
  text-align: center;
}

/* 性别选择 */
.gender-options {
  display: flex;
  gap: 16px;
}

.gender-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: white;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.gender-card.selected {
  border-color: #667eea;
  background: #f0f5ff;
}

.gender-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.gender-icon.male {
  background: #e3f2fd;
  color: #2196f3;
}

.gender-icon.female {
  background: #fce4ec;
  color: #e91e63;
}

.gender-card span {
  font-size: 14px;
  color: #666;
}

/* 确认页面 */
.confirm-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.confirm-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  margin-bottom: 16px;
}

.confirm-avatar.male {
  background: #e3f2fd;
  color: #2196f3;
}

.confirm-avatar.female {
  background: #fce4ec;
  color: #e91e63;
}

.confirm-name {
  margin: 0 0 4px 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.confirm-relation {
  margin: 0 0 24px 0;
  font-size: 14px;
  color: #999;
}

.info-list {
  width: 100%;
  background: white;
  border-radius: 16px;
  padding: 8px 0;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #f5f5f5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #999;
  font-size: 14px;
}

.info-item .value {
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.confirm-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #999;
  font-size: 13px;
}

/* 底部按钮 */
.wizard-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px calc(16px + env(safe-area-inset-bottom));
  background: white;
  border-top: 1px solid #f0f0f0;
}

.prev-btn {
  flex: 0.35;
  height: 44px;
}

.next-btn {
  flex: 0.65;
  height: 44px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.next-btn.van-button--disabled) {
  opacity: 0.5;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>
