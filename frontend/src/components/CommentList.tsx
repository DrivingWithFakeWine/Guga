import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteComment } from '../api/articles'
import { errorMessage } from '../api/client'
import { relativeTime } from '../lib/time'
import { useAuth } from '../lib/auth'
import { Button } from './ui/button'
import type { CommentItem } from '../types'

export default function CommentList({ slug, comments }: { slug: string; comments: CommentItem[] }) {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteComment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comments', slug] })
      queryClient.invalidateQueries({ queryKey: ['article', slug] })
    },
  })

  if (comments.length === 0) {
    return <p className="py-6 text-sm text-muted-foreground">还没有评论，来抢沙发吧</p>
  }

  return (
    <ul className="space-y-4">
      {comments.map((comment) => (
        <li key={comment.id} className="flex items-start justify-between gap-4 border-b pb-4 last:border-b-0">
          <div>
            <div className="text-xs text-muted-foreground">
              {comment.authorName} · {relativeTime(comment.createdAt)}
            </div>
            <p className="mt-1 whitespace-pre-wrap text-sm">{comment.content}</p>
          </div>
          {user?.username === comment.authorName && (
            <Button
              variant="ghost"
              size="sm"
              className="text-muted-foreground"
              disabled={deleteMutation.isPending}
              onClick={() => {
                if (window.confirm('确定删除这条评论吗？')) {
                  deleteMutation.mutate(comment.id)
                }
              }}
            >
              删除
            </Button>
          )}
        </li>
      ))}
      {deleteMutation.isError && <p className="text-sm text-destructive">{errorMessage(deleteMutation.error)}</p>}
    </ul>
  )
}
