package com.laxotters.clipy.feature.session.browser.webview

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class SessionNetworkAvailabilityReaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val connectivity = shadowOf(manager)
    private val reader = SessionNetworkAvailabilityReader(context)

    @Test
    fun capabilityChanges_requireInternetAndValidation() {
        val network = checkNotNull(manager.activeNetwork)
        val capabilities = validatedCapabilities()
        connectivity.setNetworkCapabilities(network, capabilities)
        assertEquals(SessionNetworkAvailability.VALIDATED, reader.snapshot())

        shadowOf(capabilities).removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        assertEquals(SessionNetworkAvailability.NOT_VALIDATED, reader.snapshot())

        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadowOf(capabilities).removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        assertEquals(SessionNetworkAvailability.NOT_VALIDATED, reader.snapshot())
    }

    @Test
    fun missingCapabilitiesOrNetwork_returnsUnknownOrUnavailable() {
        connectivity.setNetworkCapabilities(checkNotNull(manager.activeNetwork), null)
        assertEquals(SessionNetworkAvailability.UNKNOWN, reader.snapshot())
        connectivity.setActiveNetworkInfo(null)
        assertEquals(SessionNetworkAvailability.UNAVAILABLE, reader.snapshot())
    }

    private fun validatedCapabilities(): NetworkCapabilities = NetworkCapabilities().also {
        shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
