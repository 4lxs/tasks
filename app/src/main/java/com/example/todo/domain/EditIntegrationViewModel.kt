package com.example.todo.domain

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class EditIntegrationState(
    val calSource: CalendarSource,
)

sealed interface LoadingStateWrapper {
    object Loading : LoadingStateWrapper
    data class Loaded(val state: EditIntegrationState) : LoadingStateWrapper
}

@HiltViewModel
class EditIntegrationViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val _calSourceType: CalendarSourceType = checkNotNull(savedStateHandle["type"])
    val _calSourceFlow = MutableStateFlow<CalendarSource>(
        when (_calSourceType) {
            CalendarSourceType.WiseTT -> CalendarSource.WiseTTSource()
            CalendarSourceType.Google -> TODO()
        }
    )

    val state = _calSourceFlow.asStateFlow().map { calSource ->
        LoadingStateWrapper.Loaded(
            EditIntegrationState(
                calSource = calSource
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LoadingStateWrapper.Loading
    )
}
