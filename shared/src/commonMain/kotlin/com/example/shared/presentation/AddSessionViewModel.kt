package com.example.shared.presentation

import com.example.shared.domain.AddSessionUseCase
import com.example.shared.domain.DrillEntry
import com.example.shared.domain.PracticeSession
import com.example.shared.util.Ids
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock


/**
 * Temporary drill model used while the user is filling the form.
 * This is NOT saved to the database directly.
 */
data class DrillDraft(
    val id: String = Ids.newId(),   // Unique if for UI tracking
    val name: String = "",          // Drill name entered by user
    val rating: Int = 3,            // Default difficulty rating
    val notes: String? = null       // Optional notes
)

/**
 * UI state for the Add session screen.
 * Holds form values, drill drafts, and save status.
 */
data class AddSessionState(
    val dateMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val durationMinutes: Int = 60,
    val location: String = "",
    val notes: String = "",
    val drills: List<DrillDraft> = listOf(DrillDraft()),
    val saving: Boolean = false,    // True while saving to DB
    val saved: Boolean = false,     // True after successful save
    val error: String? = null       // Error message for UI
)

/**
 * ViewModel for the Add session screen.
 * Responsible for:
 * - Managing from state
 * - Validating input
 * - Saving session and drills
 */
class AddSessionViewModel(
    private val addSession: AddSessionUseCase
) {

    // Coroutine scope owned by this ViewModel
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Internal mutable state
    private val _state = MutableStateFlow(AddSessionState())

    // Public read-only state exposed to UI
    val state: StateFlow<AddSessionState> = _state.asStateFlow()

    // ----- Simple form field updates -----

    fun setDuration(mins: Int) = _state.update { it.copy(durationMinutes = mins.coerceAtLeast(1)) }

    fun setLocation(v: String) = _state.update { it.copy(location = v) }

    fun setNotes(v: String) = _state.update { it.copy(notes = v) }

    // ----- Drill editing -----

    // Adds a new empty drill row
    fun addDrillRow() =
        _state.update {
            it.copy(
                drills = it.drills + DrillDraft()
            )
        }

    // Updates drill name by id
    fun updateDrillName(id: String, name: String) =
        _state.update {
            it.copy(
                drills = it.drills.map { d ->
                    if (d.id == id) d.copy(name = name) else d
                })
        }

    // Updates drill rating by id (clamped between 1 and 5)
    fun updateDrillRating(id: String, rating: Int) =
        _state.update {
            it.copy(drills = it.drills.map { d ->
                if (d.id == id) d.copy(
                    rating = rating.coerceIn(1, 5)
                ) else d
            }
            )
        }

    /**
     * Saves the session and its drills.
     * - Validates input
     * - Converts UI drafts to domain models
     * - Calls AddSessionUseCase
     */
    fun save() {
        scope.launch {
            val snapshot = _state.value

            // Only keep drills with a name
            val validDrills = snapshot.drills.filter { it.name.isNotBlank() }
            if (validDrills.isEmpty()) {
                _state.update { it.copy(error = "Add at least one drill name") }
                return@launch
            }

            // Show saving state
            _state.update { it.copy(saving = true, error = null) }

            // Create session domain model
            val sessionId = Ids.newId()
            val session = PracticeSession(
                id = sessionId,
                dateMillis = snapshot.dateMillis,
                durationMinutes = snapshot.durationMinutes,
                location = snapshot.location.takeIf { it.isNotBlank() },
                notes = snapshot.notes.takeIf { it.isNotBlank() }
            )

            // Convert drill drafts to domain models
            val drills = validDrills.map { d ->
                DrillEntry(
                    id = d.id,
                    sessionId = sessionId,
                    name = d.name.trim(),
                    rating = d.rating,
                    notes = d.notes
                )
            }

            // Persist session and drills
            runCatching { addSession(session, drills) }
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            saving = false,
                            error = e.message ?: "Save failed"
                        )
                    }
                }
        }
    }

    // Resets from back to initial state
    fun reset() {
        _state.value = AddSessionState()
    }

    // Cancels coroutines when viewModel is destroyed
    fun clear() = scope.cancel()
}