package com.janhorak.shutterdeck.film.presentation

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.FilmRollDao
import com.janhorak.shutterdeck.core.data.db.FilmRollEntity
import com.janhorak.shutterdeck.film.domain.FilmDevelopmentStep
import com.janhorak.shutterdeck.film.domain.buildAgitationCueOffsets
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FilmDevelopmentTimerState(
    val contextLabel: String? = null,
    val steps: List<FilmDevelopmentStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val elapsedSecondsInStep: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isComplete: Boolean = false,
    val cueText: String? = null,
    val statusText: String? = null,
) {
    val currentStep: FilmDevelopmentStep?
        get() = steps.getOrNull(currentStepIndex)

    val totalDurationSeconds: Int
        get() = steps.sumOf { step -> step.durationSeconds }

    val totalRemainingSeconds: Int
        get() = if (steps.isEmpty()) {
            0
        } else {
            val completedBeforeCurrent = steps
                .take(currentStepIndex)
                .sumOf { step -> step.durationSeconds }
            (totalDurationSeconds - completedBeforeCurrent - elapsedSecondsInStep).coerceAtLeast(0)
        }
}

private data class TimerSnapshot(
    val currentStepIndex: Int,
    val stepStartedElapsedRealtimeMs: Long,
    val elapsedSecondsInStep: Int,
    val remainingSeconds: Int,
    val isComplete: Boolean,
)

@HiltViewModel
class FilmDevelopmentViewModel @Inject constructor(
    filmRollDao: FilmRollDao,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val rolls: StateFlow<List<FilmRollEntity>> = filmRollDao.observeRolls()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _timerState = MutableStateFlow(FilmDevelopmentTimerState())
    val timerState: StateFlow<FilmDevelopmentTimerState> = _timerState.asStateFlow()

    private var tickerJob: Job? = null
    private var currentStepStartedElapsedRealtimeMs: Long = 0L

    init {
        restoreTimerSession()
    }

    fun startTimer(
        steps: List<FilmDevelopmentStep>,
        contextLabel: String?,
    ) {
        if (steps.isEmpty()) {
            _timerState.value = _timerState.value.copy(statusText = "Build a recipe before starting the timer.")
            return
        }
        tickerJob?.cancel()
        currentStepStartedElapsedRealtimeMs = SystemClock.elapsedRealtime()
        _timerState.value = FilmDevelopmentTimerState(
            contextLabel = contextLabel,
            steps = steps,
            currentStepIndex = 0,
            elapsedSecondsInStep = 0,
            remainingSeconds = steps.first().durationSeconds,
            isRunning = true,
            cueText = buildCueText(step = steps.first(), elapsedSecondsInStep = 0),
            statusText = "Recipe started.",
        )
        persistRecipe(steps = steps, contextLabel = contextLabel)
        persistProgress(
            currentStepIndex = 0,
            stepStartedElapsedRealtimeMs = currentStepStartedElapsedRealtimeMs,
            elapsedSecondsInStep = 0,
            isRunning = true,
            isPaused = false,
        )
        startTicker()
    }

    fun pauseTimer() {
        val state = _timerState.value
        if (!state.isRunning) {
            return
        }
        val snapshot = computeSnapshot(SystemClock.elapsedRealtime()) ?: return
        tickerJob?.cancel()
        val currentStep = state.steps.getOrNull(snapshot.currentStepIndex) ?: return
        currentStepStartedElapsedRealtimeMs = snapshot.stepStartedElapsedRealtimeMs
        _timerState.value = state.copy(
            currentStepIndex = snapshot.currentStepIndex,
            elapsedSecondsInStep = snapshot.elapsedSecondsInStep,
            remainingSeconds = snapshot.remainingSeconds,
            isRunning = false,
            isPaused = true,
            isComplete = false,
            cueText = buildCueText(step = currentStep, elapsedSecondsInStep = snapshot.elapsedSecondsInStep),
            statusText = "Timer paused.",
        )
        persistProgress(
            currentStepIndex = snapshot.currentStepIndex,
            stepStartedElapsedRealtimeMs = snapshot.stepStartedElapsedRealtimeMs,
            elapsedSecondsInStep = snapshot.elapsedSecondsInStep,
            isRunning = false,
            isPaused = true,
        )
    }

    fun resumeTimer() {
        val state = _timerState.value
        val currentStep = state.currentStep ?: return
        if (!state.isPaused || state.isComplete) {
            return
        }
        currentStepStartedElapsedRealtimeMs = SystemClock.elapsedRealtime() - (state.elapsedSecondsInStep * 1_000L)
        _timerState.value = state.copy(
            isRunning = true,
            isPaused = false,
            remainingSeconds = (currentStep.durationSeconds - state.elapsedSecondsInStep).coerceAtLeast(0),
            cueText = buildCueText(step = currentStep, elapsedSecondsInStep = state.elapsedSecondsInStep),
            statusText = "Timer resumed.",
        )
        persistProgress(
            currentStepIndex = state.currentStepIndex,
            stepStartedElapsedRealtimeMs = currentStepStartedElapsedRealtimeMs,
            elapsedSecondsInStep = state.elapsedSecondsInStep,
            isRunning = true,
            isPaused = false,
        )
        startTicker()
    }

    fun resetTimer() {
        tickerJob?.cancel()
        currentStepStartedElapsedRealtimeMs = 0L
        clearPersistedTimer()
        _timerState.value = FilmDevelopmentTimerState(statusText = "Timer reset.")
    }

    private fun restoreTimerSession() {
        val labels = savedStateHandle.get<ArrayList<String>>(KEY_STEP_LABELS) ?: return
        val durations = savedStateHandle.get<IntArray>(KEY_STEP_DURATIONS) ?: run {
            clearPersistedTimer()
            return
        }
        val agitationIntervals = savedStateHandle.get<IntArray>(KEY_STEP_AGITATION_INTERVALS) ?: IntArray(labels.size)
        if (labels.size != durations.size || labels.size != agitationIntervals.size) {
            clearPersistedTimer()
            return
        }
        val notes = savedStateHandle.get<ArrayList<String>>(KEY_STEP_NOTES) ?: arrayListOf()
        val steps = labels.indices.map { index ->
            FilmDevelopmentStep(
                name = labels[index],
                durationSeconds = durations[index],
                agitationIntervalSeconds = agitationIntervals[index],
                note = notes.getOrNull(index)?.takeIf { value -> value.isNotBlank() },
            )
        }
        if (steps.isEmpty()) {
            clearPersistedTimer()
            return
        }

        val contextLabel = savedStateHandle.get<String>(KEY_CONTEXT_LABEL)
        val currentStepIndex = (savedStateHandle.get<Int>(KEY_CURRENT_STEP_INDEX) ?: 0)
            .coerceIn(0, steps.lastIndex)
        val isPaused = savedStateHandle.get<Boolean>(KEY_IS_PAUSED) ?: false
        val isRunning = savedStateHandle.get<Boolean>(KEY_IS_RUNNING) ?: false

        if (isPaused) {
            val elapsedSecondsInStep = (savedStateHandle.get<Int>(KEY_ELAPSED_SECONDS_IN_STEP) ?: 0)
                .coerceAtLeast(0)
            val currentStep = steps[currentStepIndex]
            currentStepStartedElapsedRealtimeMs = SystemClock.elapsedRealtime() - (elapsedSecondsInStep * 1_000L)
            _timerState.value = FilmDevelopmentTimerState(
                contextLabel = contextLabel,
                steps = steps,
                currentStepIndex = currentStepIndex,
                elapsedSecondsInStep = elapsedSecondsInStep.coerceAtMost(currentStep.durationSeconds),
                remainingSeconds = (currentStep.durationSeconds - elapsedSecondsInStep).coerceAtLeast(0),
                isPaused = true,
                cueText = buildCueText(step = currentStep, elapsedSecondsInStep = elapsedSecondsInStep),
                statusText = "Timer restored in a paused state.",
            )
            return
        }

        currentStepStartedElapsedRealtimeMs = savedStateHandle.get<Long>(KEY_STEP_STARTED_ELAPSED_REALTIME_MS)
            ?: SystemClock.elapsedRealtime()
        _timerState.value = FilmDevelopmentTimerState(
            contextLabel = contextLabel,
            steps = steps,
            currentStepIndex = currentStepIndex,
            isRunning = isRunning,
            statusText = if (isRunning) "Timer restored." else null,
        )
        if (isRunning) {
            updateRunningState(SystemClock.elapsedRealtime())
            startTicker()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                updateRunningState(SystemClock.elapsedRealtime())
                delay(500L)
            }
        }
    }

    private fun updateRunningState(nowElapsedRealtimeMs: Long) {
        val state = _timerState.value
        if (!state.isRunning || state.steps.isEmpty()) {
            return
        }
        val snapshot = computeSnapshot(nowElapsedRealtimeMs) ?: return
        if (snapshot.isComplete) {
            tickerJob?.cancel()
            clearPersistedTimer()
            val lastStepIndex = state.steps.lastIndex
            val lastStep = state.steps[lastStepIndex]
            _timerState.value = state.copy(
                currentStepIndex = lastStepIndex,
                elapsedSecondsInStep = lastStep.durationSeconds,
                remainingSeconds = 0,
                isRunning = false,
                isPaused = false,
                isComplete = true,
                cueText = "All steps finished.",
                statusText = "Recipe complete.",
            )
            currentStepStartedElapsedRealtimeMs = 0L
            return
        }

        currentStepStartedElapsedRealtimeMs = snapshot.stepStartedElapsedRealtimeMs
        val currentStep = state.steps[snapshot.currentStepIndex]
        _timerState.value = state.copy(
            currentStepIndex = snapshot.currentStepIndex,
            elapsedSecondsInStep = snapshot.elapsedSecondsInStep,
            remainingSeconds = snapshot.remainingSeconds,
            isRunning = true,
            isPaused = false,
            isComplete = false,
            cueText = buildCueText(step = currentStep, elapsedSecondsInStep = snapshot.elapsedSecondsInStep),
        )
        persistProgress(
            currentStepIndex = snapshot.currentStepIndex,
            stepStartedElapsedRealtimeMs = snapshot.stepStartedElapsedRealtimeMs,
            elapsedSecondsInStep = snapshot.elapsedSecondsInStep,
            isRunning = true,
            isPaused = false,
        )
    }

    private fun computeSnapshot(nowElapsedRealtimeMs: Long): TimerSnapshot? {
        val state = _timerState.value
        if (state.steps.isEmpty()) {
            return null
        }

        var workingStepIndex = state.currentStepIndex.coerceIn(0, state.steps.lastIndex)
        var workingStepStartMs = currentStepStartedElapsedRealtimeMs.takeIf { value -> value > 0L } ?: nowElapsedRealtimeMs

        while (workingStepIndex < state.steps.size) {
            val currentStep = state.steps[workingStepIndex]
            val elapsedSecondsInStep = ((nowElapsedRealtimeMs - workingStepStartMs) / 1_000L)
                .toInt()
                .coerceAtLeast(0)
            if (elapsedSecondsInStep < currentStep.durationSeconds) {
                return TimerSnapshot(
                    currentStepIndex = workingStepIndex,
                    stepStartedElapsedRealtimeMs = workingStepStartMs,
                    elapsedSecondsInStep = elapsedSecondsInStep,
                    remainingSeconds = currentStep.durationSeconds - elapsedSecondsInStep,
                    isComplete = false,
                )
            }
            workingStepStartMs += currentStep.durationSeconds * 1_000L
            workingStepIndex += 1
        }

        return TimerSnapshot(
            currentStepIndex = state.steps.lastIndex,
            stepStartedElapsedRealtimeMs = workingStepStartMs,
            elapsedSecondsInStep = state.steps.last().durationSeconds,
            remainingSeconds = 0,
            isComplete = true,
        )
    }

    private fun buildCueText(
        step: FilmDevelopmentStep,
        elapsedSecondsInStep: Int,
    ): String {
        val agitationCues = buildAgitationCueOffsets(
            durationSeconds = step.durationSeconds,
            agitationIntervalSeconds = step.agitationIntervalSeconds,
        )
        if (agitationCues.isEmpty()) {
            return step.note ?: "No recurring agitation cues for this step."
        }
        if (elapsedSecondsInStep == 0) {
            return step.note ?: "Start ${step.name}."
        }
        if (agitationCues.any { cueOffset -> cueOffset == elapsedSecondsInStep }) {
            return "Agitate now."
        }
        val nextCueOffset = agitationCues.firstOrNull { cueOffset -> cueOffset > elapsedSecondsInStep }
        return if (nextCueOffset != null) {
            "Next agitation in ${formatTimerSeconds(nextCueOffset - elapsedSecondsInStep)}."
        } else {
            "No more agitation cues in this step."
        }
    }

    private fun persistRecipe(
        steps: List<FilmDevelopmentStep>,
        contextLabel: String?,
    ) {
        savedStateHandle[KEY_STEP_LABELS] = ArrayList(steps.map { step -> step.name })
        savedStateHandle[KEY_STEP_DURATIONS] = steps.map { step -> step.durationSeconds }.toIntArray()
        savedStateHandle[KEY_STEP_AGITATION_INTERVALS] = steps.map { step -> step.agitationIntervalSeconds }.toIntArray()
        savedStateHandle[KEY_STEP_NOTES] = ArrayList(steps.map { step -> step.note.orEmpty() })
        savedStateHandle[KEY_CONTEXT_LABEL] = contextLabel
    }

    private fun persistProgress(
        currentStepIndex: Int,
        stepStartedElapsedRealtimeMs: Long,
        elapsedSecondsInStep: Int,
        isRunning: Boolean,
        isPaused: Boolean,
    ) {
        savedStateHandle[KEY_CURRENT_STEP_INDEX] = currentStepIndex
        savedStateHandle[KEY_STEP_STARTED_ELAPSED_REALTIME_MS] = stepStartedElapsedRealtimeMs
        savedStateHandle[KEY_ELAPSED_SECONDS_IN_STEP] = elapsedSecondsInStep
        savedStateHandle[KEY_IS_RUNNING] = isRunning
        savedStateHandle[KEY_IS_PAUSED] = isPaused
    }

    private fun clearPersistedTimer() {
        savedStateHandle.remove<ArrayList<String>>(KEY_STEP_LABELS)
        savedStateHandle.remove<IntArray>(KEY_STEP_DURATIONS)
        savedStateHandle.remove<IntArray>(KEY_STEP_AGITATION_INTERVALS)
        savedStateHandle.remove<ArrayList<String>>(KEY_STEP_NOTES)
        savedStateHandle.remove<String>(KEY_CONTEXT_LABEL)
        savedStateHandle.remove<Int>(KEY_CURRENT_STEP_INDEX)
        savedStateHandle.remove<Long>(KEY_STEP_STARTED_ELAPSED_REALTIME_MS)
        savedStateHandle.remove<Int>(KEY_ELAPSED_SECONDS_IN_STEP)
        savedStateHandle.remove<Boolean>(KEY_IS_RUNNING)
        savedStateHandle.remove<Boolean>(KEY_IS_PAUSED)
    }

    companion object {
        private const val KEY_STEP_LABELS = "filmDevelopment.stepLabels"
        private const val KEY_STEP_DURATIONS = "filmDevelopment.stepDurations"
        private const val KEY_STEP_AGITATION_INTERVALS = "filmDevelopment.stepAgitationIntervals"
        private const val KEY_STEP_NOTES = "filmDevelopment.stepNotes"
        private const val KEY_CONTEXT_LABEL = "filmDevelopment.contextLabel"
        private const val KEY_CURRENT_STEP_INDEX = "filmDevelopment.currentStepIndex"
        private const val KEY_STEP_STARTED_ELAPSED_REALTIME_MS = "filmDevelopment.stepStartedElapsedRealtimeMs"
        private const val KEY_ELAPSED_SECONDS_IN_STEP = "filmDevelopment.elapsedSecondsInStep"
        private const val KEY_IS_RUNNING = "filmDevelopment.isRunning"
        private const val KEY_IS_PAUSED = "filmDevelopment.isPaused"
    }
}

private fun formatTimerSeconds(seconds: Int): String {
    val clampedSeconds = seconds.coerceAtLeast(0)
    val minutes = clampedSeconds / 60
    val remainingSeconds = clampedSeconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
