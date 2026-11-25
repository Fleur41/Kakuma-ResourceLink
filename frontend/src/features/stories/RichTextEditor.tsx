import { useEffect, useId, useRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { EditorContent, useEditor, useEditorState, type Editor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { Markdown } from '@tiptap/markdown'
import { Placeholder } from '@tiptap/extensions'
import { Bold, Italic, List, ListOrdered, Redo2, RemoveFormatting, Undo2 } from 'lucide-react'
import { cn } from '@/lib/format'

interface Props {
  label: string
  /** Markdown. The editor shows it formatted and reports edits back as Markdown. */
  value: string
  onChange: (markdown: string) => void
  placeholder?: string
  error?: string
  required?: boolean
}

type BlockStyle = 'paragraph' | 'h1' | 'h2' | 'h3'

const MOD_KEY = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl+'

/**
 * Word-style editor for story bodies. Stories stay stored as Markdown, and the toolbar matches the
 * Android editor: headings, bold, italic and lists. Quotes in older stories are kept, just not offered.
 */
export function RichTextEditor({ label, value, onChange, placeholder, error, required }: Props) {
  const id = useId()
  const lastEmitted = useRef(value)

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [1, 2, 3] },
        code: false,
        codeBlock: false,
        horizontalRule: false,
        link: false,
        strike: false,
        underline: false,
      }),
      Markdown,
      Placeholder.configure({ placeholder: placeholder ?? '' }),
    ],
    content: value,
    contentType: 'markdown',
    immediatelyRender: true,
    onUpdate: ({ editor: e }) => {
      const md = e.isEmpty ? '' : e.getMarkdown()
      lastEmitted.current = md
      onChange(md)
    },
  })

  // Outside changes (e.g. discarding a restored draft) replace the document.
  useEffect(() => {
    if (!editor || value === lastEmitted.current) return
    lastEmitted.current = value
    editor.commands.setContent(value, { contentType: 'markdown', emitUpdate: false })
  }, [editor, value])

  useEffect(() => {
    editor?.setOptions({
      editorProps: {
        attributes: {
          id,
          role: 'textbox',
          'aria-multiline': 'true',
          'aria-labelledby': `${id}-label`,
          ...(error ? { 'aria-invalid': 'true', 'aria-describedby': `${id}-error` } : {}),
          ...(required ? { 'aria-required': 'true' } : {}),
          class: 'prose-story min-h-80 px-5 py-4 focus:outline-none sm:px-8 sm:py-6',
        },
      },
    })
  }, [editor, id, error, required])

  return (
    <div className="space-y-1.5">
      <p id={`${id}-label`} className="block text-sm font-medium text-text" onClick={() => editor?.commands.focus()}>
        {label}
        {required && <span aria-hidden className="text-error"> *</span>}
      </p>
      <div className={cn('border bg-surface focus-within:border-primary focus-within:ring-1 focus-within:ring-primary', error ? 'border-error' : 'border-border')}>
        {editor && <Toolbar editor={editor} />}
        <EditorContent editor={editor} className="rich-editor" />
      </div>
      {error && (
        <p id={`${id}-error`} className="text-sm text-error">
          {error}
        </p>
      )}
    </div>
  )
}

function Toolbar({ editor }: { editor: Editor }) {
  const { t } = useTranslation()
  const s = useEditorState({
    editor,
    selector: ({ editor: e }) => ({
      block: (e.isActive('heading', { level: 1 }) ? 'h1' : e.isActive('heading', { level: 2 }) ? 'h2' : e.isActive('heading', { level: 3 }) ? 'h3' : 'paragraph') as BlockStyle,
      bold: e.isActive('bold'),
      italic: e.isActive('italic'),
      bullet: e.isActive('bulletList'),
      ordered: e.isActive('orderedList'),
      canUndo: e.can().undo(),
      canRedo: e.can().redo(),
    }),
  })

  function setBlock(style: BlockStyle) {
    const chain = editor.chain().focus()
    if (style === 'paragraph') chain.setParagraph().run()
    else chain.setHeading({ level: Number(style[1]) as 1 | 2 | 3 }).run()
  }

  return (
    <div role="toolbar" aria-label={t('stories.editor.format.toolbar')} className="flex flex-wrap items-center gap-1 border-b border-border bg-bg px-2 py-1.5">
      <ToolButton label={t('stories.editor.format.undo')} disabled={!s.canUndo} onClick={() => editor.chain().focus().undo().run()}>
        <Undo2 size={18} aria-hidden />
      </ToolButton>
      <ToolButton label={t('stories.editor.format.redo')} disabled={!s.canRedo} onClick={() => editor.chain().focus().redo().run()}>
        <Redo2 size={18} aria-hidden />
      </ToolButton>
      <Divider />
      <select
        aria-label={t('stories.editor.format.textStyle')}
        value={s.block}
        onChange={(e) => setBlock(e.target.value as BlockStyle)}
        className="h-9 min-w-36 border border-border bg-surface px-2 text-sm text-text hover:border-muted focus:border-primary focus:outline-none"
      >
        <option value="paragraph">{t('stories.editor.format.normal')}</option>
        <option value="h1">{t('stories.editor.format.heading1')}</option>
        <option value="h2">{t('stories.editor.format.heading2')}</option>
        <option value="h3">{t('stories.editor.format.heading3')}</option>
      </select>
      <Divider />
      <ToolButton label={`${t('stories.editor.format.bold')} (${MOD_KEY}B)`} active={s.bold} onClick={() => editor.chain().focus().toggleBold().run()}>
        <Bold size={18} aria-hidden />
      </ToolButton>
      <ToolButton label={`${t('stories.editor.format.italic')} (${MOD_KEY}I)`} active={s.italic} onClick={() => editor.chain().focus().toggleItalic().run()}>
        <Italic size={18} aria-hidden />
      </ToolButton>
      <Divider />
      <ToolButton label={t('stories.editor.format.bulletList')} active={s.bullet} onClick={() => editor.chain().focus().toggleBulletList().run()}>
        <List size={18} aria-hidden />
      </ToolButton>
      <ToolButton label={t('stories.editor.format.numberedList')} active={s.ordered} onClick={() => editor.chain().focus().toggleOrderedList().run()}>
        <ListOrdered size={18} aria-hidden />
      </ToolButton>
      <Divider />
      <ToolButton label={t('stories.editor.format.clear')} onClick={() => editor.chain().focus().unsetAllMarks().clearNodes().run()}>
        <RemoveFormatting size={18} aria-hidden />
      </ToolButton>
    </div>
  )
}

function ToolButton({ label, active, disabled, onClick, children }: { label: string; active?: boolean; disabled?: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      title={label}
      aria-label={label}
      aria-pressed={active === undefined ? undefined : active}
      disabled={disabled}
      // Keep the text selection while clicking a button.
      onMouseDown={(e) => e.preventDefault()}
      onClick={onClick}
      className={cn(
        'inline-flex size-9 items-center justify-center rounded-md transition-colors disabled:opacity-40',
        active ? 'bg-primary-container text-primary' : 'text-text hover:bg-hover',
      )}
    >
      {children}
    </button>
  )
}

function Divider() {
  return <span aria-hidden className="mx-1 h-6 w-px bg-border" />
}
