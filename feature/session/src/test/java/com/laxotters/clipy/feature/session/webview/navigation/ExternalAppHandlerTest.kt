package com.laxotters.clipy.feature.session.webview.navigation

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ExternalAppHandlerTest {
    private val context = RuntimeEnvironment.getApplication()
    private val packageManager = context.packageManager
    private val appHandler = createExternalAppHandler(context)
    private val intent = requireNotNull(appHandler.createIntent("clipy-auth://callback"))

    @Test
    fun multipleHandlers_resolveAppName_returnsPreferredAppName() {
        val first = handler("com.example.first", "FirstActivity", "First app")
        val preferred = handler("com.example.preferred", "AuthActivity", "Default app")
        val shadow = shadowOf(packageManager)
        shadow.setResolveInfosForIntent(intent, listOf(first, preferred))
        shadow.setShouldShowActivityChooser(true)
        val components = arrayOf(first, preferred).map { info ->
            ComponentName(info.activityInfo.packageName, info.activityInfo.name)
        }.toTypedArray()
        val filter = IntentFilter(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addDataScheme("clipy-auth")
        }
        packageManager.addPreferredActivity(filter, IntentFilter.MATCH_CATEGORY_SCHEME, components, components[1])

        assertEquals("Default app", appHandler.resolveAppName(intent))
    }

    @Test
    fun multipleHandlersWithoutDefault_resolveAppName_returnsNull() {
        val shadow = shadowOf(packageManager)
        shadow.setResolveInfosForIntent(
            intent,
            listOf(
                handler("android", "RealHandler", "System app"),
                handler("com.example.second", "AuthActivity", "Second app"),
            ),
        )
        shadow.setShouldShowActivityChooser(true)

        assertNull(appHandler.resolveAppName(intent))
    }

    @Test
    fun realSystemHandler_resolveAppName_keepsItsLabel() {
        shadowOf(packageManager).setResolveInfosForIntent(
            intent,
            listOf(handler("android", "RealHandler", "System app")),
        )

        assertEquals("System app", appHandler.resolveAppName(intent))
    }

    @Test
    fun missingHandler_resolveAppName_returnsNull() {
        assertNull(appHandler.resolveAppName(intent))
    }

    private fun handler(packageName: String, activityName: String, label: String): ResolveInfo =
        ResolveInfo().apply {
            isDefault = true
            nonLocalizedLabel = label
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.$activityName"
                applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                }
            }
        }

    @Test
    fun appUri_create_buildsBrowsableViewIntent() {
        val intent = requireNotNull(
            appHandler.createIntent("clipy-auth://callback?token=value"),
        )

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse("clipy-auth://callback?token=value"), intent.data)
        assertEquals(setOf(Intent.CATEGORY_BROWSABLE), intent.categories)
        assertNull(intent.component)
        assertNull(intent.selector)
    }

    @Test
    fun intentUri_create_preservesPackageActionAndRegularExtras() {
        val intent = requireNotNull(
            appHandler.createIntent(
                "intent://callback/#Intent;" +
                    "scheme=clipy-auth;" +
                    "package=com.example.auth;" +
                    "action=com.example.AUTHENTICATE;" +
                    "category=com.example.AUTH;" +
                    "S.token=kept;" +
                    "S.browser_fallback_url=https%3A%2F%2Fexample.com%2Ffallback;" +
                    "end",
            ),
        )

        assertEquals("com.example.auth", intent.`package`)
        assertEquals("com.example.AUTHENTICATE", intent.action)
        assertEquals("kept", intent.getStringExtra("token"))
        assertFalse(intent.hasExtra("browser_fallback_url"))
        assertEquals(setOf("com.example.AUTH", Intent.CATEGORY_BROWSABLE), intent.categories)
    }

    @Test
    fun unsafeIntent_sanitize_removesTargetsGrantsAndNestedIntents() {
        val nestedBundle = Bundle().apply {
            putParcelable("intent", Intent(Intent.ACTION_VIEW, Uri.parse("file:///tmp/private")))
        }
        val source = Intent("com.example.AUTHENTICATE", Uri.parse("clipy-auth://callback")).apply {
            setPackage("com.example.auth")
            component = ComponentName("com.example.auth", "com.example.auth.HiddenActivity")
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            )
            putExtra("browser_fallback_url", "https://example.com/fallback")
            putExtra("token", "kept")
            putExtra("nested_intent", Intent(Intent.ACTION_VIEW, Uri.parse("file:///tmp/nested")))
            putExtra("nested_bundle", nestedBundle)
        }

        val sanitized = appHandler.sanitizeIntent(source)

        assertEquals("com.example.auth", sanitized.`package`)
        assertEquals("com.example.AUTHENTICATE", sanitized.action)
        assertEquals(Uri.parse("clipy-auth://callback"), sanitized.data)
        assertEquals("kept", sanitized.getStringExtra("token"))
        assertNull(sanitized.component)
        assertNull(sanitized.selector)
        assertFalse(sanitized.hasExtra("browser_fallback_url"))
        assertFalse(sanitized.hasExtra("nested_intent"))
        assertFalse(sanitized.hasExtra("nested_bundle"))
        assertEquals(setOf(Intent.CATEGORY_DEFAULT, Intent.CATEGORY_BROWSABLE), sanitized.categories)
        assertTrue(sanitized.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertEquals(0, sanitized.flags and URI_GRANT_FLAGS)
    }

    @Test
    fun selectorIntent_sanitize_removesNestedSelector() {
        val source = Intent("com.example.AUTHENTICATE", Uri.parse("clipy-auth://callback")).apply {
            selector = Intent(Intent.ACTION_VIEW, Uri.parse("file:///tmp/selector"))
        }

        val sanitized = appHandler.sanitizeIntent(source)

        assertNull(sanitized.selector)
        assertEquals("com.example.AUTHENTICATE", sanitized.action)
        assertEquals(Uri.parse("clipy-auth://callback"), sanitized.data)
    }

    @Test
    fun malformedExtra_create_rejectsEntireRequest() {
        val intent = appHandler.createIntent(
            "intent://callback/#Intent;scheme=clipy-auth;S.token=kept;i.count=invalid;end",
        )

        assertNull(intent)
    }

    private companion object {
        const val URI_GRANT_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}
