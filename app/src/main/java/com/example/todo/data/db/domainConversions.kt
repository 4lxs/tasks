package com.example.todo.data.db

import com.example.todo.domain.Event
import com.example.todo.domain.FilSorter
import com.example.todo.domain.Reminder
import com.example.todo.domain.Tag
import com.example.todo.domain.Task
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

fun FullTaskEntity.toDomain(): Task = Task(
    title = item.title,
    checked = item.checked,
    dueDate = item.dueDate?.let { LocalDate.ofEpochDay(it) },
    dueTime = item.dueTime?.let { LocalTime.ofSecondOfDay(it.toLong()) },
    notes = item.notes,
    priority = item.priority,
    reminders = reminders.map { it.toDomain() },
    recurrences = item.recurring?.let { RecurrenceRule(it) },
    tags = tags.asSequence().map { it.toDomain() }.toSet(),
    archived = item.archived,
    id = item.id,
)

fun ReminderEntity.toDomain(): Reminder = Reminder(
    at = LocalDateTime.ofEpochSecond(time, 0, ZoneOffset.UTC),
    id = id,
)

fun TagEntity.toDomain(): Tag = Tag(
    name = name,
    icon = icon,
    id = id,
)

fun Task.toTodoItemEntity(): TaskEntity = TaskEntity(
    title = title,
    checked = checked,
    dueDate = dueDate?.toEpochDay(),
    dueTime = dueTime?.toSecondOfDay(),
    notes = notes,
    priority = priority,
    recurring = recurrences?.toString(),
    archived = archived,
    id = id
)

fun Task.toReminderEntities(): List<ReminderEntity> =
    reminders.map { it.toTaskEntity(itemId = id) }

fun Task.toTagCrossRefs(): List<ItemTagCrossRef> =
    tags.map { ItemTagCrossRef(taskId = id, tagId = it.id) }

fun FullEventEntity.toDomain(): Event = Event(
    title = event.title,
    notes = event.notes,
    reminders = reminders.map { it.toDomain() },
    recurrences = event.recurrences?.let { RecurrenceRule(it) },
    tags = tags.asSequence().map { it.toDomain() }.toSet(),
    archived = event.archived,
    id = event.id,
    start = LocalDateTime.ofEpochSecond(event.start, 0, ZoneOffset.UTC),
    end = LocalDateTime.ofEpochSecond(event.end, 0, ZoneOffset.UTC),
    allDay = event.allDay,
)

fun Event.toEventEntity(): EventEntity = EventEntity(
    title = title,
    notes = notes,
    recurrences = recurrences?.toString(),
    archived = archived,
    id = id,
    start = start.toEpochSecond(ZoneOffset.UTC),
    end = end.toEpochSecond(ZoneOffset.UTC),
    allDay = allDay,
)

fun Event.toReminderEntities(): List<ReminderEntity> =
    reminders.map { it.toEventEntity(eventId = id) }

fun Event.toTagCrossRefs(): List<EventTagCrossRef> =
    tags.map { EventTagCrossRef(eventId = id, tagId = it.id) }

fun Reminder.toTaskEntity(itemId: Long): ReminderEntity = ReminderEntity(
    taskId = itemId,
    time = at.toEpochSecond(ZoneOffset.UTC),
    id = id,
    eventId = null,
)

fun Reminder.toEventEntity(eventId: Long): ReminderEntity = ReminderEntity(
    eventId = eventId,
    time = at.toEpochSecond(ZoneOffset.UTC),
    id = id,
    taskId = null,
)

fun Tag.toTaskEntity(): TagEntity = TagEntity(name = name, icon = icon, id = id)

fun FilSorter.toEntity(): FilSorterEntity = FilSorterEntity(
    name = name,
    icon = icon,
    tags = tags.joinToString(",") { it.toString() },
    grouping = grouping,
    sorting = sorting,
    completionSort = completionSort,
    id = id,
)

fun FilSorterEntity.toDomain(): FilSorter = FilSorter(
    name = name,
    icon = icon,
    tags = if (tags.isEmpty()) mutableSetOf() else tags.split(",")
        .mapTo(mutableSetOf()) { it.toLong() },
    grouping = grouping,
    sorting = sorting,
    completionSort = completionSort,
    id = id,
)
