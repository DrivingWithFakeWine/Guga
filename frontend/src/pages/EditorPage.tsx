import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router'
import { fetchArticle, saveArticle } from '../api/articles'
import { errorMessage } from '../api/client'
import { Button } from '../components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'
import { Textarea } from '../components/ui/textarea'
import { useAuth } from '../lib/auth'
import { editorSchema, splitTags, type EditorInput } from '../lib/schemas'

export default function EditorPage() {
  const { slug } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const editing = Boolean(slug)

  const form = useForm<EditorInput>({
    resolver: zodResolver(editorSchema),
    defaultValues: { title: '', content: '', tagsText: '' },
  })

  const { data: article } = useQuery({
    queryKey: ['article', slug],
    queryFn: () => fetchArticle(slug!),
    enabled: editing,
  })

  useEffect(() => {
    if (editing && article) {
      form.reset({
        title: article.title,
        content: article.content,
        tagsText: article.tags.join(', '),
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editing, article])

  const mutation = useMutation({
    mutationFn: (input: EditorInput) =>
      saveArticle(slug ?? null, {
        title: input.title,
        content: input.content,
        tags: splitTags(input.tagsText),
      }),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['article', saved.slug] })
      queryClient.invalidateQueries({ queryKey: ['articles'] })
      queryClient.invalidateQueries({ queryKey: ['tags'] })
      navigate(`/article/${saved.slug}`)
    },
  })

  if (!user) {
    return (
      <Card className="mx-auto mt-10 max-w-sm">
        <CardHeader>
          <CardTitle>请先登录</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            登录后才能{editing ? '编辑' : '发布'}文章。{' '}
            <Link to="/login" className="text-primary hover:underline">
              去登录
            </Link>
          </p>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="mx-auto max-w-3xl">
      <CardHeader>
        <CardTitle>{editing ? '编辑文章' : '发布文章'}</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={form.handleSubmit((input) => mutation.mutate(input))} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="title">标题</Label>
            <Input id="title" placeholder="文章标题（5-120 字）" {...form.register('title')} />
            {form.formState.errors.title && (
              <p className="text-xs text-destructive">{form.formState.errors.title.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="tagsText">标签（逗号分隔，最多 5 个）</Label>
            <Input id="tagsText" placeholder="例如：java, spring boot" {...form.register('tagsText')} />
            {form.formState.errors.tagsText && (
              <p className="text-xs text-destructive">{form.formState.errors.tagsText.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="content">正文（支持 Markdown）</Label>
            <Textarea
              id="content"
              rows={16}
              className="font-mono text-[13px] leading-relaxed"
              placeholder={'# 标题\n\n正文，支持 **Markdown** 与 ```代码块```'}
              {...form.register('content')}
            />
            {form.formState.errors.content && (
              <p className="text-xs text-destructive">{form.formState.errors.content.message}</p>
            )}
          </div>
          {mutation.isError && (
            <p className="text-sm text-destructive">{errorMessage(mutation.error, '保存失败，请稍后重试')}</p>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              取消
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? '保存中…' : editing ? '保存修改' : '发布'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
