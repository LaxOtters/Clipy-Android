package com.laxotters.clipy.core.designsystem.component.snackbar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect as AndroidRect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.createBitmap
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
internal fun BoxScope.SnackbarBackdrop(backdropBitmap: Bitmap?) {
    if (backdropBitmap != null && !backdropBitmap.isRecycled) {
        Image(
            bitmap = backdropBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .blur(
                    radius = SnackbarDefaults.blurRadius,
                    edgeTreatment = BlurredEdgeTreatment(SnackbarDefaults.shape),
                ),
            contentScale = ContentScale.FillBounds,
        )
    }
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(ClipyTheme.colors.alpha.black60),
    )
}

/** 지정된 Window 영역을 Snackbar 배경 Bitmap으로 복사합니다. */
@RequiresApi(Build.VERSION_CODES.O)
internal suspend fun captureSnackbarBackdrop(
    context: Context,
    sourceRect: AndroidRect,
): Bitmap? {
    val window = context.findActivity()?.window ?: return null
    if (sourceRect.width() <= 0 || sourceRect.height() <= 0) {
        return null
    }

    val bitmap = createBitmap(sourceRect.width(), sourceRect.height())

    return suspendCancellableCoroutine { continuation ->
        try {
            PixelCopy.request(
                window,
                sourceRect,
                bitmap,
                { result ->
                    // 취소된 요청의 Bitmap은 PixelCopy가 완료된 뒤 해제합니다.
                    if (!continuation.isActive) {
                        bitmap.recycleIfNeeded()
                    } else if (result == PixelCopy.SUCCESS) {
                        continuation.resume(
                            value = bitmap,
                            onCancellation = { _, capturedBitmap, _ ->
                                capturedBitmap.recycleIfNeeded()
                            },
                        )
                    } else {
                        bitmap.recycleIfNeeded()
                        continuation.resumeWithoutBitmap()
                    }
                },
                PixelCopyCallbackHandler,
            )
        } catch (_: IllegalArgumentException) {
            bitmap.recycleIfNeeded()
            continuation.resumeWithoutBitmap()
        }
    }
}

/** PixelCopy 요청을 Bitmap 결과 없이 완료합니다. */
private fun CancellableContinuation<Bitmap?>.resumeWithoutBitmap() {
    resumeWith(Result.success(null))
}

private val PixelCopyCallbackHandler = Handler(Looper.getMainLooper())

internal fun Bitmap.recycleIfNeeded() {
    if (!isRecycled) recycle()
}

internal fun Rect.toAndroidRect(): AndroidRect = AndroidRect(
    floor(left).toInt(),
    floor(top).toInt(),
    ceil(right).toInt(),
    ceil(bottom).toInt(),
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
