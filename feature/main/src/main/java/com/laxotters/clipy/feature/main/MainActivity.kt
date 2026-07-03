package com.laxotters.clipy.feature.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.home.HomeRoute
import com.laxotters.clipy.feature.session.SessionRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClipyTheme {
                ClipyApp()
            }
        }
    }
}

@Composable
private fun ClipyApp() {
    var sessionId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedSessionId = sessionId

    if (selectedSessionId != null) {
        SessionRoute(
            sessionId = selectedSessionId,
            onHomeClick = {
                sessionId = null
            },
        )
    } else {
        HomeRoute(
            onSessionClick = { nextSessionId ->
                sessionId = nextSessionId
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClipyAppPreview() {
    ClipyTheme {
        ClipyApp()
    }
}
