package com.example.pickletrack

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Wrapper for shared KMM DatabaseFactory using reflection
// Allows Android module to access shared database without direct dependency
class DatabaseFactory(private val context: Context) {

    // Create the shared DatabaseFactory via reflection so this module does not require
    // a compile-time dependency on the shared types. This keeps the wrapper decoupled.
    private val delegate: Any? = try {
        val clazz = Class.forName("com.example.shared.db.DatabaseFactory")
        try {
            // Prefer constructor(Context)
            clazz.getConstructor(Context::class.java).newInstance(context)
        } catch (e: Exception) {
            // Fallback to no-arg constructor
            try {
                clazz.getConstructor().newInstance()
            } catch (e2: Exception) {
                Log.w("SharedWrappers", "Shared DatabaseFactory exists but no suitable constructor", e2)
                null
            }
        }
    } catch (e: Exception) {
        Log.w("SharedWrappers", "Shared DatabaseFactory not found", e)
        null
    }

    // Call createDriver() on shared DatabaseFactory to create SQLDelight driver
    fun createDriver(): Any? = try {
        delegate?.javaClass?.getMethod("createDriver")?.invoke(delegate)
    } catch (e: Exception) {
        Log.w("SharedWrappers", "createDriver reflection failed", e)
        null
    }

    // Expose underlying shared instance for other wrappers
    internal fun delegateInstance(): Any? = delegate
}

// Wrapper for shared DatabaseProvider
// Responsible for providing database instance
class DatabaseProvider(private val factory: DatabaseFactory) {

    // Create DatabaseProvider via reflection so we don't require shared compile-time types here.
    private val providerDelegate: Any? = try {
        val providerClass = Class.forName("com.example.shared.db.DatabaseProvider")
        val factoryDelegate = factory.delegateInstance()
        if (factoryDelegate != null) {
            try {
                // Prefer constructor accepting the factory delegate's runtime class
                val ctor = providerClass.constructors.firstOrNull { c ->
                    val pts = c.parameterTypes
                    pts.size == 1 && pts[0].isAssignableFrom(factoryDelegate.javaClass)
                }
                if (ctor != null) ctor.newInstance(factoryDelegate) else providerClass.getConstructor().newInstance()
            } catch (e: Exception) {
                Log.w("SharedWrappers", "DatabaseProvider constructor invocation failed", e)
                try {
                    providerClass.getConstructor().newInstance()
                } catch (e2: Exception) {
                    Log.w("SharedWrappers", "DatabaseProvider no-arg constructor failed", e2)
                    null
                }
            }
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w("SharedWrappers", "DatabaseProvider not available via reflection", e)
        null
    }

    // Retrieve db property from provider (prefer direct typed access)
    val providerDb: Any? = try {
        providerDelegate?.javaClass?.getMethod("getDb")?.invoke(providerDelegate)
            ?: providerDelegate?.javaClass?.getField("db")?.get(providerDelegate)
    } catch (e: Exception) {
        try {
            providerDelegate?.javaClass?.getField("db")?.get(providerDelegate)
        } catch (e2: Exception) {
            Log.w("SharedWrappers", "provider.db reflection failed", e2)
            null
        }
    }
}

// Reflection helper methods

// Call method with no arguments
private fun callNoArgMethod(obj: Any?, vararg names: String): Any? {
    if (obj == null) return null
    for (n in names) {
        try {
            val m = obj.javaClass.methods.firstOrNull { it.name == n && it.parameterCount == 0 }
            if (m != null) return m.invoke(obj)
        } catch (_: Exception) {}
    }
    return null
}

// Read Boolean property - tries methods first, then direct field access
private fun boolProp(obj: Any?, vararg names: String): Boolean {
    val methodValue = callNoArgMethod(obj, *names)
    if (methodValue != null) {
        return when (methodValue) {
            is Boolean -> methodValue
            else -> false
        }
    }
    if (obj != null) {
        for (name in names) {
            try {
                val field = obj.javaClass.getDeclaredField(name)
                field.isAccessible = true
                val value = field.get(obj)
                if (value is Boolean) return value
            } catch (_: Exception) {}
        }
    }
    return false
}

// Read Long property - tries methods first, then direct field access
private fun longProp(obj: Any?, vararg names: String): Long {
    val methodValue = callNoArgMethod(obj, *names)
    if (methodValue != null) {
        return when (methodValue) {
            is Number -> methodValue.toLong()
            else -> 0L
        }
    }
    if (obj != null) {
        for (name in names) {
            try {
                val field = obj.javaClass.getDeclaredField(name)
                field.isAccessible = true
                val value = field.get(obj)
                if (value is Number) return value.toLong()
            } catch (_: Exception) {}
        }
    }
    return 0L
}

// Read String property - tries methods first, then direct field access
private fun stringProp(obj: Any?, vararg names: String): String? {
    val methodValue = callNoArgMethod(obj, *names)
    if (methodValue != null) {
        return methodValue as? String
    }
    if (obj != null) {
        for (name in names) {
            try {
                val field = obj.javaClass.getDeclaredField(name)
                field.isAccessible = true
                return field.get(obj) as? String
            } catch (_: Exception) {}
        }
    }
    return null
}

private fun listProp(obj: Any?, vararg names: String): List<Any?> {
    val v = callNoArgMethod(obj, *names) ?: return emptyList()
    return (v as? List<*>) ?: emptyList()
}

// Map shared HomeState -> Android UI state
private fun mapSharedStateToUi(obj: Any?): UiHomeState {
    if (obj == null) return UiHomeState()
    return try {
        val isLoading = boolProp(obj, "isLoading", "getIsLoading")

        // Map session list
        val sessionsRaw = listProp(obj, "getSessions", "sessions")
        val sessions = sessionsRaw.mapNotNull { item ->

            if (item == null) return@mapNotNull null

            val id = stringProp(item, "getId", "id") ?: return@mapNotNull null

            val duration = (callNoArgMethod(item, "getDurationMinutes", "durationMinutes") as? Number)?.toInt() ?: 0
            val location = stringProp(item, "getLocation", "location")
            UiPracticeSession(id = id, durationMinutes = duration, location = location)
        }

        // Stats - access the stats object first, then its properties
        val statsObj = callNoArgMethod(obj, "getStats", "stats")
        val minutesThisWeek = if (statsObj != null) {
            longProp(statsObj, "getMinutesThisWeek", "minutesThisWeek")
        } else 0L
        val totalSessions = if (statsObj != null) {
            longProp(statsObj, "getTotalSessions", "totalSessions")
        } else 0L
        
        Log.d("SharedWrappers", "Mapped ${sessions.size} sessions, statsObj=$statsObj, minutesThisWeek=$minutesThisWeek, totalSessions=$totalSessions")
        
        val error = stringProp(obj, "getError", "error")
        UiHomeState(
            isLoading = isLoading,
            sessions = sessions,
            minutesThisWeek = minutesThisWeek,
            totalSessions = totalSessions,
            error = error
        )
    } catch (e: Exception) {
        Log.w("SharedWrappers", "mapping shared state failed", e)
        UiHomeState()
    }
}
// Map shared AddSessionState -> Android UI State
private fun mapSharedAddStateToUi(obj: Any?): UiAddSessionState {
    if (obj == null) return UiAddSessionState()
    return try {
        val dateMillis = (callNoArgMethod(obj, "getDateMillis", "dateMillis") as? Number)?.toLong() ?: 0L
        val duration = (callNoArgMethod(obj, "getDurationMinutes", "durationMinutes") as? Number)?.toInt() ?: 60
        val location = stringProp(obj, "getLocation", "location") ?: ""
        val notes = stringProp(obj, "getNotes", "notes") ?: ""
        val saving = boolProp(obj, "isSaving", "getSaving", "saving")
        val saved = boolProp(obj, "isSaved", "getSaved", "saved")
        val error = stringProp(obj, "getError", "error")

        // Map drills list from shared state
        val drillsRaw = listProp(obj, "getDrills", "drills")
        val drills = drillsRaw.mapNotNull { item ->
            if (item == null) return@mapNotNull null
            val id = stringProp(item, "getId", "id") ?: return@mapNotNull null
            val name = stringProp(item, "getName", "name") ?: ""
            val rating = (callNoArgMethod(item, "getRating", "rating") as? Number)?.toInt() ?: 3
            val drillNotes = stringProp(item, "getNotes", "notes")
            UiDrillDraft(id = id, name = name, rating = rating, notes = drillNotes)
        }.ifEmpty { listOf(UiDrillDraft(id = java.util.UUID.randomUUID().toString())) }

        UiAddSessionState(
            dateMillis = dateMillis,
            durationMinutes = duration,
            location = location,
            notes = notes,
            drills = drills,
            saving = saving,
            saved = saved,
            error = error
        )
    } catch (e: Exception) {
        Log.w("SharedWrappers", "mapping shared add state failed", e)
        UiAddSessionState()
    }
}

    // Wrapper for shared AppContainer (Dependency container)
    class SharedAppContainer(private val provider: DatabaseProvider) {
        // Construct the shared AppContainer reflectively so this module does not require
        // a compile-time dependency on the shared AppContainer type.
        private val delegate: Any? = try {
            val sharedContainerClass = Class.forName("com.example.shared.AppContainer")
            val dbInstance = provider.providerDb
            if (dbInstance != null) {
                // Try to find a single-arg constructor assignable from dbInstance's runtime class
                val ctor = sharedContainerClass.constructors.firstOrNull { c ->
                    val pts = c.parameterTypes
                    pts.size == 1 && pts[0].isAssignableFrom(dbInstance.javaClass)
                }
                if (ctor != null) ctor.newInstance(dbInstance) else sharedContainerClass.getConstructor().newInstance()
            } else {
                sharedContainerClass.getConstructor().newInstance()
            }
        } catch (e: Exception) {
            Log.w("SharedWrappers", "Shared AppContainer not available", e)
            null
        }


    // HomeViewModel wrapper
    fun homeViewModel(): HomeViewModel {
        val sharedVm = try {
            delegate?.javaClass?.getMethod("homeViewModel")?.invoke(delegate)
        } catch (e: Exception) {
            Log.w("SharedWrappers", "homeViewModel() reflection failed", e)
            null
        }

        // Always maintain a local mutable state that is the source of truth for the UI
        val localState = MutableStateFlow(UiHomeState())

        // If shared VM exists and has a StateFlow, collect from it to sync to local state
        if (sharedVm != null) {
            try {
                val stateObj = callNoArgMethod(sharedVm, "getState", "state")
                if (stateObj != null) {
                    val stateInterface = Class.forName("kotlinx.coroutines.flow.StateFlow")
                    if (stateInterface.isInstance(stateObj)) {
                        @Suppress("UNCHECKED_CAST")
                        val sharedStateFlow = stateObj as StateFlow<Any?>
                        // Create a scope that lives as long as the returned object
                        val scope = CoroutineScope(Dispatchers.Main.immediate)
                        Log.d("SharedWrappers", "HomeViewModel: Starting to collect from shared VM")
                        scope.launch {
                            sharedStateFlow.collect { sharedState ->
                                val mapped = mapSharedStateToUi(sharedState)
                                Log.d("SharedWrappers", "HomeViewModel: Collected state with ${mapped.sessions.size} sessions, stats=${mapped.minutesThisWeek}min/${mapped.totalSessions}total")
                                localState.value = mapped
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SharedWrappers", "Failed to collect from shared Home VM state", e)
            }
        }

        // return Android wrapper ViewModel
        return object : HomeViewModel {
            override val state: StateFlow<UiHomeState> = localState

            override fun start() {
                try {
                    sharedVm?.javaClass?.getMethod("start")?.invoke(sharedVm)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "homeViewModel.start() failed", e)
                }
            }

            override fun clear() {
                try {
                    sharedVm?.javaClass?.getMethod("clear")?.invoke(sharedVm)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "homeViewModel.clear() failed", e)
                }
            }
        }
    }

    // Create an Android-side AddSessionViewModel wrapper that forwards to the shared KMM viewmodel via reflection.
    // Behavior summary:
    // 1) Attempt to reflectively obtain the shared AddSessionViewModel instance from the shared AppContainer delegate.
    // 2) If the shared VM exposes a StateFlow, map its values to the local UiAddSessionState so Compose UI can collect a safe, typed state.
    // 3) Provide a fallback MutableStateFlow(UiAddSessionState()) when reflection isn't available so the app still runs.
    // 4) Return an object implementing the local AddSessionViewModel interface that forwards calls (save/reset/clear and setters) to the shared VM when possible.
    fun addSessionViewModel(): AddSessionViewModel {
        val sharedVm = try {
            delegate?.javaClass?.getMethod("addSessionViewModel")?.invoke(delegate)
        } catch (e: Exception) {
            Log.w("SharedWrappers", "addSessionViewModel() reflection failed", e)
            null
        }

        // Always maintain a local mutable state that is the source of truth for the UI
        val localState = MutableStateFlow(UiAddSessionState())

        // If shared VM exists and has a StateFlow, collect from it to sync to local state
        if (sharedVm != null) {
            try {
                val stateObj = callNoArgMethod(sharedVm, "getState", "state")
                if (stateObj != null) {
                    val stateInterface = Class.forName("kotlinx.coroutines.flow.StateFlow")
                    if (stateInterface.isInstance(stateObj)) {
                        @Suppress("UNCHECKED_CAST")
                        val sharedStateFlow = stateObj as StateFlow<Any?>
                        // Create a scope that lives as long as the returned object
                        val scope = CoroutineScope(Dispatchers.Main.immediate)
                        scope.launch {
                            sharedStateFlow.collect { sharedState ->
                                localState.value = mapSharedAddStateToUi(sharedState)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SharedWrappers", "Failed to collect from shared VM state", e)
            }
        }

        // Return an adapter that implements the Android-local AddSessionViewModel interface.
        // All methods attempt reflection calls on the underlying shared VM and update localState when needed.
        return object : AddSessionViewModel {
            override val state: StateFlow<UiAddSessionState> = localState

            // Forward location changes to shared VM. Always update local state for immediate UI feedback.
            override fun setLocation(v: String) {
                try {
                    sharedVm?.javaClass?.getMethod("setLocation", String::class.java)
                        ?.invoke(sharedVm, v)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.setLocation() reflection failed, using local", e)
                }
                // Always update local state for immediate UI feedback
                localState.value = localState.value.copy(location = v)
            }

            // Forward duration changes to shared VM. Always update local state for immediate UI feedback.
            override fun setDuration(minutes: Int) {
                try {
                    try {
                        // try primitive int first
                        sharedVm?.javaClass?.getMethod("setDuration", Int::class.javaPrimitiveType!!)
                            ?.invoke(sharedVm, minutes)
                    } catch (_: Exception) {
                        // fallback to boxed Integer
                        sharedVm?.javaClass?.getMethod("setDuration", Int::class.javaObjectType!!)
                            ?.invoke(sharedVm, minutes)
                    }
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.setDuration() reflection failed, using local", e)
                }
                // Always update local state for immediate UI feedback
                localState.value = localState.value.copy(durationMinutes = minutes.coerceAtLeast(1))
            }

            // Forward notes changes to shared VM. Always update local state for immediate UI feedback.
            override fun setNotes(v: String) {
                try {
                    sharedVm?.javaClass?.getMethod("setNotes", String::class.java)
                        ?.invoke(sharedVm, v)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.setNotes() reflection failed, using local", e)
                }
                // Always update local state for immediate UI feedback
                localState.value = localState.value.copy(notes = v)
            }

            // Forward addDrillRow to shared VM. Always update local state for immediate UI feedback.
            override fun addDrillRow() {
                var usedShared = false
                if (sharedVm != null) {
                    try {
                        sharedVm.javaClass.getMethod("addDrillRow")?.invoke(sharedVm)
                        usedShared = true

                        // Attempt to read back the shared VM state so localState reflects the newly added drill (with the shared id).
                        try {
                            val stateObj = callNoArgMethod(sharedVm, "getState", "state")
                            if (stateObj != null) {
                                // If the shared VM exposes a StateFlow, read its current value via reflection.
                                val stateInterface = Class.forName("kotlinx.coroutines.flow.StateFlow")
                                val actualState = if (stateInterface.isInstance(stateObj)) {
                                    // call getValue/value on the StateFlow to obtain the current AddSessionState
                                    callNoArgMethod(stateObj, "getValue", "value")
                                } else {
                                    // stateObj is already the state object
                                    stateObj
                                }

                                if (actualState != null) {
                                    localState.value = mapSharedAddStateToUi(actualState)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("SharedWrappers", "addDrillRow: failed to refresh shared state after addDrillRow", e)
                        }
                    } catch (e: Exception) {
                        Log.w("SharedWrappers", "addSessionViewModel.addDrillRow() reflection failed, using local", e)
                    }
                }

                // Only append a local drill when the shared VM was not called or not present.
                if (!usedShared) {
                    val current = localState.value
                    val newDrill = UiDrillDraft(id = java.util.UUID.randomUUID().toString())
                    localState.value = current.copy(drills = current.drills + newDrill)
                }
            }

            // Forward updateDrillName to shared VM. Always update local state for immediate UI feedback.
            override fun updateDrillName(id: String, name: String) {
                try {
                    sharedVm?.javaClass?.getMethod("updateDrillName", String::class.java, String::class.java)
                        ?.invoke(sharedVm, id, name)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.updateDrillName() reflection failed, using local", e)
                }
                // Always update local state for immediate UI feedback
                localState.value = localState.value.copy(drills = localState.value.drills.map { d ->
                    if (d.id == id) d.copy(name = name) else d
                })
            }

            // Forward updateDrillRating to shared VM. Always update local state for immediate UI feedback.
            override fun updateDrillRating(id: String, rating: Int) {
                try {
                    try {
                        // try primitive int first
                        sharedVm?.javaClass?.getMethod("updateDrillRating", String::class.java, Int::class.javaPrimitiveType!!)
                            ?.invoke(sharedVm, id, rating)
                    } catch (_: Exception) {
                        // fallback to boxed Integer
                        sharedVm?.javaClass?.getMethod("updateDrillRating", String::class.java, Int::class.javaObjectType!!)
                            ?.invoke(sharedVm, id, rating)
                    }
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.updateDrillRating() reflection failed, using local", e)
                }
                // Always update local state for immediate UI feedback
                localState.value = localState.value.copy(drills = localState.value.drills.map { d ->
                    if (d.id == id) d.copy(rating = rating.coerceIn(1, 5)) else d
                })
            }

            // Forward save to shared VM. If a shared VM exists, push the latest local state into
            // the shared VM (location, duration, notes, drill names/ratings) before calling save()
            // to avoid divergence if earlier setter reflection calls failed. If no shared VM is
            // present, we emulate a local save.
            override fun save() {
                try {
                    if (sharedVm != null) {
                        Log.d("SharedWrappers", "Saving: pushing localState to shared VM. drills=${localState.value.drills.map { it.id + ":" + it.name }}")
                        // Push basic fields
                        val ls = localState.value
                        try {
                            sharedVm.javaClass.getMethod("setLocation", String::class.java)
                                ?.invoke(sharedVm, ls.location)
                        } catch (_: Exception) {}

                        try {
                            try {
                                sharedVm.javaClass.getMethod("setDuration", Int::class.javaPrimitiveType!!)
                                    ?.invoke(sharedVm, ls.durationMinutes)
                            } catch (_: Exception) {
                                sharedVm.javaClass.getMethod("setDuration", Int::class.javaObjectType!!)
                                    ?.invoke(sharedVm, ls.durationMinutes)
                            }
                        } catch (_: Exception) {}

                        try {
                            sharedVm.javaClass.getMethod("setNotes", String::class.java)
                                ?.invoke(sharedVm, ls.notes)
                        } catch (_: Exception) {}

                        // Push drills (update name and rating by id). These will silently fail if ids
                        // don't match the shared VM's drills but will succeed when ids align.
                        ls.drills.forEach { d ->
                            try {
                                Log.d("SharedWrappers", "Pushing drill name -> id=${d.id}, name='${d.name}'")
                                sharedVm.javaClass.getMethod("updateDrillName", String::class.java, String::class.java)
                                    .invoke(sharedVm, d.id, d.name)
                                Log.d("SharedWrappers", "updateDrillName succeeded for id=${d.id}")
                            } catch (e: Exception) {
                                Log.w("SharedWrappers", "updateDrillName failed for id=${d.id}", e)
                            }
                            try {
                                try {
                                    sharedVm.javaClass.getMethod("updateDrillRating", String::class.java, Int::class.javaPrimitiveType!!)
                                        .invoke(sharedVm, d.id, d.rating)
                                    Log.d("SharedWrappers", "updateDrillRating (primitive) succeeded for id=${d.id}")
                                } catch (e1: Exception) {
                                    try {
                                        sharedVm.javaClass.getMethod("updateDrillRating", String::class.java, Int::class.javaObjectType!!)
                                            .invoke(sharedVm, d.id, d.rating)
                                        Log.d("SharedWrappers", "updateDrillRating (boxed) succeeded for id=${d.id}")
                                    } catch (e2: Exception) {
                                        Log.w("SharedWrappers", "updateDrillRating failed for id=${d.id}", e2)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    // Finally call save on the shared VM (if present)
                    sharedVm?.javaClass?.getMethod("save")?.invoke(sharedVm)

                    // If there's no shared VM, emulate a local save so the UI completes the flow.
                    if (sharedVm == null) {
                        localState.value = localState.value.copy(saving = false, saved = true, error = null)
                    }
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.save() failed", e)
                    // If save failed while sharedVm exists, surface an error in local state so the UI can show it
                    if (sharedVm != null) {
                        localState.value = localState.value.copy(saving = false, error = e.message ?: "Save failed")
                    } else {
                        // Fallback: emulate a save so the UI flow completes
                        localState.value = localState.value.copy(saving = false, saved = true, error = null)
                    }
                }
            }

            // Forward reset to shared VM. If reflection fails, reset local state.
            override fun reset() {
                try {
                    sharedVm?.javaClass?.getMethod("reset")?.invoke(sharedVm)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.reset() failed", e)
                    localState.value = UiAddSessionState()
                }
            }

            // Forward clear to shared VM.
            override fun clear() {
                try {
                    sharedVm?.javaClass?.getMethod("clear")?.invoke(sharedVm)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.clear() failed", e)
                    // Nothing else to do
                }
            }
        }
    }

    // Create an Android-side SessionDetailViewModel adapter which forwards calls to the shared VM via reflection.
    // Purpose: allow the Android Compose UI to request loading a session detail and clear the VM without a compile-time
    // dependency on the shared viewmodel type. Methods fail silently with a log if the shared VM or methods are absent.
    fun detailViewModel(id: String): SessionDetailViewModel {
        val sharedVm = try {
            // Try to obtain the shared detailViewModel instance from the shared AppContainer delegate
            delegate?.javaClass?.getMethod("detailViewModel")?.invoke(delegate)
        } catch (e: Exception) {
            Log.w("SharedWrappers", "detailViewModel() reflection failed", e)
            null
        }

        // Return an object implementing the local SessionDetailViewModel interface.
        // The object's methods call into the reflected shared VM when available.
        return object : SessionDetailViewModel {
            // Load a specific session id by calling the shared VM's `load(String)` method via reflection.
            override fun load(id: String) {
                try {
                    sharedVm?.javaClass?.getMethod("load", String::class.java)?.invoke(sharedVm, id)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "detailViewModel.load() failed", e)
                }
            }

            // Clear resources / cancel coroutines on the shared VM by calling its `clear()` method.
            override fun clear() {
                try {
                    sharedVm?.javaClass?.getMethod("clear")?.invoke(sharedVm)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "detailViewModel.clear() failed", e)
                }
            }
        }
    }
}
