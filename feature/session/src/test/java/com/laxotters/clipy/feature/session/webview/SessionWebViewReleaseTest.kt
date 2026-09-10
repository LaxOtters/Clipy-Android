package com.laxotters.clipy.feature.session.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SessionWebViewReleaseTest {
    @Test
    fun release_failureStillRunsAllStepsAndPreservesFirstError() {
        val calls = mutableListOf<String>()
        val cancellationError = IllegalStateException("cancel failed")
        val detachError = IllegalStateException("detach failed")
        val release = SessionWebViewRelease(
            closeDialog = {
                calls += "cancel"
                throw cancellationError
            },
            detachController = {
                calls += "detach"
                throw detachError
            },
            releaseCallbacks = { calls += "callbacks" },
            removeFromParent = { calls += "remove" },
            destroy = { calls += "destroy" },
        )

        val failure = runCatching { release.release() }.exceptionOrNull()
        release.release()

        assertEquals(listOf("cancel", "detach", "callbacks", "remove", "destroy"), calls)
        assertSame(cancellationError, failure)
        assertEquals(listOf(detachError), failure?.suppressed?.toList())
    }

    @Test
    fun release_reentrantCallDoesNotRepeatCleanup() {
        val calls = mutableListOf<String>()
        lateinit var release: SessionWebViewRelease
        release = SessionWebViewRelease(
            closeDialog = {
                calls += "cancel"
                release.release()
            },
            detachController = { calls += "detach" },
            releaseCallbacks = { calls += "callbacks" },
            removeFromParent = { calls += "remove" },
            destroy = { calls += "destroy" },
        )

        release.release()
        release.release()

        assertEquals(listOf("cancel", "detach", "callbacks", "remove", "destroy"), calls)
    }
}
