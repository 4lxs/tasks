package com.example.todo.domain

import kotlinx.coroutines.flow.Flow

interface TagRepository {
    suspend fun saveTag(tag: Tag)

    fun getAllTags(): Flow<List<Tag>>
}
