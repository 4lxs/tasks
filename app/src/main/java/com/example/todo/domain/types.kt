package com.example.todo.domain

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.todo.R
import com.example.todo.domain.Grouping.None
import com.example.todo.domain.Sorting.DueDate
import com.example.todo.domain.Sorting.Smart
import com.example.todo.ui.CompletionSort
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class CalInstance(
    val date: LocalDate,
    val item: Item,
    val allDay: Boolean,
    val itemStart: LocalDateTime,
    val itemEnd: LocalDateTime,
    val multiDayItem: Boolean, // > 2
    val dayStart: LocalTime,
    val dayEnd: LocalTime,
    val firstDay: Boolean,
    val lastDay: Boolean,
    val id: String,
)

sealed interface Item {

    val title: String
    val notes: String
    val reminders: List<Reminder>
    val recurrences: RecurrenceRule?
    val tags: Set<Tag>
    val archived: Boolean
    val originalId: Long? // id of original item. this is only non-null if this is an item generated

    // with a recurrance. originalId is the event that generated it
    val id: Long
}

data class Task(
    override val title: String = "",
    val checked: Boolean = false,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    override val notes: String = "",
    val priority: Priority = Priority.P1,
    override val reminders: List<Reminder> = listOf(),
    override val recurrences: RecurrenceRule? = null,
    override val tags: Set<Tag> = setOf(),
    override val archived: Boolean = false,
    override val originalId: Long? = null,
    override val id: Long = 0
) : Item {
    fun toDayInstance(): CalInstance? {
        if (dueDate == null) return null

        return CalInstance(
            date = dueDate,
            item = this,
            allDay = dueTime == null,
            firstDay = true,
            lastDay = true,
            itemStart = (dueTime ?: LocalTime.MIN).atDate(dueDate),
            itemEnd = (dueTime ?: LocalTime.MAX).atDate(dueDate),
            multiDayItem = false,
            dayStart = dueTime ?: LocalTime.MIN,
            dayEnd = dueTime ?: LocalTime.MAX,
            id = "task_$id",
        )
    }
}

data class Event(
    override val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean = false,
    override val reminders: List<Reminder> = listOf(),
    override val recurrences: RecurrenceRule? = null,
    override val tags: Set<Tag> = setOf(),
    override val notes: String = "",
    override val archived: Boolean = false,
    override val originalId: Long? = null,
    override val id: Long = 0,
) : Item {
    fun toDayInstances(): List<CalInstance> {
        val ndays = ChronoUnit.DAYS.between(start, end)
        return (0..ndays).map { i ->
            val date = start.toLocalDate().plusDays(i)
            val firstDay = date == start.toLocalDate()
            val lastDay = date == end.toLocalDate()

            CalInstance(
                date = date,
                item = this,
                allDay = allDay || (!firstDay && !lastDay),
                firstDay = firstDay,
                lastDay = lastDay,
                itemStart = start,
                itemEnd = end,
                multiDayItem = ndays > 1,
                dayStart = if (firstDay) start.toLocalTime() else LocalTime.MIN,
                dayEnd = if (lastDay) end.toLocalTime() else LocalTime.MAX,
                id = "event_${id}_$i"
            )
        }
    }
}

data class Reminder(
    val at: LocalDateTime,
    val id: Long = 0,
)

data class Tag(
    val name: String,
    val icon: String = "tag",
    val selected: Boolean = false,
    val id: Long = 0,
)

enum class Priority(val color: Color) {
    P0(Color.Gray),
    P1(Color.White),
    P2(Color.Yellow),
    P3(Color.Red);
}

data class FilSorter(
    val name: String = "",
    val icon: Int = R.drawable.outline_filter_alt_24,
    val tags: Set<Long> = setOf(), // tag ids. empty == everything selected
    val grouping: Grouping = Grouping.DueDate,
    val sorting: Sorting = Sorting.Smart,
    val completionSort: CompletionSort = CompletionSort.Hide,
    val id: Long = 0,
) {
    fun keep(item: Item): Boolean {
        val unfilteredTags = tags.isEmpty() || item.tags.any { tags.contains(it.id) }
        val filterChecked = completionSort == CompletionSort.Hide && item is Task && item.checked
        return unfilteredTags && !filterChecked
    }

    fun toggleTag(tag: Tag): FilSorter = this.copy(
        tags = tags.toggle(tag.id)
    )

    fun focusTag(tag: Tag): FilSorter = this.copy(tags = setOf(tag.id))

    fun clearTags(): FilSorter = this.copy(tags = setOf())

    fun getComparator(): Comparator<Task> {
        val comparator = when (sorting) {
            Smart -> compareBy<Task> { it.dueDate ?: LocalDate.MIN }.thenBy { it.priority }
            DueDate -> compareBy { it.dueDate ?: LocalDate.MIN }
            Sorting.Priority -> compareBy { it.priority }
        }
        return if (completionSort == CompletionSort.Bottom) comparator.thenBy { it.checked } else comparator
    }

    fun getGroup(task: Task): Pair<Long, String> {
        val fmt = DateTimeFormatter.ofPattern("EE, MMM d")
        return when (grouping) {
            None -> 0L to ""
            Grouping.DueDate -> task.dueDate?.let { it.toEpochDay() to fmt.format(it) }
                ?: (Long.MAX_VALUE to "No due date")

            Grouping.Priority -> task.priority.ordinal.toLong() to task.priority.name
        }
    }
}


enum class Grouping(val label: String, @DrawableRes val icon: Int) {
    None("None", R.drawable.outline_block_24),
    DueDate("By due date", R.drawable.outline_event_24),
    Priority("By priority", R.drawable.outline_flag_24), ;
//    Project(Icons.Default.Category),
}

enum class Sorting(val label: String, @DrawableRes val icon: Int) {
    Smart("Smart Sort", R.drawable.outline_wand_stars_24),

    //    Custom("Custom", R.drawable.outline_drag_indicator_24),
    DueDate("By due date", R.drawable.outline_event_24),
    Priority("By priority", R.drawable.outline_flag_24), ;
//    Project(Icons.Default.Category),
}
