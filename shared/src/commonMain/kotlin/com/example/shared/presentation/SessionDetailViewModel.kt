package com.example.shared.presentation

import com.example.shared.domain.DeleteSessionUseCase
import com.example.shared.domain.GetSessionDetailUseCase
import com.example.shared.domain.SessionDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Session Detail screen.
 */
data class SessionDetailState(
    val isLoading: Boolean = true,
    val detail: SessionDetail? = null,
    val error: String? = null,
    val deleted: Boolean = false
)

/**
 * ViewModel for a single session detail screen.
 * - Loads session detail
 * - Deletes session
 * - Exposes state via StateFlow
 */
class SessionDetailViewModel(
    private val getDetail: GetSessionDetailUseCase,
    private val deleteSession: DeleteSessionUseCase
) {

    // Coroutine scope owned by this ViewModel
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Internal mutable state
    private val _state = MutableStateFlow(SessionDetailState())

    // Public read-only state
    val state: StateFlow<SessionDetailState> = _state.asStateFlow()

    /**
     * Loads session details by id.
     */
    fun load(id: String) {
        scope.launch {
            // Show loading
            _state.value = SessionDetailState(isLoading = true)

            runCatching { getDetail(id) }
                .onSuccess { detail ->
                    _state.value = SessionDetailState(
                        isLoading = false,
                        detail = detail
                    )
                }
                .onFailure { error ->
                    _state.value = SessionDetailState(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    /**
     * Deletes the session by id.
     */
    fun delete(id: String) {
        scope.launch {
            runCatching { deleteSession(id) }
                .onSuccess {
                    // Tell UI the session was deleted
                    _state.update { it.copy(deleted = true) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Clears coroutines when ViewModel is no longer needed.
     */
    fun clear() {
        scope.cancel()
    }
}