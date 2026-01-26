package com.example.shared.domain

import kotlin.Long

data class Drill(
    val id: String,
    val sessionId: String,
    val name: String,
    val rating: Long,
    val notes: String?
)
