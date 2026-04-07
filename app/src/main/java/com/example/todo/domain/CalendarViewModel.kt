package com.example.todo.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.ui.CompletionSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CalendarControls(
    val isRefreshing: Boolean = false,
    val focusedDate: LocalDate = LocalDate.now(),
    val origFilSorter: FilSorter = FilSorter(),
)

data class CalendarUiState(
    val isLoading: Boolean = true,
    val tasks: List<Pair<String, List<Task>>> = persistentListOf(),
    val tags: List<Tag> = persistentListOf(),
    val instances: Map<LocalDate, List<CalInstance>> = mapOf(),
    val c: CalendarControls = CalendarControls(),
    val filSorter: FilSorter = FilSorter(),
    val filSorters: List<FilSorter> = listOf(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val tagRepository: TagRepository,
    private val filSorterRepository: FilSorterRepository,
) :
    ViewModel() {
    private val _calendarControls = MutableStateFlow(CalendarControls())
    private val _filSorter = MutableStateFlow(FilSorter())

    private val _filteredEventsFlow =
        combine(calendarRepository.getAllEvents(), _filSorter) { events, filSorter ->
            events.filter { filSorter.keep(it) }
        }

    private val _filteredTasksFlow =
        combine(calendarRepository.getAllTasks(), _filSorter) { tasks, filSorter ->
            tasks.filter { filSorter.keep(it) }
        }

    private val _filSortedFlow = combine(
        _filteredEventsFlow,
        _filteredTasksFlow,
        _filSorter,
    ) { events, tasks, filSorter ->
        val instances = events.flatMap { it.toDayInstances() }
            .plus(tasks.mapNotNull { it.toDayInstance() })
            .groupBy { it.date }
        val tasks = tasks.sortedWith(filSorter.getComparator())
            .groupBy { filSorter.getGroup(it) }
            .entries.map { it.key.second to it.value }
        Triple(
            instances,
            tasks,
            filSorter,
        )
    }

    private val _processedTagsFlow =
        combine(tagRepository.getAllTags(), _filSorter) { tags, filSorter ->
            tags.map { if (filSorter.tags.contains(it.id)) it.copy(selected = true) else it }
        }

    val uiState = combine(
        _filSortedFlow,
        _processedTagsFlow,
        _calendarControls,
        filSorterRepository.getAllFilSorters(),
    ) { (instances, tasks, filSorter), tags, calendarControls, filSorters ->
        CalendarUiState(
            instances = instances,
            isLoading = false,
            c = calendarControls,
            tasks = tasks,
            tags = tags,
            filSorter = filSorter,
            filSorters = filSorters
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CalendarUiState()
    )

    fun focusDate(newDate: LocalDate) {
        _calendarControls.update {
            it.copy(focusedDate = newDate)
        }
    }

    fun clearTagFilter() {
        _filSorter.update { it.clearTags() }
    }

    // focus true -> selects only tag, else it just adds to the selection/removes if present
    fun filterTag(tag: Tag, focus: Boolean) {
        _filSorter.update { if (focus) it.focusTag(tag) else it.toggleTag(tag) }
    }

    fun saveTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.saveTag(tag)
        }
    }

    fun saveItem(item: Task) {
        viewModelScope.launch {
            calendarRepository.saveTask(item)
        }
    }

    fun setSorting(sorting: Sorting) {
        _filSorter.update { it.copy(sorting = sorting) }
    }

    fun setGrouping(grouping: Grouping) {
        _filSorter.update { it.copy(grouping = grouping) }
    }

    fun setCompletionSort(new: CompletionSort) {
        _filSorter.update { it.copy(completionSort = new) }
    }

    fun setFilSorter(filSorter: FilSorter) {
        _filSorter.update { filSorter }
        _calendarControls.update { it.copy(origFilSorter = filSorter) }
    }

    fun updateCurrentView() {
        check(_filSorter.value.id != 0L)
        viewModelScope.launch {
            filSorterRepository.saveFilSorter(_filSorter.value)
            _calendarControls.update { it.copy(origFilSorter = _filSorter.value) }
        }
    }

    fun saveView(view: FilSorter) {
        viewModelScope.launch {filSorterRepository.saveFilSorter(view) }
    }

    fun fetch() {
        viewModelScope.launch {
            calendarRepository.fetch()
        }
    }
}

fun <T> Set<T>.toggle(element: T): Set<T> {
    return if (contains(element)) this - element else this + element
}
