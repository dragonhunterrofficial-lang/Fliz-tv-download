package com.example.fliztv

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun currentEpochSeconds(): Long
expect fun currentIsoTimestamp(): String
expect fun currentIsoTimestampIST(): String