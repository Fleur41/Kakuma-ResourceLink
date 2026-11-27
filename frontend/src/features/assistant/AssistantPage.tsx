import { useEffect, useLayoutEffect, useRef, useState, type FormEvent, type RefObject } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { Bot, History, MessageSquarePlus, SendHorizontal, Trash2 } from 'lucide-react'
import { ApiError } from '@/api/client'
import { assistantApi } from '@/api/endpoints'
import type { AIMessage, AISource, ChatTask, ConversationDetail } from '@/api/types'
import { Button, IconButton } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/Dialog'
import { ErrorState, Spinner, useErrorMessage } from '@/components/ui/feedback'
import { PageHeader } from '@/components/ui/Page'
import { useToast } from '@/components/ui/toast-context'
import { Markdown } from '@/features/stories/Markdown'
import { currentLanguage } from '@/i18n'
import { cn, formatRelative } from '@/lib/format'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  sources?: AISource[]
  failed?: boolean
}

const SUGGESTIONS = ['scholarships', 'health', 'cv', 'mentor'] as const
const SOURCE_PATHS: Record<string, string> = { opportunity: '/opportunities', service: '/services', story: '/stories' }
/** Space left under the chat so it doesn't touch the bottom edge of the screen. */
const CHAT_BOTTOM_GAP = 24
const CHAT_MIN_HEIGHT = 420

/**
 * Sizes the chat to end at the bottom of the screen when the page is scrolled to the top, so the
 * message box is visible without scrolling. Re-measures when the window or the content above changes.
 */
function useFillViewport(ref: RefObject<HTMLElement | null>) {
  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    const fit = () => {
      const top = el.getBoundingClientRect().top + window.scrollY
      el.style.height = `${Math.max(CHAT_MIN_HEIGHT, window.innerHeight - top - CHAT_BOTTOM_GAP)}px`
    }
    fit()
    window.addEventListener('resize', fit)
    // Not every test environment has ResizeObserver; resize still covers real browsers without it.
    const observer = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(fit)
    observer?.observe(document.body)
    return () => {
      window.removeEventListener('resize', fit)
      observer?.disconnect()
    }
  }, [ref])
}

export default function AssistantPage() {
  const { t } = useTranslation()
  const { conversationId } = useParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  const [historyOpen, setHistoryOpen] = useState(false)
  const [toDelete, setToDelete] = useState<string | null>(null)
  // Bumped by "New chat" so the panel resets even when the URL is already /assistant.
  const [chatSession, setChatSession] = useState(0)

  function startNewChat() {
    setChatSession((s) => s + 1)
    setHistoryOpen(false)
    navigate('/assistant')
  }

  const conversations = useQuery({ queryKey: ['assistant', 'conversations'], queryFn: assistantApi.conversations })

  const remove = useMutation({
    mutationFn: (id: string) => assistantApi.deleteConversation(id),
    onSuccess: (_d, id) => {
      toast.success(t('assistant.deleted'))
      setToDelete(null)
      void qc.invalidateQueries({ queryKey: ['assistant', 'conversations'] })
      if (id === conversationId) navigate('/assistant', { replace: true })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const history = (
    <div className="space-y-2">
      <Button variant="tonal" className="w-full" icon={<MessageSquarePlus size={18} aria-hidden />} onClick={startNewChat}>
        {t('assistant.newChat')}
      </Button>
      {conversations.isError ? (
        <ErrorState compact error={conversations.error} onRetry={() => void conversations.refetch()} />
      ) : (
        <ul className="space-y-1">
          {conversations.data?.results.map((c) => (
            <li key={c.id} className={cn('group flex items-center rounded-md', c.id === conversationId ? 'bg-primary-container' : 'hover:bg-primary-container/60')}>
              <Link to={`/assistant/${c.id}`} className="min-w-0 flex-1 px-3 py-2" aria-current={c.id === conversationId ? 'page' : undefined}>
                <span className="block truncate text-sm font-medium">{c.title || t('assistant.untitled')}</span>
                <span className="block text-xs text-muted">{formatRelative(c.updated_at)}</span>
              </Link>
              <IconButton label={t('assistant.deleteConversation')} className="size-9" onClick={() => setToDelete(c.id)}>
                <Trash2 size={16} aria-hidden />
              </IconButton>
            </li>
          ))}
        </ul>
      )}
    </div>
  )

  return (
    <>
      <PageHeader
        title={t('assistant.title')}
        subtitle={t('assistant.subtitle')}
        actions={
          <Button variant="secondary" className="lg:hidden" icon={<History size={18} aria-hidden />} aria-expanded={historyOpen} onClick={() => setHistoryOpen((v) => !v)}>
            {t('assistant.history')}
          </Button>
        }
      />
      <div className="grid gap-6 lg:grid-cols-[260px_1fr]">
        <aside className={cn('card h-fit overflow-y-auto p-3 lg:h-0 lg:min-h-full', historyOpen ? 'block' : 'hidden lg:block')} aria-label={t('assistant.history')}>
          {history}
        </aside>

        {/* key: a different conversation starts from a clean panel */}
        <ChatPanel key={conversationId ?? `new-${chatSession}`} conversationId={conversationId} />
      </div>
      <ConfirmDialog
        open={toDelete !== null}
        title={t('assistant.deleteTitle')}
        body={t('assistant.deleteBody')}
        confirmLabel={t('common.delete')}
        destructive
        loading={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete)}
        onClose={() => setToDelete(null)}
      />
    </>
  )
}

function ChatPanel({ conversationId }: { conversationId?: string }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  const [params, setParams] = useSearchParams()
  // Only messages not yet confirmed by the server live here; saved ones come from the query cache.
  const [pending, setPending] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [unavailable, setUnavailable] = useState(false)
  const panelRef = useRef<HTMLElement>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)
  const autoTaskSent = useRef(false)
  // False once the user has left this chat (new chat or another conversation): a reply that arrives
  // afterwards is still saved to history, but must not pull the user back or show a toast here.
  const isCurrent = useRef(true)
  useEffect(() => {
    isCurrent.current = true
    return () => {
      isCurrent.current = false
    }
  }, [])

  const detailKey = ['assistant', 'conversation', conversationId]
  const detail = useQuery({
    queryKey: detailKey,
    queryFn: () => assistantApi.conversation(conversationId ?? ''),
    enabled: Boolean(conversationId),
  })
  const saved: ChatMessage[] = (detail.data?.messages ?? []).map((m) => ({ id: m.id, role: m.role, content: m.content, sources: m.sources }))
  const messages = [...saved, ...pending]

  const send = useMutation({
    mutationFn: (vars: { message: string; task: ChatTask; target_id?: string }) =>
      assistantApi.chat({ ...vars, conversation_id: conversationId ?? null, language: currentLanguage() }),
    onMutate: (vars) => {
      setPending([{ id: `local-${Date.now()}`, role: 'user', content: vars.message }])
    },
    onSuccess: (res, vars) => {
      const now = new Date().toISOString()
      const userMsg: AIMessage = { id: `u-${res.message.id}`, role: 'user', content: vars.message, sources: [], created_at: now }
      const reply: AIMessage = { ...res.message, sources: res.sources }
      // Seed the conversation cache so the answer shows instantly, then let the server copy replace it.
      qc.setQueryData<ConversationDetail>(['assistant', 'conversation', res.conversation_id], (old) => ({
        id: res.conversation_id,
        title: old?.title ?? vars.message.slice(0, 60),
        created_at: old?.created_at ?? now,
        updated_at: now,
        messages: [...(old?.messages ?? []), userMsg, reply],
      }))
      setPending([])
      void qc.invalidateQueries({ queryKey: ['assistant', 'conversations'] })
      if (!conversationId && isCurrent.current) navigate(`/assistant/${res.conversation_id}`, { replace: true })
    },
    onError: (err) => {
      if (!isCurrent.current) return
      if (err instanceof ApiError && err.code === 'ai_unavailable') setUnavailable(true)
      setPending((p) => p.map((m) => ({ ...m, failed: true })))
      toast.error(errorMessage(err))
    },
  })

  useFillViewport(panelRef)

  // Scroll the message list only, never the page, so the input stays where it is.
  useEffect(() => {
    const list = scrollRef.current
    list?.scrollTo?.({ top: list.scrollHeight, behavior: 'smooth' })
  }, [messages.length, send.isPending])

  // Deep links from opportunity/story pages: "Explain with AI" / "Summarise with AI".
  useEffect(() => {
    const task = params.get('task') as ChatTask | null
    const target = params.get('target')
    if (autoTaskSent.current || !task || !target || conversationId) return
    if (task !== 'explain_opportunity' && task !== 'summarize_story') return
    autoTaskSent.current = true
    setParams({}, { replace: true })
    send.mutate({ message: t(`assistant.tasks.${task}`), task, target_id: target })
  }, [params, setParams, conversationId, send, t])

  function submit(text: string) {
    const message = text.trim()
    if (!message || send.isPending) return
    if (message.length > 4000) {
      toast.error(t('validation.maxLength', { count: 4000 }))
      return
    }
    setInput('')
    send.mutate({ message, task: 'chat' })
    inputRef.current?.focus()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    submit(input)
  }

  return (
    <section ref={panelRef} className="card flex flex-col overflow-hidden" aria-label={t('assistant.chat')}>
          <div ref={scrollRef} className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto overscroll-contain p-4 sm:p-6" aria-live="polite" aria-busy={send.isPending}>
            {conversationId && detail.isPending && messages.length === 0 ? (
              <div className="flex justify-center py-10 text-primary">
                <Spinner size={28} />
              </div>
            ) : conversationId && detail.isError ? (
              <ErrorState error={detail.error} onRetry={() => void detail.refetch()} />
            ) : messages.length === 0 ? (
              <div className="m-auto flex flex-col items-center gap-4 py-6 text-center">
                <div className="flex size-14 items-center justify-center rounded-full bg-primary-container text-primary">
                  <Bot aria-hidden />
                </div>
                <h2 className="text-xl font-semibold">{t('assistant.emptyTitle')}</h2>
                <p className="max-w-md text-muted">{t('assistant.emptyBody')}</p>
                <div className="flex max-w-xl flex-wrap justify-center gap-2">
                  {SUGGESTIONS.map((s) => (
                    <button
                      key={s}
                      type="button"
                      disabled={send.isPending || unavailable}
                      onClick={() => submit(t(`assistant.suggestions.${s}`))}
                      className="min-h-10 rounded-full border border-border bg-surface px-4 text-sm font-medium hover:border-primary hover:text-primary disabled:opacity-50"
                    >
                      {t(`assistant.suggestions.${s}`)}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              messages.map((m) => (
                <div key={m.id} className={cn('flex', m.role === 'user' ? 'justify-end' : 'justify-start')}>
                  <div
                    className={cn(
                      'max-w-[85%] rounded-2xl px-4 py-3',
                      m.role === 'user' ? 'rounded-br-sm bg-primary text-on-primary' : 'rounded-bl-sm border border-border bg-bg',
                      m.failed && 'opacity-60',
                    )}
                  >
                    <span className="sr-only">{m.role === 'user' ? t('assistant.you') : t('assistant.assistant')}: </span>
                    {m.role === 'assistant' ? <Markdown>{m.content}</Markdown> : <p className="whitespace-pre-line">{m.content}</p>}
                    {m.failed && <p className="mt-1 text-xs">{t('assistant.notSent')}</p>}
                    {m.sources && m.sources.length > 0 && (
                      <div className="mt-3 border-t border-border pt-2">
                        <p className="mb-1 text-xs font-bold text-muted uppercase">{t('assistant.sources')}</p>
                        <ul className="space-y-1">
                          {m.sources.map((s) => (
                            <li key={`${s.type}-${s.id}`}>
                              {SOURCE_PATHS[s.type] ? (
                                <Link to={`${SOURCE_PATHS[s.type]}/${s.id}`} className="text-sm font-medium text-primary hover:underline">
                                  {s.title}
                                </Link>
                              ) : (
                                <span className="text-sm">{s.title}</span>
                              )}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                </div>
              ))
            )}
            {send.isPending && (
              <div className="flex items-center gap-2 text-muted" role="status">
                <Spinner size={16} /> {t('assistant.thinking')}
              </div>
            )}
          </div>

          {unavailable && <p className="mx-4 mb-2 shrink-0 rounded-md bg-gold-container p-3 text-sm text-gold-text">{t('assistant.unavailable')}</p>}
          <form onSubmit={onSubmit} className="flex shrink-0 items-end gap-2 border-t border-border p-3 sm:p-4">
            <label htmlFor="chat-input" className="sr-only">
              {t('assistant.placeholder')}
            </label>
            <textarea
              id="chat-input"
              ref={inputRef}
              rows={1}
              value={input}
              maxLength={4000}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
                  e.preventDefault()
                  submit(input)
                }
              }}
              placeholder={t('assistant.placeholder')}
              className="max-h-40 min-h-12 flex-1 resize-none rounded-xl border border-border bg-surface px-4 py-3 focus:border-primary focus:outline-none"
            />
            <button
              type="submit"
              disabled={!input.trim() || send.isPending}
              aria-label={t('assistant.send')}
              className="inline-flex size-12 shrink-0 items-center justify-center rounded-full bg-primary text-on-primary hover:bg-primary-strong disabled:opacity-40"
            >
              <SendHorizontal size={20} aria-hidden />
            </button>
          </form>
          <p className="shrink-0 px-4 pb-3 text-xs text-muted">{t('assistant.disclaimer')}</p>
    </section>
  )
}
