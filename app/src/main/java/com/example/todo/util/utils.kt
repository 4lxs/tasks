package com.example.todo.util

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.todo.ui.theme.MaterialSymbols

@Composable
fun MaterialSymbolIcon(
    iconName: String, // e.g., "home", "search", "delete"
    modifier: Modifier = Modifier,
    size: TextUnit = 24.sp,
    color: Color = LocalContentColor.current
) {
    Text(
        text = iconName,
        fontFamily = MaterialSymbols,
        fontSize = size,
        color = color,
        modifier = modifier
    )
}