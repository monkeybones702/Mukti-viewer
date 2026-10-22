package com.example.util

object AutoScrollScripts {

    fun buildAutoScrollStepScript(scrollAmountPx: Int = 280): String {
        val step = scrollAmountPx.coerceIn(60, 1200)
        return """
            (function() {
                try {
                    const maxScroll = Math.max(
                        document.body ? document.body.scrollHeight : 0,
                        document.documentElement ? document.documentElement.scrollHeight : 0
                    ) - window.innerHeight;
                    if (maxScroll > 20 && window.scrollY >= maxScroll - 24) {
                        window.scrollTo({ top: 0, behavior: 'smooth' });
                    } else {
                        window.scrollBy({ top: $step, behavior: 'smooth' });
                    }

                    // Keep active videos playing while enforcing mute state
                    const mediaEls = document.querySelectorAll('video, audio');
                    mediaEls.forEach(v => {
                        if (typeof window.__streamsyMuted === 'boolean') {
                            v.muted = window.__streamsyMuted;
                            v.volume = window.__streamsyMuted ? 0.0 : 1.0;
                        }
                        if (v.paused && v.readyState >= 2) {
                            v.play().catch(() => {});
                        }
                    });
                    return 'SCROLL_OK';
                } catch (e) {
                    return 'SCROLL_ERR:' + e.message;
                }
            })();
        """.trimIndent()
    }

    fun buildWindowZoomScript(
        zoomCols: Int,
        zoomRows: Int,
        fitEntirePhone: Boolean,
        proportionalZoom: Boolean
    ): String {
        val cols = zoomCols.coerceIn(1, 5)
        val rows = zoomRows.coerceIn(1, 10)
        val fitPhone = if (fitEntirePhone) "true" else "false"
        val proportional = if (proportionalZoom) "true" else "false"

        return """
            (function() {
                try {
                    const cols = $cols;
                    const rows = $rows;
                    const fitPhone = $fitPhone;
                    const proportional = $proportional;

                    const scaleX = 1.0 / cols;
                    const scaleY = proportional ? scaleX : (1.0 / rows);

                    let meta = document.querySelector('meta[name="viewport"]');
                    if (!meta) {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        if (document.head) document.head.appendChild(meta);
                    }
                    if (meta) {
                        meta.content = 'width=device-width, initial-scale=' + scaleX.toFixed(4) +
                            ', minimum-scale=0.1, maximum-scale=5.0, user-scalable=yes';
                    }

                    const root = document.documentElement;
                    if (root) {
                        if (cols === 1 && rows === 1 && fitPhone) {
                            root.style.transform = 'none';
                            root.style.transformOrigin = 'top left';
                            root.style.width = '100%';
                            root.style.minHeight = '100vh';
                            root.style.overflowX = 'hidden';
                        } else {
                            root.style.transformOrigin = '0 0';
                            root.style.transform = 'scale(' + scaleX.toFixed(4) + ', ' + scaleY.toFixed(4) + ')';
                            root.style.width = (cols * 100) + '%';
                            root.style.minHeight = (rows * 100) + 'vh';
                        }
                    }

                    window.dispatchEvent(new Event('resize'));
                    return 'ZOOM_APPLIED_' + cols + 'x' + rows;
                } catch (e) {
                    return 'ZOOM_ERR:' + e.message;
                }
            })();
        """.trimIndent()
    }

    fun buildAudioMuteScript(isMuted: Boolean): String {
        val muteFlag = if (isMuted) "true" else "false"
        return """
            (function() {
                try {
                    const muted = $muteFlag;
                    window.__streamsyMuted = muted;
                    const mediaElements = document.querySelectorAll('video, audio');
                    mediaElements.forEach(el => {
                        el.muted = muted;
                        el.volume = muted ? 0.0 : 1.0;
                    });
                    const iframes = document.querySelectorAll('iframe');
                    iframes.forEach(frame => {
                        try {
                            if (frame.contentDocument) {
                                frame.contentDocument.querySelectorAll('video, audio').forEach(el => {
                                    el.muted = muted;
                                    el.volume = muted ? 0.0 : 1.0;
                                });
                            }
                        } catch (_) {}
                    });
                    return muted ? 'AUDIO_MUTED' : 'AUDIO_ENABLED';
                } catch (e) {
                    return 'AUDIO_ERR:' + e.message;
                }
            })();
        """.trimIndent()
    }

    fun buildFeedHealthProbeScript(): String {
        return """
            (function() {
                try {
                    const ready = document.readyState || 'unknown';
                    const online = navigator.onLine !== false;
                    const videos = document.querySelectorAll('video');
                    let stalledVideo = false;
                    videos.forEach(v => {
                        if (v.error || (v.networkState === 3 && v.readyState === 0)) {
                            stalledVideo = true;
                        }
                    });
                    if (!online) return 'DROPPED:navigator_offline';
                    if (stalledVideo) return 'FROZEN:video_stream_stalled';
                    return 'HEALTHY:' + ready + ':' + Date.now();
                } catch (e) {
                    return 'HEALTHY:fallback';
                }
            })();
        """.trimIndent()
    }

    fun buildSoftRefreshRecoveryScript(): String {
        return """
            (function() {
                try {
                    const videos = document.querySelectorAll('video, audio');
                    videos.forEach(v => {
                        try {
                            v.load();
                            v.play().catch(() => {});
                        } catch (_) {}
                    });
                    window.dispatchEvent(new Event('resize'));
                    return 'SOFT_REFRESH_DOM_OK';
                } catch (e) {
                    return 'SOFT_REFRESH_ERR:' + e.message;
                }
            })();
        """.trimIndent()
    }
}
