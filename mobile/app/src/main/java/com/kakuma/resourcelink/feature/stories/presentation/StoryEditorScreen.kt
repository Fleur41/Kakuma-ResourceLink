package com.kakuma.resourcelink.feature.stories.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.ConfirmDialog
import com.kakuma.resourcelink.core.designsystem.component.CoverImage
import com.kakuma.resourcelink.core.designsystem.component.FullScreenLoading
import com.kakuma.resourcelink.core.designsystem.component.KrFilterChips
import com.kakuma.resourcelink.core.designsystem.component.KrOutlinedButton
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.locale.AppLanguage
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.auth.presentation.LanguageDropdown
import com.kakuma.resourcelink.feature.stories.domain.StoryCategory
import java.io.File

@Composable
fun StoryEditorRoute(
    onClose: () -> Unit,
    onSubmitted: () -> Unit = onClose,
    viewModel: StoryEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    UiEventEffect(viewModel.events)
    // Photo Picker: no storage permission needed; the image is copied into app storage.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { viewModel.onCoverPicked(it) }
    DisposableEffect(Unit) { onDispose { viewModel.flush() } }
    BackHandler { viewModel.flush(); onClose() }

    Scaffold(topBar = {
        KrTopBar(stringResource(R.string.editor_title), onBack = { viewModel.flush(); onClose() }, actions = {
            if (state.localId.isNotBlank()) {
                IconButton(onClick = { confirmDiscard = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_discard_draft))
                }
            }
        })
    }) { padding ->
        if (state.isLoading) { FullScreenLoading(Modifier.padding(padding)); return@Scaffold }
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(R.string.editor_offline_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Cover image
            Box(Modifier.fillMaxWidth().height(170.dp).clip(MaterialTheme.shapes.large)) {
                if (state.coverPath != null) {
                    CoverImage(File(state.coverPath!!).toURI().toString(), Modifier.fillMaxSize())
                    FilledTonalIconButton(onClick = viewModel::removeCover, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_remove_cover))
                    }
                } else {
                    CoverImage(null, Modifier.fillMaxSize())
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.isImporting) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        else KrOutlinedButton(
                            stringResource(R.string.action_add_cover),
                            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            icon = Icons.Outlined.AddPhotoAlternate,
                        )
                    }
                }
            }
            KrTextField(
                state.title, viewModel::onTitleChange, stringResource(R.string.field_story_title),
                placeholder = stringResource(R.string.placeholder_story_title), error = state.errors["title"]?.asString(),
            )
            Column {
                Text(stringResource(R.string.field_category), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                KrFilterChips(
                    StoryCategory.entries.map { it to it.label() }, state.category, viewModel::onCategoryChange,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                )
            }
            LanguageDropdown(AppLanguage.fromTag(state.language)) { viewModel.onLanguageChange(it.tag) }
            KrTextField(
                state.body, viewModel::onBodyChange, stringResource(R.string.field_story_body),
                placeholder = stringResource(R.string.placeholder_story_body), error = state.errors["body"]?.asString(),
                singleLine = false, minLines = 8, imeAction = ImeAction.Default,
            )
            Text(
                pluralStringResource(R.plurals.editor_word_count, state.wordCount, state.wordCount),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Row(
                    Modifier.fillMaxWidth().toggleable(state.consent, role = Role.Checkbox, onValueChange = viewModel::onConsentChange),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(checked = state.consent, onCheckedChange = null)
                    Text(stringResource(R.string.editor_consent), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                }
                state.errors["consent"]?.let {
                    Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Text(stringResource(R.string.editor_review_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            KrPrimaryButton(
                stringResource(R.string.action_submit_review), onClick = { viewModel.submit(onSubmitted) },
                loading = state.isSubmitting, modifier = Modifier.fillMaxWidth(),
            )
            KrOutlinedButton(stringResource(R.string.action_save_draft), viewModel::saveDraft, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.lg))
        }
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_discard_title),
            message = stringResource(R.string.dialog_discard_body),
            confirmLabel = stringResource(R.string.action_discard_draft),
            onConfirm = { confirmDiscard = false; viewModel.discard(onClose) },
            onDismiss = { confirmDiscard = false },
        )
    }
}
