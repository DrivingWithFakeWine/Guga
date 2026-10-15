import { beforeEach, describe, expect, it } from 'vitest'
import { clearAuth, loadAuth, saveAuth } from '../lib/auth'

describe('auth 存取', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('保存后能读出 token 和用户', () => {
    saveAuth('jwt-token', { id: 1, username: 'dev', email: 'dev@test.com' })
    expect(loadAuth()).toEqual({
      token: 'jwt-token',
      user: { id: 1, username: 'dev', email: 'dev@test.com' },
    })
  })

  it('未登录时返回空状态', () => {
    expect(loadAuth()).toEqual({ token: null, user: null })
  })

  it('登出后清空登录态', () => {
    saveAuth('jwt-token', { id: 1, username: 'dev', email: 'dev@test.com' })
    clearAuth()
    expect(loadAuth()).toEqual({ token: null, user: null })
  })

  it('用户数据损坏时按未登录处理而不是抛异常', () => {
    localStorage.setItem('guga.token', 'jwt-token')
    localStorage.setItem('guga.user', 'not-json')
    expect(loadAuth()).toEqual({ token: null, user: null })
  })
})
