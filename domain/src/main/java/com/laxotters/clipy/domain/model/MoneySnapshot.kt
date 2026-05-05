package com.laxotters.clipy.domain.model

data class MoneySnapshot(
    val amount: Long,
    val currency: String,
    val rawText: String?,
)
