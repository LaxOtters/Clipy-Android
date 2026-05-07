package com.laxotters.clipy.data.storage.room.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.laxotters.clipy.data.storage.room.ClipyDatabase
import com.laxotters.clipy.data.storage.room.entity.CaptureEntity
import com.laxotters.clipy.data.storage.room.entity.DecisionEntity
import com.laxotters.clipy.data.storage.room.entity.ItemEntity
import com.laxotters.clipy.data.storage.room.entity.SessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionDaoTest {
    private lateinit var database: ClipyDatabase
    private lateinit var sessionDao: SessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room
            .inMemoryDatabaseBuilder(context, ClipyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = database.sessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test(expected = SQLiteConstraintException::class)
    fun unknownSessionId_insertItem_throwsConstraintException() = runBlocking {
        sessionDao.insertItems(listOf(itemEntity(id = "item-1", sessionId = "unknown-session")))
    }

    @Test
    fun savedSessions_getSessions_returnsUpdatedAtDescending() = runBlocking {
        sessionDao.insertSession(sessionEntity(id = "session-old", updatedAtMillis = 1_000L))
        sessionDao.insertSession(sessionEntity(id = "session-new", updatedAtMillis = 2_000L))

        val sessions = sessionDao.getSessions().first()

        assertEquals(listOf("session-new", "session-old"), sessions.map { it.id })
    }

    @Test
    fun savedSessionDetails_getSessionWithDetails_returnsSessionItemsCapturesAndDecisions() = runBlocking {
        insertSessionGraph()

        val sessionWithDetails = requireNotNull(sessionDao.getSessionWithDetails("session-1"))
        val itemsById = sessionWithDetails.items.associateBy { it.item.id }

        assertEquals("session-1", sessionWithDetails.session.id)
        assertEquals(setOf("item-1", "item-2"), itemsById.keys)
        assertEquals(listOf("capture-1"), itemsById.getValue("item-1").captures.map { it.id })
        assertEquals(listOf("capture-2"), itemsById.getValue("item-2").captures.map { it.id })
        assertEquals(setOf("decision-1", "decision-2"), sessionWithDetails.decisions.map { it.id }.toSet())
    }

    @Test
    fun savedSessionDetails_deleteSession_deletesItemsCapturesAndDecisions() = runBlocking {
        insertSessionGraph()

        sessionDao.deleteSession("session-1")

        assertNull(sessionDao.getSessionWithDetails("session-1"))
        assertEquals(0, database.countRows("sessions"))
        assertEquals(0, database.countRows("items"))
        assertEquals(0, database.countRows("captures"))
        assertEquals(0, database.countRows("decisions"))
    }

    @Test
    fun savedSessionDetails_deleteItem_deletesOnlyItsCapturesAndDecisions() = runBlocking {
        insertSessionGraph()

        sessionDao.deleteItem("item-1")

        val sessionWithDetails = requireNotNull(sessionDao.getSessionWithDetails("session-1"))

        assertEquals(listOf("item-2"), sessionWithDetails.items.map { it.item.id })
        assertEquals(listOf("capture-2"), sessionWithDetails.items.single().captures.map { it.id })
        assertEquals(listOf("decision-2"), sessionWithDetails.decisions.map { it.id })
        assertEquals(1, database.countRows("sessions"))
        assertEquals(1, database.countRows("items"))
        assertEquals(1, database.countRows("captures"))
        assertEquals(1, database.countRows("decisions"))
    }

    private suspend fun insertSessionGraph() {
        sessionDao.insertSession(sessionEntity(id = "session-1"))
        sessionDao.insertItems(
            listOf(
                itemEntity(id = "item-1", sessionId = "session-1"),
                itemEntity(id = "item-2", sessionId = "session-1"),
            ),
        )
        sessionDao.insertCaptures(
            listOf(
                captureEntity(id = "capture-1", itemId = "item-1"),
                captureEntity(id = "capture-2", itemId = "item-2"),
            ),
        )
        sessionDao.insertDecisions(
            listOf(
                decisionEntity(id = "decision-1", sessionId = "session-1", itemId = "item-1"),
                decisionEntity(id = "decision-2", sessionId = "session-1", itemId = "item-2"),
            ),
        )
    }

    private fun sessionEntity(
        id: String,
        updatedAtMillis: Long = 1_000L,
    ) = SessionEntity(
        id = id,
        name = "Session $id",
        status = "DRAFT",
        createdAtMillis = 1_000L,
        updatedAtMillis = updatedAtMillis,
        closedAtMillis = null,
        abandonedAtMillis = null,
    )

    private fun itemEntity(
        id: String,
        sessionId: String,
    ) = ItemEntity(
        id = id,
        sessionId = sessionId,
        sourceUrl = "https://example.com/$id",
        productName = "Product $id",
        priceAmount = 10_000L,
        priceCurrency = "KRW",
        priceRawText = "10,000원",
        thumbnailRemoteUrl = "https://example.com/$id.jpg",
        thumbnailLocalPath = "clipy/images/$sessionId/$id.jpg",
        note = null,
        intentState = "INTERESTED",
        createdAtMillis = 1_000L,
        updatedAtMillis = 1_000L,
    )

    private fun captureEntity(
        id: String,
        itemId: String,
    ) = CaptureEntity(
        id = id,
        itemId = itemId,
        imageRemoteUrl = "https://example.com/$id.jpg",
        imageLocalPath = "clipy/images/$itemId/$id.jpg",
        capturedAtMillis = 1_000L,
        memo = null,
    )

    private fun decisionEntity(
        id: String,
        sessionId: String,
        itemId: String,
    ) = DecisionEntity(
        id = id,
        sessionId = sessionId,
        itemId = itemId,
        decidedAtMillis = 1_000L,
    )

    // 테스트 검증용 조회를 production DAO에 추가하지 않기 위해 raw query로 row 수만 확인합니다.
    private fun ClipyDatabase.countRows(tableName: String): Int {
        val cursor = openHelper.readableDatabase.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM $tableName"))
        cursor.use {
            it.moveToFirst()
            return it.getInt(0)
        }
    }
}
