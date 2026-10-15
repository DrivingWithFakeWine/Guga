import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import type { User } from '../types'

const TOKEN_KEY = 'guga.token'
const USER_KEY = 'guga.user'

export function loadAuth(): { token: string | null; user: User | null } {
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    const raw = localStorage.getItem(USER_KEY)
    if (token && raw) {
      return { token, user: JSON.parse(raw) as User }
    }
  } catch {
    // 存储内容损坏时按未登录处理
  }
  return { token: null, user: null }
}

export function saveAuth(token: string, user: User) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

interface AuthContextValue {
  token: string | null
  user: User | null
  setAuth: (token: string, user: User) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState(loadAuth)

  const setAuth = useCallback((token: string, user: User) => {
    saveAuth(token, user)
    setState({ token, user })
  }, [])

  const logout = useCallback(() => {
    clearAuth()
    setState({ token: null, user: null })
  }, [])

  const value = useMemo(() => ({ ...state, setAuth, logout }), [state, setAuth, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth 必须在 AuthProvider 内使用')
  }
  return ctx
}
