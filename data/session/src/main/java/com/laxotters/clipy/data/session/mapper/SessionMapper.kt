package com.laxotters.clipy.data.session.mapper

import com.laxotters.clipy.data.storage.room.entity.CaptureEntity
import com.laxotters.clipy.data.storage.room.entity.DecisionEntity
import com.laxotters.clipy.data.storage.room.entity.ItemEntity
import com.laxotters.clipy.data.storage.room.entity.SessionEntity
import com.laxotters.clipy.data.storage.room.relation.ItemWithCaptures
import com.laxotters.clipy.data.storage.room.relation.SessionWithItemsAndDecisions
import com.laxotters.clipy.domain.model.Capture
import com.laxotters.clipy.domain.model.Decision
import com.laxotters.clipy.domain.model.ImageRef
import com.laxotters.clipy.domain.model.Item
import com.laxotters.clipy.domain.model.ItemIntentState
import com.laxotters.clipy.domain.model.MoneySnapshot
import com.laxotters.clipy.domain.model.Session
import com.laxotters.clipy.domain.model.SessionStatus
import java.time.Instant
import java.util.UUID

// Domain -> storage
internal fun Session.toEntity() = SessionEntity(
    id = id.toString(),
    name = name,
    status = status.name,
    createdAtMillis = createdAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli(),
    closedAtMillis = closedAt?.toEpochMilli(),
    abandonedAtMillis = abandonedAt?.toEpochMilli(),
)

internal fun Item.toEntity() = ItemEntity(
    id = id.toString(),
    sessionId = sessionId.toString(),
    sourceUrl = sourceUrl,
    productName = productName,
    priceAmount = priceSnapshot?.amount,
    priceCurrency = priceSnapshot?.currency,
    priceRawText = priceSnapshot?.rawText,
    thumbnailRemoteUrl = thumbnailImage?.remoteUrl,
    thumbnailLocalPath = thumbnailImage?.localPath,
    note = note,
    intentState = intentState.name,
    createdAtMillis = createdAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli(),
)

internal fun Capture.toEntity() = CaptureEntity(
    id = id.toString(),
    itemId = itemId.toString(),
    imageRemoteUrl = imageRef.remoteUrl,
    imageLocalPath = imageRef.localPath,
    capturedAtMillis = capturedAt.toEpochMilli(),
    memo = memo,
)

internal fun Decision.toEntity() = DecisionEntity(
    id = id.toString(),
    sessionId = sessionId.toString(),
    itemId = itemId.toString(),
    decidedAtMillis = decidedAt.toEpochMilli(),
)

// Storage -> domain
internal fun SessionWithItemsAndDecisions.toDomain() = Session(
    id = UUID.fromString(session.id),
    name = session.name,
    status = session.status.toSessionStatus(),
    createdAt = Instant.ofEpochMilli(session.createdAtMillis),
    updatedAt = Instant.ofEpochMilli(session.updatedAtMillis),
    closedAt = session.closedAtMillis?.let(Instant::ofEpochMilli),
    abandonedAt = session.abandonedAtMillis?.let(Instant::ofEpochMilli),
    items = items.map { it.toDomain() },
    decisions = decisions.map { it.toDomain() },
)

private fun ItemWithCaptures.toDomain() = Item(
    id = UUID.fromString(item.id),
    sessionId = UUID.fromString(item.sessionId),
    sourceUrl = item.sourceUrl,
    productName = item.productName,
    priceSnapshot = item.toMoneySnapshot(),
    thumbnailImage = item.toThumbnailImage(),
    note = item.note,
    intentState = item.intentState.toItemIntentState(),
    captures = captures.map { it.toDomain() },
    createdAt = Instant.ofEpochMilli(item.createdAtMillis),
    updatedAt = Instant.ofEpochMilli(item.updatedAtMillis),
)

private fun CaptureEntity.toDomain() = Capture(
    id = UUID.fromString(id),
    itemId = UUID.fromString(itemId),
    imageRef = requireNotNull(toImageRef(imageRemoteUrl, imageLocalPath)),
    capturedAt = Instant.ofEpochMilli(capturedAtMillis),
    memo = memo,
)

private fun DecisionEntity.toDomain() = Decision(
    id = UUID.fromString(id),
    sessionId = UUID.fromString(sessionId),
    itemId = UUID.fromString(itemId),
    decidedAt = Instant.ofEpochMilli(decidedAtMillis),
)

// Value object mapping
private fun ItemEntity.toMoneySnapshot(): MoneySnapshot? {
    val amount = priceAmount ?: return null
    val currency = priceCurrency ?: return null

    return MoneySnapshot(
        amount = amount,
        currency = currency,
        rawText = priceRawText,
    )
}

private fun ItemEntity.toThumbnailImage(): ImageRef? =
    toImageRef(
        remoteUrl = thumbnailRemoteUrl,
        localPath = thumbnailLocalPath,
    )

private fun toImageRef(
    remoteUrl: String?,
    localPath: String?,
): ImageRef? {
    if (remoteUrl == null && localPath == null) return null

    return ImageRef(
        remoteUrl = remoteUrl,
        localPath = localPath,
    )
}

// 저장된 enum 문자열을 안전하게 복원하기 위한 대체 매핑
private fun String.toSessionStatus(): SessionStatus =
    runCatching { SessionStatus.valueOf(this) }
        .getOrDefault(SessionStatus.DRAFT)

private fun String.toItemIntentState(): ItemIntentState =
    runCatching { ItemIntentState.valueOf(this) }
        .getOrDefault(ItemIntentState.INTERESTED)
