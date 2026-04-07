@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.example.todo.ui

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.todo.R
import com.example.todo.domain.CalendarUiState
import com.example.todo.domain.CalendarViewModel
import com.example.todo.domain.Event
import com.example.todo.domain.FilSorter
import com.example.todo.domain.Grouping
import com.example.todo.domain.Item
import com.example.todo.domain.Sorting
import com.example.todo.domain.Tag
import com.example.todo.domain.Task
import com.example.todo.ui.features.Calendar
import com.example.todo.ui.features.LoadingBox
import com.example.todo.ui.features.TasksSheetContent
import com.example.todo.ui.theme.TodoTheme
import com.example.todo.util.MaterialSymbolIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime


enum class Focus { Calendar, Tasks }
enum class CompletionSort {
    None, Bottom, Hide;

    fun next(): CompletionSort = entries[(ordinal + 1) % entries.size]
}

@Composable
fun CalendarScreen(
    onNavToSettings: () -> Unit,
    navToEditItem: (Item) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val bottomSheetState =
        rememberStandardBottomSheetState(initialValue = SheetValue.Expanded)
    val focused =
        if (bottomSheetState.targetValue == SheetValue.Expanded) Focus.Tasks else Focus.Calendar

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var editView: FilSorter? by remember { mutableStateOf(null) }
    LaunchedEffect(editView) { if (editView != null) drawerState.open() }
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Closed) editView = null
    }

    ModalNavigationDrawer(
        drawerContent = {
            DrawerContent(
                onNavToSettings = onNavToSettings,
                sections = {
                    if (!state.filSorters.isEmpty() || editView != null) ViewDrawerSection(
                        views = state.filSorters,
                        current = state.filSorter,
                        selectFilter = { viewModel.setFilSorter(it) },
                        editView = editView,
                        onEditView = { editView = it },
                        onSaveView = { viewModel.saveView(it); editView = null },
                    )
                    if (!state.tags.isEmpty()) TagsDrawerSection(
                        state.tags,
                        onFocusTag = { tag, add ->
                            viewModel.filterTag(tag, !add)
                            if (!add) coroutineScope.launch { drawerState.close() }
                        })
                }
            )
        },
        drawerState = drawerState,
        gesturesEnabled = drawerState.currentValue == DrawerValue.Open
    ) {
        Scaffold(
            floatingActionButton = {
                ToolbarFAB(
                    focused = focused,
                    onAddTask = { navToEditItem(Task()) },
                    onAddEvent = {
                        navToEditItem(
                            Event(
                                title = "",
                                start = LocalDateTime.now(),
                                end = LocalDateTime.now()
                            )
                        )
                    },
                    focusDate = { viewModel.focusDate(it) },
                    drawerState = drawerState,
                    grouping = state.filSorter.grouping,
                    updateGrouping = { viewModel.setGrouping(it) },
                    sorting = state.filSorter.sorting,
                    updateSorting = { viewModel.setSorting(it) },
                    updateCurrentView = if (state.filSorter == state.c.origFilSorter || state.filSorter.id == 0L) null else {
                        {
                            viewModel.updateCurrentView()
                        }
                    },
                    saveCurrentView = { editView = state.filSorter.copy(id = 0) },
                    completionSort = state.filSorter.completionSort,
                    completionSortUpdate = { viewModel.setCompletionSort(it) },
                    fetch = { viewModel.fetch() }
                )
            },
            floatingActionButtonPosition = FabPosition.End,
        ) { innerPadding ->
//            BottomSheetScaffold(
//                sheetPeekHeight = 112.dp,
//                scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState),
//                sheetContent = {
//                    TasksSheetContent(
//                        onEditTask = { navToEditItem(it) },
//                        saveTask = { viewModel.saveItem(it) },
//                        items = state.tasks,
//                        modifier = Modifier.fillMaxSize(),
//                    )
//                },
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(innerPadding)
//            ) { innerInnerPadding ->
            Calendar(
                state.c.focusedDate, events = state.instances, onEditItem = navToEditItem,
                onFocusDate = { viewModel.focusDate(it) },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
            if (state.isLoading) {
                LoadingBox(Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun DrawerContent(onNavToSettings: () -> Unit, sections: @Composable ColumnScope.() -> Unit) {
    val progress by animateFloatAsState(
        0.8f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    ModalDrawerSheet {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's progress: ", style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(progress = { progress })
            }
        }

        Column(Modifier.weight(1f)) {
            sections()

            NavigationDrawerItem(
                icon = { Icon(painterResource(R.drawable.outline_delete_24), "delete icon") },
                label = { Text("Deleted Items") },
                selected = false,
                onClick = {}
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            icon = {
                Icon(
                    painterResource(R.drawable.outline_settings_24),
                    "settings"
                )
            },
            label = { Text("Settings") },
            selected = false,
            onClick = { onNavToSettings() }
        )
    }
}

val LocalDrawerOffset = compositionLocalOf { 0.dp }

@Composable
fun DrawerSection(
    name: String,
    icon: @Composable () -> Unit,
    defaultExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val rotation by animateFloatAsState(
        if (expanded) -180f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val offset = LocalDrawerOffset.current
    Surface(
        modifier = Modifier.padding(PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            DrawerItem(
                name = name,
                icon = icon,
                onClick = { expanded = !expanded },
                badge = {
                    Icon(
                        painterResource(R.drawable.outline_keyboard_arrow_down_24),
                        null,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation })
                },
            )
            val density = LocalDensity.current
            AnimatedVisibility(
                visible = expanded,
                enter = slideInVertically { with(density) { -40.dp.roundToPx() } } + expandVertically(
                    expandFrom = Alignment.Top
                ) + fadeIn(initialAlpha = 0.3f),
                exit = slideOutVertically() + shrinkVertically() + fadeOut()
            ) {
                Column {
                    CompositionLocalProvider(LocalDrawerOffset provides offset + 24.dp) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    name: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    badge: @Composable () -> Unit = {},
    selected: Boolean = false,
    onIconClick: () -> Unit = onClick,
    editSave: ((String) -> Unit)? = null,
) {
    val offset = LocalDrawerOffset.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(editSave) {
        if (editSave != null) focusRequester.requestFocus()
    }

    NavigationDrawerItem(
        icon = {
            Row {
                Spacer(Modifier.width(offset))
                IconButton(onClick = onIconClick) {
                    if (selected) {
                        Icon(painterResource(R.drawable.outline_check_small_24), null)
                    } else {
                        icon()
                    }
                }
            }
        },
        label = {
            if (editSave == null) Text(name)
            else {
                val editViewState = rememberTextFieldState("")
                BasicTextField(
                    editViewState,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.focusRequester(focusRequester),
                    onKeyboardAction = { editSave(editViewState.text.toString()) })
            }
        },
        selected = selected,
        onClick = onClick,
        badge = badge,
    )
}

@Composable
fun ViewDrawerSection(
    views: List<FilSorter>,
    current: FilSorter,
    selectFilter: (FilSorter) -> Unit,
    editView: FilSorter?,
    onEditView: (FilSorter) -> Unit,
    onSaveView: (FilSorter) -> Unit
) {
    DrawerSection(
        "Views", icon = { Icon(painterResource(R.drawable.outline_filter_alt_24), null) }
    ) {
        sequence {
            if (editView != null && editView.id == 0L) {
                yield(editView)
            }
            yieldAll(views)
        }.forEach {
            DrawerItem(
                name = it.name,
                icon = { Icon(painterResource(it.icon), null) },
                onClick = { selectFilter(it) },
                selected = current.id == it.id,
                editSave = if (it.id == editView?.id) {
                    { newName -> onSaveView(it.copy(name = newName)) }
                } else null
            )
        }
    }
}

@Composable
fun TagsDrawerSection(tags: List<Tag>, onFocusTag: (Tag, Boolean) -> Unit) {
    DrawerSection("Tags", { Icon(painterResource(R.drawable.outline_bookmarks_24), null) }) {
        tags.forEach {
            DrawerItem(
                name = it.name,
                icon = { MaterialSymbolIcon(it.icon) },
                onClick = { onFocusTag(it, false) },
                selected = it.selected,
                onIconClick = { onFocusTag(it, true) },
            )
        }
    }
}


@Composable
fun ToolbarFAB(
    focused: Focus,
    onAddTask: () -> Unit,
    onAddEvent: () -> Unit,
    focusDate: (LocalDate) -> Unit,
    drawerState: DrawerState,

    grouping: Grouping,
    updateGrouping: (Grouping) -> Unit,
    sorting: Sorting,
    updateSorting: (Sorting) -> Unit,
    updateCurrentView: (() -> Unit)?,
    saveCurrentView: () -> Unit,
    completionSort: CompletionSort,
    completionSortUpdate: (CompletionSort) -> Unit,
    fetch: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var displayFocused by remember { mutableStateOf(focused) }
    var isExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(focused) {
        isExpanded = false
        delay(300)
        displayFocused = focused
        isExpanded = true
    }

    HorizontalFloatingToolbar(
        expanded = isExpanded,
        floatingActionButton = {
            AddItemFab(
                focused,
                onAddTask = onAddTask,
                onAddEvent = onAddEvent
            )
        },
    ) {

        IconButton(onClick = {
            fetch()
        }) {
            Icon(Icons.Filled.Call, null)
        }

        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
            Icon(Icons.Filled.Menu, "open tags menu")
        }
        when (displayFocused) {
            Focus.Calendar -> IconButton(onClick = { focusDate(LocalDate.now()) }) {
                Icon(Icons.Filled.Today, "today")
            }

            Focus.Tasks -> SortingMenuButton(
                grouping,
                updateGrouping,
                sorting,
                updateSorting,
                updateCurrentView,
                saveCurrentView,
                completionSort,
                completionSortUpdate,
            )
        }
    }
}

@Composable
fun SortingMenuButton(
    grouping: Grouping,
    updateGrouping: (Grouping) -> Unit,
    sorting: Sorting,
    updateSorting: (Sorting) -> Unit,
    updateCurrentView: (() -> Unit)?,
    saveCurrentView: () -> Unit,
    completionSort: CompletionSort,
    completionSortUpdate: (CompletionSort) -> Unit
) {
    var popupMenuExpanded by remember { mutableStateOf(false) }
    val saveCurrentView = { popupMenuExpanded = false; saveCurrentView() }

    IconButton(onClick = { popupMenuExpanded = true }) {
        ToolBarMenuPopup(
            popupMenuExpanded, onDismiss = { popupMenuExpanded = false },
            groups = listOf(
                @Composable {
                    GroupingMenuItem(
                        shape = MenuDefaults.leadingItemShape,
                        selected = grouping,
                        update = updateGrouping,
                    )
                    SortingMenuItem(
                        shape = MenuDefaults.middleItemShape,
                        selected = sorting,
                        update = updateSorting,
                    )
                    ToolBarMenuPopupItem(
                        text = "Completed Items",
                        desc = when (completionSort) {
                            CompletionSort.Bottom -> "Move completed items to bottom"
                            CompletionSort.Hide -> "don't show completed items"
                            CompletionSort.None -> "normal completed items sorting"
                        },
                        onClick = { completionSortUpdate(completionSort.next()) },
                        leadingIcon = {
                            when (completionSort) {
                                CompletionSort.Bottom -> Icon(
                                    Icons.Filled.MoveDown,
                                    null
                                )

                                CompletionSort.Hide -> Icon(
                                    Icons.Filled.VisibilityOff,
                                    null
                                )

                                CompletionSort.None -> Icon(
                                    Icons.Filled.Check,
                                    null
                                )
                            }

                        },
                        shape = MenuDefaults.middleItemShape,
                    )
                    HorizontalDivider()
                    val canUpdate = updateCurrentView != null
                    ToolBarMenuPopupItem(
                        text = if (canUpdate) "Save View" else "Create View",
                        onClick = if (canUpdate) updateCurrentView else saveCurrentView,
                        leadingIcon = {
                            Icon(
                                if (canUpdate) Icons.Default.Save else Icons.Default.BookmarkAdd,
                                null
                            )
                        },
                        trailingIcon = if (canUpdate) {
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    VerticalDivider(Modifier.height(24.dp))
                                    IconButton(onClick = { saveCurrentView() }) {
                                        Icon(Icons.Default.BookmarkAdd, null)
                                    }
                                }
                            }
                        } else {
                            { Box(Modifier.size(48.dp)) }
                        },
                        shape = MenuDefaults.trailingItemShape,
                    )
                },
            )
        )
        Icon(painterResource(R.drawable.outline_swap_vert_24), null)
    }
}

@Composable
fun SortingMenuItem(
    shape: Shape,
    selected: Sorting,
    update: (Sorting) -> Unit,
) {
    ToolBarMenuPopupItem(
        "Sorting",
        desc = selected.label,
        onClick = {},
        leadingIcon = {
            Icon(painterResource(R.drawable.outline_swap_vert_24), null)
        },
        shape = shape,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Sorting.entries.forEachIndexed { ix, value ->
            if (ix != 0) VerticalDivider(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(SegmentedButtonDefaults.itemShape(ix, Sorting.entries.size))
                    .clickable(onClick = { update(value) })
                    .minimumInteractiveComponentSize()
                    .padding(20.dp, 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(value.icon), null,
                    tint = if (selected == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GroupingMenuItem(shape: Shape, selected: Grouping, update: (Grouping) -> Unit) {
    ToolBarMenuPopupItem(
        "Grouping",
        desc = selected.label,
        onClick = {},
        leadingIcon = {
            Icon(painterResource(R.drawable.outline_grid_view_24), null)
        },
        shape = shape,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Grouping.entries.forEachIndexed { ix, value ->
            if (ix != 0) VerticalDivider(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(SegmentedButtonDefaults.itemShape(ix, Grouping.entries.size))
                    .clickable(onClick = { update(value) })
                    .minimumInteractiveComponentSize()
                    .padding(20.dp, 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(value.icon), null,
                    tint = if (selected == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ToolBarMenuPopupItem(
    text: String,
    desc: String? = null,
    onClick: () -> Unit,
    shape: Shape,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    DropdownMenuItem(
        leadingIcon = leadingIcon,
        text = { Text(text, style = MaterialTheme.typography.titleMedium) },
        onClick = onClick,
        shape = shape,
        supportingText = desc?.let { desc ->
            {
                Text(
                    desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        },
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            leadingIconColor = MaterialTheme.colorScheme.primary // Makes the icon pop
        ),
        trailingIcon = trailingIcon,
        modifier = Modifier.widthIn(min = 280.dp),
    )
}

@Composable
fun ToolBarMenuPopup(
    expanded: Boolean,
    groups: List<@Composable () -> Unit>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        groups.forEachIndexed { gix, groupContent ->
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(gix, groups.size),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    width = Dp.Hairline,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                groupContent()
            }
        }
    }
}

@Composable
fun AddItemFab(focused: Focus, onAddTask: () -> Unit, onAddEvent: () -> Unit) {
    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = {
        when (focused) {
            Focus.Tasks -> onAddTask()
            Focus.Calendar -> onAddEvent()
        }
    }) {
        val motion = MaterialTheme.motionScheme
        AnimatedContent(
            targetState = focused,
            transitionSpec = {
                scaleIn(animationSpec = motion.slowEffectsSpec()).togetherWith(
                    scaleOut(animationSpec = motion.fastEffectsSpec())
                )
            }
        ) {
            when (it) {
                Focus.Tasks -> Icon(
                    painterResource(R.drawable.outline_add_task_24),
                    "add task"
                )

                Focus.Calendar -> Icon(
                    painterResource(R.drawable.outline_more_time_24),
                    "add event"
                )
            }
        }
    }
}

@Preview
@Composable
fun DrawerContentPreview() {
    TodoTheme {
        Surface {
            DrawerContent(
                onNavToSettings = {},
                sections = {
                    TagsDrawerSection(
                        listOf(
                            Tag(name = "hey", selected = true),
                            Tag(name = "a"),
                            Tag(name = "b", selected = true),
                            Tag(name = "c"),
                        ), onFocusTag = { _, _ -> })
                }
            )
        }
    }
}