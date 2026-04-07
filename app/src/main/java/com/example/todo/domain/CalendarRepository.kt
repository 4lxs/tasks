package com.example.todo.domain

import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    suspend fun saveEvent(event: Event)
    fun getAllEvents(): Flow<List<Event>>

    suspend fun saveTask(item: Task)

    fun getAllTasks(): Flow<List<Task>>

    suspend fun getTask(id: Long): Task

    suspend fun fetch()
}
