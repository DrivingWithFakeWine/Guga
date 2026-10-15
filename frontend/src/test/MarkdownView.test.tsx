import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import MarkdownView from '../components/MarkdownView'

describe('MarkdownView 安全渲染', () => {
  it('script 标签不会渲染为元素', () => {
    const { container } = render(<MarkdownView content={'你好\n\n<script>alert(1)</script>'} />)
    expect(container.querySelector('script')).toBeNull()
    expect(container.textContent).toContain('你好')
  })

  it('javascript: 链接被净化', () => {
    render(<MarkdownView content={'[点我](javascript:alert(1))'} />)
    const link = screen.getByText('点我')
    // sanitize 会把非法协议的 href 整个剥掉
    const href = link.getAttribute('href')
    expect(href === null || !href.includes('javascript:')).toBe(true)
  })

  it('内联事件属性被剥除', () => {
    const { container } = render(<MarkdownView content={'<img src=x onerror=alert(1)>图片'} />)
    const img = container.querySelector('img')
    expect(img).toBeNull()
  })

  it('代码块保留语言与高亮 class', () => {
    const { container } = render(<MarkdownView content={'```java\npublic class Main {\n}\n```'} />)
    expect(container.querySelector('code.language-java')).not.toBeNull()
    expect(container.querySelector('.hljs-keyword')).not.toBeNull()
  })

  it('GFM 表格正常渲染', () => {
    const { container } = render(
      <MarkdownView content={'| a | b |\n| - | - |\n| 1 | 2 |'} />,
    )
    expect(container.querySelector('table')).not.toBeNull()
  })
})
