package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.JourneyData
import com.example.data.JourneyRepository
import com.example.data.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class JourneyViewModel(private val repository: JourneyRepository) : ViewModel() {

    // List of completed task IDs
    val completedTaskIds: StateFlow<Set<String>> = repository.completedTasks
        .map { list -> list.map { it.taskId }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // User progress details (active car location stage)
    val userProgress: StateFlow<UserProgress> = repository.userProgress
        .map { progress -> progress ?: UserProgress(currentStageId = 1, totalSavingsJod = 0.0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProgress(currentStageId = 1, totalSavingsJod = 0.0)
        )

    // The stage currently selected/clicked by the user on the map or panel
    private val _selectedStageId = MutableStateFlow(1)
    val selectedStageId: StateFlow<Int> = _selectedStageId.asStateFlow()

    // Initialize: Set selected stage to matching the db progress stage on load
    init {
        viewModelScope.launch {
            repository.userProgress.collect { progress ->
                if (progress != null && _selectedStageId.value == 1) {
                    _selectedStageId.value = progress.currentStageId
                }
            }
        }
    }

    // Toggle a task status (complete/incomplete) and autosave
    fun toggleTask(taskId: String, stageId: Int) {
        viewModelScope.launch {
            val currentCompleted = completedTaskIds.value
            if (currentCompleted.contains(taskId)) {
                repository.unmarkTaskCompleted(taskId)
            } else {
                repository.markTaskCompleted(taskId)
                // If checking a task of a higher stage than current car stage, auto move car there!
                val progressVal = userProgress.value
                if (stageId > progressVal.currentStageId) {
                    repository.saveProgress(stageId, progressVal.totalSavingsJod)
                }
            }
            updateActiveStageBasedOnTasks()
        }
    }

    // Update active car stage to match the highest stage which has progress or is unlocked
    private fun updateActiveStageBasedOnTasks() {
        viewModelScope.launch {
            val completed = completedTaskIds.value
            // Find the highest stage that has all preceding stages tasks fully completed
            var highestUnlocked = 1
            for (stage in JourneyData.stages) {
                val stageTasks = stage.tasks
                val completedInStage = stageTasks.filter { completed.contains(it.id) }.size
                
                if (stage.id > 1) {
                    // Check if previous stage had substantial progress (at least 50% done) to unlock car movement
                    val prevStage = JourneyData.stages.find { it.id == stage.id - 1 }
                    if (prevStage != null) {
                        val prevCompleted = prevStage.tasks.filter { completed.contains(it.id) }.size
                        val prevRatio = prevCompleted.toFloat() / prevStage.tasks.size
                        if (prevRatio >= 0.5f) {
                            highestUnlocked = stage.id
                        }
                    }
                }
                
                // If current stage is completely checked, definitely unlock next
                val isCurrentStageFinished = completedInStage == stageTasks.size
                if (isCurrentStageFinished && stage.id < 6) {
                    highestUnlocked = maxOf(highestUnlocked, stage.id + 1)
                }
            }

            val curProgress = userProgress.value
            if (highestUnlocked != curProgress.currentStageId) {
                repository.saveProgress(highestUnlocked, curProgress.totalSavingsJod)
                _selectedStageId.value = highestUnlocked
            }
        }
    }

    // Explicitly set the active stage (move car)
    fun setCarStage(stageId: Int) {
        viewModelScope.launch {
            val progressVal = userProgress.value
            repository.saveProgress(stageId, progressVal.totalSavingsJod)
            _selectedStageId.value = stageId
        }
    }

    // Select which stage to inspect/focus
    fun selectStage(stageId: Int) {
        _selectedStageId.value = stageId
    }

    // Reset the full journey
    fun resetJourney() {
        viewModelScope.launch {
            repository.resetJourney()
            _selectedStageId.value = 1
        }
    }

    // Add virtual savings (dynamic gamification feature)
    fun addSavings(amount: Double) {
        viewModelScope.launch {
            val progressVal = userProgress.value
            val newSavings = maxOf(0.0, progressVal.totalSavingsJod + amount)
            repository.saveProgress(progressVal.currentStageId, newSavings)
        }
    }
}

class JourneyViewModelFactory(private val repository: JourneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JourneyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JourneyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
