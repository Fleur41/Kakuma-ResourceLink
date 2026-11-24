import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { moderationApi } from '@/api/endpoints'
import type { ReportReason, ReportTarget } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { useErrorMessage } from '@/components/ui/feedback'
import { SelectField, TextArea } from '@/components/ui/Field'
import { useToast } from '@/components/ui/toast-context'

const REASONS: ReportReason[] = ['spam', 'harassment', 'privacy', 'misinformation', 'scam', 'other']

export function ReportDialog({ open, onClose, target, targetId }: { open: boolean; onClose: () => void; target: ReportTarget; targetId: string }) {
  const { t } = useTranslation()
  const toast = useToast()
  const errorMessage = useErrorMessage()
  const [reason, setReason] = useState<ReportReason>('spam')
  const [details, setDetails] = useState('')
  const mutation = useMutation({
    mutationFn: () => moderationApi.report({ target_type: target, target_id: targetId, reason, details: details.trim() || undefined }),
    onSuccess: () => {
      toast.success(t('report.sent'))
      setDetails('')
      onClose()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('report.title')}
      footer={
        <>
          <Button variant="text" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button variant="danger" loading={mutation.isPending} onClick={() => mutation.mutate()}>
            {t('report.submit')}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <p className="text-muted">{t('report.body')}</p>
        <SelectField label={t('report.reason')} value={reason} onChange={(e) => setReason(e.target.value as ReportReason)} options={REASONS.map((r) => ({ value: r, label: t(`report.reasons.${r}`) }))} />
        <TextArea label={t('report.details')} value={details} maxLength={1000} onChange={(e) => setDetails(e.target.value)} />
      </div>
    </Dialog>
  )
}
