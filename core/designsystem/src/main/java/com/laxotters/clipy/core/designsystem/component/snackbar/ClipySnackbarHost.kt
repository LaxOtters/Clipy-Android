package com.laxotters.clipy.core.designsystem.component.snackbar

import android.graphics.Bitmap
import android.graphics.Rect as AndroidRect
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.R
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
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
    val context = LocalContext.current
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
    var captureRect by remember(snackbarData) { mutableStateOf<AndroidRect?>(null) }
    val isBackdropResolved = backdropState !is SnackbarBackdropState.Pending
    val bitmap = (backdropState as? SnackbarBackdropState.Captured)?.bitmap

    SideEffect {
        if (isBackdropResolved) {
            onOutsideTapEnabled()
        }
    }

    LaunchedEffect(snackbarData) {
        if (supportsBackdropBlur) {
            val capturedBitmap = withTimeoutOrNull(SnackbarPolicy.BACKDROP_CAPTURE_TIMEOUT_MILLIS) {
                val rect = snapshotFlow { captureRect }
                    .filterNotNull()
                    .first()
                captureSnackbarBackdrop(
                    context = context,
                    sourceRect = rect,
                )
            }
            backdropState = capturedBitmap?.let { SnackbarBackdropState.Captured(it) }
                ?: SnackbarBackdropState.Fallback
        }

        delay(SnackbarPolicy.DISPLAY_DURATION_MILLIS)
        snackbarData.dismiss()
    }

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
        onPositioned = { rootBounds, windowBounds ->
            onBoundsChanged(rootBounds)
            if (supportsBackdropBlur && captureRect == null) {
                captureRect = windowBounds.toAndroidRect()
            }
        },
    )
}

@Composable
private fun SnackbarSurface(
    visuals: SnackbarVisuals,
    backdropBitmap: Bitmap?,
    isVisible: Boolean,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit,
    onPositioned: (
        rootBounds: Rect,
        windowBounds: Rect,
    ) -> Unit = { _, _ -> },
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(
                if (isVisible) {
                    1f
                } else {
                    0f
                },
            )
            .dropShadow(
                shape = SnackbarDefaults.shape,
                shadow = SnackbarDefaults.shadow,
            )
            .clip(SnackbarDefaults.shape)
            .then(
                if (isVisible) {
                    Modifier.consumeSnackbarPointerInput()
                } else {
                    Modifier
                },
            )
            .onGloballyPositioned { coordinates ->
                onPositioned(
                    coordinates.boundsInRoot(),
                    coordinates.boundsInWindow(),
                )
            }
            .then(
                if (isVisible) {
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        dismiss {
                            onDismiss()
                            true
                        }
                    }
                } else {
                    Modifier.clearAndSetSemantics {}
                },
            ),
    ) {
        SnackbarBackdrop(backdropBitmap)
        SnackbarContent(
            message = visuals.message,
            actionLabel = visuals.actionLabel,
            iconRes = (visuals as? ClipySnackbarVisuals)?.icon?.drawableRes,
            actionEnabled = isVisible,
            onActionClick = onActionClick,
        )
    }
}

/** Snackbar 영역의 터치를 소비해 아래 앱 콘텐츠에 전달하지 않습니다. */
private fun Modifier.consumeSnackbarPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Main)
                .changes
                .forEach { change ->
                    if (!change.isConsumed) {
                        change.consume()
                    }
                }
        }
    }
}

@Composable
private fun SnackbarContent(
    message: String,
    actionLabel: String?,
    @DrawableRes iconRes: Int?,
    actionEnabled: Boolean,
    onActionClick: () -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SnackbarDefaults.contentHorizontalPadding,
            ),
    ) {
        val actionLayout = actionLabel?.let {
            measureSnackbarActionLayout(
                message = message,
                actionLabel = it,
                availableWidth = constraints.maxWidth,
                textMeasurer = textMeasurer,
            )
        } ?: SnackbarActionLayout.Inline

        when (actionLayout) {
            SnackbarActionLayout.Inline -> InlineSnackbarContent(
                message = message,
                actionLabel = actionLabel,
                iconRes = iconRes,
                actionEnabled = actionEnabled,
                onActionClick = onActionClick,
            )

            SnackbarActionLayout.Stacked -> StackedSnackbarContent(
                message = message,
                actionLabel = requireNotNull(actionLabel),
                iconRes = iconRes,
                actionEnabled = actionEnabled,
                onActionClick = onActionClick,
            )
        }
    }
}

@Composable
private fun InlineSnackbarContent(
    message: String,
    actionLabel: String?,
    @DrawableRes iconRes: Int?,
    actionEnabled: Boolean,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SnackbarDefaults.contentSpacing),
    ) {
        SnackbarMessage(
            message = message,
            iconRes = iconRes,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = SnackbarDefaults.contentVerticalPadding),
        )
        if (actionLabel != null) {
            SnackbarAction(
                label = actionLabel,
                enabled = actionEnabled,
                onClick = onActionClick,
                modifier = Modifier.padding(vertical = SnackbarDefaults.contentVerticalPadding),
            )
        }
    }
}

@Composable
private fun StackedSnackbarContent(
    message: String,
    actionLabel: String,
    @DrawableRes iconRes: Int?,
    actionEnabled: Boolean,
    onActionClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        SnackbarMessage(
            message = message,
            iconRes = iconRes,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SnackbarDefaults.contentVerticalPadding),
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            SnackbarAction(
                label = actionLabel,
                enabled = actionEnabled,
                onClick = onActionClick,
                modifier = Modifier.padding(
                    top = SnackbarDefaults.stackedActionSpacing,
                    bottom = SnackbarDefaults.contentVerticalPadding,
                ),
            )
        }
    }
}

@Composable
private fun measureSnackbarActionLayout(
    message: String,
    actionLabel: String,
    availableWidth: Int,
    textMeasurer: TextMeasurer,
): SnackbarActionLayout {
    val density = LocalDensity.current
    val messageWidth = message
        .split('\n')
        .maxOf { line ->
            textMeasurer.measure(
                text = AnnotatedString(line),
                style = ClipyTheme.typography.body1Medium,
                overflow = TextOverflow.Clip,
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(),
            ).size.width
        }
    val actionWidth = textMeasurer.measure(
        text = AnnotatedString(actionLabel),
        style = ClipyTheme.typography.body1SemiBold,
        overflow = TextOverflow.Clip,
        softWrap = false,
        maxLines = 1,
        constraints = Constraints(),
    ).size.width
    val spacing = with(density) { SnackbarDefaults.contentSpacing.roundToPx() }

    return SnackbarPolicy.resolveActionLayout(
        messageWidth = messageWidth,
        actionWidth = actionWidth,
        messageActionSpacing = spacing,
        availableWidth = availableWidth,
    )
}

@Composable
private fun SnackbarMessage(
    message: String,
    @DrawableRes iconRes: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SnackbarDefaults.iconSpacing),
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(SnackbarDefaults.iconSize),
            )
        }
        Text(
            text = message,
            modifier = Modifier.weight(
                weight = 1f,
                fill = false,
            ),
            color = ClipyTheme.colors.primary.indigo50,
            style = ClipyTheme.typography.body1Medium,
        )
    }
}

@Composable
private fun SnackbarAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = Modifier
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(modifier),
        color = ClipyTheme.colors.primary.indigo300,
        style = ClipyTheme.typography.body1SemiBold,
        maxLines = 1,
    )
}

private sealed interface SnackbarBackdropState {
    data object Pending : SnackbarBackdropState
    data object Fallback : SnackbarBackdropState
    data class Captured(val bitmap: Bitmap) : SnackbarBackdropState
}

@Composable
private fun SnackbarPreviewSurface(
    message: String,
    actionLabel: String? = null,
    icon: ClipySnackbarIcon? = null,
) {
    ClipyTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClipyTheme.colors.neutral.gray100)
                .padding(20.dp),
        ) {
            SnackbarSurface(
                visuals = ClipySnackbarVisuals(
                    message = message,
                    actionLabel = actionLabel,
                    icon = icon,
                ),
                backdropBitmap = null,
                isVisible = true,
                onActionClick = {},
                onDismiss = {},
            )
        }
    }
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

internal object SnackbarDefaults {
    val screenHorizontalPadding = 20.dp
    val statusBarSpacing = 10.dp
    val contentHorizontalPadding = 16.dp
    val contentVerticalPadding = 12.dp
    val contentSpacing = 12.dp
    val stackedActionSpacing = 12.dp
    val iconSize = 24.dp
    val iconSpacing = 12.dp
    val shape = RoundedCornerShape(8.dp)
    val blurRadius = 10.dp
    val shadow = Shadow(
        radius = 8.dp,
        spread = 0.dp,
        offset = DpOffset(
            x = 0.dp,
            y = 4.dp,
        ),
        color = Color.Black.copy(alpha = 0.10f),
    )
}

@Preview(
    name = "Snackbar",
    showBackground = true,
    widthDp = 390,
)
@Composable
private fun SnackbarPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SnackbarPreviewSurface(message = "text")
        SnackbarPreviewSurface(message = "text\ntext")
        SnackbarPreviewSurface(
            message = "text",
            actionLabel = "Action",
        )
        SnackbarPreviewSurface(
            message = "text\ntext",
            actionLabel = "Action",
        )
        SnackbarPreviewSurface(
            message = "Long text Snackbar Example",
            actionLabel = "Long Action Example",
        )
        SnackbarPreviewSurface(
            message = "Long text Snackbar Example\nLong text Snackbar Example",
            actionLabel = "Long Action Example",
        )
        SnackbarPreviewSurface(
            message = "Error",
            icon = ClipySnackbarIcon.Error,
        )
        SnackbarPreviewSurface(
            message = "Saved",
            icon = ClipySnackbarIcon.Success,
        )
        SnackbarPreviewSurface(
            message = "Saved\nYou can continue browsing.",
            icon = ClipySnackbarIcon.Success,
        )
    }
}

@get:DrawableRes
private val ClipySnackbarIcon.drawableRes: Int
    get() = when (this) {
        ClipySnackbarIcon.Success -> R.drawable.ic_circle_check
        ClipySnackbarIcon.Error -> R.drawable.ic_circle_exclamation_red
    }
