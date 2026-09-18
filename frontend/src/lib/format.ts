export const won = (n: number) => (n === 0 ? '나눔' : `${n.toLocaleString('ko-KR')}원`)

export function timeAgo(iso: string | null | undefined) {
  if (!iso) return ''
  const diff = (Date.now() - new Date(iso).getTime()) / 1000
  if (diff < 60) return '방금 전'
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  if (diff < 86400 * 30) return `${Math.floor(diff / 86400)}일 전`
  return new Date(iso).toLocaleDateString('ko-KR')
}

export function distance(m: number | null) {
  if (m === null || m === undefined) return ''
  return m < 1000 ? `${Math.round(m)}m` : `${(m / 1000).toFixed(1)}km`
}

export const STATUS_LABEL: Record<string, string> = { ON_SALE: '판매중', RESERVED: '예약중', SOLD: '거래완료' }

export function chatTime(iso: string) {
  const d = new Date(iso)
  return d.toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit' })
}
