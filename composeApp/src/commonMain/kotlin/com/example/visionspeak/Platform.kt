package com.example.visionspeak

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform