export interface User {
  id: number
  username: string
  email: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface ErrorResponse {
  status: number
  error: string
  message: string
  path: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ArticleSummary {
  id: number
  slug: string
  title: string
  excerpt: string
  authorName: string
  tags: string[]
  likeCount: number
  commentCount: number
  createdAt: string
}

export interface ArticleDetail {
  id: number
  slug: string
  title: string
  content: string
  author: User
  tags: string[]
  likeCount: number
  commentCount: number
  likedByMe: boolean | null
  createdAt: string
  updatedAt: string
}

export interface CommentItem {
  id: number
  content: string
  authorName: string
  createdAt: string
}

export interface TagStat {
  name: string
  articleCount: number
}
