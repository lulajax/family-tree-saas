import { Page } from '@playwright/test'

/**
 * 测试辅助函数：用户认证
 */

export interface UserCredentials {
  phone: string
  password: string
}

export const testUser: UserCredentials = {
  phone: '13800138000',
  password: 'Test123456'
}

/**
 * 执行登录操作
 */
export async function login(page: Page, credentials: UserCredentials = testUser): Promise<void> {
  await page.goto('/login')

  // 填写手机号
  await page.locator('input[type="tel"]').fill(credentials.phone)

  // 填写密码
  await page.locator('input[type="password"]').fill(credentials.password)

  // 点击登录按钮
  await page.locator('button:has-text("登录")').click()

  // 等待导航完成（根据实际登录后的跳转调整）
  await page.waitForURL(/.*groups|.*home/, { timeout: 5000 }).catch(() => {
    // 如果超时，可能只是弹出了提示，页面没有跳转
    console.log('登录后可能没有页面跳转')
  })
}

/**
 * 执行登出操作
 */
export async function logout(page: Page): Promise<void> {
  // 根据实际UI实现调整
  await page.goto('/profile')

  // 查找登出按钮
  const logoutButton = page.locator('button:has-text("退出登录"), .logout-btn')
  if (await logoutButton.isVisible().catch(() => false)) {
    await logoutButton.click()

    // 确认登出（如果有确认对话框）
    const confirmButton = page.locator('button:has-text("确认"), .van-dialog__confirm')
    if (await confirmButton.isVisible().catch(() => false)) {
      await confirmButton.click()
    }
  }
}

/**
 * 检查是否已登录
 */
export async function isLoggedIn(page: Page): Promise<boolean> {
  await page.goto('/groups')
  const url = page.url()
  return !url.includes('login')
}
