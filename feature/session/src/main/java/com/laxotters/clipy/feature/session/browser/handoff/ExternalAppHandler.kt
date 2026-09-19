package com.laxotters.clipy.feature.session.browser.handoff

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.net.toUri
import java.net.URISyntaxException

/** 외부 앱에 전달할 Intent를 정제하고 처리 앱 이름 조회와 OS 실행을 담당합니다. */
internal class ExternalAppHandler(
    val resolveAppName: (Intent) -> String?,
    val launch: (Intent) -> Boolean,
) {
    fun createIntent(uriString: String): Intent? =
        try {
            val intent = if (uriString.startsWith(INTENT_SCHEME_PREFIX, ignoreCase = true)) {
                Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, uriString.toUri())
            }
            sanitizeIntent(intent)
        } catch (_: URISyntaxException) {
            null
        } catch (_: RuntimeException) {
            // Intent를 해석하거나 extra를 정제하지 못하면 일부 값을 지운 채 실행하지 않고 요청 전체를 거절합니다.
            null
        }

    internal fun sanitizeIntent(source: Intent): Intent {
        val sanitized = Intent(source)
        sanitized.component = null
        sanitized.selector = null
        sanitized.flags = sanitized.flags and URI_GRANT_FLAGS.inv()
        sanitized.removeExtra(BROWSER_FALLBACK_URL_EXTRA)
        sanitized.removeNestedIntentExtras()
        sanitized.addCategory(Intent.CATEGORY_BROWSABLE)
        return sanitized
    }

    @Suppress("DEPRECATION")
    private fun Intent.removeNestedIntentExtras() {
        val currentExtras = extras ?: return
        currentExtras.keySet().toList().forEach { key ->
            val value = currentExtras.get(key)
            if (value.containsIntent()) {
                removeExtra(key)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Any?.containsIntent(): Boolean = when (this) {
        is Intent -> true
        is Bundle -> keySet().any { key -> get(key).containsIntent() }
        is Array<*> -> any { value -> value.containsIntent() }
        is Iterable<*> -> any { value -> value.containsIntent() }
        else -> false
    }

    private companion object {
        private const val INTENT_SCHEME_PREFIX = "intent:"
        private const val BROWSER_FALLBACK_URL_EXTRA = "browser_fallback_url"
        private const val URI_GRANT_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}

internal fun createExternalAppHandler(context: Context): ExternalAppHandler =
    ExternalAppHandler(
        resolveAppName = { intent ->
            try {
                val packageManager = context.packageManager
                val resolveInfo = packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY,
                ) ?: return@ExternalAppHandler null
                val resolvedActivity = resolveInfo.activityInfo ?: return@ExternalAppHandler null
                // resolveActivity는 앱 선택 화면도 반환할 수 있어 실제 처리 후보의 Activity와 대조합니다.
                val isHandler = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    .any { candidate ->
                        candidate.activityInfo?.let { activity ->
                            activity.packageName == resolvedActivity.packageName &&
                                activity.name == resolvedActivity.name
                        } == true
                    }
                // 여러 후보가 있어도 기본 앱은 표시하고, 앱 선택 화면은 특정 앱 이름으로 표시하지 않습니다.
                if (!isHandler) {
                    return@ExternalAppHandler null
                }
                resolveInfo.loadLabel(packageManager)
                    ?.toString()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            } catch (_: RuntimeException) {
                null
            }
        },
        launch = { intent ->
            val launchIntent = Intent(intent).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            try {
                context.startActivity(launchIntent)
                true
            } catch (_: RuntimeException) {
                false
            }
        },
    )
