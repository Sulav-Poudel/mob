package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SupplementInput
import com.example.data.TrackerRepository
import com.example.data.UniversityInput
import com.example.model.ApplicationStatus
import com.example.model.FinancialAidDocStatus
import com.example.model.ProgressCalculator
import com.example.model.ProgressMetrics
import com.example.model.ProgressMode
import com.example.model.Supplement
import com.example.model.SupplementStatus
import com.example.model.SupplementType
import com.example.model.University
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class TrackerSnapshot(
    val universities: List<University>,
    val supplements: List<Supplement>,
    val supplementTypes: List<SupplementType>,
    val supplementsByUniversity: Map<String, List<Supplement>>,
    val progressByUniversity: Map<String, ProgressMetrics>,
    val overallProgress: ProgressMetrics,
    val progressMode: ProgressMode
)

class TrackerViewModel(
    private val repository: TrackerRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _progressMode = MutableStateFlow(ProgressMode.WORD_WEIGHTED)
    val progressMode: StateFlow<ProgressMode> = _progressMode.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedStarterSupplementTypesIfEmpty()
        }
    }

    val uiState: StateFlow<UiState<TrackerSnapshot>> = combine(
        repository.observeUniversities(currentUserId),
        repository.observeSupplements(currentUserId),
        repository.observeSupplementTypes(currentUserId),
        _progressMode
    ) { universities, supplements, types, mode ->
        val byUni = supplements.groupBy { it.universityId }
        val progressMap = universities.associate { uni ->
            uni.id to ProgressCalculator.compute(byUni[uni.id].orEmpty())
        }
        val overall = ProgressCalculator.compute(supplements)
        UiState.Success(
            TrackerSnapshot(
                universities = universities,
                supplements = supplements,
                supplementTypes = types,
                supplementsByUniversity = byUni,
                progressByUniversity = progressMap,
                overallProgress = overall,
                progressMode = mode
            )
        ) as UiState<TrackerSnapshot>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing tracker data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync your college tracker data."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun setProgressMode(mode: ProgressMode) {
        _progressMode.value = mode
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun saveUniversity(
        input: UniversityInput,
        existingSupplementCount: Int = 0,
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveUniversity(input, existingSupplementCount)
            _isMutating.value = false
            result.onSuccess { uniId ->
                _feedbackMessage.value = if (input.id.isNullOrBlank()) {
                    "Added ${input.name.trim()}"
                } else {
                    "Updated ${input.name.trim()}"
                }
                onSaved(uniId)
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not save school."
            }
        }
    }

    fun updateUniversityQuickStatus(
        universityId: String,
        applicationStatus: ApplicationStatus? = null,
        financialAidDocStatus: FinancialAidDocStatus? = null
    ) {
        viewModelScope.launch {
            val result = repository.updateUniversityQuickStatus(
                universityId = universityId,
                applicationStatus = applicationStatus,
                financialAidDocStatus = financialAidDocStatus
            )
            result.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not update status."
            }
        }
    }

    fun deleteUniversity(universityId: String, name: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteUniversity(universityId)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "Removed $name"
                onDeleted()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not delete school."
            }
        }
    }

    fun generateMissingSupplementSlots(universityId: String, currentCount: Int, targetCount: Int) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.generateMissingSupplementSlots(universityId, currentCount, targetCount)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "Created supplement slots"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not create supplement slots."
            }
        }
    }

    fun saveSupplement(
        input: SupplementInput,
        existingTypes: List<SupplementType>,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveSupplement(input, existingTypes)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = if (input.id.isNullOrBlank()) {
                    "Supplement added"
                } else {
                    "Supplement saved"
                }
                onSaved()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not save supplement."
            }
        }
    }

    fun quickUpdateSupplementStatus(supplementId: String, status: SupplementStatus) {
        viewModelScope.launch {
            val result = repository.quickUpdateSupplementStatus(supplementId, status)
            result.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not update supplement status."
            }
        }
    }

    fun deleteSupplement(supplementId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteSupplement(supplementId)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "Supplement deleted"
                onDeleted()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not delete supplement."
            }
        }
    }

    fun addSupplementType(name: String, existingTypes: List<SupplementType>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addSupplementType(name, existingTypes)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "Added essay type \"${name.trim()}\""
                onSuccess()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not add essay type."
            }
        }
    }

    fun renameSupplementType(typeId: String, newName: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.renameSupplementType(typeId, newName)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "Updated essay type"
                onSuccess()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not rename essay type."
            }
        }
    }

    fun deleteSupplementType(typeId: String, name: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteSupplementType(typeId)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "Removed \"$name\""
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not remove essay type."
            }
        }
    }

    private companion object {
        const val TAG = "TrackerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
