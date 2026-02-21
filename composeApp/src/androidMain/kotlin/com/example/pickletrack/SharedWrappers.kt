package com.example.pickletrack

import android.content.Context
import android.util.Log

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

// Runtime wrapper for com.example.shared.AppContainer
class AppContainer(private val provider: DatabaseProvider) {
    private val delegate: Any? = try {
        // If provider.providerDb is a com.example.shared.db.PickleTrackDatabase instance, try to pass it
        val sharedContainerClass = Class.forName("com.example.shared.AppContainer")
        val dbInstance = provider.providerDb
        if (dbInstance != null) {
            try {
                sharedContainerClass.getConstructor(dbInstance.javaClass).newInstance(dbInstance)
            } catch (e: Exception) {
                // fallback to any single-arg constructor attempt
                try {
                    val ctor = sharedContainerClass.constructors.firstOrNull { it.parameterTypes.size == 1 }
                    ctor?.newInstance(dbInstance)
                } catch (e2: Exception) {
                    Log.w("SharedWrappers", "AppContainer constructor(db) failed", e2)
                    null
                }
            }
        } else {
            // try no-arg constructor
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

    // Expose a HomeViewModel adapter that delegates to shared homeViewModel instance if available
    fun homeViewModel(): HomeViewModel {
        val sharedVm = try {
            delegate?.javaClass?.getMethod("homeViewModel")?.invoke(delegate)
        } catch (e: Exception) {
            Log.w("SharedWrappers", "homeViewModel() reflection failed", e)
            null
        }

        return object : HomeViewModel {
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

    // Expose an AddSessionViewModel adapter that delegates to shared addSessionViewModel instance if available
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

    // Expose a detail view model adapter; the shared container exposes detailViewModel(): SessionDetailViewModel
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
