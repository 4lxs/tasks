@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.todo.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.todo.R
import com.example.todo.domain.EditScreenFocus
import com.example.todo.util.MaterialSymbolIcon
import com.example.todo.domain.EditTaskViewModel
import com.example.todo.domain.EditUiState
import com.example.todo.domain.Event
import com.example.todo.domain.Tag
import com.example.todo.domain.TagOfItem
import com.example.todo.domain.Task
import com.example.todo.ui.features.DatePickerModal
import com.example.todo.ui.features.LoadingBox
import com.example.todo.ui.features.PriorityChips
import com.example.todo.ui.features.TimePickerModal
import com.example.todo.ui.theme.TodoTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.collections.plus

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ItemEditScreen(onNavigateBack: () -> Unit, viewModel: EditTaskViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold { innerPadding ->
        var searching by remember { mutableStateOf(false) }

        if (searching) {
            TagSearch(
                tags = state.allTags,
                onTagSelect = {
                    viewModel.updateItem(
                        state.c.task.copy(
                            tags = if (state.c.task.tags.contains(it)) state.c.task.tags - it else state.c.task.tags + it
                        )
                    )
                },
                onCloseSearch = { searching = false },
                query = state.c.tagQuery,
                onQueryChange = { viewModel.updateSearch(it) },
                onTagCreate = {
                    viewModel.saveTag(it)
                }, modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                Heading(
                    titleState = state.c.title,
                    focusRequester = focusRequester,
                    focusedScreen = state.c.focusedScreen,
                    setFocus = { viewModel.setFocus(it) },
                    exit = { onNavigateBack() },
                    saveAndExit = { viewModel.saveItem(); onNavigateBack() },
                )
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = state.c.focusedScreen,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn())
                            .togetherWith(slideOutVertically { it } + fadeOut())
                            .using(SizeTransform(clip = false))
                    }) {
                    when (it) {
                        EditScreenFocus.Task -> TaskEdit(
                            state = state,
                            onItemChange = { newItem -> viewModel.updateItem(newItem) },
                            onTagSearch = { searching = true },
                        )

                        EditScreenFocus.Event -> EventEdit(
                            state = state,
                            onEventChange = { newItem -> viewModel.updateItem(newItem) },
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            LoadingBox(Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun Heading(
    titleState: TextFieldState,
    focusRequester: FocusRequester,
    focusedScreen: EditScreenFocus,
    setFocus: (EditScreenFocus) -> Unit,
    exit: () -> Unit,
    saveAndExit: () -> Unit,
) {
    val transparentColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = { exit() }) { Icon(Icons.Default.Close, null) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { saveAndExit() }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            ) {
                Icon(Icons.Default.Save, null)
                AnimatedContent(focusedScreen) {
                    Text(
                        if (it == EditScreenFocus.Task) "Save Task" else "Save Event",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
        Column(Modifier.offset(y = (-12).dp)) {
            TextField(
                state = titleState,
                lineLimits = TextFieldLineLimits.SingleLine,
                contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                    top = 0.dp,
                    bottom = 0.dp
                ),
                placeholder = {
                    Text(
                        "Add title",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                colors = transparentColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            AnimatedVisibility(visible = !titleState.text.isEmpty()) {
                val descState = rememberTextFieldState("")
                TextField(
                    state = descState,
                    contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                        top = 0.dp,
                        bottom = 0.dp
                    ),
                    placeholder = {
                        Text(
                            "Add description",
                            style = MaterialTheme.typography.bodySmallEmphasized,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = transparentColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = -12.dp)
                )
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .height(42.dp)
                .padding(bottom = 8.dp)
        ) {
            val list =
                listOf("Task" to EditScreenFocus.Task, "Event" to EditScreenFocus.Event)
            list.forEachIndexed { i, (name, focus) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = i,
                        count = list.size
                    ),
                    onClick = { setFocus(focus) },
                    selected = focusedScreen == focus,
                    label = { Text(name) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    icon = {},
                )
            }
        }
    }
}

@Composable
fun EventEdit(state: EditUiState, onEventChange: (Event) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow, // Subtle tonal shift
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateTimeSelector(
                    selected = state.c.event.start,
                    onChange = { onEventChange(state.c.event.copy(start = it)) },
                    modifier = Modifier.weight(1f),
                )
                Icon(painterResource(R.drawable.outline_arrow_right_alt_24), null)
                DateTimeSelector(
                    selected = state.c.event.end,
                    onChange = { onEventChange(state.c.event.copy(end = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun DateTimeSelector(
    selected: LocalDateTime,
    onChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val fmtDate = DateTimeFormatter.ofPattern("E, MMM d")
    val fmtTime = DateTimeFormatter.ofPattern("HH:mm")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            border = BorderStroke(
                Dp.Hairline,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                fmtDate.format(selected),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(2.dp))
        OutlinedButton(
            onClick = { showTimePicker = true },
            border = BorderStroke(
                Dp.Hairline,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent, // Pure transparent background
                contentColor = MaterialTheme.colorScheme.primary // Vibrant text
            )
        ) {
            Text(
                fmtTime.format(selected),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { onChange(selected.with(it)) },
            onDismiss = { showDatePicker = false })
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selected.hour,
            initialMinute = selected.minute,
        )

        TimePickerModal(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                onChange(selected.withHour(timePickerState.hour).withMinute(timePickerState.minute))
                showTimePicker = false
            },
            timePickerState = timePickerState,
        )
    }
}

@Composable
fun TopBar(itemTitle: String?, onItemSave: () -> Unit) {
    TopAppBar(title = { Text(itemTitle ?: "") }, navigationIcon = {
        IconButton(
            onClick = {
                onItemSave()
            },
        ) {
            Icon(Icons.Filled.Save, "Save Item")
        }
    })
}

@Composable
fun TaskEdit(
    state: EditUiState,
    onItemChange: (Task) -> Unit,
    onTagSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.c.task

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow, // Subtle tonal shift
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            DateChips(
                selectedDate = item.dueDate,
                onDateSelected = { onItemChange(item.copy(dueDate = it)) })
            TagChips(tags = state.allTags, onTagSelect = {
                onItemChange(item.copy(tags = if (item.tags.contains(it)) item.tags - it else item.tags + it))
            }, onTagSearch = onTagSearch)

            PriorityChips(
                priority = item.priority,
                onPriorityChange = { onItemChange(item.copy(priority = it)) })

            OutlinedTextField(
                state = state.c.notes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                lineLimits = TextFieldLineLimits.MultiLine(3),
            )
        }
    }
}

@Composable
fun TagChips(tags: List<TagOfItem>, onTagSelect: (Tag) -> Unit, onTagSearch: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = false,
            onClick = { onTagSearch() },
            label = { Icon(Icons.Default.Add, "Add tag icon") })
        tags.take(5).forEach {
            FilterChip(
                selected = it.ofItem,
                onClick = { onTagSelect(it.tag) },
                leadingIcon = { MaterialSymbolIcon(it.tag.icon) },
                label = { Text(it.tag.name) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    tags: List<TagOfItem>,
    onTagSelect: (Tag) -> Unit,
    onCloseSearch: () -> Unit,
    onTagCreate: (tag: Tag) -> Unit,
    modifier: Modifier = Modifier,
) {
    DockedSearchBar(
        modifier = modifier.fillMaxSize(),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { if (tags.isEmpty()) onTagCreate(Tag(name = it)) },
                expanded = true,
                onExpandedChange = { if (!it) onCloseSearch() },
                placeholder = { Text("Search") })
        },
        expanded = true, onExpandedChange = { if (!it) onCloseSearch() },
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (tags.isEmpty()) {
                item {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Add, "add icon") },
                        headlineContent = {
                            Text(
                                "Create tag '$query'",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        modifier = Modifier
                            .clickable { onTagCreate(Tag(query)) }
                            .fillMaxWidth())
                }
            }

            items(tags, key = { it.tag.id }) {
                ListItem(
                    leadingContent = {
                        MaterialSymbolIcon(
                            it.tag.icon,
                            color = if (it.ofItem) MaterialTheme.colorScheme.primary else Color.Unspecified
                        )
                    }, headlineContent = {
                        Text(
                            it.tag.name,
                            color = if (it.ofItem) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            fontWeight = if (it.ofItem) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }, trailingContent = {
                        Icon(
                            imageVector = if (it.ofItem) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (it.ofItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }, modifier = Modifier
                        .clickable { onTagSelect(it.tag) }
                        .fillMaxWidth())
            }
        }
    }
}

@Composable
fun DateChips(selectedDate: LocalDate?, onDateSelected: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val tomorrow = LocalDate.now().plusDays(1)
    val customDate =
        if (selectedDate == today || selectedDate == tomorrow) null else selectedDate
    var showDatePicker by remember { mutableStateOf(false) };

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { onDateSelected(it) },
            onDismiss = { showDatePicker = false })
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = customDate != null,
            onClick = {
                showDatePicker = true
            },
            leadingIcon = { Icon(Icons.Filled.CalendarToday, "calendar") },
            label = {
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                Text(customDate?.format(formatter) ?: "Pick Date...")
            },
        )
        arrayOf("Today" to today, "Tomorrow" to tomorrow).forEach { (text, date) ->
            FilterChip(
                selected = selectedDate == date,
                onClick = { onDateSelected(date) },
                label = { Text(text) })
        }
    }
}

@Preview
@Composable
fun TagSearchPreview() {
    Scaffold(topBar = { TopBar("item 1") {} }) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TagSearch(
                query = "",
                onQueryChange = {},
                tags = listOf(
                    TagOfItem(Tag("t1", "shop", id = 1), false),
                    TagOfItem(Tag("t1", "manage_search", id = 2), true),
                ),
                onTagSelect = { },
                onCloseSearch = { },
                onTagCreate = {},
            )
        }
    }
}

@Preview
@Composable
fun TaskEditPreview() {
    TodoTheme {
        Surface {
            Column(Modifier.fillMaxSize()) {
                Heading(
                    titleState = TextFieldState("hey"),
                    focusRequester = remember { FocusRequester() },
                    focusedScreen = EditScreenFocus.Task,
                    setFocus = {},
                    exit = {},
                    saveAndExit = {},
                )
                Spacer(Modifier.height(8.dp))
                TaskEdit(state = EditUiState(), onItemChange = {}, onTagSearch = {})
            }
        }
    }
}

@Preview
@Composable
fun EventEditPreview() {
    TodoTheme {
        Surface {
            Column(Modifier.fillMaxSize()) {
                Heading(
                    titleState = TextFieldState("hey"),
                    focusRequester = remember { FocusRequester() },
                    focusedScreen = EditScreenFocus.Event,
                    setFocus = {},
                    exit = {},
                    saveAndExit = {},
                )
                Spacer(Modifier.height(8.dp))
                EventEdit(
                    state = EditUiState(),
                    onEventChange = {},
                )
            }
        }
    }
}
