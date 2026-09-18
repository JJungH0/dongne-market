import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { TopBar } from '../components/TopBar'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { api } from '../lib/api'
import type { Region } from '../lib/types'

export function RegionPage() {
  const { member, refresh } = useAuth()
  const nav = useNavigate()
  const toast = useToast()
  const [regions, setRegions] = useState<Region[]>([])
  const [selected, setSelected] = useState<Region | null>(member ? { name: member.regionName, latitude: member.latitude, longitude: member.longitude } : null)
  const [radius, setRadius] = useState(member?.radiusKm ?? 3)
  const [locating, setLocating] = useState(false)

  useEffect(() => { api.regions().then(setRegions) }, [])

  const useCurrent = () => {
    if (!navigator.geolocation) return toast('이 브라우저는 위치를 지원하지 않아요', 'error')
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        // 가장 가까운 프리셋 이름을 빌려 쓴다 (실서비스는 역지오코딩)
        const { latitude, longitude } = pos.coords
        const nearest = regions.reduce((a, b) => dist(a, latitude, longitude) < dist(b, latitude, longitude) ? a : b)
        setSelected({ name: `${nearest.name} 근처`, latitude, longitude })
        setLocating(false)
      },
      () => { toast('위치를 가져오지 못했어요', 'error'); setLocating(false) },
      { timeout: 8000 },
    )
  }

  const save = async () => {
    if (!selected) return
    await api.updateRegion({ regionName: selected.name, latitude: selected.latitude, longitude: selected.longitude, radiusKm: radius })
    await refresh()
    toast('내 동네를 변경했어요')
    nav('/', { replace: true })
  }

  return (
    <div className="page">
      <TopBar back title="내 동네 설정" right={<button className="textbtn" onClick={save} disabled={!selected}>완료</button>} />
      <div className="form">
        <button className="btn btn--ghost btn--block" onClick={useCurrent} disabled={locating || regions.length === 0}>
          <Icon name="pin" size={18} /> {locating ? '위치 확인 중…' : '현재 위치로 설정'}
        </button>
        <label className="label">동네 선택</label>
        <div className="region-grid">
          {regions.map((r) => (
            <button key={r.name} className={`chip chip--lg ${selected?.name === r.name ? 'is-active' : ''}`} onClick={() => setSelected(r)}>{r.name}</button>
          ))}
        </div>
        <label className="label">거래 반경 <strong>{radius}km</strong> <span className="muted small">— 이 안의 물건만 보여요</span></label>
        <input type="range" min={1} max={20} value={radius} onChange={(e) => setRadius(Number(e.target.value))} className="range" />
        <div className="range__labels"><span>1km · 가까운 동네</span><span>20km · 먼 동네까지</span></div>
        {selected && <p className="muted small">선택: {selected.name} ({selected.latitude.toFixed(4)}, {selected.longitude.toFixed(4)})</p>}
      </div>
    </div>
  )
}

function dist(r: Region, lat: number, lng: number) {
  return (r.latitude - lat) ** 2 + (r.longitude - lng) ** 2
}
