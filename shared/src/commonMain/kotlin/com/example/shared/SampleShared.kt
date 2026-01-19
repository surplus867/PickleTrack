package com.example.shared

expect class Platform() {
    fun name(): String
}

class Greeting {
    fun greeting(): String = "Hello from shared module on ${Platform().name()}"
}
