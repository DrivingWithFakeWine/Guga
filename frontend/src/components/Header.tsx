import { Link, useNavigate } from 'react-router'
import { useAuth } from '../lib/auth'
import { Button } from './ui/button'

export default function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <header className="sticky top-0 z-10 border-b bg-background/95 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
        <Link to="/" className="text-lg font-bold text-primary">
          Guga
        </Link>
        <nav className="flex items-center gap-3">
          {user ? (
            <>
              <Button size="sm" onClick={() => navigate('/editor')}>
                写文章
              </Button>
              <span className="text-sm text-muted-foreground">{user.username}</span>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  logout()
                  navigate('/')
                }}
              >
                退出
              </Button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm text-muted-foreground hover:text-foreground">
                登录
              </Link>
              <Button size="sm" onClick={() => navigate('/register')}>
                注册
              </Button>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
