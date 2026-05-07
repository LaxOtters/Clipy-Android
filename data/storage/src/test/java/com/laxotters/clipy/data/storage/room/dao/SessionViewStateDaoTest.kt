package com.laxotters.clipy.data.storage.room.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.laxotters.clipy.data.storage.room.ClipyDatabase
import com.laxotters.clipy.data.storage.room.entity.SessionEntity
import com.laxotters.clipy.data.storage.room.entity.SessionViewStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionViewStateDaoTest {
    private lateinit var database: ClipyDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var sessionViewStateDao: SessionViewStateDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room
            .inMemoryDatabaseBuilder(context, ClipyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = database.sessionDao()
        sessionViewStateDao = database.sessionViewStateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savedViewState_getSessionViewState_returnsSavedViewState() = runBlocking {
        sessionDao.insertSession(sessionEntity(id = "session-1"))
        sessionViewStateDao.saveSessionViewState(sessionViewStateEntity(sessionId = "session-1"))

        val sessionViewState = requireNotNull(sessionViewStateDao.getSessionViewState("session-1"))

        assertEquals("session-1", sessionViewState.sessionId)
        assertEquals("https://example.com/session-1", sessionViewState.lastWebUrl)
        assertEquals("HIDDEN", sessionViewState.bottomSheetState)
        assertEquals(2_000L, sessionViewState.lastOpenedAtMillis)
    }

    @Test
    fun savedViewState_saveSessionViewState_updatesSavedViewState() = runBlocking {
        sessionDao.insertSession(sessionEntity(id = "session-1"))
        sessionViewStateDao.saveSessionViewState(sessionViewStateEntity(sessionId = "session-1"))

        sessionViewStateDao.saveSessionViewState(
            sessionViewStateEntity(
                sessionId = "session-1",
                lastWebUrl = "https://example.com/updated",
                bottomSheetState = "EXPANDED",
                lastOpenedAtMillis = 3_000L,
            ),
        )

        val sessionViewState = requireNotNull(sessionViewStateDao.getSessionViewState("session-1"))

        assertEquals("https://example.com/updated", sessionViewState.lastWebUrl)
        assertEquals("EXPANDED", sessionViewState.bottomSheetState)
        assertEquals(3_000L, sessionViewState.lastOpenedAtMillis)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun unknownSessionId_saveSessionViewState_throwsConstraintException() = runBlocking {
        sessionViewStateDao.saveSessionViewState(sessionViewStateEntity(sessionId = "unknown-session"))
    }

    @Test
    fun savedViewState_deleteSession_deletesSessionViewState() = runBlocking {
        sessionDao.insertSession(sessionEntity(id = "session-1"))
        sessionViewStateDao.saveSessionViewState(sessionViewStateEntity(sessionId = "session-1"))

        sessionDao.deleteSession("session-1")

        assertNull(sessionViewStateDao.getSessionViewState("session-1"))
    }

    private fun sessionEntity(id: String) = SessionEntity(
        id = id,
        name = "Session $id",
        status = "DRAFT",
        createdAtMillis = 1_000L,
        updatedAtMillis = 1_000L,
        closedAtMillis = null,
        abandonedAtMillis = null,
    )

    private fun sessionViewStateEntity(
        sessionId: String,
        lastWebUrl: String? = "https://example.com/$sessionId",
        bottomSheetState: String = "HIDDEN",
        lastOpenedAtMillis: Long = 2_000L,
    ) = SessionViewStateEntity(
        sessionId = sessionId,
        lastWebUrl = lastWebUrl,
        bottomSheetState = bottomSheetState,
        lastOpenedAtMillis = lastOpenedAtMillis,
    )
}
