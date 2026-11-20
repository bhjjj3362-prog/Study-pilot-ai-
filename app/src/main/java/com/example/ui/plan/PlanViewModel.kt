package com.example.ui.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DemoPlanData
import com.example.data.PlanStore
import com.example.data.RevisionPlan
import com.example.data.StudyDateUtils
import com.example.data.StudyStreakStore
import com.example.data.buildDistributedPlanTasks
import com.example.data.parseSubjectsList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val planStore = PlanStore(application.applicationContext)
    private val streakStore = StudyStreakStore(application.applicationContext)

    val plan: StateFlow<RevisionPlan> = planStore.planFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DemoPlanData.samplePlan(),
        )

    private val _examNameInput = MutableStateFlow("")
    val examNameInput: StateFlow<String> = _examNameInput.asStateFlow()

    private val _targetDateInput = MutableStateFlow(
        StudyDateUtils.addDaysIso(StudyDateUtils.todayIso(), 7)
    )
    val targetDateInput: StateFlow<String> = _targetDateInput.asStateFlow()

    private val _subjectsInput = MutableStateFlow("")
    val subjectsInput: StateFlow<String> = _subjectsInput.asStateFlow()

    private val _dailyHoursInput = MutableStateFlow("2.5")
    val dailyHoursInput: StateFlow<String> = _dailyHoursInput.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = planStore.planFlow.first()
            _examNameInput.value = loaded.examName
            _targetDateInput.value = loaded.targetDateIso
            _subjectsInput.value = loaded.subjectsRaw
            _dailyHoursInput.value = loaded.dailyHours.toString().removeSuffix(".0")
        }
    }

    fun onExamNameChanged(value: String) {
        _examNameInput.value = value
        _formError.value = null
    }

    fun onTargetDateChanged(value: String) {
        _targetDateInput.value = value
        _formError.value = null
    }

    fun onTargetDatePicked(utcMillis: Long) {
        _targetDateInput.value = StudyDateUtils.utcPickerMillisToLocalIso(utcMillis)
        _formError.value = null
    }

    fun onSubjectsChanged(value: String) {
        _subjectsInput.value = value
        _formError.value = null
    }

    fun onDailyHoursChanged(value: String) {
        _dailyHoursInput.value = value
        _formError.value = null
    }

    fun createPlan() {
        val examName = _examNameInput.value.trim()
        val targetIso = _targetDateInput.value.trim()
        val subjectsRaw = _subjectsInput.value.trim()
        val subjects = parseSubjectsList(subjectsRaw)
        val hours = _dailyHoursInput.value.trim().toDoubleOrNull()

        when {
            examName.isEmpty() -> {
                _formError.value = "Enter an exam name."
                return
            }
            !StudyDateUtils.isValidIsoDate(targetIso) -> {
                _formError.value = "Enter a valid target date (YYYY-MM-DD)."
                return
            }
            subjects.isEmpty() -> {
                _formError.value = "Enter at least one subject separated by commas."
                return
            }
            hours == null || hours <= 0.0 -> {
                _formError.value = "Enter valid daily study hours (e.g. 2.5)."
                return
            }
        }

        val todayIso = StudyDateUtils.todayIso()
        val daysRemaining = StudyDateUtils.daysBetweenIso(todayIso, targetIso)
        val tasks = buildDistributedPlanTasks(
            subjects = subjects,
            daysRemaining = daysRemaining,
            dailyHours = hours,
        )

        val newPlan = RevisionPlan(
            examName = examName,
            targetDateIso = targetIso,
            subjectsRaw = subjectsRaw,
            dailyHours = hours,
            tasks = tasks,
            isSample = false,
        )

        viewModelScope.launch {
            planStore.savePlan(newPlan)
            _formError.value = null
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            val becameCompleted = planStore.toggleTask(taskId)
            if (becameCompleted) {
                streakStore.recordQualifyingActivity()
            }
        }
    }
}
