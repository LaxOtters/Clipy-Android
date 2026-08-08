package com.laxotters.clipy.feature.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.home.navigation.homeEntry
import com.laxotters.clipy.feature.main.navigation.rememberAppNavigationState
import com.laxotters.clipy.feature.main.navigation.rememberAppNavigator
import com.laxotters.clipy.feature.session.navigation.sessionEntry
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClipyTheme {
                ClipyApp(onExit = ::finish)
            }
        }
    }
}

@Composable
private fun ClipyApp(
    onExit: () -> Unit,
) {
    val navigationState = rememberAppNavigationState()
    val navigator = rememberAppNavigator(navigationState)
    val appEntryProvider = entryProvider {
        homeEntry(navigateToSession = navigator::navigateToSession)
        sessionEntry(navigateToHome = navigator::navigateToHome)
    }

    NavDisplay(
        entries = navigationState.toEntries(appEntryProvider),
        onBack = {
            if (!navigator.goBack()) {
                onExit()
            }
        },
    )
}
