import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { AuthProvider, useAuth } from './hooks/useAuth'
import { StompProvider } from './hooks/useStomp'
import { ToastProvider } from './hooks/useToast'
import { UnreadProvider } from './hooks/useUnread'
import { LoginPage, SignupPage } from './pages/AuthPages'
import { ChatListPage } from './pages/ChatListPage'
import { ChatRoomPage } from './pages/ChatRoomPage'
import { HomePage } from './pages/HomePage'
import { MePage } from './pages/MePage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { RegionPage } from './pages/RegionPage'
import { WritePage } from './pages/WritePage'

function RequireAuth() {
  const { member } = useAuth()
  return member ? <Outlet /> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <StompProvider>
            <UnreadProvider>
              <Routes>
                <Route element={<Layout />}>
                  <Route path="/" element={<HomePage />} />
                  <Route path="/products/:id" element={<ProductDetailPage />} />
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/signup" element={<SignupPage />} />
                  <Route element={<RequireAuth />}>
                    <Route path="/write" element={<WritePage />} />
                    <Route path="/products/:id/edit" element={<WritePage />} />
                    <Route path="/chat" element={<ChatListPage />} />
                    <Route path="/chat/:roomId" element={<ChatRoomPage />} />
                    <Route path="/me" element={<MePage />} />
                    <Route path="/region" element={<RegionPage />} />
                  </Route>
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Route>
              </Routes>
            </UnreadProvider>
          </StompProvider>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  )
}
