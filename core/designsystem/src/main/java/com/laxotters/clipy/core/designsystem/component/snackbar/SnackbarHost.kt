package com.laxotters.clipy.core.designsystem.component.snackbar

import android.graphics.Bitmap
import android.graphics.Rect as AndroidRect
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 앱 콘텐츠와 Snackbar 영역을 함께 배치하고, Snackbar 영역 밖 터치로 현재 Snackbar를 닫습니다. */
@Composable
fun ClipySnackbarLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val manager = rememberClipySnackbarManager()
    val hostState = manager.hostState
    var hostPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var outsideTapState by remember { mutableStateOf(SnackbarOutsideTapState()) }

    CompositionLocalProvider(
        LocalClipySnackbarManager provides manager,
    ) {
        Box(
            modifier = modifier
                .onGloballyPositioned { hostPositionInRoot = it.positionInRoot() }
                .notifySnackbarPointerDown { position ->
                    val shouldDismiss = outsideTapState.shouldDismiss(
                        currentSnackbarData = hostState.currentSnackbarData,
                        tapPosition = position,
                    )
                    if (shouldDismiss) {
                        hostState.currentSnackbarData?.dismiss()
                    }
                },
        ) {
            content()
            ClipySnackbarHost(
                hostState = hostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = SnackbarDefaults.screenHorizontalPadding,
                        top = SnackbarDefaults.statusBarSpacing,
                        end = SnackbarDefaults.screenHorizontalPadding,
                    ),
                onBoundsChanged = { snackbarData, boundsInRoot ->
                    outsideTapState = outsideTapState.updateBounds(
                        activeSnackbarData = hostState.currentSnackbarData,
                        snackbarData = snackbarData,
                        bounds = boundsInRoot.translate(-hostPositionInRoot),
                    )
                },
                onOutsideTapEnabled = { snackbarData ->
                    outsideTapState = outsideTapState.enableDismiss(
                        activeSnackbarData = hostState.currentSnackbarData,
                        snackbarData = snackbarData,
                    )
                },
                onDisposed = { snackbarData ->
                    outsideTapState = outsideTapState.clearIfMatches(snackbarData)
                },
            )
        }
    }
}

@Composable
private fun ClipySnackbarHost(
    hostState: SnackbarHostState,
    onBoundsChanged: (
        snackbarData: SnackbarData,
        boundsInRoot: Rect,
    ) -> Unit,
    onOutsideTapEnabled: (SnackbarData) -> Unit,
    onDisposed: (SnackbarData) -> Unit,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { snackbarData ->
        SnackbarEntry(
            snackbarData = snackbarData,
            onBoundsChanged = { boundsInRoot ->
                onBoundsChanged(
                    snackbarData,
                    boundsInRoot,
                )
            },
            onOutsideTapEnabled = {
                onOutsideTapEnabled(snackbarData)
            },
            onDisposed = {
                onDisposed(snackbarData)
            },
        )
    }
}

@Composable
private fun SnackbarEntry(
    snackbarData: SnackbarData,
    onBoundsChanged: (Rect) -> Unit,
    onOutsideTapEnabled: () -> Unit,
    onDisposed: () -> Unit,
) {
    val supportsBackdropBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var backdropState by remember(snackbarData) {
        mutableStateOf<SnackbarBackdropState>(
            if (supportsBackdropBlur) {
                SnackbarBackdropState.Pending
            } else {
                SnackbarBackdropState.Fallback
            },
        )
    }
    // Snackbar 배경을 준비할 영역이며, 레이아웃이 완료되면 Window 좌표로 설정됩니다.
    var backdropBounds by remember(snackbarData) {
        mutableStateOf<AndroidRect?>(null)
    }
    val isBackdropResolved = backdropState !is SnackbarBackdropState.Pending
    val bitmap = (backdropState as? SnackbarBackdropState.Captured)?.bitmap

    SideEffect {
        if (isBackdropResolved) {
            onOutsideTapEnabled()
        }
    }

    SnackbarEntryLifecycleEffect(
        snackbarData = snackbarData,
        captureBounds = { backdropBounds },
        onBackdropStateChanged = { backdropState = it },
    )

    DisposableEffect(snackbarData) {
        onDispose {
            (backdropState as? SnackbarBackdropState.Captured)?.bitmap?.recycleIfNeeded()
            onDisposed()
        }
    }

    SnackbarSurface(
        visuals = snackbarData.visuals,
        backdropBitmap = bitmap,
        isVisible = isBackdropResolved,
        onActionClick = snackbarData::performAction,
        onDismiss = snackbarData::dismiss,
        modifier = Modifier.onGloballyPositioned { coordinates ->
            onBoundsChanged(coordinates.boundsInRoot())
            if (supportsBackdropBlur && backdropBounds == null) {
                backdropBounds = coordinates.boundsInWindow().toAndroidRect()
            }
        },
    )
}

/**
 * Snackbar 배경이 준비되는 시점과 표시 종료 시점을 관리합니다.
 *
 * [captureBounds]는 배경을 준비할 Snackbar 영역을 제공합니다.
 * [onBackdropStateChanged]는 준비된 배경 상태를 호출부에 전달합니다.
 */
@Composable
private fun SnackbarEntryLifecycleEffect(
    snackbarData: SnackbarData,
    captureBounds: () -> AndroidRect?,
    onBackdropStateChanged: (SnackbarBackdropState) -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(snackbarData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val capturedBitmap = withTimeoutOrNull(
                SnackbarPolicy.BACKDROP_CAPTURE_TIMEOUT_MILLIS,
            ) {
                val snackbarBounds = snapshotFlow(captureBounds)
                    .filterNotNull()
                    .first()
                captureSnackbarBackdrop(
                    context = context,
                    sourceRect = snackbarBounds,
                )
            }
            onBackdropStateChanged(
                capturedBitmap?.let(SnackbarBackdropState::Captured)
                    ?: SnackbarBackdropState.Fallback,
            )
        }

        delay(SnackbarPolicy.DISPLAY_DURATION_MILLIS)
        snackbarData.dismiss()
    }
}

private sealed interface SnackbarBackdropState {
    data object Pending : SnackbarBackdropState
    data object Fallback : SnackbarBackdropState
    data class Captured(val bitmap: Bitmap) : SnackbarBackdropState
}

/** Snackbar 영역 밖 터치 dismiss 여부를 판단하고, 이벤트는 소비하지 않습니다. */
private fun Modifier.notifySnackbarPointerDown(
    onPointerDown: (Offset) -> Unit,
): Modifier = this then SnackbarPointerDownElement(onPointerDown)

private data class SnackbarPointerDownElement(
    val onPointerDown: (Offset) -> Unit,
) : ModifierNodeElement<SnackbarPointerDownNode>() {
    override fun create(): SnackbarPointerDownNode = SnackbarPointerDownNode(onPointerDown)

    override fun update(node: SnackbarPointerDownNode) {
        node.onPointerDown = onPointerDown
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "notifySnackbarPointerDown"
    }
}

private class SnackbarPointerDownNode(
    var onPointerDown: (Offset) -> Unit,
) : Modifier.Node(), PointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass != PointerEventPass.Initial) {
            return
        }
        pointerEvent.changes
            .filter { it.pressed && !it.previousPressed }
            .forEach { onPointerDown(it.position) }
    }

    override fun onCancelPointerInput() = Unit
}
