import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { ProductCard } from '../components/ProductCard'
import { Empty, Spinner } from '../components/Spinner'
import { useAuth } from '../hooks/useAuth'
import { api } from '../lib/api'
import type { Category, ProductSummary } from '../lib/types'

/** 비로그인 기본 기준점: 역삼동 */
const GUEST = { lat: 37.5006, lng: 127.0365, radiusKm: 3, name: '역삼동' }

export function HomePage() {
  const { member } = useAuth()
  const [categories, setCategories] = useState<Category[]>([])
  const [category, setCategory] = useState<string>('')
  const [keyword, setKeyword] = useState('')
  const [searching, setSearching] = useState(false)
  const [items, setItems] = useState<ProductSummary[]>([])
  const [cursor, setCursor] = useState<string | null>(null)
  const [hasNext, setHasNext] = useState(true)
  const [loading, setLoading] = useState(false)
  const sentinel = useRef<HTMLDivElement>(null)
  const reqId = useRef(0)

  useEffect(() => { api.categories().then(setCategories).catch(() => {}) }, [])

  const load = useCallback(async (reset: boolean) => {
    const id = ++reqId.current
    setLoading(true)
    try {
      const page = await api.products({
        category: category || undefined,
        keyword: keyword || undefined,
        cursor: reset ? null : cursor,
        size: 20,
        ...(member ? {} : { lat: GUEST.lat, lng: GUEST.lng, radiusKm: GUEST.radiusKm }),
      })
      if (id !== reqId.current) return   // 더 최신 요청이 있으면 버린다
      setItems((prev) => (reset ? page.items : [...prev, ...page.items]))
      setCursor(page.nextCursor)
      setHasNext(page.hasNext)
    } finally {
      if (id === reqId.current) setLoading(false)
    }
  }, [category, keyword, cursor, member])

  // 필터가 바뀌면 처음부터
  useEffect(() => { load(true) }, [category, keyword, member]) // eslint-disable-line react-hooks/exhaustive-deps

  // 무한 스크롤: 바닥 센티널이 보이면 다음 커서
  useEffect(() => {
    const el = sentinel.current
    if (!el) return
    const io = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && hasNext && !loading) load(false)
    }, { rootMargin: '400px' })
    io.observe(el)
    return () => io.disconnect()
  }, [hasNext, loading, load])

  const regionName = member?.regionName ?? GUEST.name
  const radius = member?.radiusKm ?? GUEST.radiusKm

  return (
    <div className="page">
      <header className="topbar topbar--home">
        {searching ? (
          <form className="searchbar" onSubmit={(e) => { e.preventDefault(); setKeyword((e.currentTarget.elements.namedItem('q') as HTMLInputElement).value.trim()) }}>
            <Icon name="search" size={18} />
            <input name="q" autoFocus placeholder="검색어를 입력하세요" defaultValue={keyword} />
            <button type="button" className="iconbtn" onClick={() => { setSearching(false); setKeyword('') }} aria-label="닫기"><Icon name="x" size={18} /></button>
          </form>
        ) : (
          <>
            <Link to={member ? '/region' : '/login'} className="region-btn">
              <strong>{regionName}</strong>
              <span className="muted small">{radius}km</span>
              <Icon name="chevron" size={16} />
            </Link>
            <div className="topbar__right">
              <button className="iconbtn" onClick={() => setSearching(true)} aria-label="검색"><Icon name="search" /></button>
            </div>
          </>
        )}
      </header>

      <div className="chips">
        <button className={`chip ${category === '' ? 'is-active' : ''}`} onClick={() => setCategory('')}>전체</button>
        {categories.map((c) => (
          <button key={c.code} className={`chip ${category === c.code ? 'is-active' : ''}`} onClick={() => setCategory(c.code)}>{c.label}</button>
        ))}
      </div>

      {keyword && <p className="result-label">‘{keyword}’ 검색 결과</p>}

      <div className="grid">
        {items.map((p) => <ProductCard key={p.id} p={p} />)}
      </div>
      {loading && <Spinner />}
      {!loading && items.length === 0 && (
        <Empty text="근처에 올라온 물건이 없어요" sub={member ? '동네 반경을 넓혀 보세요' : '로그인하면 내 동네 기준으로 보여드려요'} />
      )}
      <div ref={sentinel} style={{ height: 1 }} />

      {member && (
        <Link to="/write" className="fab" aria-label="글쓰기">
          <Icon name="plus" size={24} /> <span>글쓰기</span>
        </Link>
      )}
    </div>
  )
}
