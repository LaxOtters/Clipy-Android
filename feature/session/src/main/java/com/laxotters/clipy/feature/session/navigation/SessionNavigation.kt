package com.laxotters.clipy.feature.session.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.laxotters.clipy.core.navigation.Route
import com.laxotters.clipy.feature.session.SessionRoute
import com.laxotters.clipy.feature.session.SessionViewModel

fun EntryProviderScope<NavKey>.sessionEntry(
    navigateToHome: () -> Unit,
) {
    entry<Route.Session> { route ->
        val viewModel = hiltViewModel<SessionViewModel, SessionViewModel.Factory>(
            creationCallback = { factory -> factory.create(route) },
        )
        SessionRoute(
            viewModel = viewModel,
            navigateToHome = navigateToHome,
        )
    }
}
