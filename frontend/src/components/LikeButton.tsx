import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router'
import { api } from '../api/client'
import { Button } from './ui/button'
import { useAuth } from '../lib/auth'

export default function LikeButton({ slug, liked, count }: { slug: string; liked: boolean | null; count: number }) {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: async () => (await api.post<{ liked: boolean; likeCount: number }>(`/articles/${slug}/like`)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['article', slug] })
      queryClient.invalidateQueries({ queryKey: ['articles'] })
    },
  })

  const active = liked === true

  if (!user) {
    return (
      <Link
        to="/login"
        className="inline-flex h-8 items-center rounded-md border border-input px-3 text-xs text-muted-foreground hover:text-foreground"
      >
        👍 点赞 · {count}
      </Link>
    )
  }

  return (
    <Button
      variant={active ? 'default' : 'outline'}
      size="sm"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      👍 {active ? '已赞' : '点赞'} · {count}
    </Button>
  )
}
