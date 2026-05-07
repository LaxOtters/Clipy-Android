package com.laxotters.clipy.domain.model

data class ImageRef(
    // TODO: 이미지 업로드 도입 시 remoteUrl nullable 정책 재검토
    val remoteUrl: String?,
    val localPath: String?,
)
