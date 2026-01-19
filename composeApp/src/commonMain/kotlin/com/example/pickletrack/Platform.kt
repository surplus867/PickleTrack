package com.example.pickletrack

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform