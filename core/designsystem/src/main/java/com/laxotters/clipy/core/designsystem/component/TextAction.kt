package com.laxotters.clipy.core.designsystem.component

/** 텍스트 레이블로 표시되는 컴포넌트 동작입니다. */
data class ClipyTextAction(
    val label: String,
    val onClick: () -> Unit,
)

/** 입력값을 전달하는 텍스트 action입니다. */
data class ClipyTextInputAction(
    val label: String,
    val onClick: (String) -> Unit,
)
