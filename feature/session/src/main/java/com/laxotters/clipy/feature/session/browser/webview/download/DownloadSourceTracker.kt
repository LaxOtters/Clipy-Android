package com.laxotters.clipy.feature.session.browser.webview.download

import com.laxotters.clipy.feature.session.browser.webview.navigation.WebNavigationPolicy

/**
 * 같은 URL의 탐색 기록으로 다운로드 요청이 본문에서 발생했는지 판정합니다.
 * 출처는 본문 또는 iframe을 뜻하며, 요청을 시작한 특정 frame을 직접 찾지는 않습니다.
 * 확인된 기록은 판정 후 제거해 한 번만 사용합니다.
 */
internal class DownloadSourceTracker {
    private val evidenceByUrl = linkedMapOf<String, FrameEvidence>()
    private var isTrackingStopped = false

    fun recordNavigation(
        url: String,
        isForMainFrame: Boolean,
    ) {
        if (!WebNavigationPolicy.isHttpUrlWithHost(url)) return

        val evidence = getOrCreateEvidence(url) ?: return
        if (isForMainFrame) {
            // 본문 탐색 한 번과 중복만 구분하므로 2에서 더 늘리지 않습니다.
            evidence.mainFrameNavigationCount = (evidence.mainFrameNavigationCount + 1).coerceAtMost(2)
        } else {
            evidence.hasSubframeNavigation = true
        }
    }

    fun consumeConfirmedMainFrame(url: String): Boolean {
        if (!WebNavigationPolicy.isHttpUrlWithHost(url)) return false

        val evidence = getOrCreateEvidence(url) ?: return false
        val isConfirmed = evidence.mainFrameNavigationCount == 1 &&
            !evidence.hasSubframeNavigation &&
            !evidence.hasUnattributedDownload
        if (isConfirmed) {
            evidenceByUrl.remove(url)
        } else {
            // DownloadListener에 frame 정보가 없으므로 출처 불명 요청도 이후 같은 URL 판정에 남깁니다.
            evidence.hasUnattributedDownload = true
        }
        return isConfirmed
    }

    fun clear() {
        evidenceByUrl.clear()
        isTrackingStopped = false
    }

    private fun getOrCreateEvidence(url: String): FrameEvidence? {
        if (isTrackingStopped) return null

        if (url !in evidenceByUrl && evidenceByUrl.size >= MAX_TRACKED_URLS) {
            // 일부 기록만 지우면 같은 URL의 iframe 근거를 잊어버릴 수 있어 페이지 초기화까지 판정을 중단합니다.
            evidenceByUrl.clear()
            isTrackingStopped = true
            return null
        }
        return evidenceByUrl.getOrPut(url, ::FrameEvidence)
    }

    private class FrameEvidence(
        var mainFrameNavigationCount: Int = 0,
        var hasSubframeNavigation: Boolean = false,
        var hasUnattributedDownload: Boolean = false,
    )

    private companion object {
        const val MAX_TRACKED_URLS = 64
    }
}
