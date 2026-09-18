import { Link } from 'react-router-dom'
import { Icon } from './Icon'
import { STATUS_LABEL, distance, timeAgo, won } from '../lib/format'
import type { ProductSummary } from '../lib/types'

export function ProductCard({ p }: { p: ProductSummary }) {
  return (
    <Link to={`/products/${p.id}`} className="card">
      <div className="card__thumb">
        {p.thumbnailUrl ? <img src={p.thumbnailUrl} alt="" loading="lazy" /> : <div className="card__noimg"><Icon name="camera" /></div>}
        {p.status !== 'ON_SALE' && <span className={`card__status card__status--${p.status.toLowerCase()}`}>{STATUS_LABEL[p.status]}</span>}
      </div>
      <div className="card__body">
        <h3 className="card__title">{p.title}</h3>
        <p className="card__meta">
          {p.regionName}
          {p.distanceMeters !== null && ` · ${distance(p.distanceMeters)}`}
          {` · ${timeAgo(p.bumpedAt)}`}
        </p>
        <p className="card__price">{won(p.price)}</p>
        <p className="card__counts">
          {p.chatCount > 0 && <span><Icon name="chat" size={14} /> {p.chatCount}</span>}
          {p.likeCount > 0 && <span><Icon name="heart" size={14} /> {p.likeCount}</span>}
        </p>
      </div>
    </Link>
  )
}
