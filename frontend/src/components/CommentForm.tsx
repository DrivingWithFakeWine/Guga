import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { addComment } from '../api/articles'
import { errorMessage } from '../api/client'
import { commentSchema, type CommentInput } from '../lib/schemas'
import { Button } from './ui/button'
import { Textarea } from './ui/textarea'

export default function CommentForm({ slug }: { slug: string }) {
  const queryClient = useQueryClient()
  const form = useForm<CommentInput>({
    resolver: zodResolver(commentSchema),
    defaultValues: { content: '' },
  })

  const mutation = useMutation({
    mutationFn: (input: CommentInput) => addComment(slug, input.content),
    onSuccess: () => {
      form.reset()
      queryClient.invalidateQueries({ queryKey: ['comments', slug] })
      queryClient.invalidateQueries({ queryKey: ['article', slug] })
    },
  })

  return (
    <form
      onSubmit={form.handleSubmit((input) => mutation.mutate(input))}
      className="space-y-3"
    >
      <Textarea
        placeholder="写下你的评论…"
        rows={3}
        {...form.register('content')}
      />
      <div className="flex items-center justify-between">
        {form.formState.errors.content ? (
          <p className="text-xs text-destructive">{form.formState.errors.content.message}</p>
        ) : mutation.isError ? (
          <p className="text-xs text-destructive">{errorMessage(mutation.error)}</p>
        ) : (
          <span />
        )}
        <Button type="submit" size="sm" disabled={mutation.isPending}>
          {mutation.isPending ? '发布中…' : '发布评论'}
        </Button>
      </div>
    </form>
  )
}
