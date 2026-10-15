import { useQuery } from '@tanstack/react-query'
import { fetchTags } from '../api/articles'
import { cn } from '../lib/utils'
import { Badge } from './ui/badge'

export default function TagFilter({
  active,
  onSelect,
}: {
  active?: string
  onSelect: (tag: string | undefined) => void
}) {
  const { data: tags } = useQuery({
    queryKey: ['tags'],
    queryFn: fetchTags,
    staleTime: 5 * 60_000,
  })

  if (!tags || tags.length === 0) {
    return null
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <button
        type="button"
        onClick={() => onSelect(undefined)}
        className={cn('text-sm', active === undefined ? 'font-semibold text-primary' : 'text-muted-foreground hover:text-foreground')}
      >
        全部
      </button>
      {tags.map((tag) => (
        <Badge
          key={tag.name}
          role="button"
          tabIndex={0}
          variant={active === tag.name ? 'default' : 'outline'}
          className="cursor-pointer"
          onClick={() => onSelect(active === tag.name ? undefined : tag.name)}
        >
          {tag.name} · {tag.articleCount}
        </Badge>
      ))}
    </div>
  )
}
