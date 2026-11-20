package com.example.ui.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlanTask
import com.example.data.RevisionPlan
import com.example.data.StudyDateUtils
import com.example.data.formatHoursLabel
import com.example.ui.theme.AnalogyTeal
import com.example.ui.theme.AnalogyTealBg
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LogicIndigo
import com.example.ui.theme.LogicIndigoBg
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateSurfaceHighest
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    plan: RevisionPlan,
    examNameInput: String,
    targetDateInput: String,
    subjectsInput: String,
    dailyHoursInput: String,
    formError: String?,
    onExamNameChanged: (String) -> Unit,
    onTargetDateChanged: (String) -> Unit,
    onTargetDatePicked: (Long) -> Unit,
    onSubjectsChanged: (String) -> Unit,
    onDailyHoursChanged: (String) -> Unit,
    onCreatePlanClick: () -> Unit,
    onToggleTask: (String) -> Unit,
    onReturnToSolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onReturnToSolve()
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }
    var formExpanded by rememberSaveable { mutableStateOf(true) }

    val daysRemaining = remember(plan.targetDateIso) {
        StudyDateUtils.daysBetweenIso(StudyDateUtils.todayIso(), plan.targetDateIso)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onTargetDatePicked(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = SlateTextSecondary)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
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
            item(key = "countdown_hero") {
                CountdownSummaryCard(
                    plan = plan,
                    daysRemaining = daysRemaining,
                )
            }

            item(key = "plan_builder") {
                PlanBuilderCard(
                    expanded = formExpanded,
                    onToggleExpand = { formExpanded = !formExpanded },
                    examNameInput = examNameInput,
                    targetDateInput = targetDateInput,
                    subjectsInput = subjectsInput,
                    dailyHoursInput = dailyHoursInput,
                    formError = formError,
                    onExamNameChanged = onExamNameChanged,
                    onTargetDateChanged = onTargetDateChanged,
                    onOpenDatePicker = { showDatePicker = true },
                    onSubjectsChanged = onSubjectsChanged,
                    onDailyHoursChanged = onDailyHoursChanged,
                    onCreatePlanClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onCreatePlanClick()
                    },
                )
            }

            item(key = "checklist_header") {
                ChecklistSectionHeader(
                    completedCount = plan.completedCount,
                    totalCount = plan.tasks.size,
                )
            }

            itemsIndexed(
                items = plan.tasks,
                key = { _, task -> task.id },
            ) { index, task ->
                PlanTaskCard(
                    index = index,
                    task = task,
                    onToggle = { onToggleTask(task.id) },
                )
            }

            item(key = "plan_bottom_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CountdownSummaryCard(
    plan: RevisionPlan,
    daysRemaining: Int,
) {
    val targetFraction = plan.completionFraction
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "plan_completion_progress",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("days_remaining_badge"),
        shape = RoundedCornerShape(22.dp),
        color = SlateSurfaceElevated,
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = plan.examName,
                        style = MaterialTheme.typography.titleLarge,
                        color = IceWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = plan.targetDateIso,
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextSecondary,
                        )
                        Text(
                            text = "· ${formatHoursLabel(plan.dailyHours)}/day",
                            style = MaterialTheme.typography.labelSmall,
                            color = AnalogyTeal,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CyanContainer,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${daysRemaining}d",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ElectricCyan,
                        )
                        Text(
                            text = stringResource(R.string.plan_days_remaining_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = IceWhite,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${plan.completedCount} of ${plan.tasks.size} tasks complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextSecondary,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (plan.completionPercent == 100) AnalogyTealBg else MidnightSurface,
                        border = BorderStroke(
                            1.dp,
                            if (plan.completionPercent == 100) AnalogyTeal.copy(alpha = 0.5f) else SlateBorder,
                        ),
                    ) {
                        Text(
                            text = "${plan.completionPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AnalogyTeal,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("plan_progress_percentage"),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MidnightSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = animatedProgress,
                                range = 0f..1f,
                            )
                        }
                        .testTag("plan_progress_bar"),
                ) {
                    if (animatedProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(ElectricCyan, AnalogyTeal)
                                    )
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanBuilderCard(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    examNameInput: String,
    targetDateInput: String,
    subjectsInput: String,
    dailyHoursInput: String,
    formError: String?,
    onExamNameChanged: (String) -> Unit,
    onTargetDateChanged: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
    onSubjectsChanged: (String) -> Unit,
    onDailyHoursChanged: (String) -> Unit,
    onCreatePlanClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = SlateSurfaceElevated,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventAvailable,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.plan_section_builder),
                        style = MaterialTheme.typography.titleMedium,
                        color = IceWhite,
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.plan_edit_toggle_hide else R.string.plan_edit_toggle_show
                    ),
                    tint = SlateTextSecondary,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SlateBorder,
                        cursorColor = ElectricCyan,
                    )

                    OutlinedTextField(
                        value = examNameInput,
                        onValueChange = onExamNameChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_exam_name_input"),
                        label = { Text(stringResource(R.string.plan_exam_name_label)) },
                        placeholder = { Text(stringResource(R.string.plan_exam_name_hint), color = SlateTextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = fieldColors,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = targetDateInput,
                            onValueChange = onTargetDateChanged,
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("plan_target_date_input"),
                            label = { Text(stringResource(R.string.plan_target_date_label)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = onOpenDatePicker,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("plan_date_picker_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = stringResource(R.string.plan_pick_date_desc),
                                        tint = ElectricCyan,
                                    )
                                }
                            },
                            colors = fieldColors,
                        )

                        OutlinedTextField(
                            value = dailyHoursInput,
                            onValueChange = onDailyHoursChanged,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_hours_input"),
                            label = { Text(stringResource(R.string.plan_hours_label)) },
                            placeholder = { Text(stringResource(R.string.plan_hours_hint), color = SlateTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors,
                        )
                    }

                    OutlinedTextField(
                        value = subjectsInput,
                        onValueChange = onSubjectsChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_subjects_input"),
                        label = { Text(stringResource(R.string.plan_subjects_label)) },
                        placeholder = { Text(stringResource(R.string.plan_subjects_hint), color = SlateTextMuted) },
                        minLines = 1,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = fieldColors,
                    )

                    if (!formError.isNullOrBlank()) {
                        Text(
                            text = formError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorCoral,
                        )
                    }

                    Button(
                        onClick = onCreatePlanClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("create_plan_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF041E2B),
                            disabledContainerColor = SlateSurfaceHighest,
                            disabledContentColor = SlateTextMuted,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.plan_create_button),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistSectionHeader(
    completedCount: Int,
    totalCount: Int,
) {
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
                imageVector = Icons.Outlined.TaskAlt,
                contentDescription = null,
                tint = AnalogyTeal,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.plan_checklist_title),
                style = MaterialTheme.typography.titleMedium,
                color = IceWhite,
            )
        }

        Text(
            text = "$completedCount / $totalCount",
            style = MaterialTheme.typography.labelSmall,
            color = if (totalCount > 0 && completedCount == totalCount) AnalogyTeal else SlateTextSecondary,
        )
    }
}

@Composable
private fun PlanTaskCard(
    index: Int,
    task: PlanTask,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .minimumInteractiveComponentSize()
            .testTag("plan_task_$index"),
        shape = RoundedCornerShape(16.dp),
        color = if (task.isCompleted) AnalogyTealBg.copy(alpha = 0.55f) else MidnightSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isCompleted) AnalogyTeal.copy(alpha = 0.45f) else SlateBorder,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (task.isCompleted) {
                    Icons.Filled.CheckBox
                } else {
                    Icons.Filled.CheckBoxOutlineBlank
                },
                contentDescription = null,
                tint = if (task.isCompleted) AnalogyTeal else SlateTextSecondary,
                modifier = Modifier.size(24.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LogicIndigoBg,
                        border = BorderStroke(1.dp, LogicIndigo.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = stringResource(R.string.plan_day_prefix, task.dayNumber),
                            style = MaterialTheme.typography.labelSmall,
                            color = LogicIndigo,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = SlateTextMuted,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = task.hoursLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextSecondary,
                        )
                    }
                }

                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isCompleted) SlateTextSecondary else IceWhite,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                )
            }
        }
    }
}
