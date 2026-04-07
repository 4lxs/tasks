@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.todo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.todo.domain.CalendarSource
import com.example.todo.domain.EditIntegrationState
import com.example.todo.domain.EditIntegrationViewModel
import com.example.todo.domain.LoadingStateWrapper
import com.example.todo.ui.features.LoadingBox

@Composable
fun EditIntegrationScreen(viewModel: EditIntegrationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    when (val s = state) {
        is LoadingStateWrapper.Loading -> LoadingBox()
        is LoadingStateWrapper.Loaded -> EditIntegrationContent(
            state = s.state
        )
    }
}

@Composable
fun EditIntegrationContent(state: EditIntegrationState) {
    val textFieldState = rememberTextFieldState()
    Scaffold(
        topBar = { EditIntegrationTopBar(state.calSource.title) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TextField(
                state = textFieldState,
                label = { Text("url") },
            )
            Button(onClick = {

            }) {
                Text("Confirm")
            }
        }
    }
}

@Composable
fun EditIntegrationTopBar(title: String) {
    TopAppBar(
        title = { Text("$title integration") }
    )
}

@Preview
@Composable
fun EditintegrationPreview() {
    EditIntegrationContent(
        state = EditIntegrationState(
            calSource = CalendarSource.WiseTTSource()
        )
    )
}