package com.laxotters.clipy.feature.session.browser.webview

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability

/** 요청 시점의 기본 네트워크 검증 상태를 조회합니다. */
internal class SessionNetworkAvailabilityReader(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    fun snapshot(): SessionNetworkAvailability {
        val network = manager.activeNetwork
        return if (network == null) {
            SessionNetworkAvailability.UNAVAILABLE
        } else {
            manager.getNetworkCapabilities(network)?.toAvailability() ?: SessionNetworkAvailability.UNKNOWN
        }
    }
}

private fun NetworkCapabilities.toAvailability(): SessionNetworkAvailability =
    if (hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    ) {
        SessionNetworkAvailability.VALIDATED
    } else {
        SessionNetworkAvailability.NOT_VALIDATED
    }
