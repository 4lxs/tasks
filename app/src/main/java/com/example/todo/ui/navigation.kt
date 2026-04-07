@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.todo.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.todo.Navigator
import com.example.todo.domain.CalendarSourceType
import com.example.todo.domain.EditTaskViewModel
import com.example.todo.rememberNavigationState
import com.example.todo.toEntries
import kotlinx.serialization.Serializable

@Composable
fun Do2App() {
    val navigationState = rememberNavigationState(
        startRoute = Destination.Calendar,
        topLevelRoutes = setOf(Destination.Calendar),
    )

    val navigator = remember { Navigator(navigationState) }

    val editTaskViewModel: EditTaskViewModel = hiltViewModel()

    val entryProvider = entryProvider<NavKey> {
        entry<Destination.Settings> { SettingsScreen(
            onNavigateBack = { navigator.goBack() },
            navToEditWiseTT = { navigator.navigate(Destination.EditWiseTT(it)) }
        ) }
        entry<Destination.EditItem> { ItemEditScreen(onNavigateBack = { navigator.goBack() }, viewModel = editTaskViewModel) }
        entry<Destination.Calendar> {
            CalendarScreen(
                navToEditItem = {
                    editTaskViewModel.setItem(it)
                    navigator.navigate(Destination.EditItem)
                },
                onNavToSettings = { navigator.navigate(Destination.Settings) },
            )
        }
        entry<Destination.EditWiseTT> { EditIntegrationScreen() }
    }

    NavDisplay(
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() },
    )
}

sealed interface Destination : NavKey {

    @Serializable
    object Calendar : Destination

    @Serializable
    object Settings : Destination

    @Serializable
    object EditItem : Destination

    @Serializable
    data class EditWiseTT(val type: CalendarSourceType) : Destination

}

