package com.example.ui.recall

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FlashcardItem
import com.example.data.GeminiModelOption
import com.example.data.QuizQuestionItem
import com.example.data.RecallStudySet
import com.example.data.SolveAttemptProgress
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
import kotlinx.coroutines.launch

@Composable
fun RecallScreen(
    notesText: String,
    selectedModel: GeminiModelOption,
    generationState: RecallGenerationState,
    studySet: RecallStudySet,
    flippedCardIds: Set<String>,
    selectedAnswers: Map<String, Int>,
    onNotesChanged: (String) -> Unit,
    onClearNotes: () -> Unit,
    onSelectModel: (GeminiModelOption) -> Unit,
    onGenerateClick: () -> Unit,
    onToggleFlashcard: (String) -> Unit,
    onSelectQuizOption: (String, Int) -> Unit,
    onResetQuiz: () -> Unit,
    onReturnToSolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onReturnToSolve()
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isLoading = generationState is RecallGenerationState.Loading

    val triggerGenerate = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onGenerateClick()
    }

    val correctCount = studySet.quizQuestions.count { question ->
        selectedAnswers[question.id] == question.correctIndex
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
            item(key = "notes_generator_card") {
                NotesGeneratorCard(
                    notesText = notesText,
                    selectedModel = selectedModel,
                    isLoading = isLoading,
                    progress = (generationState as? RecallGenerationState.Loading)?.progress,
                    onNotesChanged = onNotesChanged,
                    onClearNotes = onClearNotes,
                    onSelectModel = onSelectModel,
                    onGenerateClick = triggerGenerate,
                )
            }

            if (generationState is RecallGenerationState.Error) {
                item(key = "recall_error_card") {
                    RecallErrorCard(
                        message = generationState.message,
                        onRetry = triggerGenerate,
                    )
                }
            }

            if (studySet.usedFallback) {
                item(key = "recall_fallback_note") {
                    FallbackStatusBanner(
                        requestedModelId = studySet.requestedModelId,
                        answeredModelId = studySet.answeredModelId,
                        testTag = "recall_fallback_note",
                    )
                }
            }

            item(key = "study_set_header") {
                StudySetBanner(studySet = studySet)
            }

            item(key = "flashcards_deck") {
                FlashcardDeckSection(
                    flashcards = studySet.flashcards,
                    flippedCardIds = flippedCardIds,
                    onToggleCard = onToggleFlashcard,
                )
            }

            item(key = "quiz_header") {
                QuizSectionHeader(
                    answeredCount = selectedAnswers.size,
                    correctCount = correctCount,
                    totalCount = studySet.quizQuestions.size,
                    onResetQuiz = onResetQuiz,
                )
            }

            itemsIndexed(
                items = studySet.quizQuestions,
                key = { _, item -> item.id },
            ) { index, question ->
                QuizQuestionCard(
                    index = index,
                    question = question,
                    selectedOptionIndex = selectedAnswers[question.id],
                    onSelectOption = { optIndex ->
                        onSelectQuizOption(question.id, optIndex)
                    },
                )
            }

            item(key = "recall_bottom_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NotesGeneratorCard(
    notesText: String,
    selectedModel: GeminiModelOption,
    isLoading: Boolean,
    progress: SolveAttemptProgress?,
    onNotesChanged: (String) -> Unit,
    onClearNotes: () -> Unit,
    onSelectModel: (GeminiModelOption) -> Unit,
    onGenerateClick: () -> Unit,
) {
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
                triggerTestTag = "recall_model_selector_trigger",
            )

            OutlinedTextField(
                value = notesText,
                onValueChange = onNotesChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recall_notes_input"),
                enabled = !isLoading,
                minLines = 3,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = IceWhite),
                placeholder = {
                    Text(
                        text = stringResource(R.string.recall_notes_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SlateTextMuted,
                    )
                },
                trailingIcon = {
                    if (notesText.isNotEmpty() && !isLoading) {
                        IconButton(
                            onClick = onClearNotes,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("recall_clear_notes_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.recall_clear_notes_desc),
                                tint = SlateTextSecondary,
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
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

            Button(
                onClick = onGenerateClick,
                enabled = !isLoading && notesText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("generate_study_set_button"),
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
                            R.string.recall_generating_status,
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
                        text = stringResource(R.string.recall_generate_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudySetBanner(
    studySet: RecallStudySet,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = studySet.topicTitle,
            style = MaterialTheme.typography.titleLarge,
            color = IceWhite,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (studySet.isDemo) LogicIndigoBg else AnalogyTealBg,
            border = BorderStroke(
                1.dp,
                if (studySet.isDemo) LogicIndigo.copy(alpha = 0.4f) else AnalogyTeal.copy(alpha = 0.4f),
            ),
        ) {
            Text(
                text = if (studySet.isDemo) {
                    stringResource(R.string.recall_demo_badge)
                } else {
                    stringResource(R.string.recall_ai_badge, studySet.answeredModelId)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (studySet.isDemo) LogicIndigo else AnalogyTeal,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun FlashcardDeckSection(
    flashcards: List<FlashcardItem>,
    flippedCardIds: Set<String>,
    onToggleCard: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { flashcards.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(flashcards) {
        if (pagerState.currentPage >= flashcards.size && flashcards.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Style,
                    contentDescription = null,
                    tint = AnalogyTeal,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.recall_flashcards_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                )
            }

            if (flashcards.isNotEmpty()) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${flashcards.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary,
                )
            }
        }

        if (flashcards.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcards_pager"),
            ) { page ->
                val card = flashcards[page]
                val isFlipped = card.id in flippedCardIds
                FlipFlashcard(
                    index = page,
                    card = card,
                    isFlipped = isFlipped,
                    onClick = { onToggleCard(card.id) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                        coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("prev_flashcard_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.recall_prev_card_desc),
                        tint = if (pagerState.currentPage > 0) ElectricCyan else SlateTextMuted,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(flashcards.size) { index ->
                        val active = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(width = if (active) 18.dp else 7.dp, height = 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) ElectricCyan else SlateBorder
                                )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val next = (pagerState.currentPage + 1).coerceAtMost(flashcards.lastIndex)
                        coroutineScope.launch { pagerState.animateScrollToPage(next) }
                    },
                    enabled = pagerState.currentPage < flashcards.lastIndex,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("next_flashcard_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.recall_next_card_desc),
                        tint = if (pagerState.currentPage < flashcards.lastIndex) ElectricCyan else SlateTextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipFlashcard(
    index: Int,
    card: FlashcardItem,
    isFlipped: Boolean,
    onClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "flashcard_flip",
    )
    val showBack = rotation > 90f

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 210.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .testTag("flashcard_$index"),
        shape = RoundedCornerShape(22.dp),
        color = if (showBack) CyanContainer.copy(alpha = 0.72f) else SlateSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (showBack) ElectricCyan.copy(alpha = 0.6f) else AnalogyTeal.copy(alpha = 0.45f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .graphicsLayer {
                    if (showBack) {
                        rotationY = 180f
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 166.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (showBack) MidnightSurface else AnalogyTealBg,
                        border = BorderStroke(
                            1.dp,
                            if (showBack) ElectricCyan.copy(alpha = 0.4f) else AnalogyTeal.copy(alpha = 0.4f),
                        ),
                    ) {
                        Text(
                            text = stringResource(
                                if (showBack) R.string.recall_card_back_tag else R.string.recall_card_front_tag
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showBack) ElectricCyan else AnalogyTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.Flip,
                        contentDescription = null,
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Text(
                    text = if (showBack) card.answer else card.question,
                    style = if (showBack) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = IceWhite,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(vertical = 14.dp),
                )

                Text(
                    text = stringResource(
                        if (showBack) R.string.recall_tap_to_flip_back else R.string.recall_tap_to_flip
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun QuizSectionHeader(
    answeredCount: Int,
    correctCount: Int,
    totalCount: Int,
    onResetQuiz: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Quiz,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.recall_quiz_title),
                style = MaterialTheme.typography.titleMedium,
                color = IceWhite,
            )
            if (totalCount > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateSurfaceElevated,
                    border = BorderStroke(1.dp, SlateBorder),
                ) {
                    Text(
                        text = "$correctCount/$totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (answeredCount == totalCount && correctCount == totalCount) {
                            AnalogyTeal
                        } else {
                            ElectricCyan
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        if (answeredCount > 0) {
            TextButton(
                onClick = onResetQuiz,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("reset_quiz_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.recall_reset_quiz),
                    style = MaterialTheme.typography.labelMedium,
                    color = SlateTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun QuizQuestionCard(
    index: Int,
    question: QuizQuestionItem,
    selectedOptionIndex: Int?,
    onSelectOption: (Int) -> Unit,
) {
    val isAnswered = selectedOptionIndex != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_card_$index"),
        shape = RoundedCornerShape(20.dp),
        color = SlateSurfaceElevated,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LogicIndigoBg,
                    border = BorderStroke(1.dp, LogicIndigo.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = "Q${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LogicIndigo,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                    modifier = Modifier.weight(1f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEachIndexed { optionIndex, optionText ->
                    val isSelected = selectedOptionIndex == optionIndex
                    val isCorrectOption = optionIndex == question.correctIndex

                    val containerColor = when {
                        !isAnswered -> MidnightSurface
                        isCorrectOption -> AnalogyTealBg
                        isSelected -> ErrorCoralBg
                        else -> MidnightSurface.copy(alpha = 0.65f)
                    }
                    val borderColor = when {
                        !isAnswered -> SlateBorder
                        isCorrectOption -> AnalogyTeal
                        isSelected -> ErrorCoral
                        else -> SlateBorder.copy(alpha = 0.5f)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isAnswered) { onSelectOption(optionIndex) }
                            .minimumInteractiveComponentSize()
                            .testTag("quiz_${index}_option_$optionIndex"),
                        shape = RoundedCornerShape(14.dp),
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = ('A' + optionIndex).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    !isAnswered -> ElectricCyan
                                    isCorrectOption -> AnalogyTeal
                                    isSelected -> ErrorCoral
                                    else -> SlateTextMuted
                                },
                            )
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (!isAnswered || isCorrectOption || isSelected) {
                                    IceWhite
                                } else {
                                    SlateTextSecondary
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (isAnswered && isCorrectOption) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.recall_correct_label),
                                    tint = AnalogyTeal,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else if (isAnswered && isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Cancel,
                                    contentDescription = stringResource(R.string.recall_incorrect_label),
                                    tint = ErrorCoral,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isAnswered) {
                val isCorrectChoice = selectedOptionIndex == question.correctIndex
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_${index}_explanation"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCorrectChoice) AnalogyTealBg.copy(alpha = 0.7f) else MidnightSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isCorrectChoice) AnalogyTeal.copy(alpha = 0.45f) else SlateBorder,
                    ),
                ) {
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IceWhite,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecallErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recall_error_card"),
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
                        .testTag("recall_retry_button"),
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
