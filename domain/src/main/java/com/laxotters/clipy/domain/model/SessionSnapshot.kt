package com.laxotters.clipy.domain.model

data class SessionSnapshot(
    val session: Session,
    val viewState: SessionViewState,
)
