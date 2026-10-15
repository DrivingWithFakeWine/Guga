import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import Header from './components/Header'
import { AuthProvider } from './lib/auth'
import ArticleDetailPage from './pages/ArticleDetailPage'
import EditorPage from './pages/EditorPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <div className="min-h-screen">
            <Header />
            <main className="mx-auto max-w-5xl px-4 py-6">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/article/:slug" element={<ArticleDetailPage />} />
                <Route path="/editor" element={<EditorPage />} />
                <Route path="/editor/:slug" element={<EditorPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="*" element={<div className="py-20 text-center text-muted-foreground">页面不存在</div>} />
              </Routes>
            </main>
          </div>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
