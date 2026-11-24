package com.kakuma.resourcelink.core.ui

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kakuma.resourcelink.core.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

/**
 * One-time UI effects. ViewModels send them through a [Channel] so each event is delivered exactly once —
 * never replayed on recomposition, rotation or when returning to a screen.
 *
 * - [Toast]: short, non-actionable confirmations ("Saved", "Signed in").
 * - [Snackbar]: contextual feedback with an optional action (Retry, Undo).
 */
sealed interface UiEvent {
    data class Toast(val message: UiText) : UiEvent
    data class Snackbar(val message: UiText, val actionLabel: UiText? = null, val actionId: String? = null) : UiEvent
}

/** Convenience holder for a ViewModel's event channel. */
class UiEventChannel {
    private val channel = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = channel.receiveAsFlow()
    fun toast(message: UiText) { channel.trySend(UiEvent.Toast(message)) }
    fun snackbar(message: UiText, actionLabel: UiText? = null, actionId: String? = null) {
        channel.trySend(UiEvent.Snackbar(message, actionLabel, actionId))
    }
}

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

/** Collects [flow] only while the screen is at least STARTED, on Main.immediate so no event is lost. */
@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, key: Any? = null, onEvent: suspend (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner.lifecycle, key, flow) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) { flow.collect { onEvent(it) } }
        }
    }
}

/** Standard handler: Toasts are short; Snackbars may carry an action reported via [onAction]. */
@Composable
fun UiEventEffect(events: Flow<UiEvent>, onAction: (String) -> Unit = {}) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarHostState.current
    ObserveAsEvents(events) { event ->
        when (event) {
            is UiEvent.Toast -> Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT).show()
            is UiEvent.Snackbar -> {
                snackbar.currentSnackbarData?.dismiss() // never stack duplicates
                val result = snackbar.showSnackbar(
                    message = event.message.asString(context),
                    actionLabel = event.actionLabel?.asString(context),
                    duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) event.actionId?.let(onAction)
            }
        }
    }
}
