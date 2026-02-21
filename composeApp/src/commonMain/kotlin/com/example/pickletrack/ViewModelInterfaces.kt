package com.example.pickletrack

// Common interface for simple ViewModel lifecycle used by the Android entrypoint.
// Placing this in commonMain makes it visible to both UI (common) and platform adapters (androidMain).
interface HomeViewModel {
    fun start()
    fun clear()
}

// AddSessionViewModel used by the Add screen; minimal contract required by MainActivity (clear on dispose).
interface AddSessionViewModel {
    fun save()
    fun reset()
    fun clear()
}

// SessionDetailViewModel used by the detail screen: can load a specific id and be cleared
interface SessionDetailViewModel {
    fun load(id: String)
    fun clear()
}
