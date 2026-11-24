import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

/**
 * Story bodies are CommonMark written by users. Raw HTML is never rendered (skipHtml), and links/images
 * are limited to http(s)/mailto so a story cannot inject script or javascript: URLs.
 */
export function Markdown({ children }: { children: string }) {
  return (
    <div className="prose-story">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        skipHtml
        urlTransform={(url) => (/^(https?:|mailto:)/i.test(url) || url.startsWith('/') || url.startsWith('#') ? url : '')}
        components={{
          a: ({ href, children: c }) => (
            <a href={href} target={href?.startsWith('http') ? '_blank' : undefined} rel="noopener noreferrer nofollow">
              {c}
            </a>
          ),
          img: ({ src, alt }) => (typeof src === 'string' && src ? <img src={src} alt={alt ?? ''} loading="lazy" className="my-4 rounded-lg" /> : null),
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  )
}
