package com.example.todo.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.todo.domain.Grouping
import com.example.todo.domain.Priority
import com.example.todo.domain.Sorting
import com.example.todo.ui.CompletionSort

@Entity
data class TaskEntity(
    val title: String,
    val checked: Boolean,
    val dueDate: Long?, // epochdays
    val dueTime: Int?, // sec of day
    val notes: String,
    val priority: Priority,
    val recurring: String?, // rfc 5545 rrule
    val archived: Boolean,
    @PrimaryKey(autoGenerate = true) val id: Long,
)

@Entity
data class EventEntity(
    val title: String,
    val start: Long, // epoch
    val end: Long, // epoch
    val allDay: Boolean,
    val recurrences: String? = null,
    val notes: String = "",
    val archived: Boolean = false,
    val externalId: String = "",
    @PrimaryKey(autoGenerate = true) val id: Long,
)

@Entity
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val time: Long,
    val taskId: Long?,
    val eventId: Long?,
)

@Entity
data class TagEntity(
    val name: String,
    val icon: String,
    @PrimaryKey(autoGenerate = true) val id: Long,
)

@Entity(primaryKeys = ["taskId", "tagId"])
data class ItemTagCrossRef(
    val taskId: Long,
    val tagId: Long,
)

@Entity(primaryKeys = ["eventId", "tagId"])
data class EventTagCrossRef(
    val eventId: Long,
    val tagId: Long,
)

data class FullTaskEntity(
    @Embedded val item: TaskEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "taskId",
    )
    val reminders: List<ReminderEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            ItemTagCrossRef::class,
            parentColumn = "taskId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
)

data class FullEventEntity(
    @Embedded val event: EventEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "eventId",
    )
    val reminders: List<ReminderEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            EventTagCrossRef::class,
            parentColumn = "eventId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
)

@Entity
data class FilSorterEntity(
    val name: String,
    val icon: Int,
    val tags: String, // comma separated tag ids
    val grouping: Grouping,
    val sorting: Sorting,
    val completionSort: CompletionSort,
    @PrimaryKey(autoGenerate = true) val id: Long,
)
