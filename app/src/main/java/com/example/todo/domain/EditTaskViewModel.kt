package com.example.todo.domain

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.Boolean

data class TagOfItem(val tag: Tag, val ofItem: Boolean)

enum class EditScreenFocus { Task, Event }

data class EditUiControls(
    val focusedScreen: EditScreenFocus = EditScreenFocus.Task,
    val title: TextFieldState = TextFieldState(""),
    val notes: TextFieldState = TextFieldState(""),
    val task: Task = Task(),
    val event: Event = Event(title = "", start = LocalDateTime.now(), end = LocalDateTime.now()),
    val tagQuery: String = "",
)

data class EditUiState(
    val allTags: List<TagOfItem> = listOf(),
    val c: EditUiControls = EditUiControls(),
    val isLoading: Boolean = true,
)

// setTask is the entry point of this viewmodel. it should be called first to load the new task and only once
@HiltViewModel
class EditTaskViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    private val _tags = tagRepository.getAllTags()
    private val _uiControls = MutableStateFlow(EditUiControls())

    val uiState = combine(_uiControls, _tags) { controls, tags ->
        EditUiState(
            allTags = if (controls.tagQuery.isEmpty()) tags.map {
                TagOfItem(
                    it, controls.task.tags.contains(it)
                )
            }.sortedByDescending { it.ofItem }
            else tags.filter { it.name.contains(controls.tagQuery, ignoreCase = true) }
                .map { TagOfItem(it, controls.task.tags.contains(it)) },
            c = controls,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = EditUiState()
    )

    fun setItem(item: Item) {
        viewModelScope.launch {
            when (item) {
                is Event -> _uiControls.value = EditUiControls(
                    focusedScreen = EditScreenFocus.Event,
                    title = TextFieldState(item.title),
                    notes = TextFieldState(item.notes),
                    event = item,
                    tagQuery = ""
                )
                is Task -> _uiControls.value = EditUiControls(
                    focusedScreen = EditScreenFocus.Task,
                    title = TextFieldState(item.title),
                    notes = TextFieldState(item.notes),
                    task = item,
                    tagQuery = ""
                )
            }
        }
    }

    fun saveTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.saveTag(tag)
        }
    }

    fun saveItem() {
        viewModelScope.launch {
            val c = _uiControls.value
            when (c.focusedScreen) {
                EditScreenFocus.Task -> calendarRepository.saveTask(
                    c.task.copy(
                        title = c.title.text.toString(),
                        notes = c.notes.text.toString(),
                    )
                )

                EditScreenFocus.Event -> calendarRepository.saveEvent(
                    c.event.copy(
                        title = c.title.text.toString(),
                        notes = c.notes.text.toString(),
                    )
                )
            }
        }
    }

    fun updateItem(newItem: Task) {
        _uiControls.update {
            it.copy(task = newItem)
        }
    }

    fun updateItem(newItem: Event) {
        _uiControls.update {
            it.copy(event = newItem)
        }
    }

    fun updateSearch(newSearchString: String) {
        _uiControls.update {
            it.copy(tagQuery = newSearchString)
        }
    }

    fun setFocus(newFocus: EditScreenFocus) {
        _uiControls.update {
            it.copy(
                focusedScreen = newFocus
            )
        }
    }
}
