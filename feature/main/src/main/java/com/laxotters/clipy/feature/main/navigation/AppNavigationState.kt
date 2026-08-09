package com.laxotters.clipy.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.laxotters.clipy.core.navigation.Route

enum class TopLevelRoute {
    HOME,
    SESSION,
}

class AppNavigationState(
    topLevelRoute: MutableState<TopLevelRoute>,
    val homeBackStack: NavBackStack<NavKey>,
    val sessionBackStack: NavBackStack<NavKey>,
) {
    var topLevelRoute: TopLevelRoute by topLevelRoute

    @Composable
    fun toEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
    ): List<NavEntry<NavKey>> {
        // Back stack별 saveable state와 ViewModel scope를 분리하기 위해 decorator를 각각 생성합니다.
        val homeEntryDecorators = rememberBackStackEntryDecorators()
        val sessionEntryDecorators = rememberBackStackEntryDecorators()
        val homeEntries = rememberDecoratedNavEntries(
            backStack = homeBackStack,
            entryDecorators = homeEntryDecorators,
            entryProvider = entryProvider,
        )
        val sessionEntries = rememberDecoratedNavEntries(
            backStack = sessionBackStack,
            entryDecorators = sessionEntryDecorators,
            entryProvider = entryProvider,
        )

        return when (topLevelRoute) {
            TopLevelRoute.HOME -> homeEntries
            TopLevelRoute.SESSION -> homeEntries + sessionEntries
        }
    }
}

@Composable
fun rememberAppNavigationState(): AppNavigationState {
    val topLevelRoute = rememberSaveable {
        mutableStateOf(TopLevelRoute.HOME)
    }
    val homeBackStack = rememberNavBackStack(Route.Home)
    val sessionBackStack = rememberNavBackStack()

    return remember(
        topLevelRoute,
        homeBackStack,
        sessionBackStack,
    ) {
        AppNavigationState(
            topLevelRoute = topLevelRoute,
            homeBackStack = homeBackStack,
            sessionBackStack = sessionBackStack,
        )
    }
}

@Composable
private fun rememberBackStackEntryDecorators(): List<NavEntryDecorator<NavKey>> {
    val saveableStateDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()

    return remember(
        saveableStateDecorator,
        viewModelStoreDecorator,
    ) {
        listOf(
            saveableStateDecorator,
            viewModelStoreDecorator,
        )
    }
}
