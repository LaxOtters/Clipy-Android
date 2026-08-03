package com.laxotters.clipy.feature.main.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.laxotters.clipy.core.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {
    @Test
    fun home_navigateToSession_startsSessionBackStack() {
        val navigationState = navigationState()
        val navigator = AppNavigator(navigationState)

        navigator.navigateToSession("session-1")

        assertEquals(TopLevelRoute.SESSION, navigationState.topLevelRoute)
        assertEquals(listOf(Route.Home), navigationState.homeBackStack)
        assertEquals(listOf(Route.Session("session-1")), navigationState.sessionBackStack)
    }

    @Test
    fun sameSession_navigateToSession_returnsToSessionRoot() {
        val navigationState = navigationState(
            topLevelRoute = TopLevelRoute.HOME,
            sessionRoutes = listOf(
                Route.Session("session-1"),
                SessionChild,
            ),
        )
        val navigator = AppNavigator(navigationState)

        navigator.navigateToSession("session-1")

        assertEquals(TopLevelRoute.SESSION, navigationState.topLevelRoute)
        assertEquals(listOf(Route.Session("session-1")), navigationState.sessionBackStack)
    }

    @Test
    fun differentSession_navigateToSession_replacesSessionBackStack() {
        val navigationState = navigationState(
            topLevelRoute = TopLevelRoute.SESSION,
            sessionRoutes = listOf(Route.Session("session-1")),
        )
        val navigator = AppNavigator(navigationState)

        navigator.navigateToSession("session-2")

        assertEquals(listOf(Route.Session("session-2")), navigationState.sessionBackStack)
    }

    @Test
    fun session_navigateToHome_preservesSessionBackStack() {
        val navigationState = navigationState(
            topLevelRoute = TopLevelRoute.SESSION,
            sessionRoutes = listOf(Route.Session("session-1")),
        )
        val navigator = AppNavigator(navigationState)

        navigator.navigateToHome()

        assertEquals(TopLevelRoute.HOME, navigationState.topLevelRoute)
        assertEquals(listOf(Route.Session("session-1")), navigationState.sessionBackStack)
    }

    @Test
    fun sessionRoot_goBack_navigatesToHomeAndPreservesSessionBackStack() {
        val navigationState = navigationState(
            topLevelRoute = TopLevelRoute.SESSION,
            sessionRoutes = listOf(Route.Session("session-1")),
        )
        val navigator = AppNavigator(navigationState)

        val wentBack = navigator.goBack()

        assertTrue(wentBack)
        assertEquals(TopLevelRoute.HOME, navigationState.topLevelRoute)
        assertEquals(listOf(Route.Session("session-1")), navigationState.sessionBackStack)
    }

    @Test
    fun sessionChild_goBack_removesOnlySessionChild() {
        val navigationState = navigationState(
            topLevelRoute = TopLevelRoute.SESSION,
            sessionRoutes = listOf(
                Route.Session("session-1"),
                SessionChild,
            ),
        )
        val navigator = AppNavigator(navigationState)

        val wentBack = navigator.goBack()

        assertTrue(wentBack)
        assertEquals(TopLevelRoute.SESSION, navigationState.topLevelRoute)
        assertEquals(listOf(Route.Session("session-1")), navigationState.sessionBackStack)
    }

    @Test
    fun homeRoot_goBack_returnsFalseAndPreservesHome() {
        val navigationState = navigationState()
        val navigator = AppNavigator(navigationState)

        val wentBack = navigator.goBack()

        assertFalse(wentBack)
        assertEquals(listOf(Route.Home), navigationState.homeBackStack)
    }

    private fun navigationState(
        topLevelRoute: TopLevelRoute = TopLevelRoute.HOME,
        sessionRoutes: List<NavKey> = emptyList(),
    ) = AppNavigationState(
        topLevelRoute = mutableStateOf(topLevelRoute),
        homeBackStack = NavBackStack(Route.Home),
        sessionBackStack = NavBackStack(*sessionRoutes.toTypedArray()),
    )

    private data object SessionChild : NavKey
}
