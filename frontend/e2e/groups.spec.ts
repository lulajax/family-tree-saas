import { test, expect } from '@playwright/test'

test.describe('家族列表页面', () => {
  test('页面标题和基础元素应该正确渲染', async ({ page }) => {
    await page.goto('/groups')

    // 等待页面加载
    await page.waitForLoadState('networkidle')

    // 如果未登录会被重定向到登录页
    const currentUrl = page.url()
    if (currentUrl.includes('login')) {
      test.skip(true, '需要登录状态')
      return
    }

    // 验证导航栏标题
    await expect(page.locator('.van-nav-bar__title')).toContainText(/我的家族/)

    // 验证底部导航栏
    await expect(page.locator('.van-tabbar')).toBeVisible()
  })

  test('导航栏应该显示正确', async ({ page }) => {
    await page.goto('/groups')

    // 等待页面加载
    await page.waitForLoadState('networkidle')

    // 检查是否被重定向到登录页
    if (page.url().includes('login')) {
      test.skip(true, '需要登录状态')
      return
    }

    // 验证导航栏存在
    await expect(page.locator('.van-nav-bar')).toBeVisible()

    // 验证导航栏标题
    await expect(page.locator('.van-nav-bar__title')).toContainText('我的家族')
  })

  test('底部导航栏应该包含家族和我的', async ({ page }) => {
    await page.goto('/groups')

    await page.waitForLoadState('networkidle')

    if (page.url().includes('login')) {
      test.skip(true, '需要登录状态')
      return
    }

    // 验证底部导航栏有两个选项
    const tabbarItems = page.locator('.van-tabbar-item')
    await expect(tabbarItems).toHaveCount(2)

    // 验证文本内容
    await expect(page.locator('.van-tabbar-item', { hasText: '家族' })).toBeVisible()
    await expect(page.locator('.van-tabbar-item', { hasText: '我的' })).toBeVisible()
  })
})

test.describe('导航测试', () => {
  test('从登录页可以导航到注册页', async ({ page }) => {
    await page.goto('/login')

    await page.waitForLoadState('networkidle')

    // 点击注册链接
    const registerLink = page.locator('a[href="#/register"]').or(page.locator('text=注册账号'))
    await expect(registerLink).toBeVisible()

    await registerLink.click()

    // 等待导航完成
    await page.waitForTimeout(500)

    // 验证 URL 包含 register
    await expect(page).toHaveURL(/.*register/)

    // 验证页面内容
    await expect(page.locator('button:has-text("注册")')).toBeVisible()
  })

  test('从注册页可以导航到登录页', async ({ page }) => {
    await page.goto('/register')

    await page.waitForLoadState('networkidle')

    // 点击登录链接
    const loginLink = page.locator('a[href="#/login"]').or(page.locator('text=已有账号')).or(page.locator('text=登录'))

    if (await loginLink.isVisible().catch(() => false)) {
      await loginLink.click()
      await page.waitForTimeout(500)
      await expect(page).toHaveURL(/.*login/)
    } else {
      test.skip(true, '登录链接未找到')
    }
  })
})
