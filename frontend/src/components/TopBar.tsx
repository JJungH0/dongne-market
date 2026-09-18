import { useNavigate } from 'react-router-dom'
import { Icon } from './Icon'
import type { ReactNode } from 'react'

interface Props {
  title?: ReactNode
  back?: boolean
  right?: ReactNode
  transparent?: boolean
}

export function TopBar({ title, back, right, transparent }: Props) {
  const nav = useNavigate()
  return (
    <header className={`topbar ${transparent ? 'topbar--transparent' : ''}`}>
      <div className="topbar__left">
        {back && (
          <button className="iconbtn" onClick={() => (history.length > 1 ? nav(-1) : nav('/'))} aria-label="뒤로">
            <Icon name="back" />
          </button>
        )}
      </div>
      <div className="topbar__title">{title}</div>
      <div className="topbar__right">{right}</div>
    </header>
  )
}
