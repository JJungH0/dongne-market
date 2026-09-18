import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, auth } from '../lib/api'
import type { Member } from '../lib/types'

interface AuthState {
  member: Member | null
  login: (email: string, password: string) => Promise<void>
  signup: (body: object) => Promise<void>
  logout: () => void
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthState>(null!)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [member, setMember] = useState<Member | null>(() => auth.member())

  const logout = useCallback(() => {
    auth.clear()
    setMember(null)
  }, [])

  useEffect(() => {
    window.addEventListener('dongne:logout', logout)
    return () => window.removeEventListener('dongne:logout', logout)
  }, [logout])

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.login(email, password)
    auth.save(res.accessToken, res.member)
    setMember(res.member)
  }, [])

  const signup = useCallback(async (body: object) => {
    const res = await api.signup(body)
    auth.save(res.accessToken, res.member)
    setMember(res.member)
  }, [])

  const refresh = useCallback(async () => {
    const me = await api.me()
    auth.saveMember(me)
    setMember(me)
  }, [])

  return <AuthContext.Provider value={{ member, login, signup, logout, refresh }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
