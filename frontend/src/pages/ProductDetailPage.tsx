import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { Spinner } from '../components/Spinner'
import { TopBar } from '../components/TopBar'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { api } from '../lib/api'
import { STATUS_LABEL, timeAgo, won } from '../lib/format'
import type { ProductDetailResponse, ProductStatus } from '../lib/types'

export function ProductDetailPage() {
  const { id } = useParams()
  const productId = Number(id)
  const nav = useNavigate()
  const { member } = useAuth()
  const toast = useToast()
  const [data, setData] = useState<ProductDetailResponse | null>(null)
  const [slide, setSlide] = useState(0)
  const [busy, setBusy] = useState(false)

  const reload = () => api.product(productId).then(setData).catch((e) => { toast(e.message, 'error'); nav('/') })
  useEffect(() => { reload() }, [productId]) // eslint-disable-line react-hooks/exhaustive-deps

  if (!data) return <div className="page"><TopBar back transparent /><Spinner /></div>
  const { product: p, liked, mine } = data

  const like = async () => {
    if (!member) return nav('/login')
    const res = await api.toggleLike(p.id)
    setData({ ...data, liked: res.liked, product: { ...p, likeCount: p.likeCount + (res.liked ? 1 : -1) } })
  }

  const chat = async () => {
    if (!member) return nav('/login')
    setBusy(true)
    try {
      const { roomId } = await api.openRoom(p.id)
      nav(`/chat/${roomId}`)
    } catch (e) {
      toast((e as Error).message, 'error')
    } finally {
      setBusy(false)
    }
  }

  const changeStatus = async (status: ProductStatus) => {
    await api.changeStatus(p.id, status)
    toast(`${STATUS_LABEL[status]}(으)로 변경했어요`)
    reload()
  }

  const bump = async () => {
    try {
      await api.bump(p.id)
      toast('끌어올렸어요! 목록 맨 위로 올라갑니다')
      reload()
    } catch (e) {
      toast((e as Error).message, 'error')
    }
  }

  const remove = async () => {
    if (!confirm('정말 삭제할까요?')) return
    await api.deleteProduct(p.id)
    toast('삭제했어요')
    nav('/')
  }

  const temp = Number(p.seller.mannerTemperature)

  return (
    <div className="page page--detail">
      <TopBar back transparent right={mine && (
        <Link to={`/products/${p.id}/edit`} className="iconbtn" aria-label="수정"><Icon name="edit" /></Link>
      )} />

      <div className="carousel" onScroll={(e) => setSlide(Math.round(e.currentTarget.scrollLeft / e.currentTarget.clientWidth))}>
        {p.imageUrls.length === 0 && <div className="carousel__noimg"><Icon name="camera" size={40} /></div>}
        {p.imageUrls.map((u, i) => <img key={i} src={u} alt="" />)}
        {p.imageUrls.length > 1 && (
          <div className="carousel__dots">{p.imageUrls.map((_, i) => <span key={i} className={i === slide ? 'is-active' : ''} />)}</div>
        )}
      </div>

      <section className="seller">
        <span className="avatar">{p.seller.nickname.slice(0, 1)}</span>
        <div className="seller__info">
          <strong>{p.seller.nickname}</strong>
          <span className="muted small">{p.seller.regionName}</span>
        </div>
        <div className="manner">
          <strong className={temp >= 40 ? 'manner--hot' : ''}>{temp.toFixed(1)}°C</strong>
          <div className="manner__bar"><span style={{ width: `${Math.min(100, temp)}%` }} /></div>
          <span className="muted small">매너온도</span>
        </div>
      </section>

      <section className="detail">
        {p.status !== 'ON_SALE' && <span className={`pill pill--${p.status.toLowerCase()}`}>{STATUS_LABEL[p.status]}</span>}
        <h1>{p.title}</h1>
        <p className="muted small">{p.categoryLabel} · {timeAgo(p.bumpedAt)}</p>
        <p className="detail__desc">{p.description}</p>
        <p className="muted small">관심 {p.likeCount} · 채팅 {p.chatCount} · 조회 {p.viewCount}</p>
      </section>

      {mine && (
        <section className="owner">
          <label className="muted small">판매 상태</label>
          <div className="owner__row">
            {(['ON_SALE', 'RESERVED', 'SOLD'] as ProductStatus[]).map((s) => (
              <button key={s} className={`chip ${p.status === s ? 'is-active' : ''}`} onClick={() => changeStatus(s)}>{STATUS_LABEL[s]}</button>
            ))}
          </div>
          <div className="owner__row">
            <button className="btn btn--ghost" onClick={bump}><Icon name="arrowUp" size={16} /> 끌어올리기</button>
            <button className="btn btn--ghost btn--danger" onClick={remove}><Icon name="trash" size={16} /> 삭제</button>
          </div>
        </section>
      )}

      <footer className="bottombar">
        <button className={`likebtn ${liked ? 'is-liked' : ''}`} onClick={like} aria-label="관심">
          <Icon name={liked ? 'heartFill' : 'heart'} />
        </button>
        <div className="bottombar__price">
          <strong>{won(p.price)}</strong>
          <span className="muted small">{p.price === 0 ? '무료 나눔' : '가격 제안 불가'}</span>
        </div>
        {mine ? (
          <Link to="/chat" className="btn btn--primary">대화 중인 채팅 {p.chatCount}</Link>
        ) : (
          <button className="btn btn--primary" onClick={chat} disabled={busy || p.status === 'SOLD'}>
            {p.status === 'SOLD' ? '거래완료' : '채팅하기'}
          </button>
        )}
      </footer>
    </div>
  )
}
