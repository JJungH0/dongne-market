import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { Empty, Spinner } from '../components/Spinner'
import { TopBar } from '../components/TopBar'
import { useAuth } from '../hooks/useAuth'
import { useStomp } from '../hooks/useStomp'
import { api } from '../lib/api'
import { timeAgo } from '../lib/format'
import type { ChatRoom } from '../lib/types'

export function ChatListPage() {
  const { member } = useAuth()
  const { connected, subscribe } = useStomp()
  const [rooms, setRooms] = useState<ChatRoom[] | null>(null)

  const load = () => api.rooms().then(setRooms).catch(() => setRooms([]))
  useEffect(() => { load() }, [])
  useEffect(() => {
    if (!connected || !member) return
    return subscribe(`/topic/users/${member.id}`, load)   // 새 메시지가 오면 목록 갱신
  }, [connected, member, subscribe])

  return (
    <div className="page">
      <TopBar title={<strong>채팅</strong>} right={
        <span className={`dot ${connected ? 'dot--on' : ''}`} title={connected ? '실시간 연결됨' : '연결 중'} />
      } />
      {rooms === null && <Spinner />}
      {rooms?.length === 0 && <Empty text="아직 채팅이 없어요" sub="관심 있는 물건에 채팅을 보내보세요" />}
      <ul className="rooms">
        {rooms?.map((r) => (
          <li key={r.id}>
            <Link to={`/chat/${r.id}`} className="room">
              <span className="avatar">{r.counterpart?.nickname.slice(0, 1) ?? '?'}</span>
              <div className="room__body">
                <div className="room__head">
                  <strong>{r.counterpart?.nickname ?? '알 수 없음'}</strong>
                  <span className="muted small">{r.counterpart?.regionName} · {timeAgo(r.lastMessageAt)}</span>
                </div>
                <p className={`room__last ${r.unreadCount > 0 ? 'is-unread' : ''}`}>{r.lastMessage ?? '대화를 시작해 보세요'}</p>
              </div>
              {r.unreadCount > 0 && <span className="badge badge--lg">{r.unreadCount}</span>}
              <span className="room__thumb">
                {r.product?.thumbnailUrl ? <img src={r.product.thumbnailUrl} alt="" /> : <Icon name="camera" size={16} />}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
