import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../api/client'

function mockAdapter() {
  const config = { headers: {} as Record<string, unknown>, status: 0 }
  const adapter = vi.fn(async (cfg: { headers: Record<string, unknown>; status?: number }) => {
    config.headers = cfg.headers
    return {
      data: {},
      status: cfg.status ?? 200,
      statusText: 'OK',
      headers: {},
      config: cfg,
    }
  })
  return { adapter, config }
}

describe('axios 客户端拦截器', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('登录态存在时请求自动附带 Authorization', async () => {
    localStorage.setItem('guga.token', 'jwt-abc')
    const { adapter, config } = mockAdapter()
    await api.get('/articles', { adapter: adapter as never })
    expect(config.headers.Authorization).toBe('Bearer jwt-abc')
  })

  it('未登录时不附带 Authorization', async () => {
    const { adapter, config } = mockAdapter()
    await api.get('/articles', { adapter: adapter as never })
    expect(config.headers.Authorization).toBeUndefined()
  })

  it('401 响应会清除本地登录态', async () => {
    localStorage.setItem('guga.token', 'jwt-abc')
    localStorage.setItem('guga.user', '{"id":1}')
    const adapter = vi.fn(async (cfg: Record<string, unknown>) => {
      const error = new Error('Unauthorized') as Error & {
        response?: unknown
        config?: unknown
        isAxiosError: boolean
      }
      error.response = { status: 401, data: {}, headers: {}, config: cfg }
      error.config = cfg
      error.isAxiosError = true
      throw error
    })
    // jsdom 不允许替换 location.assign，导航会是 no-op 并打印告警，这里只断言登录态被清除
    await expect(api.get('/auth/me', { adapter: adapter as never })).rejects.toThrow()
    expect(localStorage.getItem('guga.token')).toBeNull()
    expect(localStorage.getItem('guga.user')).toBeNull()
  })
})
