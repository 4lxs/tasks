@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.todo.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.todo.R
import com.example.todo.domain.CalendarSourceType
import com.example.todo.ui.theme.TodoTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, navToEditWiseTT: (CalendarSourceType) -> Unit) {
    var addAccountExpanded by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopBar(onNavigateBack) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SettingsGroup("Integrations") {
                SettingButton(
                    title = "svensek.luka@pm.me",
                    icon = R.drawable.google_symbol_0,
                    onClick = {},
                )
                SettingButton(
                    title = "Add account",
                    icon = R.drawable.outline_add_24,
                    onClick = { addAccountExpanded = true })
                AddAccountPopup(
                    expanded = addAccountExpanded,
                    onDismiss = { addAccountExpanded = false },
                    navToEditWiseTT)
            }
            SettingsGroup("App settings") {
                SettingButton(
                    title = "Events & Tasks",
                    icon = R.drawable.outline_pending_actions_24,
                    onClick = {},
                )
                SettingButton(
                    title = "Calendar",
                    icon = R.drawable.outline_calendar_today_24,
                    onClick = {},
                )
                SettingButton(
                    title = "Notifications",
                    icon = R.drawable.outline_notifications_24,
                    onClick = {},
                )
                SettingButton(
                    title = "Import",
                    icon = R.drawable.outline_upload_24,
                    onClick = { addAccountExpanded = true })
            }
        }
    }
}

@Composable
fun AddAccountPopup(expanded: Boolean, onDismiss: () -> Unit, navToEditWiseTT: (CalendarSourceType) -> Unit) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        val possibles = listOf(
            Triple("Google", { navToEditWiseTT(CalendarSourceType.Google) }, R.drawable.google_symbol_0),
            Triple("Wise Timetable", { navToEditWiseTT(CalendarSourceType.WiseTT) }, R.drawable.www_wise_tt_com),
        )
        DropdownMenuGroup(MenuDefaults.groupShapes(), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            possibles.forEach { (title, onClick, icon) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = onClick,
                    leadingIcon = {
                        Image(
                            painterResource(icon),
                            null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
//        Text(
//            text = title,
//            style = MaterialTheme.typography.labelLarge,
//            color = MaterialTheme.colorScheme.primary,
//            modifier = Modifier.padding(18.dp, 9.dp)
//        )
//        HorizontalDivider()
        content()
    }
}

@Composable
fun SettingButton(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    description: String? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(description) } },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = { Icon(painterResource(icon), null, Modifier.size(24.dp)) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent, // Makes the background see-through
            headlineColor = MaterialTheme.colorScheme.onSurface,
            leadingIconColor = MaterialTheme.colorScheme.primary
        ),
    )
}

@Composable
fun TopBar(onNavigateBack: () -> Unit) {
    val searchState = rememberSearchBarState(
        animationSpecForExpand = MaterialTheme.motionScheme.defaultSpatialSpec(),
        animationSpecForCollapse = MaterialTheme.motionScheme.defaultSpatialSpec(),
    )
    val scope = rememberCoroutineScope()
    Box {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = { onNavigateBack() }) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                }
            },
            actions = {
                IconButton(
                    onClick = { scope.launch { searchState.animateToExpanded() } },
                    modifier = Modifier.onGloballyPositioned { searchState.collapsedCoords = it }) {
                    Icon(Icons.Default.Search, null)
                }
            }
        )
        ExpandedFullScreenSearchBar(
            state = searchState,
            inputField = {
                SearchBarDefaults.InputField(
                    searchBarState = searchState,
                    textFieldState = rememberTextFieldState(),
                    onSearch = { scope.launch { searchState.animateToCollapsed() } },
                    leadingIcon = {
                        IconButton(onClick = { scope.launch { searchState.animateToCollapsed() } }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                        }
                    },
                )
            },
            content = {}
        )
    }
}

@Preview
@Composable
fun SettingsPreview() {
    TodoTheme {
        SettingsScreen(onNavigateBack = {}, navToEditWiseTT = {})
    }
}