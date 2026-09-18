import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { TopBar } from '../components/TopBar'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { api } from '../lib/api'
import type { Region } from '../lib/types'

export function LoginPage() {
  const { login } = useAuth()
  const nav = useNavigate()
  const toast = useToast()
  const [email, setEmail] = useState('minsu@example.com')
  const [password, setPassword] = useState('password123')
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    try { await login(email, password); nav('/', { replace: true }) }
    catch (err) { toast((err as Error).message, 'error') }
    finally { setBusy(false) }
  }

  return (
    <div className="page">
      <TopBar back title="로그인" />
      <form className="form form--auth" onSubmit={submit}>
        <div className="brand"><span className="brand__mark">동</span><h1>동네마켓</h1><p className="muted">우리 동네 중고 직거래</p></div>
        <input className="input" type="email" placeholder="이메일" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <input className="input" type="password" placeholder="비밀번호" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <button className="btn btn--primary btn--block" disabled={busy}>로그인</button>
        <p className="muted small center">시드 계정: minsu / yuna / jihoon / sora @example.com · password123</p>
        <p className="center">계정이 없나요? <Link to="/signup" className="link">회원가입</Link></p>
      </form>
    </div>
  )
}

export function SignupPage() {
  const { signup } = useAuth()
  const nav = useNavigate()
  const toast = useToast()
  const [regions, setRegions] = useState<Region[]>([])
  const [form, setForm] = useState({ email: '', password: '', nickname: '', regionName: '역삼동' })
  const [busy, setBusy] = useState(false)

  useEffect(() => { api.regions().then(setRegions) }, [])

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const region = regions.find((r) => r.name === form.regionName)
    if (!region) return
    setBusy(true)
    try {
      await signup({ ...form, latitude: region.latitude, longitude: region.longitude })
      nav('/', { replace: true })
    } catch (err) { toast((err as Error).message, 'error') }
    finally { setBusy(false) }
  }

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm({ ...form, [k]: e.target.value })

  return (
    <div className="page">
      <TopBar back title="회원가입" />
      <form className="form form--auth" onSubmit={submit}>
        <input className="input" type="email" placeholder="이메일" value={form.email} onChange={set('email')} required />
        <input className="input" type="password" placeholder="비밀번호 (8자 이상)" minLength={8} value={form.password} onChange={set('password')} required />
        <input className="input" placeholder="닉네임 (2~20자)" minLength={2} maxLength={20} value={form.nickname} onChange={set('nickname')} required />
        <label className="label">내 동네</label>
        <select className="input" value={form.regionName} onChange={set('regionName')}>
          {regions.map((r) => <option key={r.name} value={r.name}>{r.name}</option>)}
        </select>
        <button className="btn btn--primary btn--block" disabled={busy}>가입하고 시작하기</button>
      </form>
    </div>
  )
}
