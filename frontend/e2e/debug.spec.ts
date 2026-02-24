import { test, expect } from '@playwright/test'

test('调试页面加载问题', async ({ page }) => {
  // 捕获控制台日志
  const logs: string[] = []
  page.on('console', msg => {
    logs.push(`[${msg.type()}] ${msg.text()}`)
  })

  // 捕获页面错误
  const errors: string[] = []
  page.on('pageerror', error => {
    errors.push(error.message)
  })

  // 捕获请求失败
  const failedRequests: string[] = []
  page.on('requestfailed', request => {
    failedRequests.push(`${request.url()} - ${request.failure()?.errorText}`)
  })

  await page.goto('/login')

  // 等待页面完全加载
  await page.waitForLoadState('domcontentloaded')
  await page.waitForTimeout(5000)

  // 输出日志
  console.log('\n=== 控制台日志 ===')
  logs.forEach(log => console.log(log))

  console.log('\n=== 页面错误 ===')
  errors.forEach(err => console.log(err))

  console.log('\n=== 失败的请求 ===')
  failedRequests.forEach(req => console.log(req))

  // 检查 main.ts 是否加载
  const mainScript = await page.locator('script[src*="main.ts"]').count()
  console.log(`\nmain.ts 脚本数量: ${mainScript}`)

  // 获取页面内容
  const html = await page.content()
  console.log('\n=== 页面 HTML ===')
  console.log(html.substring(0, 2000))
})
