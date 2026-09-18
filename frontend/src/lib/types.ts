export interface Member {
  id: number
  email: string
  nickname: string
  regionName: string
  latitude: number
  longitude: number
  radiusKm: number
  mannerTemperature: number
}

export interface Region {
  name: string
  latitude: number
  longitude: number
}

export interface Category {
  code: string
  label: string
}

export type ProductStatus = 'ON_SALE' | 'RESERVED' | 'SOLD'

export interface ProductSummary {
  id: number
  title: string
  price: number
  category: string
  status: ProductStatus
  regionName: string
  thumbnailUrl: string | null
  likeCount: number
  chatCount: number
  viewCount: number
  distanceMeters: number | null
  bumpedAt: string
}

export interface CursorPage<T> {
  items: T[]
  nextCursor: string | null
  hasNext: boolean
}

export interface PublicProfile {
  id: number
  nickname: string
  regionName: string
  mannerTemperature: number
}

export interface ProductDetail {
  id: number
  title: string
  description: string
  price: number
  category: string
  categoryLabel: string
  status: ProductStatus
  regionName: string
  latitude: number
  longitude: number
  imageUrls: string[]
  viewCount: number
  likeCount: number
  chatCount: number
  createdAt: string
  bumpedAt: string
  seller: PublicProfile
}

export interface ProductDetailResponse {
  product: ProductDetail
  liked: boolean
  mine: boolean
}

export interface ChatMessage {
  id: number
  roomId: number
  senderId: number
  content: string
  createdAt: string
  readAt: string | null
}

export interface ChatRoom {
  id: number
  product: { id: number; title: string; price: number; status: ProductStatus; thumbnailUrl: string | null } | null
  counterpart: { id: number; nickname: string; regionName: string } | null
  iAmSeller: boolean
  lastMessage: string | null
  lastMessageAt: string | null
  unreadCount: number
}

export interface RoomEvent {
  type: 'MESSAGE' | 'READ'
  message: ChatMessage | null
  readerId: number | null
  readAt: string | null
}
