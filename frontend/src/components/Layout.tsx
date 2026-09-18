import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { Icon } from './Icon'
import { useUnread } from '../hooks/useUnread'

/** 모바일 앱형 프레임: 중앙 컬럼 + 하단 탭. 채팅방·글쓰기 같은 전체화면 페이지에선 탭 숨김 */
export function Layout() {
  const { total } = useUnread()
  const { pathname } = useLocation()
  const hideTab = /^\/products\/\d+|^\/chat\/\d+|^\/write|\/edit$|^\/login|^\/signup|^\/region/.test(pathname)

  return (
    <div className="frame">
      <div className="frame__body">
        <Outlet />
      </div>
      {!hideTab && (
        <nav className="tabbar">
          <NavLink to="/" end className={({ isActive }) => `tabbar__btn ${isActive ? 'is-active' : ''}`}>
            <Icon name="home" /> <span>홈</span>
          </NavLink>
          <NavLink to="/chat" className={({ isActive }) => `tabbar__btn ${isActive ? 'is-active' : ''}`}>
            <span className="tabbar__icon">
              <Icon name="chat" />
              {total > 0 && <span className="badge">{total > 99 ? '99+' : total}</span>}
            </span>
            <span>채팅</span>
          </NavLink>
          <NavLink to="/me" className={({ isActive }) => `tabbar__btn ${isActive ? 'is-active' : ''}`}>
            <Icon name="user" /> <span>나의 동네</span>
          </NavLink>
        </nav>
      )}
    </div>
  )
}
