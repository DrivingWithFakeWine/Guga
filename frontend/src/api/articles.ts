import { api } from './client'
import type { ArticleDetail, ArticleSummary, CommentItem, PageResponse, TagStat } from '../types'

export interface ArticleQuery {
  page?: number
  size?: number
  tag?: string
  q?: string
  sort?: 'latest' | 'hottest'
}

export async function fetchArticles(query: ArticleQuery): Promise<PageResponse<ArticleSummary>> {
  const { data } = await api.get<PageResponse<ArticleSummary>>('/articles', { params: query })
  return data
}

export async function fetchArticle(slug: string): Promise<ArticleDetail> {
  const { data } = await api.get<ArticleDetail>(`/articles/${slug}`)
  return data
}

export async function fetchTags(): Promise<TagStat[]> {
  const { data } = await api.get<TagStat[]>('/tags')
  return data
}

export async function saveArticle(slug: string | null, input: { title: string; content: string; tags: string[] }): Promise<ArticleDetail> {
  if (slug) {
    const { data } = await api.put<ArticleDetail>(`/articles/${slug}`, input)
    return data
  }
  const { data } = await api.post<ArticleDetail>('/articles', input)
  return data
}

export async function deleteArticle(slug: string): Promise<void> {
  await api.delete(`/articles/${slug}`)
}

export async function fetchComments(slug: string): Promise<CommentItem[]> {
  const { data } = await api.get<CommentItem[]>(`/articles/${slug}/comments`)
  return data
}

export async function addComment(slug: string, content: string): Promise<CommentItem> {
  const { data } = await api.post<CommentItem>(`/articles/${slug}/comments`, { content })
  return data
}

export async function deleteComment(id: number): Promise<void> {
  await api.delete(`/comments/${id}`)
}
