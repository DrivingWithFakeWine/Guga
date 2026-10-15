import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { api } from '../api/client'
import { Badge } from '../components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card'
import type { ArticleSummary, PageResponse } from '../types'

export default function HomePage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['articles'],
    queryFn: async () => {
      const { data } = await api.get<PageResponse<ArticleSummary>>('/articles', {
        params: { page: 0, size: 10 },
      })
      return data
    },
  })

  if (isLoading) {
    return <div className="py-20 text-center text-muted-foreground">加载中…</div>
  }
  if (isError) {
    return <div className="py-20 text-center text-muted-foreground">加载失败，请稍后重试</div>
  }
  if (!data || data.content.length === 0) {
    return <div className="py-20 text-center text-muted-foreground">还没有文章，登录后发布第一篇吧</div>
  }

  return (
    <div className="space-y-4">
      {data.content.map((article) => (
        <Card key={article.id} className="transition-shadow hover:shadow-md">
          <CardHeader>
            <CardTitle className="text-base">
              <Link to={`/article/${article.slug}`} className="hover:text-primary">
                {article.title}
              </Link>
            </CardTitle>
            <div className="flex flex-wrap gap-1.5 pt-1">
              {article.tags.map((tag) => (
                <Badge key={tag} variant="secondary">
                  {tag}
                </Badge>
              ))}
            </div>
          </CardHeader>
          <CardContent>
            <p className="line-clamp-2 text-sm text-muted-foreground">{article.excerpt}</p>
            <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
              <span>{article.authorName}</span>
              <span>{new Date(article.createdAt).toLocaleDateString('zh-CN')}</span>
              <span>👍 {article.likeCount}</span>
              <span>💬 {article.commentCount}</span>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
