package com.example.todo.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CalendarSource(
    @PrimaryKey(autoGenerate = true) val id: Int
)