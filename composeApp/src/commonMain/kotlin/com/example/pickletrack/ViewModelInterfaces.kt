package com.example.pickletrack

// Purpose: local ViewModel interfaces for the Compose UI so runtime adapters can provide shared ViewModels without a compile-time dependency.

import kotlinx.coroutines.flow.StateFlow

// Local ViewModel contracts used by the Compose app entrypoint.
interface HomeViewModel {
    val state: StateFlow<UiHomeState>
    fun start()
    fun clear()
}

// AddSessionViewModel used by the Add screen; minimal contract required by MainActivity (clear on dispose).
interface AddSessionViewModel {
    val state: StateFlow<UiAddSessionState>
    fun setLocation(v: String)
    fun setDuration(minutes: Int)
    fun setNotes(v: String)
    fun addDrillRow()
    fun updateDrillName(id: String, name: String)
    fun updateDrillRating(id: String, rating: Int)
    fun save()
    fun reset()
    fun clear()
}

// SessionDetailViewModel used by the detail screen: can load a specific id and be cleared
interface SessionDetailViewModel {
    fun load(id: String)
    fun clear()
}
