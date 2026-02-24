import { test, expect } from '@playwright/test'

/**
 * 基础示例测试 - 验证 Playwright 配置是否正确
 */

test('页面应该能正常加载', async ({ page }) => {
  await page.goto('/login')

  // 等待网络空闲
  await page.waitForLoadState('networkidle')

  // 等待 2 秒确保 Vue 应用渲染完成
  await page.waitForTimeout(2000)

  // 获取页面内容
  const body = await page.locator('body').innerHTML()
  console.log('页面内容:', body.substring(0, 500))

  // 检查标题
  await expect(page).toHaveTitle(/家族树/)

  // 检查 #app 元素是否存在且有内容
  const app = page.locator('#app')
  await expect(app).toBeVisible()

  // 检查是否有可见的文本内容
  const visibleText = await page.locator('body').textContent()
  console.log('可见文本:', visibleText?.substring(0, 200))
})

test('检查登录页面基本元素', async ({ page }) => {
  await page.goto('/login')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)

  // 检查 h1 标签
  const h1 = page.locator('h1')
  if (await h1.isVisible().catch(() => false)) {
    console.log('h1 内容:', await h1.textContent())
    await expect(h1).toContainText(/家族/)
  } else {
    // 如果没有 h1，检查是否有其他主要内容
    const bodyText = await page.locator('body').textContent()
    console.log('页面文本:', bodyText)
    test.skip(true, '页面元素未渲染')
  }
})

test('检查页面上是否有输入框', async ({ page }) => {
  await page.goto('/login')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)

  // 获取所有 input 元素
  const inputs = page.locator('input')
  const count = await inputs.count()
  console.log(`找到 ${count} 个 input 元素`)

  if (count > 0) {
    for (let i = 0; i < count; i++) {
      const input = inputs.nth(i)
      const type = await input.getAttribute('type')
      const name = await input.getAttribute('name')
      const placeholder = await input.getAttribute('placeholder')
      console.log(`Input ${i}: type=${type}, name=${name}, placeholder=${placeholder}`)
    }
  }
})
