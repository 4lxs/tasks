package com.example.todo.domain

enum class CalendarSourceType {
    WiseTT,
    Google
}

sealed interface CalendarSource {
    val id: Int
    val title: String

    data class WiseTTSource(
        override val id: Int = 0,
        override val title: String = "",
        val url: String = "",
    ) : CalendarSource
}