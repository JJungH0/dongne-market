import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { Spinner } from '../components/Spinner'
import { TopBar } from '../components/TopBar'
import { useAuth } from '../hooks/useAuth'
import { useStomp } from '../hooks/useStomp'
import { useUnread } from '../hooks/useUnread'
import { api } from '../lib/api'
import { STATUS_LABEL, chatTime, won } from '../lib/format'
import type { ChatMessage, ChatRoom, RoomEvent } from '../lib/types'

export function ChatRoomPage() {
  const roomId = Number(useParams().roomId)
  const { member } = useAuth()
  const { connected, subscribe, publish } = useStomp()
  const { refresh: refreshUnread } = useUnread()
  const [room, setRoom] = useState<ChatRoom | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])   // 오래된 → 최신
  const [hasMore, setHasMore] = useState(false)
  const [text, setText] = useState('')
  const listRef = useRef<HTMLDivElement>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  const markRead = useCallback(() => api.markRead(roomId).then(refreshUnread).catch(() => {}), [roomId, refreshUnread])

  useEffect(() => {
    api.room(roomId).then(setRoom)
    api.messages(roomId).then((page) => {
      setMessages([...page].reverse())
      setHasMore(page.length >= 50)
      markRead()
      setTimeout(() => bottomRef.current?.scrollIntoView(), 0)
    })
  }, [roomId, markRead])

  // 방 토픽 구독: 새 메시지 / 읽음 이벤트
  useEffect(() => {
    if (!connected) return
    return subscribe(`/topic/rooms/${roomId}`, (raw) => {
      const ev = raw as RoomEvent
      if (ev.type === 'MESSAGE' && ev.message) {
        const m = ev.message
        setMessages((prev) => (prev.some((x) => x.id === m.id) ? prev : [...prev, m]))
        if (m.senderId !== member?.id) markRead()
        setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 0)
      }
      if (ev.type === 'READ' && ev.readerId !== member?.id) {
        setMessages((prev) => prev.map((m) => (m.senderId === member?.id && !m.readAt ? { ...m, readAt: ev.readAt } : m)))
      }
    })
  }, [connected, roomId, subscribe, member, markRead])

  const loadOlder = async () => {
    const first = messages[0]
    if (!first) return
    const el = listRef.current
    const prevHeight = el?.scrollHeight ?? 0
    const page = await api.messages(roomId, first.id)
    setMessages((prev) => [...[...page].reverse(), ...prev])
    setHasMore(page.length >= 50)
    requestAnimationFrame(() => { if (el) el.scrollTop = el.scrollHeight - prevHeight })
  }

  const send = async (e?: FormEvent) => {
    e?.preventDefault()
    const content = text.trim()
    if (!content) return
    setText('')
    // WebSocket 이 살아 있으면 STOMP, 아니면 REST 폴백 (둘 다 같은 서비스 메서드로 저장·브로드캐스트)
    if (!publish(`/app/chat/${roomId}`, { content })) {
      const m = await api.sendMessage(roomId, content)
      setMessages((prev) => [...prev, m])
    }
  }

  if (!room) return <div className="page"><TopBar back /><Spinner /></div>

  // 내가 보낸 메시지 중 마지막으로 읽힌 것 뒤에만 "읽음" 표시하지 않고, 안 읽힌 첫 메시지에 "1"
  return (
    <div className="page page--room">
      <TopBar back title={
        <div className="room-title">
          <strong>{room.counterpart?.nickname}</strong>
          <span className="muted small">{room.counterpart?.regionName}</span>
        </div>
      } right={<span className={`dot ${connected ? 'dot--on' : ''}`} />} />

      {room.product && (
        <Link to={`/products/${room.product.id}`} className="room-product">
          {room.product.thumbnailUrl ? <img src={room.product.thumbnailUrl} alt="" /> : <span className="room-product__noimg"><Icon name="camera" size={16} /></span>}
          <div>
            <p><span className={`pill pill--${room.product.status.toLowerCase()}`}>{STATUS_LABEL[room.product.status]}</span> {room.product.title}</p>
            <strong>{won(room.product.price)}</strong>
          </div>
          <Icon name="chevron" className="rot-270" size={16} />
        </Link>
      )}

      <div className="msgs" ref={listRef}>
        {hasMore && <button className="textbtn msgs__more" onClick={loadOlder}>이전 메시지 보기</button>}
        {messages.map((m, i) => {
          const mine = m.senderId === member?.id
          const showDate = i === 0 || new Date(m.createdAt).toDateString() !== new Date(messages[i - 1].createdAt).toDateString()
          return (
            <div key={m.id}>
              {showDate && <div className="msgs__date">{new Date(m.createdAt).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })}</div>}
              <div className={`msg ${mine ? 'msg--mine' : ''}`}>
                <div className="msg__bubble">{m.content}</div>
                <div className="msg__meta">
                  {mine && !m.readAt && <span className="msg__unread">1</span>}
                  <span>{chatTime(m.createdAt)}</span>
                </div>
              </div>
            </div>
          )
        })}
        <div ref={bottomRef} />
      </div>

      <form className="composer" onSubmit={send}>
        <input className="composer__input" placeholder="메시지 보내기" value={text} onChange={(e) => setText(e.target.value)} maxLength={1000} />
        <button className="composer__send" type="submit" disabled={!text.trim()} aria-label="전송"><Icon name="send" size={18} /></button>
      </form>
    </div>
  )
}
