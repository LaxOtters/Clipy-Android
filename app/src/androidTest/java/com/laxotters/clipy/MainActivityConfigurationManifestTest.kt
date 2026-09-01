package com.laxotters.clipy

import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laxotters.clipy.feature.main.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityConfigurationManifestTest {
    @Test
    fun mainActivity_handlesEveryWebViewContinuityConfiguration() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

        assertEquals(
            EXPECTED_CONFIG_CHANGES,
            activityInfo.configChanges and EXPECTED_CONFIG_CHANGES,
        )
    }

    private companion object {
        const val EXPECTED_CONFIG_CHANGES =
            ActivityInfo.CONFIG_ORIENTATION or
                ActivityInfo.CONFIG_SCREEN_SIZE or
                ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
                ActivityInfo.CONFIG_SCREEN_LAYOUT or
                ActivityInfo.CONFIG_UI_MODE or
                ActivityInfo.CONFIG_FONT_SCALE or
                ActivityInfo.CONFIG_FONT_WEIGHT_ADJUSTMENT or
                ActivityInfo.CONFIG_DENSITY
    }
}
