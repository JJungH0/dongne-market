import { Client, type IMessage } from '@stomp/stompjs'
import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { auth } from '../lib/api'
import { useAuth } from './useAuth'

interface StompState {
  connected: boolean
  subscribe: (destination: string, handler: (body: unknown) => void) => () => void
  publish: (destination: string, body: object) => boolean
}

const StompContext = createContext<StompState>(null!)

/**
 * 앱 전체에서 WebSocket 연결 하나를 공유한다.
 * 로그인하면 연결, 로그아웃하면 끊고, 끊기면 stompjs 가 5초 간격으로 재접속한다.
 */
export function StompProvider({ children }: { children: ReactNode }) {
  const { member } = useAuth()
  const clientRef = useRef<Client | null>(null)
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    if (!member) {
      clientRef.current?.deactivate()
      clientRef.current = null
      setConnected(false)
      return
    }
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${proto}://${location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${auth.token()}` },
      reconnectDelay: 5000,
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    })
    client.activate()
    clientRef.current = client
    return () => {
      client.deactivate()
      setConnected(false)
    }
  }, [member])

  const subscribe = useCallback((destination: string, handler: (body: unknown) => void) => {
    const client = clientRef.current
    if (!client || !client.connected) return () => {}
    const sub = client.subscribe(destination, (msg: IMessage) => {
      try { handler(JSON.parse(msg.body)) } catch { /* 무시 */ }
    })
    return () => sub.unsubscribe()
  }, [])

  const publish = useCallback((destination: string, body: object) => {
    const client = clientRef.current
    if (!client || !client.connected) return false
    client.publish({ destination, body: JSON.stringify(body) })
    return true
  }, [])

  return <StompContext.Provider value={{ connected, subscribe, publish }}>{children}</StompContext.Provider>
}

export const useStomp = () => useContext(StompContext)
