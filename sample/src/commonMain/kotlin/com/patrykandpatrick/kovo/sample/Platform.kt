package com.patrykandpatrick.kovo.sample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
