package com.laxotters.clipy.core.designsystem.component.snackbar

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.R
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

@Composable
internal fun SnackbarSurface(
    visuals: SnackbarVisuals,
    backdropBitmap: Bitmap?,
    isVisible: Boolean,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionModifier = if (isVisible) {
        Modifier
            .consumeSnackbarPointerInput()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                dismiss {
                    onDismiss()
                    true
                }
            }
    } else {
        Modifier.clearAndSetSemantics {}
    }

    Box(
        modifier = modifier
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
            .then(interactionModifier),
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
                contentPadding = PaddingValues(
                    vertical = SnackbarDefaults.contentVerticalPadding,
                ),
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
                contentPadding = PaddingValues(
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
    contentPadding: PaddingValues,
) {
    Text(
        text = label,
        modifier = Modifier
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        color = ClipyTheme.colors.primary.indigo300,
        style = ClipyTheme.typography.body1SemiBold,
        maxLines = 1,
    )
}

/** Snackbar 영역 내에서는 action만 터치로 동작하고, 나머지 터치는 아래 화면으로 전달하지 않습니다. */
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
