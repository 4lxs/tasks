@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.todo.ui.features

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.todo.R
import com.example.todo.domain.Priority
import com.example.todo.domain.Task

@Composable
fun TasksSheetContent(
    items: List<Pair<String, List<Task>>>,
    onEditTask: (Task) -> Unit,
    saveTask: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandeds: MutableMap<String, Boolean> = remember(items) { mutableStateMapOf() }
    LazyColumn(modifier = modifier) {
        items.forEach { (groupName, tasks) ->
            val expanded = expandeds.getOrDefault(groupName, true)
            if (!groupName.isEmpty()) {
                stickyHeader(key = groupName) {
                    Header(groupName, expanded, toggleExpand = { expandeds[groupName] = !expanded })
                }
            }

            if (expanded) {
                items(tasks, key = { it.id }) { item ->
                    TodoItem(
                        item,
                        onCheck = { checked -> saveTask(item.copy(checked = checked)); },
                        onEdit = { onEditTask(item) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
fun Header(title: String, expanded: Boolean, toggleExpand: () -> Unit) {
    val rotation by animateFloatAsState(
        if (expanded) -180f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    Row(
        Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = { toggleExpand() })
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(12.dp)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            painterResource(R.drawable.outline_keyboard_arrow_down_24),
            null,
            modifier = Modifier.graphicsLayer { rotationZ = rotation }.padding(12.dp))
    }
}


@Composable
fun PriorityChips(priority: Priority, onPriorityChange: (Priority) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Priority.entries.forEach {
            FilterChip(
                selected = it == priority,
                onClick = { onPriorityChange(it) },
                label = { Icon(Icons.Default.Flag, "Add tag icon", tint = it.color) })
        }
    }
}


@Composable
fun TodoItem(item: Task, onCheck: (Boolean) -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onCheck(!item.checked) },
                onLongClick = { onEdit() })
    ) {
        Checkbox(checked = item.checked, onCheckedChange = { onCheck(!item.checked) })
        Text(
            item.title,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}
