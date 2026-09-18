import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { ProductCard } from '../components/ProductCard'
import { Empty, Spinner } from '../components/Spinner'
import { TopBar } from '../components/TopBar'
import { useAuth } from '../hooks/useAuth'
import { api } from '../lib/api'
import type { ProductSummary } from '../lib/types'

export function MePage() {
  const { member, logout } = useAuth()
  const nav = useNavigate()
  const [tab, setTab] = useState<'sales' | 'likes'>('sales')
  const [items, setItems] = useState<ProductSummary[] | null>(null)

  useEffect(() => {
    setItems(null)
    ;(tab === 'sales' ? api.mySales() : api.myLikes()).then(setItems).catch(() => setItems([]))
  }, [tab])

  if (!member) return null
  const temp = Number(member.mannerTemperature)

  return (
    <div className="page">
      <TopBar title={<strong>나의 동네</strong>} right={
        <button className="iconbtn" onClick={() => { logout(); nav('/') }} aria-label="로그아웃"><Icon name="logout" /></button>
      } />
      <section className="profile">
        <span className="avatar avatar--lg">{member.nickname.slice(0, 1)}</span>
        <div>
          <strong className="profile__name">{member.nickname}</strong>
          <p className="muted small">{member.email}</p>
        </div>
        <div className="manner">
          <strong className={temp >= 40 ? 'manner--hot' : ''}>{temp.toFixed(1)}°C</strong>
          <div className="manner__bar"><span style={{ width: `${Math.min(100, temp)}%` }} /></div>
          <span className="muted small">매너온도</span>
        </div>
      </section>
      <Link to="/region" className="row-link">
        <Icon name="pin" size={18} />
        <span>내 동네: <strong>{member.regionName}</strong> · 반경 {member.radiusKm}km</span>
        <Icon name="chevron" size={16} className="rot-270" />
      </Link>

      <div className="tabs">
        <button className={`tab ${tab === 'sales' ? 'is-active' : ''}`} onClick={() => setTab('sales')}>판매내역</button>
        <button className={`tab ${tab === 'likes' ? 'is-active' : ''}`} onClick={() => setTab('likes')}>관심목록</button>
      </div>
      {items === null && <Spinner />}
      {items?.length === 0 && <Empty text={tab === 'sales' ? '판매한 물건이 없어요' : '관심 물건이 없어요'} />}
      <div className="grid">{items?.map((p) => <ProductCard key={p.id} p={p} />)}</div>
    </div>
  )
}
