import { Link } from 'react-router'
import { Badge } from './ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from './ui/card'
import { relativeTime } from '../lib/time'
import type { ArticleSummary } from '../types'

export default function ArticleCard({ article }: { article: ArticleSummary }) {
  return (
    <Card className="transition-shadow hover:shadow-md">
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
          <span>{relativeTime(article.createdAt)}</span>
          <span>👍 {article.likeCount}</span>
          <span>💬 {article.commentCount}</span>
        </div>
      </CardContent>
    </Card>
  )
}
