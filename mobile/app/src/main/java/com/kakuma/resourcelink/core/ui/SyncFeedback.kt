package com.kakuma.resourcelink.core.ui

import androidx.annotation.StringRes
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.feature.opportunities.domain.SyncOutcome

/** Standard feedback for optimistic toggles: short Toast on success/queued, Snackbar on failure. */
fun UiEventChannel.report(outcome: SyncOutcome, @StringRes successMessage: Int?) {
    when (outcome) {
        SyncOutcome.Synced -> successMessage?.let { toast(UiText.Res(it)) }
        SyncOutcome.QueuedOffline -> toast(UiText.Res(R.string.toast_saved_offline_sync_later))
        is SyncOutcome.Failed -> snackbar(outcome.error.toUiText())
    }
}
