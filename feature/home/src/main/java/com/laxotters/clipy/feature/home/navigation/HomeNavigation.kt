package com.laxotters.clipy.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.laxotters.clipy.core.navigation.Route
import com.laxotters.clipy.feature.home.HomeRoute

fun EntryProviderScope<NavKey>.homeEntry(
    navigateToSession: (sessionId: String) -> Unit,
) {
    entry<Route.Home> {
        HomeRoute(onSessionClick = navigateToSession)
    }
}
