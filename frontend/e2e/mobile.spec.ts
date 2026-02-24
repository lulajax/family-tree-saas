import { test, expect } from '@playwright/test'

test.describe('移动端适配', () => {
  test('登录页面在移动端应该正确显示', async ({ page }) => {
    // 设置移动端视口
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/login')

    await page.waitForLoadState('networkidle')

    // 验证页面内容在移动端可见
    await expect(page.locator('input[name="phone"]')).toBeVisible()
    await expect(page.locator('input[name="password"]')).toBeVisible()

    // 验证登录按钮可见
    await expect(page.locator('button:has-text("登录")')).toBeVisible()

    // 验证没有水平滚动条（内容适配屏幕宽度）
    const body = page.locator('body')
    const bodyWidth = await body.evaluate(el => el.scrollWidth)
    const viewportWidth = await page.evaluate(() => window.innerWidth)
    expect(bodyWidth).toBeLessThanOrEqual(viewportWidth + 1) // 允许 1px 误差
  })

  test('家族列表在移动端应该正确显示', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/groups')

    await page.waitForLoadState('networkidle')

    // 如果未登录则跳过
    if (page.url().includes('login')) {
      test.skip(true, '需要登录状态')
      return
    }

    // 验证导航栏在移动端存在
    await expect(page.locator('.van-nav-bar')).toBeVisible()

    // 验证底部导航栏在移动端存在
    await expect(page.locator('.van-tabbar')).toBeVisible()
  })

  test('不同尺寸的移动端设备都应该正确渲染', async ({ page }) => {
    const devices = [
      { width: 375, height: 667, name: 'iPhone 8' },
      { width: 414, height: 896, name: 'iPhone 11' },
      { width: 360, height: 740, name: 'Galaxy S9' }
    ]

    for (const device of devices) {
      await page.setViewportSize({ width: device.width, height: device.height })
      await page.goto('/login')
      await page.waitForLoadState('networkidle')

      // 验证关键元素可见
      await expect(page.locator('h1')).toBeVisible()
      await expect(page.locator('button:has-text("登录")')).toBeVisible()

      // 验证没有水平滚动
      const bodyWidth = await page.locator('body').evaluate(el => el.scrollWidth)
      const viewportWidth = await page.evaluate(() => window.innerWidth)
      expect(bodyWidth).toBeLessThanOrEqual(viewportWidth + 1,
        `${device.name} 出现水平滚动条`)
    }
  })
})
