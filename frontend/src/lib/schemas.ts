import { z } from 'zod'

export const loginSchema = z.object({
  email: z.email('邮箱格式不正确'),
  password: z.string().min(1, '请输入密码'),
})

export const registerSchema = z.object({
  username: z.string().regex(/^[a-zA-Z0-9_]{3,20}$/, '须为 3-20 位字母、数字或下划线'),
  email: z.email('邮箱格式不正确'),
  password: z.string().min(8, '密码至少 8 位').max(72, '密码最多 72 位'),
})

export const articleSchema = z.object({
  title: z.string().min(5, '标题至少 5 个字').max(120, '标题最多 120 个字'),
  content: z.string().min(10, '正文至少 10 个字').max(50000, '正文过长'),
  tags: z.array(z.string().min(1).max(30, '标签最长 30 个字')).max(5, '标签最多 5 个'),
})

export const commentSchema = z.object({
  content: z.string().min(1, '请输入评论内容').max(1000, '评论最长 1000 个字'),
})

export type LoginInput = z.infer<typeof loginSchema>
export type RegisterInput = z.infer<typeof registerSchema>
export type ArticleInput = z.infer<typeof articleSchema>
export type CommentInput = z.infer<typeof commentSchema>
