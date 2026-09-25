@file:Suppress("MatchingDeclarationName")

package com.laxotters.clipy.core.designsystem.component.dialog

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.R
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction
import com.laxotters.clipy.core.designsystem.component.button.ClipyButton
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonType
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

/**
 * 웹 콘텐츠가 요청한 단일 JavaScript Dialog를 표시합니다.
 *
 * 요청의 수락, 교체와 표시 수명은 호출 계층이 관리합니다.
 */
@Composable
fun ClipyJsDialog(
    source: String,
    state: JsDialogState,
    modifier: Modifier = Modifier,
) {
    val promptInitialValue = (state as? JsDialogState.Prompt)
        ?.initialValue
        .orEmpty()
    // 글꼴 크기나 density 변경으로 Dialog 창이 재생성돼도 입력값을 유지합니다.
    var promptValue by rememberSaveable(
        source,
        state.title,
        state.description,
        promptInitialValue,
    ) {
        mutableStateOf(promptInitialValue)
    }

    ClipyDialogWindow(
        modifier = modifier,
    ) {
        JsDialogContent(
            source = source,
            state = state,
            promptValue = promptValue,
            onPromptValueChange = { promptValue = it },
        )
    }
}

@Composable
private fun ColumnScope.JsDialogContent(
    source: String,
    state: JsDialogState,
    promptValue: String,
    onPromptValueChange: (String) -> Unit,
) {
    JsDialogBody(
        source = source,
        state = state,
        promptValue = promptValue,
        onPromptValueChange = onPromptValueChange,
        modifier = Modifier.weight(
            weight = 1f,
            fill = false,
        ),
    )
    JsDialogActions(
        state = state,
        promptValue = promptValue,
    )
}

@Composable
private fun JsDialogBody(
    source: String,
    state: JsDialogState,
    promptValue: String,
    onPromptValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        val messageModifier = Modifier
            .weight(
                weight = 1f,
                fill = false,
            )
            .padding(top = DialogDefaults.headingTopPadding)

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
        Spacer(modifier = Modifier.height(JsDialogDefaults.bodySpacing))
        DialogMessage(
            title = state.title,
            description = state.description,
            modifier = messageModifier,
            textAlign = if (state is JsDialogState.Prompt) {
                TextAlign.Start
            } else {
                TextAlign.Center
            },
            scrollTitleWithDescription = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
        )
        if (state is JsDialogState.Prompt) {
            Spacer(modifier = Modifier.height(DialogDefaults.textSpacing))
            JsPromptInput(
                value = promptValue,
                onValueChange = onPromptValueChange,
            )
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
    state: JsDialogState,
    promptValue: String,
) {
    val (primaryLabel, onPrimaryClick) = when (state) {
        is JsDialogState.Alert -> state.confirmAction.label to state.confirmAction.onClick
        is JsDialogState.Confirm -> state.confirmAction.label to state.confirmAction.onClick
        is JsDialogState.Prompt -> {
            state.confirmAction.label to {
                state.confirmAction.onClick(promptValue)
            }
        }
    }
    val secondaryAction = when (state) {
        is JsDialogState.Alert -> null
        is JsDialogState.Confirm -> state.cancelAction
        is JsDialogState.Prompt -> state.cancelAction
    }
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
            onClick = onPrimaryClick,
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
                text = primaryLabel,
                style = ClipyTheme.typography.body1Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(
    name = "Dialog · JavaScript States",
    showBackground = true,
    widthDp = 390,
    heightDp = 800,
)
@Composable
private fun JsDialogStatesPreview(
    @PreviewParameter(JsDialogPreviewProvider::class)
    state: JsDialogState,
) {
    var promptValue by rememberSaveable(state) {
        mutableStateOf((state as? JsDialogState.Prompt)?.initialValue.orEmpty())
    }
    ClipyTheme {
        DialogLayout {
            JsDialogContent(
                source = "Request from example.com",
                state = state,
                promptValue = promptValue,
                onPromptValueChange = { promptValue = it },
            )
        }
    }
}

private class JsDialogPreviewProvider : PreviewParameterProvider<JsDialogState> {
    override val values = sequenceOf(
        JsDialogState.Alert(
            title = "알림",
            description = "웹사이트에서 메시지를 보냈습니다.",
            confirmAction = PreviewConfirmAction,
        ),
        JsDialogState.Confirm(
            title = "계속 진행할까요?",
            description = "웹사이트 요청을 확인해주세요.",
            confirmAction = PreviewConfirmAction,
            cancelAction = PreviewCancelAction,
        ),
        JsDialogState.Prompt(
            title = "이름을 입력해주세요",
            description = "웹사이트에 전달할 값을 입력하세요.",
            confirmAction = PreviewPromptConfirmAction,
            initialValue = "Clipy",
            cancelAction = PreviewCancelAction,
        ),
        JsDialogState.Prompt(
            title = "정보를 입력해주세요",
            description = LongJsDialogDescription,
            confirmAction = PreviewPromptConfirmAction,
            initialValue = "Clipy",
            cancelAction = PreviewCancelAction,
        ),
    )
}

private val LongJsDialogDescription = (
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
