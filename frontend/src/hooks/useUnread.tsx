import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from '../lib/api'
import { useAuth } from './useAuth'
import { useStomp } from './useStomp'

/** 하단 탭 배지용 안 읽은 메시지 총합. 개인 토픽 이벤트가 오면 다시 센다. */
const UnreadContext = createContext<{ total: number; refresh: () => void }>({ total: 0, refresh: () => {} })

export function UnreadProvider({ children }: { children: ReactNode }) {
  const { member } = useAuth()
  const { connected, subscribe } = useStomp()
  const [total, setTotal] = useState(0)

  const refresh = useCallback(() => {
    if (!member) { setTotal(0); return }
    api.rooms().then((rooms) => setTotal(rooms.reduce((s, r) => s + r.unreadCount, 0))).catch(() => {})
  }, [member])

  useEffect(refresh, [refresh])
  useEffect(() => {
    if (!connected || !member) return
    return subscribe(`/topic/users/${member.id}`, refresh)
  }, [connected, member, subscribe, refresh])

  return <UnreadContext.Provider value={{ total, refresh }}>{children}</UnreadContext.Provider>
}

export const useUnread = () => useContext(UnreadContext)
