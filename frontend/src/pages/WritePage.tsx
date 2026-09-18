import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { TopBar } from '../components/TopBar'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { api } from '../lib/api'
import type { Category } from '../lib/types'

export function WritePage() {
  const { id } = useParams()
  const editing = id !== undefined
  const nav = useNavigate()
  const { member } = useAuth()
  const toast = useToast()
  const [categories, setCategories] = useState<Category[]>([])
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('DIGITAL')
  const [price, setPrice] = useState('')
  const [description, setDescription] = useState('')
  const [images, setImages] = useState<string[]>([])
  const [uploading, setUploading] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.categories().then(setCategories).catch(() => {})
    if (editing) {
      api.product(Number(id)).then(({ product: p, mine }) => {
        if (!mine) { toast('본인 글만 수정할 수 있어요', 'error'); nav(-1); return }
        setTitle(p.title); setCategory(p.category); setPrice(String(p.price)); setDescription(p.description); setImages(p.imageUrls)
      })
    }
  }, [editing, id]) // eslint-disable-line react-hooks/exhaustive-deps

  const upload = async (files: FileList | null) => {
    if (!files) return
    setUploading(true)
    try {
      for (const f of Array.from(files).slice(0, 10 - images.length)) {
        const { url } = await api.uploadImage(f)
        setImages((prev) => [...prev, url])
      }
    } catch (e) {
      toast((e as Error).message, 'error')
    } finally {
      setUploading(false)
    }
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    const body = { title, description, price: Number(price.replace(/[^0-9]/g, '') || 0), category, imageUrls: images }
    try {
      if (editing) {
        await api.updateProduct(Number(id), body)
        nav(`/products/${id}`, { replace: true })
      } else {
        const { id: newId } = await api.createProduct(body)
        nav(`/products/${newId}`, { replace: true })
      }
    } catch (err) {
      toast((err as Error).message, 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="page">
      <TopBar back title={editing ? '글 수정' : '내 물건 팔기'} right={
        <button className="textbtn" form="write" type="submit" disabled={saving || !title || !description}>완료</button>
      } />
      <form id="write" className="form" onSubmit={submit}>
        <div className="photos">
          <label className="photos__add">
            <input type="file" accept="image/*" multiple hidden onChange={(e) => upload(e.target.files)} disabled={uploading || images.length >= 10} />
            <Icon name="camera" />
            <span>{images.length}/10</span>
          </label>
          {images.map((u, i) => (
            <div key={u} className="photos__item">
              <img src={u} alt="" />
              {i === 0 && <span className="photos__rep">대표</span>}
              <button type="button" className="photos__del" onClick={() => setImages(images.filter((x) => x !== u))} aria-label="삭제"><Icon name="x" size={12} /></button>
            </div>
          ))}
        </div>

        <input className="input" placeholder="글 제목" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={100} required />

        <div className="chips chips--wrap">
          {categories.map((c) => (
            <button type="button" key={c.code} className={`chip ${category === c.code ? 'is-active' : ''}`} onClick={() => setCategory(c.code)}>{c.label}</button>
          ))}
        </div>

        <div className="input input--price">
          <span className="muted">₩</span>
          <input inputMode="numeric" placeholder="가격 (0이면 나눔)" value={price ? Number(price).toLocaleString('ko-KR') : ''}
                 onChange={(e) => setPrice(e.target.value.replace(/[^0-9]/g, ''))} />
        </div>

        <textarea className="input input--area" rows={8} maxLength={5000} required value={description} onChange={(e) => setDescription(e.target.value)}
                  placeholder={`${member?.regionName ?? '우리 동네'}에 올릴 게시글 내용을 작성해 주세요.\n(판매 금지 물품은 게시가 제한될 수 있어요)`} />
        <p className="muted small">거래 희망 장소: {member?.regionName} (내 동네 기준으로 노출돼요)</p>
      </form>
    </div>
  )
}
