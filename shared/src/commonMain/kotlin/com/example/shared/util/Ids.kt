package com.example.shared.util

import kotlin.random.Random

object Ids {
    // Simple UUID-ish (good enough for MVP). You can replace with real UUID lib later.
    fun newId(): String {
        val part1 = Random.nextLong().toString(16)
        val part2 = Random.nextLong().toString(16)
        return (part1 + part2).replace("-", "")
    }
}