package org.better.urn

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform