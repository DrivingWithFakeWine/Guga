import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'
import rehypeSanitize, { defaultSchema } from 'rehype-sanitize'
import remarkGfm from 'remark-gfm'
import 'highlight.js/styles/github.css'

// 高亮插件先于 sanitize 执行，因此 sanitize schema 需放行高亮产物的 class
const sanitizeSchema = {
  ...defaultSchema,
  attributes: {
    ...defaultSchema.attributes,
    code: [...(defaultSchema.attributes?.code ?? []), ['className', /^(language-|hljs)/]],
    span: [...(defaultSchema.attributes?.span ?? []), ['className', /^hljs-/]],
    pre: [...(defaultSchema.attributes?.pre ?? []), ['className', /^hljs/]],
  },
}

export default function MarkdownView({ content }: { content: string }) {
  return (
    <article className="prose prose-neutral max-w-none">
      {/* 顺序即执行顺序：先高亮，后 sanitize；不引入 rehype-raw，原始 HTML 一律不渲染 */}
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight, [rehypeSanitize, sanitizeSchema]]}
      >
        {content}
      </ReactMarkdown>
    </article>
  )
}
