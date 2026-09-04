package com.laxotters.clipy.core.designsystem.component.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.laxotters.clipy.core.designsystem.R
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.button.ClipyButton
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonType
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

/** primary action 하나를 제공하는 앱 Dialog입니다. */
@Composable
fun ClipySingleDialog(
    title: String,
    description: String,
    primaryAction: ClipyTextAction,
    modifier: Modifier = Modifier,
    style: DialogStyle = DialogStyle.Default,
) {
    ClipyDialogWindow(
        modifier = modifier,
    ) {
        DialogContent(
            title = title,
            description = description,
            primaryAction = primaryAction,
            style = style,
        )
    }
}

/** primary와 secondary action을 제공하는 앱 Dialog입니다. */
@Composable
fun ClipyDualDialog(
    title: String,
    description: String,
    primaryAction: ClipyTextAction,
    secondaryAction: ClipyTextAction,
    modifier: Modifier = Modifier,
    style: DialogStyle = DialogStyle.Default,
) {
    ClipyDialogWindow(
        modifier = modifier,
    ) {
        DialogContent(
            title = title,
            description = description,
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
            style = style,
        )
    }
}

@Composable
internal fun ClipyDialogWindow(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = ClipyDialogProperties,
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val dimAmount = ClipyTheme.colors.overlayBackground.alpha
        SideEffect { window?.setDimAmount(dimAmount) }
        DialogLayout(
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
internal fun DialogLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = DialogDefaults.screenHorizontalPadding,
                vertical = DialogDefaults.screenVerticalPadding,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .widthIn(max = DialogDefaults.maxWidth)
                .fillMaxWidth()
                .dropShadow(
                    shape = DialogDefaults.shape,
                    shadow = DialogDefaults.shadow,
                )
                .clip(DialogDefaults.shape)
                .background(ClipyTheme.colors.primary.indigo50)
                .padding(DialogDefaults.contentPadding),
            verticalArrangement = Arrangement.spacedBy(DialogDefaults.contentSpacing),
            content = content,
        )
    }
}

@Composable
private fun ColumnScope.DialogContent(
    title: String,
    description: String,
    primaryAction: ClipyTextAction,
    secondaryAction: ClipyTextAction? = null,
    style: DialogStyle = DialogStyle.Default,
) {
    DialogMessage(
        title = title,
        description = description,
        modifier = Modifier
            .weight(
                weight = 1f,
                fill = false,
            )
            .then(
                if (style == DialogStyle.Error) {
                    Modifier
                } else {
                    Modifier.padding(top = DialogDefaults.headingTopPadding)
                },
            ),
        showErrorIcon = style == DialogStyle.Error,
    )
    DialogActions(
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
    )
}

@Composable
private fun DialogActions(
    primaryAction: ClipyTextAction,
    secondaryAction: ClipyTextAction?,
) {
    if (secondaryAction == null) {
        ClipyButton(
            text = primaryAction.label,
            onClick = primaryAction.onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DialogDefaults.actionSpacing),
        ) {
            ClipyButton(
                text = secondaryAction.label,
                onClick = secondaryAction.onClick,
                modifier = Modifier.weight(1f),
                type = ButtonType.Secondary,
            )
            ClipyButton(
                text = primaryAction.label,
                onClick = primaryAction.onClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun DialogMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    showErrorIcon: Boolean = false,
) {
    val descriptionScrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (textAlign == TextAlign.Start) {
            Alignment.Start
        } else {
            Alignment.CenterHorizontally
        },
    ) {
        if (showErrorIcon) {
            Image(
                painter = painterResource(R.drawable.ic_circle_exclamation),
                contentDescription = null,
                modifier = Modifier.size(DialogDefaults.iconSize),
            )
            Spacer(modifier = Modifier.height(DialogDefaults.errorIconSpacing))
        }
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            color = ClipyTheme.colors.neutral.gray800,
            style = ClipyTheme.typography.heading2,
            textAlign = textAlign,
        )
        Spacer(modifier = Modifier.height(DialogDefaults.textSpacing))
        Text(
            text = description,
            modifier = Modifier
                .fillMaxWidth()
                .weight(
                    weight = 1f,
                    fill = false,
                )
                .verticalScroll(descriptionScrollState),
            color = ClipyTheme.colors.neutral.gray500,
            style = ClipyTheme.typography.body1Regular,
            textAlign = textAlign,
        )
    }
}

private val ClipyDialogProperties = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
)

@Preview(
    name = "Dialog · App States",
    showBackground = true,
    widthDp = 390,
    heightDp = 800,
)
@Composable
private fun DialogStatesPreview(
    @PreviewParameter(DialogPreviewProvider::class)
    preview: DialogPreview,
) {
    ClipyTheme {
        DialogLayout {
            DialogContent(
                title = preview.title,
                description = preview.description,
                primaryAction = preview.primaryAction,
                secondaryAction = preview.secondaryAction,
                style = preview.style,
            )
        }
    }
}

private data class DialogPreview(
    val title: String,
    val description: String,
    val primaryAction: ClipyTextAction,
    val secondaryAction: ClipyTextAction? = null,
    val style: DialogStyle = DialogStyle.Default,
)

private class DialogPreviewProvider : PreviewParameterProvider<DialogPreview> {
    override val values = sequenceOf(
        DialogPreview(
            title = "연결할 수 없습니다",
            description = "잠시 후 다시 시도해주세요.",
            primaryAction = PreviewConfirmAction,
        ),
        DialogPreview(
            title = "항목을 삭제할까요?",
            description = "삭제한 항목은 되돌릴 수 없습니다.",
            primaryAction = ClipyTextAction(
                label = "삭제",
                onClick = {},
            ),
            secondaryAction = PreviewCancelAction,
        ),
        DialogPreview(
            title = "연결할 수 없습니다",
            description = "잠시 후 다시 시도해주세요.",
            primaryAction = PreviewConfirmAction,
            style = DialogStyle.Error,
        ),
        DialogPreview(
            title = "항목을 삭제할 수 없습니다",
            description = "잠시 후 다시 시도하거나 취소해주세요.",
            primaryAction = ClipyTextAction(
                label = "재시도",
                onClick = {},
            ),
            secondaryAction = PreviewCancelAction,
            style = DialogStyle.Error,
        ),
        DialogPreview(
            title = "이용 안내",
            description = LongDialogDescription,
            primaryAction = PreviewConfirmAction,
        ),
    )
}

private val LongDialogDescription = (
    """
        긴 본문은 제목과 버튼의 위치를 유지한 채 본문 영역 안에서만 스크롤됩니다.
        사용자는 내용을 위아래로 이동해 모두 확인할 수 있습니다.
        화면 높이가 충분하면 별도의 스크롤 없이 내용 높이에 맞춰 표시됩니다.
        화면 높이가 부족한 경우에도 확인과 취소 버튼은 항상 같은 위치에 남습니다.
        입력창이 있는 경우에는 본문만 스크롤되고 입력창은 고정됩니다.
    """.trimIndent() + "\n"
    ).repeat(6)

private val PreviewConfirmAction = ClipyTextAction(
    label = "확인",
    onClick = {},
)

private val PreviewCancelAction = ClipyTextAction(
    label = "취소",
    onClick = {},
)
