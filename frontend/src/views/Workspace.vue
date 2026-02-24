<template>
  <div class="workspace-page">
    <van-nav-bar
      title="编辑家谱"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    >
      <template #right>
        <van-icon name="success" size="20" @click="showCommitDialog = true" />
      </template>
    </van-nav-bar>
    
    <div class="workspace-content">
      <!-- 工作区状态 -->
      <van-notice-bar
        v-if="workspace"
        :text="workspaceStatusText"
        :color="workspaceStatusColor"
        background="#ecf9ff"
        left-icon="info-o"
      />
      
      <!-- 变更列表 -->
      <van-cell-group inset title="我的变更" class="changes-list">
        <van-pull-refresh v-model="refreshing" @refresh="fetchChanges">
          <van-empty v-if="changes.length === 0" description="暂无变更，点击下方按钮添加人物" />
          
          <van-cell
            v-for="change in changes"
            :key="change.id"
            :title="changeTitle(change)"
            :label="changeLabel(change)"
            center
          >
            <template #icon>
              <van-icon
                :name="changeIcon(change)"
                :color="changeColor(change)"
                size="24"
                style="margin-right: 12px;"
              />
            </template>
            <template #value>
              <van-tag :type="changeTagType(change)">
                {{ changeTypeText(change) }}
              </van-tag>
            </template>
          </van-cell>
        </van-pull-refresh>
      </van-cell-group>
      
      <!-- 操作按钮 -->
      <div class="action-buttons">
        <van-button type="primary" block @click="showAddPerson = true">
          <van-icon name="plus" /> 添加人物
        </van-button>
        <van-button v-if="changes.length > 0" type="danger" block plain @click="resetWorkspace">
          <van-icon name="delete-o" /> 重置变更
        </van-button>
      </div>
    </div>
    
    <!-- 添加人物弹窗 -->
    <van-popup
      v-model:show="showAddPerson"
      position="bottom"
      :style="{ height: '80%' }"
      round
    >
      <van-nav-bar
        title="添加人物"
        left-arrow
        @click-left="showAddPerson = false"
      />
      <van-form @submit="onAddPerson">
        <van-cell-group inset>
          <van-field
            v-model="newPerson.firstName"
            name="firstName"
            label="姓名"
            placeholder="请输入姓名"
            :rules="[{ required: true, message: '请输入姓名' }]"
          />
          <van-field
            v-model="newPerson.lastName"
            name="lastName"
            label="姓氏"
            placeholder="请输入姓氏"
          />
          <van-field name="gender" label="性别">
            <template #input>
              <van-radio-group v-model="newPerson.gender" direction="horizontal">
                <van-radio name="MALE">男</van-radio>
                <van-radio name="FEMALE">女</van-radio>
              </van-radio-group>
            </template>
          </van-field>
          <van-field
            v-model="newPerson.birthDate"
            name="birthDate"
            label="出生日期"
            placeholder="选择日期"
            readonly
            @click="showBirthPicker = true"
          />
          <van-field
            v-model="newPerson.deathDate"
            name="deathDate"
            label="逝世日期"
            placeholder="选择日期（选填）"
            readonly
            @click="showDeathPicker = true"
          />
          <van-field
            v-model="newPerson.birthPlace"
            name="birthPlace"
            label="出生地"
            placeholder="请输入出生地"
          />
        </van-cell-group>
        
        <div style="margin: 16px;">
          <van-button round block type="primary" native-type="submit">
            添加
          </van-button>
        </div>
      </van-form>
    </van-popup>
    
    <!-- 提交变更弹窗 -->
    <van-dialog
      v-model:show="showCommitDialog"
      title="提交变更"
      show-cancel-button
      @confirm="commitChanges"
    >
      <van-field
        v-model="commitTitle"
        label="标题"
        placeholder="请输入变更标题"
        :rules="[{ required: true }]"
      />
      <van-field
        v-model="commitDescription"
        label="描述"
        type="textarea"
        placeholder="请输入变更描述（选填）"
        rows="3"
      />
    </van-dialog>
    
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast, showConfirmDialog } from 'vant'
import { useWorkspaceStore } from '@/stores/workspace'
import type { ChangeSet } from '@/types'

const route = useRoute()
const router = useRouter()
const workspaceStore = useWorkspaceStore()
const groupId = route.params.groupId as string

const workspace = computed(() => workspaceStore.currentWorkspace)
const changes = computed(() => workspaceStore.changes)
const refreshing = ref(false)

const showAddPerson = ref(false)
const showCommitDialog = ref(false)
const showBirthPicker = ref(false)
const showDeathPicker = ref(false)

const commitTitle = ref('')
const commitDescription = ref('')

const newPerson = reactive({
  firstName: '',
  lastName: '',
  gender: 'MALE' as 'MALE' | 'FEMALE' | 'UNKNOWN',
  birthDate: '',
  deathDate: '',
  birthPlace: ''
})

const birthDatePicker = ref(['1990', '01', '01'])
const deathDatePicker = ref(['2020', '01', '01'])

const workspaceStatusText = computed(() => {
  const status = workspace.value?.status
  switch (status) {
    case 'EDITING':
      return `编辑中 - 已保存 ${changes.value.length} 个变更`
    case 'SUBMITTED':
      return '已提交审核，请等待管理员审批'
    case 'MERGED':
      return '变更已合并到主家谱'
    case 'CONFLICT':
      return '存在冲突，请解决后重新提交'
    default:
      return ''
  }
})

const workspaceStatusColor = computed(() => {
  const status = workspace.value?.status
  switch (status) {
    case 'EDITING':
      return '#1989fa'
    case 'SUBMITTED':
      return '#ff976a'
    case 'MERGED':
      return '#07c160'
    case 'CONFLICT':
      return '#ee0a24'
    default:
      return '#1989fa'
  }
})

const initWorkspace = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    await workspaceStore.createOrGetWorkspace(groupId)
    closeToast()
  } catch (error) {
    closeToast()
    showToast('加载工作区失败')
  }
}

const fetchChanges = async () => {
  if (!workspace.value) return
  refreshing.value = true
  try {
    await workspaceStore.fetchChanges(workspace.value.id)
  } catch (error) {
    showToast('获取变更失败')
  } finally {
    refreshing.value = false
  }
}

const onAddPerson = async () => {
  showLoadingToast({ message: '添加中...', forbidClick: true })
  
  try {
    await workspaceStore.addPersonChange({
      groupId,
      firstName: newPerson.firstName,
      lastName: newPerson.lastName,
      gender: newPerson.gender,
      birthDate: newPerson.birthDate || undefined,
      deathDate: newPerson.deathDate || undefined,
      birthPlace: newPerson.birthPlace || undefined
    })
    
    closeToast()
    showToast('添加成功')
    showAddPerson.value = false
    
    // 重置表单
    newPerson.firstName = ''
    newPerson.lastName = ''
    newPerson.gender = 'MALE'
    newPerson.birthDate = ''
    newPerson.deathDate = ''
    newPerson.birthPlace = ''
  } catch (error) {
    closeToast()
    showToast('添加失败')
  }
}

const commitChanges = async () => {
  if (!commitTitle.value.trim()) {
    showToast('请输入标题')
    return
  }
  
  showLoadingToast({ message: '提交中...', forbidClick: true })
  
  try {
    await workspaceStore.commitChanges({
      title: commitTitle.value,
      description: commitDescription.value
    })
    
    closeToast()
    showToast('提交成功，等待审核')
    commitTitle.value = ''
    commitDescription.value = ''
  } catch (error) {
    closeToast()
    showToast('提交失败')
  }
}

const resetWorkspace = async () => {
  try {
    await showConfirmDialog({
      title: '确认重置',
      message: '确定要清空所有变更吗？此操作不可恢复。'
    })
    
    showLoadingToast({ message: '重置中...', forbidClick: true })
    await workspaceStore.resetWorkspace()
    closeToast()
    showToast('已重置')
  } catch (error) {
    // 用户取消
  }
}

const onBirthDateConfirm = ({ selectedValues }: { selectedValues: string[] }) => {
  newPerson.birthDate = selectedValues.join('-')
  showBirthPicker.value = false
}

const onDeathDateConfirm = ({ selectedValues }: { selectedValues: string[] }) => {
  newPerson.deathDate = selectedValues.join('-')
  showDeathPicker.value = false
}

const changeTitle = (change: ChangeSet) => {
  const entityName = change.payload?.firstName || change.previousPayload?.firstName || '未知'
  return entityName
}

const changeLabel = (change: ChangeSet) => {
  return new Date(change.createdAt).toLocaleString()
}

const changeIcon = (change: ChangeSet) => {
  switch (change.actionType) {
    case 'CREATE':
      return 'add-o'
    case 'UPDATE':
      return 'edit'
    case 'DELETE':
      return 'delete-o'
    default:
      return 'records'
  }
}

const changeColor = (change: ChangeSet) => {
  switch (change.actionType) {
    case 'CREATE':
      return '#07c160'
    case 'UPDATE':
      return '#1989fa'
    case 'DELETE':
      return '#ee0a24'
    default:
      return '#999'
  }
}

const changeTagType = (change: ChangeSet) => {
  switch (change.actionType) {
    case 'CREATE':
      return 'success'
    case 'UPDATE':
      return 'primary'
    case 'DELETE':
      return 'danger'
    default:
      return 'default'
  }
}

const changeTypeText = (change: ChangeSet) => {
  switch (change.actionType) {
    case 'CREATE':
      return '新增'
    case 'UPDATE':
      return '修改'
    case 'DELETE':
      return '删除'
    default:
      return '未知'
  }
}

onMounted(() => {
  initWorkspace()
})
</script>

<style scoped>
.workspace-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.workspace-content {
  padding: 10px;
}

.changes-list {
  margin-top: 10px;
  min-height: 300px;
}

.action-buttons {
  margin: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
