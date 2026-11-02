package com.laxotters.clipy.feature.session.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetDefaults.HeaderHeight
import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetValue
import com.laxotters.clipy.core.designsystem.component.bottomsheet.ClipyBottomSheetLayout
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.SessionTopBarState
import com.laxotters.clipy.feature.session.SessionUiState

internal data class TopBarActions(
    val onHomeClick: () -> Unit,
    val onAddItemClick: () -> Unit,
    val onTopBarFoldClick: () -> Unit,
)

internal data class BrowserBarState(
    val urlLabel: String,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val canRefresh: Boolean,
)

internal data class BrowserBarActions(
    val onBackClick: () -> Unit,
    val onForwardClick: () -> Unit,
    val onRefreshClick: () -> Unit,
)

/** Session의 TopBar와 BottomSheet를 렌더링하고 사용자 입력을 호출자에게 전달합니다. */
@Composable
internal fun SessionChrome(
    state: SessionUiState,
    topBarActions: TopBarActions,
    browserBarState: BrowserBarState,
    browserBarActions: BrowserBarActions,
    onBottomSheetValueChange: (BottomSheetValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SessionTopBar(
            topBarState = state.topBarState,
            sessionName = "Untitled",
            isFoldEnabled = !state.isWebViewRootScrolling,
            actions = topBarActions,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(70.dp),
        )
        ClipyBottomSheetLayout(
            value = state.bottomSheetState.toBottomSheetValue(),
            onValueChange = onBottomSheetValueChange,
            modifier = Modifier.fillMaxSize(),
            headerContent = { renderedValue ->
                SheetHeader(
                    value = renderedValue,
                    browserBarState = browserBarState,
                    browserBarActions = browserBarActions,
                )
            },
            sheetContent = { renderedValue -> SheetContent(value = renderedValue) },
        )
    }
}

@Composable
private fun SessionTopBar(
    topBarState: SessionTopBarState,
    sessionName: String,
    isFoldEnabled: Boolean,
    actions: TopBarActions,
    modifier: Modifier = Modifier,
) {
    val foldControlText = when (topBarState) {
        SessionTopBarState.FOLDED -> "펼치기"
        SessionTopBarState.UNFOLDED -> "접기"
    }

    Box(
        modifier = modifier,
    ) {
        if (topBarState == SessionTopBarState.UNFOLDED) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        clip = false,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(
                    text = "홈",
                    onClick = actions.onHomeClick,
                    minWidth = 44.dp,
                )
                Text(
                    text = sessionName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ControlButton(
                    text = "추가",
                    onClick = actions.onAddItemClick,
                    minWidth = 44.dp,
                )
            }
        }
        ControlButton(
            text = foldControlText,
            onClick = actions.onTopBarFoldClick,
            enabled = isFoldEnabled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-2).dp),
            minWidth = 52.dp,
            height = 28.dp,
        )
    }
}

@Composable
private fun SheetHeader(
    value: BottomSheetValue,
    browserBarState: BrowserBarState,
    browserBarActions: BrowserBarActions,
    modifier: Modifier = Modifier,
) {
    when (value) {
        BottomSheetValue.MINIMIZED,
        BottomSheetValue.PEEK,
        -> BrowserBar(
            state = browserBarState,
            actions = browserBarActions,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        BottomSheetValue.HIDDEN,
        BottomSheetValue.EXPANDED,
        -> Unit
    }
}

@Composable
private fun SheetContent(
    value: BottomSheetValue,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (value) {
            BottomSheetValue.HIDDEN -> Unit
            BottomSheetValue.MINIMIZED -> Unit
            BottomSheetValue.PEEK -> PeekPlaceholder()
            BottomSheetValue.EXPANDED -> ExpandedPlaceholder()
        }
    }
}

@Composable
private fun BrowserBar(
    state: BrowserBarState,
    actions: BrowserBarActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            text = "<",
            onClick = actions.onBackClick,
            enabled = state.canGoBack,
            minWidth = 20.dp,
            height = 20.dp,
        )
        ControlButton(
            text = ">",
            onClick = actions.onForwardClick,
            enabled = state.canGoForward,
            minWidth = 20.dp,
            height = 20.dp,
        )
        UrlDisplay(
            url = state.urlLabel,
            modifier = Modifier.weight(1f),
        )
        ControlButton(
            text = "새로고침",
            onClick = actions.onRefreshClick,
            enabled = state.canRefresh,
            minWidth = 64.dp,
            height = 32.dp,
        )
    }
}

@Composable
private fun UrlDisplay(
    url: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(HeaderHeight)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PeekPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(128.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "비교할 아이템 없음",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ExpandedPlaceholder(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Session",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Item ${index + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minWidth: Dp = 44.dp,
    height: Dp = 36.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(
                minWidth = minWidth,
                minHeight = height,
            )
            .height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionChromePreview() {
    ClipyTheme {
        SessionChrome(
            state = SessionUiState(),
            topBarActions = TopBarActions({}, {}, {}),
            browserBarState = BrowserBarState("google.com", false, false, true),
            browserBarActions = BrowserBarActions({}, {}, {}),
            onBottomSheetValueChange = {},
        )
    }
}

private fun BottomSheetState.toBottomSheetValue(): BottomSheetValue =
    when (this) {
        BottomSheetState.HIDDEN -> BottomSheetValue.HIDDEN
        BottomSheetState.MINIMIZED -> BottomSheetValue.MINIMIZED
        BottomSheetState.PEEK -> BottomSheetValue.PEEK
        BottomSheetState.EXPANDED -> BottomSheetValue.EXPANDED
    }
