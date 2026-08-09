package com.laxotters.clipy.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.laxotters.clipy.core.navigation.Route

class AppNavigator(
    private val navigationState: AppNavigationState,
) {
    fun navigateToSession(sessionId: String) {
        val sessionRoute = Route.Session(sessionId)
        val sessionBackStack = navigationState.sessionBackStack

        if (sessionBackStack.firstOrNull() == sessionRoute) {
            popUpToSessionRoot()
        } else {
            sessionBackStack.clear()
            sessionBackStack.add(sessionRoute)
        }

        navigationState.topLevelRoute = TopLevelRoute.SESSION
    }

    fun navigateToHome() {
        // Home으로 전환해도 활성 Session stack은 보존해 같은 Session의 UI 상태를 이어갑니다.
        navigationState.topLevelRoute = TopLevelRoute.HOME
    }

    fun goBack(): Boolean =
        when (navigationState.topLevelRoute) {
            TopLevelRoute.HOME -> popHomeBackStack()
            TopLevelRoute.SESSION -> popSessionBackStack()
        }

    private fun popHomeBackStack(): Boolean {
        val homeBackStack = navigationState.homeBackStack
        if (homeBackStack.size <= 1) {
            return false
        }

        homeBackStack.removeAt(homeBackStack.lastIndex)
        return true
    }

    private fun popSessionBackStack(): Boolean {
        val sessionBackStack = navigationState.sessionBackStack
        check(sessionBackStack.isNotEmpty()) {
            "Session stack must contain the active session route."
        }

        if (sessionBackStack.size > 1) {
            sessionBackStack.removeAt(sessionBackStack.lastIndex)
        } else {
            navigateToHome()
        }
        return true
    }

    // 다른 destination에도 popUpTo가 필요해질 때 target route를 받는 범용 함수로 확장합니다.
    private fun popUpToSessionRoot() {
        val sessionBackStack = navigationState.sessionBackStack
        while (sessionBackStack.size > 1) {
            sessionBackStack.removeAt(sessionBackStack.lastIndex)
        }
    }
}

@Composable
fun rememberAppNavigator(
    navigationState: AppNavigationState,
): AppNavigator = remember(navigationState) {
    AppNavigator(navigationState)
}
