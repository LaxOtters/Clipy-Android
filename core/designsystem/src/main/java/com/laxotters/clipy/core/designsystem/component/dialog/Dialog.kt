package com.laxotters.clipy.core.designsystem.component.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.laxotters.clipy.core.designsystem.R
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction
import com.laxotters.clipy.core.designsystem.component.button.ClipyButton
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonType
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

/** 앱 Dialog의 기본·오류 시각 타입입니다. */
enum class ClipyDialogStyle {
    Default,
    Error,
}

/** JavaScript Dialog의 화면 상태와 action을 정의합니다. */
sealed interface ClipyJsDialogState {
    val title: String
    val description: String

    data class Alert(
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextAction,
    ) : ClipyJsDialogState

    data class Confirm(
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextAction,
        val cancelAction: ClipyTextAction,
    ) : ClipyJsDialogState

    data class Prompt(
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextInputAction,
        val initialValue: String,
        val cancelAction: ClipyTextAction,
    ) : ClipyJsDialogState
}

/** primary action 하나를 제공하는 앱 Dialog입니다. */
@Composable
fun ClipySingleDialog(
    title: String,
    description: String,
    primaryAction: ClipyTextAction,
    modifier: Modifier = Modifier,
    style: ClipyDialogStyle = ClipyDialogStyle.Default,
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
    style: ClipyDialogStyle = ClipyDialogStyle.Default,
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

/** 웹 콘텐츠가 요청한 alert, confirm, prompt를 네이티브 UI로 표시합니다. */
@Composable
fun ClipyJsDialog(
    source: String,
    state: ClipyJsDialogState,
    modifier: Modifier = Modifier,
) {
    ClipyDialogWindow(
        modifier = modifier,
    ) {
        JsDialogContent(
            source = source,
            state = state,
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
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val verticalPadding = DialogDefaults.screenVerticalPadding * 2
        val maxDialogHeight = (maxHeight - verticalPadding).coerceAtLeast(0.dp)

        ClipyDialogSurface(
            modifier = modifier.heightIn(max = maxDialogHeight),
            content = content,
        )
    }
}

@Composable
internal fun ClipyDialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .padding(horizontal = DialogDefaults.screenHorizontalPadding)
            .widthIn(max = DialogDefaults.maxWidth)
            .fillMaxWidth()
            .dropShadow(
                shape = DialogDefaults.shape,
                shadow = DialogDefaults.shadow,
            )
            .clip(DialogDefaults.shape)
            .background(ClipyTheme.colors.primary.indigo50),
    ) {
        Column(
            modifier = Modifier.padding(DialogDefaults.contentPadding),
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
    style: ClipyDialogStyle = ClipyDialogStyle.Default,
) {
    DialogTextContent(
        title = title,
        description = description,
        modifier = Modifier
            .weight(
                weight = 1f,
                fill = false,
            )
            .then(
                if (style == ClipyDialogStyle.Error) {
                    Modifier
                } else {
                    Modifier.padding(top = DialogDefaults.headingTopPadding)
                },
            ),
        showErrorIcon = style == ClipyDialogStyle.Error,
    )
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
internal fun DialogTextContent(
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

@Composable
private fun ColumnScope.JsDialogContent(
    source: String,
    state: ClipyJsDialogState,
) {
    val promptInitialValue = (state as? ClipyJsDialogState.Prompt)
        ?.initialValue
        .orEmpty()
    var promptValue by rememberSaveable(
        source,
        state.title,
        state.description,
        promptInitialValue,
    ) {
        mutableStateOf(promptInitialValue)
    }

    JsDialogBody(
        source = source,
        state = state,
        promptValue = promptValue,
        onPromptValueChange = { promptValue = it },
        modifier = Modifier.weight(
            weight = 1f,
            fill = false,
        ),
    )
    when (state) {
        is ClipyJsDialogState.Alert -> {
            JsDialogActions(
                primaryAction = state.confirmAction,
            )
        }

        is ClipyJsDialogState.Confirm -> {
            JsDialogActions(
                primaryAction = state.confirmAction,
                secondaryAction = state.cancelAction,
            )
        }

        is ClipyJsDialogState.Prompt -> {
            JsDialogActions(
                primaryAction = ClipyTextAction(
                    label = state.confirmAction.label,
                    onClick = {
                        val confirmedValue = promptValue
                        promptValue = promptInitialValue
                        state.confirmAction.onClick(confirmedValue)
                    },
                ),
                secondaryAction = ClipyTextAction(
                    label = state.cancelAction.label,
                    onClick = {
                        promptValue = promptInitialValue
                        state.cancelAction.onClick()
                    },
                ),
            )
        }
    }
}

@Composable
private fun JsDialogBody(
    source: String,
    state: ClipyJsDialogState,
    promptValue: String,
    onPromptValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(JsDialogDefaults.bodySpacing),
    ) {
        val messageModifier = Modifier
            .weight(
                weight = 1f,
                fill = false,
            )
            .then(
                if (state is ClipyJsDialogState.Alert) {
                    Modifier
                } else {
                    Modifier.padding(top = DialogDefaults.headingTopPadding)
                },
            )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JsDialogDefaults.sourceSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_circle_exclamation_gray),
                contentDescription = null,
                modifier = Modifier.size(JsDialogDefaults.sourceIconSize),
            )
            Text(
                text = source,
                color = ClipyTheme.colors.neutral.gray700,
                style = ClipyTheme.typography.body1Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        JsDialogMessage(
            title = state.title,
            description = state.description,
            modifier = messageModifier,
            textAlign = if (state is ClipyJsDialogState.Prompt) {
                TextAlign.Start
            } else {
                TextAlign.Center
            },
        ) {
            if (state is ClipyJsDialogState.Prompt) {
                JsPromptInput(
                    value = promptValue,
                    onValueChange = onPromptValueChange,
                )
            }
        }
    }
}

@Composable
private fun JsDialogMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    promptInput: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val contentScrollState = rememberScrollState()
    val horizontalAlignment = if (textAlign == TextAlign.Start) {
        Alignment.Start
    } else {
        Alignment.CenterHorizontally
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
    ) {
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
                .verticalScroll(contentScrollState),
            color = ClipyTheme.colors.neutral.gray500,
            style = ClipyTheme.typography.body1Regular,
            textAlign = textAlign,
        )
        if (promptInput != null) {
            Spacer(modifier = Modifier.height(DialogDefaults.textSpacing))
            promptInput()
        }
    }
}

@Composable
private fun JsPromptInput(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(JsDialogDefaults.inputHeight)
            .background(
                color = ClipyTheme.colors.primary.indigo50,
                shape = JsDialogDefaults.inputShape,
            )
            .border(
                width = 1.dp,
                color = ClipyTheme.colors.neutral.gray200,
                shape = JsDialogDefaults.inputShape,
            )
            .padding(JsDialogDefaults.inputPadding),
        textStyle = ClipyTheme.typography.body1Regular.copy(
            color = ClipyTheme.colors.neutral.gray800,
        ),
        singleLine = true,
        cursorBrush = SolidColor(ClipyTheme.colors.primary.indigo500),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun JsDialogActions(
    primaryAction: ClipyTextAction,
    secondaryAction: ClipyTextAction? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DialogDefaults.actionSpacing),
    ) {
        if (secondaryAction != null) {
            ClipyButton(
                text = secondaryAction.label,
                onClick = secondaryAction.onClick,
                modifier = Modifier.weight(1f),
                type = ButtonType.Secondary,
            )
        }
        Button(
            onClick = primaryAction.onClick,
            modifier = Modifier
                .weight(1f)
                .height(JsDialogDefaults.actionHeight),
            shape = JsDialogDefaults.actionShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ClipyTheme.colors.primary.indigo500,
                contentColor = ClipyTheme.colors.primary.indigo50,
            ),
            contentPadding = PaddingValues(horizontal = DialogDefaults.contentPadding),
        ) {
            Text(
                text = primaryAction.label,
                style = ClipyTheme.typography.body1Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val ClipyDialogProperties = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
)

@Preview(
    name = "Dialog · States",
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
            when (preview) {
                is DialogPreview.App -> DialogContent(
                    title = preview.title,
                    description = preview.description,
                    primaryAction = preview.primaryAction,
                    secondaryAction = preview.secondaryAction,
                    style = preview.style,
                )

                is DialogPreview.JavaScript -> JsDialogContent(
                    source = "Request from example.com",
                    state = preview.state,
                )
            }
        }
    }
}

private sealed interface DialogPreview {
    data class App(
        val title: String,
        val description: String,
        val primaryAction: ClipyTextAction,
        val secondaryAction: ClipyTextAction? = null,
        val style: ClipyDialogStyle = ClipyDialogStyle.Default,
    ) : DialogPreview

    data class JavaScript(
        val state: ClipyJsDialogState,
    ) : DialogPreview
}

private class DialogPreviewProvider : PreviewParameterProvider<DialogPreview> {
    override val values = sequenceOf(
        DialogPreview.App(
            title = "연결할 수 없습니다",
            description = "잠시 후 다시 시도해주세요.",
            primaryAction = PreviewConfirmAction,
        ),
        DialogPreview.App(
            title = "항목을 삭제할까요?",
            description = "삭제한 항목은 되돌릴 수 없습니다.",
            primaryAction = ClipyTextAction(
                label = "삭제",
                onClick = {},
            ),
            secondaryAction = PreviewCancelAction,
        ),
        DialogPreview.App(
            title = "연결할 수 없습니다",
            description = "잠시 후 다시 시도해주세요.",
            primaryAction = PreviewConfirmAction,
            style = ClipyDialogStyle.Error,
        ),
        DialogPreview.App(
            title = "항목을 삭제할 수 없습니다",
            description = "잠시 후 다시 시도하거나 취소해주세요.",
            primaryAction = ClipyTextAction(
                label = "재시도",
                onClick = {},
            ),
            secondaryAction = PreviewCancelAction,
            style = ClipyDialogStyle.Error,
        ),
        DialogPreview.App(
            title = "이용 안내",
            description = LongDialogDescription,
            primaryAction = PreviewConfirmAction,
        ),
        DialogPreview.JavaScript(
            state = ClipyJsDialogState.Alert(
                title = "알림",
                description = "웹사이트에서 메시지를 보냈습니다.",
                confirmAction = PreviewConfirmAction,
            ),
        ),
        DialogPreview.JavaScript(
            state = ClipyJsDialogState.Confirm(
                title = "계속 진행할까요?",
                description = "웹사이트 요청을 확인해주세요.",
                confirmAction = PreviewConfirmAction,
                cancelAction = PreviewCancelAction,
            ),
        ),
        DialogPreview.JavaScript(
            state = ClipyJsDialogState.Prompt(
                title = "이름을 입력해주세요",
                description = "웹사이트에 전달할 값을 입력하세요.",
                confirmAction = PreviewPromptConfirmAction,
                initialValue = "Clipy",
                cancelAction = PreviewCancelAction,
            ),
        ),
        DialogPreview.JavaScript(
            state = ClipyJsDialogState.Prompt(
                title = "정보를 입력해주세요",
                description = LongDialogDescription,
                confirmAction = PreviewPromptConfirmAction,
                initialValue = "Clipy",
                cancelAction = PreviewCancelAction,
            ),
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

private val PreviewPromptConfirmAction = ClipyTextInputAction(
    label = "확인",
    onClick = {},
)
