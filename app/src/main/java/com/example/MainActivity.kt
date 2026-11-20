package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StreakState
import com.example.data.StudyStreakStore
import com.example.data.StudyTab
import com.example.firebase.configureAppCheck
import com.example.ui.components.StudyPilotBottomBar
import com.example.ui.components.StudyPilotTopHeader
import com.example.ui.focus.FocusScreen
import com.example.ui.focus.FocusViewModel
import com.example.ui.plan.PlanScreen
import com.example.ui.plan.PlanViewModel
import com.example.ui.recall.RecallScreen
import com.example.ui.recall.RecallViewModel
import com.example.ui.solve.SolveScreen
import com.example.ui.solve.SolveViewModel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.StudyPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before rendering UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        setContent {
            StudyPilotTheme {
                StudyPilotApp()
            }
        }
    }
}

@Composable
fun StudyPilotApp(
    solveViewModel: SolveViewModel = viewModel(),
    recallViewModel: RecallViewModel = viewModel(),
    planViewModel: PlanViewModel = viewModel(),
    focusViewModel: FocusViewModel = viewModel(),
) {
    val context = LocalContext.current
    val streakStore = remember(context) { StudyStreakStore(context.applicationContext) }
    val streakState by streakStore.streakFlow.collectAsStateWithLifecycle(
        initialValue = StreakState()
    )

    var selectedTab by rememberSaveable { mutableStateOf(StudyTab.Solve) }
    val questionText by solveViewModel.questionText.collectAsStateWithLifecycle()
    val attachedPhoto by solveViewModel.attachedPhoto.collectAsStateWithLifecycle()
    val selectedModel by solveViewModel.selectedModel.collectAsStateWithLifecycle()
    val history by solveViewModel.history.collectAsStateWithLifecycle()
    val uiState by solveViewModel.uiState.collectAsStateWithLifecycle()

    val recallNotesText by recallViewModel.notesText.collectAsStateWithLifecycle()
    val recallSelectedModel by recallViewModel.selectedModel.collectAsStateWithLifecycle()
    val recallGenerationState by recallViewModel.generationState.collectAsStateWithLifecycle()
    val recallStudySet by recallViewModel.studySet.collectAsStateWithLifecycle()
    val recallFlippedIds by recallViewModel.flippedCardIds.collectAsStateWithLifecycle()
    val recallSelectedAnswers by recallViewModel.selectedAnswers.collectAsStateWithLifecycle()

    val plan by planViewModel.plan.collectAsStateWithLifecycle()
    val planExamName by planViewModel.examNameInput.collectAsStateWithLifecycle()
    val planTargetDate by planViewModel.targetDateInput.collectAsStateWithLifecycle()
    val planSubjects by planViewModel.subjectsInput.collectAsStateWithLifecycle()
    val planHours by planViewModel.dailyHoursInput.collectAsStateWithLifecycle()
    val planFormError by planViewModel.formError.collectAsStateWithLifecycle()

    val focusTimerState by focusViewModel.timerState.collectAsStateWithLifecycle()
    val focusDailyStats by focusViewModel.dailyStats.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            topBar = {
                StudyPilotTopHeader(streakState = streakState)
            },
            bottomBar = {
                if (!useNavigationRail) {
                    StudyPilotBottomBar(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    StudyPilotNavigationRail(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                    )
                }

                when (selectedTab) {
                    StudyTab.Solve -> SolveScreen(
                        questionText = questionText,
                        attachedPhoto = attachedPhoto,
                        selectedModel = selectedModel,
                        history = history,
                        uiState = uiState,
                        onQuestionChanged = solveViewModel::onQuestionChanged,
                        onClearQuestion = solveViewModel::clearQuestion,
                        onSelectModel = solveViewModel::selectModel,
                        onAttachPhotoUri = solveViewModel::attachPhotoFromUri,
                        onRemovePhoto = solveViewModel::removeAttachedPhoto,
                        onCameraUnavailable = solveViewModel::reportCameraUnavailable,
                        onSelectExample = solveViewModel::selectExample,
                        onClearHistory = solveViewModel::clearHistory,
                        onSolveClick = { solveViewModel.solve() },
                        modifier = Modifier.weight(1f),
                    )
                    StudyTab.Recall -> RecallScreen(
                        notesText = recallNotesText,
                        selectedModel = recallSelectedModel,
                        generationState = recallGenerationState,
                        studySet = recallStudySet,
                        flippedCardIds = recallFlippedIds,
                        selectedAnswers = recallSelectedAnswers,
                        onNotesChanged = recallViewModel::onNotesChanged,
                        onClearNotes = recallViewModel::clearNotes,
                        onSelectModel = recallViewModel::selectModel,
                        onGenerateClick = recallViewModel::generateStudySet,
                        onToggleFlashcard = recallViewModel::toggleFlashcard,
                        onSelectQuizOption = recallViewModel::selectQuizOption,
                        onResetQuiz = recallViewModel::resetQuiz,
                        onReturnToSolve = { selectedTab = StudyTab.Solve },
                        modifier = Modifier.weight(1f),
                    )
                    StudyTab.Plan -> PlanScreen(
                        plan = plan,
                        examNameInput = planExamName,
                        targetDateInput = planTargetDate,
                        subjectsInput = planSubjects,
                        dailyHoursInput = planHours,
                        formError = planFormError,
                        onExamNameChanged = planViewModel::onExamNameChanged,
                        onTargetDateChanged = planViewModel::onTargetDateChanged,
                        onTargetDatePicked = planViewModel::onTargetDatePicked,
                        onSubjectsChanged = planViewModel::onSubjectsChanged,
                        onDailyHoursChanged = planViewModel::onDailyHoursChanged,
                        onCreatePlanClick = planViewModel::createPlan,
                        onToggleTask = planViewModel::toggleTask,
                        onReturnToSolve = { selectedTab = StudyTab.Solve },
                        modifier = Modifier.weight(1f),
                    )
                    StudyTab.Focus -> FocusScreen(
                        timerState = focusTimerState,
                        dailyStats = focusDailyStats,
                        onSwitchMode = focusViewModel::switchMode,
                        onStartClick = focusViewModel::startTimer,
                        onPauseClick = focusViewModel::pauseTimer,
                        onResetClick = focusViewModel::resetTimer,
                        onReturnToSolve = { selectedTab = StudyTab.Solve },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyPilotNavigationRail(
    selectedTab: StudyTab,
    onSelectTab: (StudyTab) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MidnightSurface,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        StudyTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val (labelRes, selectedIcon, unselectedIcon) = when (tab) {
                StudyTab.Solve -> Triple(
                    R.string.tab_solve,
                    Icons.Filled.AutoAwesome,
                    Icons.Outlined.AutoAwesome,
                )
                StudyTab.Recall -> Triple(
                    R.string.tab_recall,
                    Icons.Filled.Style,
                    Icons.Outlined.Style,
                )
                StudyTab.Plan -> Triple(
                    R.string.tab_plan,
                    Icons.Filled.CalendarMonth,
                    Icons.Outlined.CalendarMonth,
                )
                StudyTab.Focus -> Triple(
                    R.string.tab_focus,
                    Icons.Filled.CenterFocusStrong,
                    Icons.Outlined.CenterFocusStrong,
                )
            }
            val label = stringResource(labelRes)
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) selectedIcon else unselectedIcon,
                        contentDescription = label,
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = ElectricCyan,
                    selectedTextColor = ElectricCyan,
                    indicatorColor = SlateSurfaceElevated,
                    unselectedIconColor = SlateTextSecondary,
                    unselectedTextColor = SlateTextSecondary,
                ),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
