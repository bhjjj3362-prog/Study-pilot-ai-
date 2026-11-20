package com.example.ui.solve

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GeminiModelOption
import com.example.data.ImageUtils
import com.example.data.MAX_HISTORY_ITEMS
import com.example.data.SolveAnswer
import com.example.data.SolveAttemptProgress
import com.example.data.SolveHistoryItem
import com.example.data.SolveResult
import com.example.ui.components.CompactModelSelectorRow
import com.example.ui.components.FallbackStatusBanner
import com.example.ui.theme.AnalogyTeal
import com.example.ui.theme.AnalogyTealBg
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.ErrorCoralBg
import com.example.ui.theme.ErrorCoralBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LogicIndigo
import com.example.ui.theme.LogicIndigoBg
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateSurfaceHighest
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.TrapAmber
import com.example.ui.theme.TrapAmberBg
import com.example.ui.theme.TrapAmberBorder

private val sampleQuestions = listOf(
    "Why does entropy always increase in a closed system?",
    "Difference between osmosis and active transport",
    "How does Bayes' theorem update probabilities?",
    "Why is merge sort O(n log n) in the worst case?",
)

@Composable
fun SolveScreen(
    questionText: String,
    attachedPhoto: Bitmap?,
    selectedModel: GeminiModelOption,
    history: List<SolveHistoryItem>,
    uiState: SolveUiState,
    onQuestionChanged: (String) -> Unit,
    onClearQuestion: () -> Unit,
    onSelectModel: (GeminiModelOption) -> Unit,
    onAttachPhotoUri: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onCameraUnavailable: () -> Unit,
    onSelectExample: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSolveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isLoading = uiState is SolveUiState.Loading

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAttachPhotoUri(uri)
            }
        },
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { saved ->
            val capturedUri = pendingCameraUri
            pendingCameraUri = null
            if (saved && capturedUri != null) {
                onAttachPhotoUri(capturedUri)
            }
        },
    )

    val launchPhotoPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val launchCamera = {
        try {
            val photoUri = ImageUtils.createCameraPhotoUri(context)
            pendingCameraUri = photoUri
            takePictureLauncher.launch(photoUri)
        } catch (_: ActivityNotFoundException) {
            pendingCameraUri = null
            onCameraUnavailable()
        } catch (_: Exception) {
            pendingCameraUri = null
            onCameraUnavailable()
        }
    }

    val triggerSolve = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSolveClick()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "question_composer") {
                QuestionInputCard(
                    questionText = questionText,
                    attachedPhoto = attachedPhoto,
                    selectedModel = selectedModel,
                    isLoading = isLoading,
                    progress = (uiState as? SolveUiState.Loading)?.progress,
                    onQuestionChanged = onQuestionChanged,
                    onClearQuestion = onClearQuestion,
                    onSelectModel = onSelectModel,
                    onUploadPhotoClick = launchPhotoPicker,
                    onTakePhotoClick = launchCamera,
                    onRemovePhoto = onRemovePhoto,
                    onSolveClick = triggerSolve,
                )
            }

            if (uiState is SolveUiState.Idle && attachedPhoto == null) {
                item(key = "starter_chips") {
                    StarterPromptsRow(
                        onSelectExample = { prompt ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onSelectExample(prompt)
                        }
                    )
                }
            }

            if (uiState is SolveUiState.Error) {
                item(key = "error_card") {
                    SolveErrorCard(
                        message = uiState.message,
                        onRetry = triggerSolve,
                    )
                }
            }

            if (uiState is SolveUiState.Success) {
                item(key = "answer_cards") {
                    SolveAnswerSection(result = uiState.result)
                }
            }

            item(key = "history_header") {
                HistorySectionHeader(
                    itemCount = history.size,
                    onClearHistory = onClearHistory,
                )
            }

            if (history.isEmpty()) {
                item(key = "history_empty") {
                    HistoryEmptyCard()
                }
            } else {
                itemsIndexed(
                    items = history,
                    key = { _, item -> item.id },
                ) { index, item ->
                    HistoryItemCard(
                        index = index,
                        item = item,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionInputCard(
    questionText: String,
    attachedPhoto: Bitmap?,
    selectedModel: GeminiModelOption,
    isLoading: Boolean,
    progress: SolveAttemptProgress?,
    onQuestionChanged: (String) -> Unit,
    onClearQuestion: () -> Unit,
    onSelectModel: (GeminiModelOption) -> Unit,
    onUploadPhotoClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSolveClick: () -> Unit,
) {
    val canSolve = !isLoading && (questionText.isNotBlank() || attachedPhoto != null)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = SlateSurfaceElevated,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompactModelSelectorRow(
                selectedModel = selectedModel,
                enabled = !isLoading,
                onSelectModel = onSelectModel,
            )

            OutlinedTextField(
                value = questionText,
                onValueChange = onQuestionChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_input"),
                enabled = !isLoading,
                minLines = 3,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = IceWhite),
                placeholder = {
                    Text(
                        text = stringResource(R.string.question_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SlateTextMuted,
                    )
                },
                trailingIcon = {
                    if (questionText.isNotEmpty() && !isLoading) {
                        IconButton(
                            onClick = onClearQuestion,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_question_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_question_desc),
                                tint = SlateTextSecondary,
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSolve) {
                            onSolveClick()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MidnightSurface,
                    unfocusedContainerColor = MidnightSurface,
                    disabledContainerColor = MidnightSurface.copy(alpha = 0.65f),
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = SlateBorder,
                    cursorColor = ElectricCyan,
                ),
            )

            if (attachedPhoto != null) {
                AttachedPhotoPreview(
                    bitmap = attachedPhoto,
                    enabled = !isLoading,
                    onRemove = onRemovePhoto,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onUploadPhotoClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("upload_photo_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.upload_photo),
                        style = MaterialTheme.typography.labelMedium,
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                OutlinedButton(
                    onClick = onTakePhotoClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("take_photo_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = AnalogyTeal,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.take_photo),
                        style = MaterialTheme.typography.labelMedium,
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Button(
                onClick = onSolveClick,
                enabled = canSolve,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("solve_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF041E2B),
                    disabledContainerColor = SlateSurfaceHighest,
                    disabledContentColor = SlateTextMuted,
                ),
            ) {
                if (isLoading && progress != null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.4.dp,
                        color = ElectricCyan,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val statusLabel = when {
                        progress.isFallback -> stringResource(
                            R.string.fallback_model_status,
                            progress.modelId,
                            progress.attempt,
                            progress.maxAttempts,
                        )
                        progress.attempt > 1 -> stringResource(
                            R.string.retrying_model_status,
                            progress.modelId,
                            progress.attempt,
                            progress.maxAttempts,
                        )
                        else -> stringResource(
                            R.string.solving_model_status,
                            progress.modelId,
                        )
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.solve_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachedPhotoPreview(
    bitmap: Bitmap,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attached_photo_preview"),
        shape = RoundedCornerShape(16.dp),
        color = MidnightSurface,
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.attached_photo_desc),
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.attached_photo_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = IceWhite,
                )
                Text(
                    text = "${bitmap.width} × ${bitmap.height}px",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary,
                )
            }

            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .background(SlateSurfaceHighest, CircleShape)
                    .testTag("remove_photo_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove_photo_desc),
                    tint = IceWhite,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun StarterPromptsRow(
    onSelectExample: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sampleQuestions.forEachIndexed { index, prompt ->
            SuggestionChip(
                onClick = { onSelectExample(prompt) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("sample_chip_$index"),
                label = {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextSecondary,
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MidnightSurface,
                    labelColor = SlateTextSecondary,
                ),
                border = BorderStroke(1.dp, SlateBorder),
            )
        }
    }
}

@Composable
private fun SolveAnswerSection(
    result: SolveResult,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 12 },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (result.usedFallback) {
                FallbackStatusBanner(
                    requestedModelId = result.requestedModelId,
                    answeredModelId = result.answeredModelId,
                )
            }

            SolveAnswerCards(answer = result.answer)
        }
    }
}

@Composable
private fun SolveAnswerCards(
    answer: SolveAnswer,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Card 1: 2-Sentence Analogy
        AnswerSectionCard(
            badgeNumber = "01",
            title = stringResource(R.string.card_analogy_title),
            icon = Icons.Outlined.Lightbulb,
            accentColor = AnalogyTeal,
            badgeBackground = AnalogyTealBg,
            cardBackground = SlateSurfaceElevated,
            cardBorder = SlateBorder,
            testTag = "card_analogy",
        ) {
            Text(
                text = answer.analogy,
                style = MaterialTheme.typography.bodyLarge,
                color = IceWhite,
            )
        }

        // Card 2: Step-by-Step Logic
        AnswerSectionCard(
            badgeNumber = "02",
            title = stringResource(R.string.card_logic_title),
            icon = Icons.Outlined.FormatListNumbered,
            accentColor = ElectricCyan,
            badgeBackground = CyanContainer,
            cardBackground = SlateSurfaceElevated,
            cardBorder = SlateBorder,
            testTag = "card_logic",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                answer.stepByStepLogic.forEachIndexed { index, stepText ->
                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = SlateBorder.copy(alpha = 0.55f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LogicIndigoBg,
                            border = BorderStroke(1.dp, LogicIndigo.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = (index + 1).toString().padStart(2, '0'),
                                style = MaterialTheme.typography.labelSmall,
                                color = LogicIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = IceWhite,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Card 3: Common Exam Trap
        AnswerSectionCard(
            badgeNumber = "03",
            title = stringResource(R.string.card_trap_title),
            icon = Icons.Outlined.WarningAmber,
            accentColor = TrapAmber,
            badgeBackground = Color(0xFF3D280A),
            cardBackground = TrapAmberBg,
            cardBorder = TrapAmberBorder,
            testTag = "card_trap",
        ) {
            Text(
                text = answer.commonExamTrap,
                style = MaterialTheme.typography.bodyLarge,
                color = IceWhite,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AnswerSectionCard(
    badgeNumber: String,
    title: String,
    icon: ImageVector,
    accentColor: Color,
    badgeBackground: Color,
    cardBackground: Color,
    cardBorder: Color,
    testTag: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        color = cardBackground,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBackground,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = badgeNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                )
            }

            content()
        }
    }
}

@Composable
private fun SolveErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solve_error_card"),
        shape = RoundedCornerShape(20.dp),
        color = ErrorCoralBg,
        border = BorderStroke(1.dp, ErrorCoralBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = ErrorCoral,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IceWhite,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("retry_button"),
                ) {
                    Text(
                        text = stringResource(R.string.try_again),
                        style = MaterialTheme.typography.labelLarge,
                        color = ErrorCoral,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySectionHeader(
    itemCount: Int,
    onClearHistory: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .testTag("history_section_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.history_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = IceWhite,
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SlateSurfaceElevated,
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Text(
                    text = "$itemCount/$MAX_HISTORY_ITEMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        if (itemCount > 0) {
            TextButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("clear_history_button"),
            ) {
                Text(
                    text = stringResource(R.string.history_clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = SlateTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun HistoryEmptyCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_empty_card"),
        shape = RoundedCornerShape(18.dp),
        color = MidnightSurface,
        border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.7f)),
    ) {
        Text(
            text = stringResource(R.string.history_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = SlateTextMuted,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun HistoryItemCard(
    index: Int,
    item: SolveHistoryItem,
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }

    val displayQuestion = item.question.ifBlank {
        stringResource(R.string.history_photo_only_question)
    }
    val relativeTime = remember(item.timestampMillis) {
        if (item.timestampMillis > 0L) {
            DateUtils.getRelativeTimeSpanString(
                item.timestampMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()
        } else {
            ""
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("history_item_$index"),
        shape = RoundedCornerShape(18.dp),
        color = MidnightSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (expanded) ElectricCyan.copy(alpha = 0.45f) else SlateBorder,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = displayQuestion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = IceWhite,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (item.hasPhoto) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AnalogyTealBg,
                                border = BorderStroke(1.dp, AnalogyTeal.copy(alpha = 0.35f)),
                            ) {
                                Text(
                                    text = stringResource(R.string.history_photo_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AnalogyTeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        Text(
                            text = item.answeredModelId,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (relativeTime.isNotEmpty()) {
                            Text(
                                text = "· $relativeTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextMuted,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.history_collapse_desc else R.string.history_expand_desc
                    ),
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = SlateBorder.copy(alpha = 0.6f),
                    )
                    if (item.usedFallback) {
                        FallbackStatusBanner(
                            requestedModelId = item.requestedModelId,
                            answeredModelId = item.answeredModelId,
                        )
                    }
                    SolveAnswerCards(answer = item.answer)
                }
            }
        }
    }
}
