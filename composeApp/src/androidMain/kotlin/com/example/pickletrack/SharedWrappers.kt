package com.example.pickletrack

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

// Runtime wrapper for com.example.shared.db.DatabaseFactory
class DatabaseFactory(private val context: Context) {
    private val delegate: Any? = try {
        val clazz = Class.forName("com.example.shared.db.DatabaseFactory")
        // try constructor(Context)
        clazz.getConstructor(android.content.Context::class.java).newInstance(context)
    } catch (e: Exception) {
        Log.w("SharedWrappers", "Shared DatabaseFactory not found", e)
        null
    }

    // Attempt to call createDriver on delegate, returns Any? (SqlDriver) or null
    fun createDriver(): Any? = try {
        delegate?.javaClass?.getMethod("createDriver")?.invoke(delegate)
    } catch (e: Exception) {
        Log.w("SharedWrappers", "createDriver reflection failed", e)
        null
    }

    // Expose delegate for other reflective wrappers
    internal fun delegateInstance(): Any? = delegate
}

// Runtime wrapper for com.example.shared.db.DatabaseProvider
class DatabaseProvider(private val factory: DatabaseFactory) {
    val db: Any? = try {
        // try to construct com.example.shared.db.DatabaseProvider(factoryDelegate)
        val providerClass = Class.forName("com.example.shared.db.DatabaseProvider")
        val factoryDelegate = factory.delegateInstance()
        if (factoryDelegate != null) {
            try {
                providerClass.getConstructor(factoryDelegate.javaClass).newInstance(factoryDelegate)
            } catch (e: Exception) {
                // try constructor with Any / no specific param
                providerClass.getConstructor().newInstance()
            }
        } else {
            // fallback: maybe provider has constructor taking SqlDriver -> but we don't have driver
            null
        }
    } catch (e: Exception) {
        Log.w("SharedWrappers", "DatabaseProvider not available via reflection", e)
        null
    }

    // Try to get the `db` field or property from provider
    val providerDb: Any? = try {
        db?.javaClass?.getMethod("getDb")?.invoke(db) ?: db?.javaClass?.getField("db")?.get(db)
    } catch (e: Exception) {
        try {
            db?.javaClass?.getField("db")?.get(db)
        } catch (e2: Exception) {
            Log.w("SharedWrappers", "provider.db reflection failed", e2)
            null
        }
    }
}

// Reflection helpers
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

private fun boolProp(obj: Any?, vararg names: String): Boolean {
    val v = callNoArgMethod(obj, *names) ?: return false
    return when (v) {
        is Boolean -> v
        else -> false
    }
}

private fun longProp(obj: Any?, vararg names: String): Long {
    val v = callNoArgMethod(obj, *names) ?: return 0L
    return when (v) {
        is Number -> v.toLong()
        else -> 0L
    }
}

private fun stringProp(obj: Any?, vararg names: String): String? {
    val v = callNoArgMethod(obj, *names) ?: return null
    return v as? String
}

private fun listProp(obj: Any?, vararg names: String): List<Any?> {
    val v = callNoArgMethod(obj, *names) ?: return emptyList()
    return (v as? List<*>) ?: emptyList()
}

private fun mapSharedStateToUi(obj: Any?): UiHomeState {
    if (obj == null) return UiHomeState()
    return try {
        val isLoading = boolProp(obj, "isLoading", "getIsLoading")
        val sessionsRaw = listProp(obj, "getSessions", "sessions")
        val sessions = sessionsRaw.mapNotNull { item ->
            if (item == null) return@mapNotNull null
            val id = stringProp(item, "getId", "id") ?: return@mapNotNull null
            val duration = (callNoArgMethod(item, "getDurationMinutes", "durationMinutes") as? Number)?.toInt() ?: 0
            val location = stringProp(item, "getLocation", "location")
            UiPracticeSession(id = id, durationMinutes = duration, location = location)
        }
        val minutesThisWeek = longProp(callNoArgMethod(obj, "getStats", "stats"), "getMinutesThisWeek", "minutesThisWeek")
        val totalSessions = longProp(callNoArgMethod(obj, "getStats", "stats"), "getTotalSessions", "totalSessions")
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

// Runtime wrapper for com.example.shared.AppContainer
class AppContainer(private val provider: DatabaseProvider) {
    private val delegate: Any? = try {
        val sharedContainerClass = Class.forName("com.example.shared.AppContainer")
        val dbInstance = provider.providerDb
        if (dbInstance != null) {
            try {
                sharedContainerClass.getConstructor(dbInstance.javaClass).newInstance(dbInstance)
            } catch (e: Exception) {
                try {
                    val ctor = sharedContainerClass.constructors.firstOrNull { it.parameterTypes.size == 1 }
                    ctor?.newInstance(dbInstance)
                } catch (e2: Exception) {
                    Log.w("SharedWrappers", "AppContainer constructor(db) failed", e2)
                    null
                }
            }
        } else {
            try {
                sharedContainerClass.getConstructor().newInstance()
            } catch (e: Exception) {
                Log.w("SharedWrappers", "AppContainer no-arg constructor failed", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.w("SharedWrappers", "Shared AppContainer not available", e)
        null
    }

    fun homeViewModel(): HomeViewModel {
        val sharedVm = try {
            delegate?.javaClass?.getMethod("homeViewModel")?.invoke(delegate)
        } catch (e: Exception) {
            Log.w("SharedWrappers", "homeViewModel() reflection failed", e)
            null
        }

        // Try to read a StateFlow from the shared vm and map it to UiHomeState. If not available, use a fallback.
        val stateFlow: StateFlow<UiHomeState> = try {
            val stateObj = callNoArgMethod(sharedVm, "getState", "state")
            if (stateObj != null) {
                val stateInterface = Class.forName("kotlinx.coroutines.flow.StateFlow")
                if (stateInterface.isInstance(stateObj)) {
                    @Suppress("UNCHECKED_CAST")
                    val sharedStateFlow = stateObj as kotlinx.coroutines.flow.StateFlow<Any?>
                    val scope = CoroutineScope(Dispatchers.Main.immediate)
                    sharedStateFlow.map { s -> mapSharedStateToUi(s) }
                        .stateIn(scope, SharingStarted.Eagerly, UiHomeState())
                } else {
                    MutableStateFlow(UiHomeState())
                }
            } else {
                MutableStateFlow(UiHomeState())
            }
        } catch (e: Exception) {
            Log.w("SharedWrappers", "accessing sharedVm.state failed", e)
            MutableStateFlow(UiHomeState())
        }

        return object : HomeViewModel {
            override val state: StateFlow<UiHomeState> = stateFlow

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

    fun addSessionViewModel(): AddSessionViewModel {
        val sharedVm = try {
            delegate?.javaClass?.getMethod("addSessionViewModel")?.invoke(delegate)
        } catch (e: Exception) {
            Log.w("SharedWrappers", "addSessionViewModel() reflection failed", e)
            null
        }

        return object : AddSessionViewModel {
            override fun save() {
                try {
                    sharedVm?.javaClass?.getMethod("save")?.invoke(sharedVm)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.save() failed", e)
                }
            }

            override fun reset() {
                try {
                    sharedVm?.javaClass?.getMethod("reset")?.invoke(sharedVm)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.reset() failed", e)
                }
            }

            override fun clear() {
                try {
                    sharedVm?.javaClass?.getMethod("clear")?.invoke(sharedVm)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "addSessionViewModel.clear() failed", e)
                }
            }
        }
    }

    fun detailViewModel(id: String): SessionDetailViewModel {
        val sharedVm = try {
            delegate?.javaClass?.getMethod("detailViewModel")?.invoke(delegate)
        } catch (e: Exception) {
            Log.w("SharedWrappers", "detailViewModel() reflection failed", e)
            null
        }

        return object : SessionDetailViewModel {
            override fun load(id: String) {
                try {
                    sharedVm?.javaClass?.getMethod("load", String::class.java)?.invoke(sharedVm, id)
                } catch (e: Exception) {
                    Log.w("SharedWrappers", "detailViewModel.load() failed", e)
                }
            }

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
