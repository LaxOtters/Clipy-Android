package com.laxotters.clipy.feature.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

@Composable
fun SessionRoute(
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SessionScreen(
        state = state,
        modifier = modifier,
    )
}

@Composable
private fun SessionScreen(
    state: SessionUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = state.title)
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionScreenPreview() {
    ClipyTheme {
        SessionScreen(
            state = SessionUiState(),
        )
    }
}
