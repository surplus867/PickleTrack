package com.example.shared

import com.example.shared.data.SessionRepositoryImpl
import com.example.shared.domain.AddSessionUseCase
import com.example.shared.domain.DeleteSessionUseCase
import com.example.shared.domain.GetBasicStatsUseCase
import com.example.shared.domain.GetSessionDetailUseCase
import com.example.shared.domain.ObserveSessionsUseCase
import com.example.shared.domain.SessionRepository
import com.example.shared.presentation.AddSessionViewModel
import com.example.shared.presentation.HomeViewModel
import com.example.shared.presentation.SessionDetailViewModel
import com.example.shared.db.PickleTrackDatabase

class AppContainer(db: PickleTrackDatabase) {

    // Repository implementation backed by SQLDelight database
    private val repo: SessionRepository = SessionRepositoryImpl(db)

    // ---- Domain use cases ----
    private val observeSessions = ObserveSessionsUseCase(repo)
    private val getStats = GetBasicStatsUseCase(repo)
    private val addSession = AddSessionUseCase(repo)
    private val getDetail = GetSessionDetailUseCase(repo)
    private val deleteSession = DeleteSessionUseCase(repo)

    // ---- ViewModel factories ----

    // Home screen ViewModel (session list + stats)
    fun homeViewModel(): HomeViewModel =
        HomeViewModel(observeSessions, getStats)

    // Add session screen ViewModel
    fun addSessionViewModel(): AddSessionViewModel = AddSessionViewModel(addSession)

    // Session detail screen ViewModel
    fun detailViewModel(): SessionDetailViewModel =
        SessionDetailViewModel(getDetail, deleteSession)
}