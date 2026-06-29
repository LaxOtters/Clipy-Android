package com.laxotters.clipy.feature.session.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.laxotters.clipy.core.navigation.Route
import com.laxotters.clipy.feature.session.SessionRoute

fun EntryProviderScope<NavKey>.sessionScreen() {
    entry<Route.Session> { route ->
        SessionRoute(
            sessionId = route.sessionId,
        )
    }
}
