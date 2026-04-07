package com.example.todo.domain

import kotlinx.coroutines.flow.Flow

interface FilSorterRepository {
    suspend fun saveFilSorter(item: FilSorter)

    fun getAllFilSorters(): Flow<List<FilSorter>>
}
