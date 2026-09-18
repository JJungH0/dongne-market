import type {
  Category, ChatMessage, ChatRoom, CursorPage, Member, ProductDetailResponse, ProductStatus, ProductSummary, Region,
} from './types'

const TOKEN_KEY = 'dongne.token'
const MEMBER_KEY = 'dongne.member'

export const auth = {
  token: (): string | null => {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
  },
  member: (): Member | null => {
    try { const raw = localStorage.getItem(MEMBER_KEY); return raw ? JSON.parse(raw) : null } catch { return null }
  },
  save(token: string, member: Member) {
    try { localStorage.setItem(TOKEN_KEY, token); localStorage.setItem(MEMBER_KEY, JSON.stringify(member)) } catch { /* 무시 */ }
  },
  saveMember(member: Member) {
    try { localStorage.setItem(MEMBER_KEY, JSON.stringify(member)) } catch { /* 무시 */ }
  },
  clear() {
    try { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(MEMBER_KEY) } catch { /* 무시 */ }
  },
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as Record<string, string> ?? {}) }
  if (!(init.body instanceof FormData)) headers['Content-Type'] = 'application/json'
  const token = auth.token()
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(url, { ...init, headers })
  if (res.status === 401 && token) {
    auth.clear()
    window.dispatchEvent(new Event('dongne:logout'))
  }
  if (!res.ok) {
    let detail = res.statusText
    try { const p = await res.json(); detail = p.detail ?? p.title ?? detail } catch { /* JSON 아님 */ }
    throw new ApiError(res.status, detail)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export interface ProductQuery {
  category?: string
  keyword?: string
  cursor?: string | null
  size?: number
  lat?: number
  lng?: number
  radiusKm?: number
}

export const api = {
  signup: (body: object) => request<{ accessToken: string; member: Member }>('/api/auth/signup', { method: 'POST', body: JSON.stringify(body) }),
  login: (email: string, password: string) =>
    request<{ accessToken: string; member: Member }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  me: () => request<Member>('/api/me'),
  updateRegion: (body: { regionName: string; latitude: number; longitude: number; radiusKm: number }) =>
    request<Member>('/api/me/region', { method: 'PUT', body: JSON.stringify(body) }),
  regions: () => request<Region[]>('/api/regions'),
  categories: () => request<Category[]>('/api/products/categories'),

  products: (q: ProductQuery) => {
    const p = new URLSearchParams()
    Object.entries(q).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') p.set(k, String(v)) })
    return request<CursorPage<ProductSummary>>(`/api/products?${p}`)
  },
  product: (id: number) => request<ProductDetailResponse>(`/api/products/${id}`),
  createProduct: (body: object) => request<{ id: number }>('/api/products', { method: 'POST', body: JSON.stringify(body) }),
  updateProduct: (id: number, body: object) => request<void>(`/api/products/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  changeStatus: (id: number, status: ProductStatus) =>
    request<void>(`/api/products/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  bump: (id: number) => request<void>(`/api/products/${id}/bump`, { method: 'POST' }),
  deleteProduct: (id: number) => request<void>(`/api/products/${id}`, { method: 'DELETE' }),
  toggleLike: (id: number) => request<{ liked: boolean }>(`/api/products/${id}/like`, { method: 'POST' }),
  mySales: () => request<ProductSummary[]>('/api/me/products'),
  myLikes: () => request<ProductSummary[]>('/api/me/likes'),

  uploadImage: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return request<{ url: string }>('/api/images', { method: 'POST', body: fd })
  },

  openRoom: (productId: number) => request<{ roomId: number }>(`/api/chat/rooms?productId=${productId}`, { method: 'POST' }),
  rooms: () => request<ChatRoom[]>('/api/chat/rooms'),
  room: (id: number) => request<ChatRoom>(`/api/chat/rooms/${id}`),
  messages: (id: number, beforeId?: number) =>
    request<ChatMessage[]>(`/api/chat/rooms/${id}/messages?size=50${beforeId ? `&beforeId=${beforeId}` : ''}`),
  sendMessage: (id: number, content: string) =>
    request<ChatMessage>(`/api/chat/rooms/${id}/messages`, { method: 'POST', body: JSON.stringify({ content }) }),
  markRead: (id: number) => request<{ updated: number }>(`/api/chat/rooms/${id}/read`, { method: 'POST' }),
}
