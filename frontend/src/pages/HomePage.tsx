import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { fetchArticles } from '../api/articles'
import ArticleCard from '../components/ArticleCard'
import TagFilter from '../components/TagFilter'
import { Button } from '../components/ui/button'
import { Input } from '../components/ui/input'
import { cn } from '../lib/utils'

const PAGE_SIZE = 10

export default function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Math.max(Number(searchParams.get('page') ?? 0) || 0, 0)
  const tag = searchParams.get('tag') ?? undefined
  const q = searchParams.get('q') ?? undefined
  const sort = searchParams.get('sort') === 'hottest' ? 'hottest' : 'latest'
  const [searchText, setSearchText] = useState(q ?? '')

  const { data, isLoading, isError } = useQuery({
    queryKey: ['articles', { page, tag, q, sort }],
    queryFn: () => fetchArticles({ page, size: PAGE_SIZE, tag, q, sort }),
  })

  function updateParams(changes: Record<string, string | undefined>) {
    const next = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(changes)) {
      if (value === undefined) {
        next.delete(key)
      } else {
        next.set(key, value)
      }
    }
    if (!('page' in changes)) {
      next.delete('page')
    }
    setSearchParams(next)
  }

  const total = data?.totalPages ?? 0

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-3">
        <form
          className="flex flex-1 gap-2"
          onSubmit={(event) => {
            event.preventDefault()
            updateParams({ q: searchText.trim() || undefined })
          }}
        >
          <Input
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
            placeholder="搜索文章标题或内容…"
            className="max-w-xs"
          />
          <Button type="submit" variant="outline" size="sm">
            搜索
          </Button>
          {q && (
            <Button type="button" variant="ghost" size="sm" onClick={() => { setSearchText(''); updateParams({ q: undefined }) }}>
              清除
            </Button>
          )}
        </form>
        <div className="flex items-center gap-1 text-sm">
          <button
            type="button"
            className={cn('rounded-md px-2 py-1', sort === 'latest' ? 'bg-secondary font-medium' : 'text-muted-foreground hover:text-foreground')}
            onClick={() => updateParams({ sort: undefined })}
          >
            最新
          </button>
          <button
            type="button"
            className={cn('rounded-md px-2 py-1', sort === 'hottest' ? 'bg-secondary font-medium' : 'text-muted-foreground hover:text-foreground')}
            onClick={() => updateParams({ sort: 'hottest' })}
          >
            热门
          </button>
        </div>
      </div>

      <TagFilter active={tag} onSelect={(next) => updateParams({ tag: next })} />

      {isLoading ? (
        <div className="py-20 text-center text-muted-foreground">加载中…</div>
      ) : isError ? (
        <div className="py-20 text-center text-muted-foreground">加载失败，请稍后重试</div>
      ) : !data || data.content.length === 0 ? (
        <div className="py-20 text-center text-muted-foreground">
          {q || tag ? '没有匹配的文章，换个条件试试' : '还没有文章，登录后发布第一篇吧'}
        </div>
      ) : (
        <div className="space-y-4">
          {data.content.map((article) => (
            <ArticleCard key={article.id} article={article} />
          ))}
        </div>
      )}

      {total > 1 && (
        <div className="flex items-center justify-center gap-3 pt-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => updateParams({ page: String(page - 1) })}>
            上一页
          </Button>
          <span className="text-sm text-muted-foreground">
            第 {page + 1} / {total} 页
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= total - 1}
            onClick={() => updateParams({ page: String(page + 1) })}
          >
            下一页
          </Button>
        </div>
      )}
    </div>
  )
}
