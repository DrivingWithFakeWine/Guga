import { useQuery } from '@tanstack/react-query'
import { lazy, Suspense, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { deleteArticle, fetchArticle, fetchComments } from '../api/articles'
import { errorMessage } from '../api/client'
import CommentForm from '../components/CommentForm'
import CommentList from '../components/CommentList'
import LikeButton from '../components/LikeButton'
import { Badge } from '../components/ui/badge'
import { Button } from '../components/ui/button'
import { useAuth } from '../lib/auth'
import { relativeTime } from '../lib/time'

// Markdown 渲染栈较重，按需加载
const MarkdownView = lazy(() => import('../components/MarkdownView'))

export default function ArticleDetailPage() {
  const { slug = '' } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [deleteError, setDeleteError] = useState('')

  const { data: article, isLoading, isError } = useQuery({
    queryKey: ['article', slug],
    queryFn: () => fetchArticle(slug),
  })

  const { data: comments } = useQuery({
    queryKey: ['comments', slug],
    queryFn: () => fetchComments(slug),
  })

  if (isLoading) {
    return <div className="py-20 text-center text-muted-foreground">加载中…</div>
  }
  if (isError || !article) {
    return <div className="py-20 text-center text-muted-foreground">文章不存在或已被删除</div>
  }

  const isAuthor = user?.username === article.author.username

  async function handleDelete() {
    if (!article || !window.confirm('确定删除这篇文章吗？此操作不可恢复。')) {
      return
    }
    try {
      await deleteArticle(article.slug)
      navigate('/')
    } catch (error) {
      setDeleteError(errorMessage(error, '删除失败'))
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Link to="/" className="inline-block text-sm text-muted-foreground hover:text-foreground">
        ← 返回首页
      </Link>
      <article>
        <h1 className="text-2xl font-bold leading-snug">{article.title}</h1>
        <div className="mt-3 flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
          <span>{article.author.username}</span>
          <span>{relativeTime(article.createdAt)}</span>
          <span className="flex gap-1.5">
            {article.tags.map((tag) => (
              <Link key={tag} to={`/?tag=${tag}`}>
                <Badge variant="secondary" className="cursor-pointer hover:bg-secondary/80">
                  {tag}
                </Badge>
              </Link>
            ))}
          </span>
        </div>
        <div className="mt-4 flex items-center gap-3">
          <LikeButton slug={article.slug} liked={article.likedByMe} count={article.likeCount} />
          {isAuthor && (
            <>
              <Button variant="outline" size="sm" onClick={() => navigate(`/editor/${article.slug}`)}>
                编辑
              </Button>
              <Button variant="ghost" size="sm" className="text-destructive" onClick={handleDelete}>
                删除
              </Button>
            </>
          )}
          {deleteError && <span className="text-sm text-destructive">{deleteError}</span>}
        </div>
        <div className="mt-6 border-t pt-6">
          <Suspense fallback={<div className="py-10 text-center text-muted-foreground">渲染中…</div>}>
            <MarkdownView content={article.content} />
          </Suspense>
        </div>
      </article>

      <section className="border-t pt-6">
        <h2 className="text-lg font-semibold">
          评论 <span className="text-sm font-normal text-muted-foreground">({article.commentCount})</span>
        </h2>
        <div className="mt-4">
          {user ? (
            <CommentForm slug={slug} />
          ) : (
            <p className="text-sm text-muted-foreground">
              <Link to="/login" className="text-primary hover:underline">
                登录
              </Link>{' '}
              后参与评论
            </p>
          )}
        </div>
        <div className="mt-6">
          <CommentList slug={slug} comments={comments ?? []} />
        </div>
      </section>
    </div>
  )
}
