package com.example.data.model

data class IntelSourceInfo(
    val id: String,
    val platformName: String,
    val feedCategory: String,
    val documentationSummary: String,
    val officialSourceUrl: String,
    val sourceCodeRepoUrl: String,
    val generatedScriptSnippet: String,
    val keyFeatures: List<String>
)

object IntelRegistry {
    const val UPTOPLAY_URL = "https://www.uptoplay.net"

    val curatedFeeds: List<CuratedFeed> = listOf(
        CuratedFeed(
            title = "UptoPlay Android Hub",
            url = UPTOPLAY_URL,
            feedType = FeedType.GAME_CLOUD,
            description = "Primary cloud Android emulator & app workspace",
            isMainChoice = true,
            defaultZoomCols = 1,
            defaultZoomRows = 1
        ),
        CuratedFeed(
            title = "RetroArch Web Player",
            url = "https://web.libretro.com",
            feedType = FeedType.GAME_CLOUD,
            description = "Browser-based libretro emulator frontend",
            defaultZoomCols = 1,
            defaultZoomRows = 1
        ),
        CuratedFeed(
            title = "JS-Linux x86 Terminal",
            url = "https://bellard.org/jslinux/vm.html?url=alpine-x86.cfg&mem=192",
            feedType = FeedType.GAME_CLOUD,
            description = "Live x86 Alpine Linux emulator instance",
            defaultZoomCols = 1,
            defaultZoomRows = 1
        ),
        CuratedFeed(
            title = "USGS Global Seismic Feed",
            url = "https://earthquake.usgs.gov/earthquakes/map/",
            feedType = FeedType.INTEL_DASHBOARD,
            description = "Real-time tectonic & seismic sensor monitor",
            defaultZoomCols = 2,
            defaultZoomRows = 2
        ),
        CuratedFeed(
            title = "NASA ISS Live Telemetry",
            url = "https://eyes.nasa.gov/apps/solar-system/#/home",
            feedType = FeedType.LIVE_TV,
            description = "Orbital telemetry & live space visualization",
            defaultZoomCols = 1,
            defaultZoomRows = 1
        ),
        CuratedFeed(
            title = "Windy Atmospheric Radar",
            url = "https://www.windy.com",
            feedType = FeedType.INTEL_DASHBOARD,
            description = "Global wind, satellite & radar weather matrix",
            defaultZoomCols = 2,
            defaultZoomRows = 2
        ),
        CuratedFeed(
            title = "Hacker News Live Wire",
            url = "https://news.ycombinator.com/newest",
            feedType = FeedType.SOCIAL_FEED,
            description = "Real-time engineering & tech intelligence stream",
            defaultZoomCols = 1,
            defaultZoomRows = 2
        ),
        CuratedFeed(
            title = "Wikipedia Recent Changes",
            url = "https://en.wikipedia.org/wiki/Special:RecentChanges",
            feedType = FeedType.INTEL_DASHBOARD,
            description = "Live global knowledge edit stream",
            defaultZoomCols = 1,
            defaultZoomRows = 2
        ),
        CuratedFeed(
            title = "PCjs Classic IBM PC",
            url = "https://www.pcjs.org/machines/pcx86/ibm/5170/ega/",
            feedType = FeedType.GAME_CLOUD,
            description = "Cycle-accurate x86 hardware emulator in browser",
            defaultZoomCols = 2,
            defaultZoomRows = 2
        ),
        CuratedFeed(
            title = "CoinGecko Market Pulse",
            url = "https://www.coingecko.com",
            feedType = FeedType.INTEL_DASHBOARD,
            description = "Real-time digital asset liquidity & tick monitor",
            defaultZoomCols = 2,
            defaultZoomRows = 3
        )
    )

    fun getDefaultWindows(): List<StreamWindow> {
        return curatedFeeds.take(4).mapIndexed { index, feed ->
            StreamWindow(
                id = "win_default_${index + 1}",
                title = feed.title,
                url = feed.url,
                feedType = feed.feedType,
                isAutoScrollEnabled = index >= 2,
                scrollIntervalSec = 6,
                isMuted = true,
                loginState = LoginState.LOGGED_OUT,
                zoomWidthCols = feed.defaultZoomCols,
                zoomHeightRows = feed.defaultZoomRows,
                fitEntirePhone = true,
                proportionalZoom = false,
                orderIndex = index,
                isMainChoice = feed.isMainChoice
            )
        }
    }

    val SOURCES = listOf(
        IntelSourceInfo(
            id = "uptoplay",
            platformName = "UptoPlay Online Android Emulator",
            feedCategory = "Online Android OS & APK Virtualization (Main Choice)",
            documentationSummary = "Online web browser Android emulator by UptoPlay that runs real Android OS and mobile apps directly in the browser with interactive HTML5 canvas, touchscreen virtualization, and APK testing.",
            officialSourceUrl = "https://www.uptoplay.net/playonline/androidemulator.php",
            sourceCodeRepoUrl = "https://github.com/aistudio/multistream-uptoplay-emulator",
            generatedScriptSnippet = """
                // UptoPlay Android Emulator Session Bridge & Keep-Alive Script
                (function() {
                  console.log('[MultiStream] Initializing UptoPlay Android Emulator bridge (Main Choice)');
                  // Keep emulator active and focused
                  const target = document.querySelector('canvas') || document.querySelector('iframe') || document.body;
                  if (target) {
                    target.dispatchEvent(new MouseEvent('mousemove', { bubbles: true }));
                    if (target.focus) target.focus();
                  }
                  window.__uptoplayWatchdog = setInterval(function() {
                    const canvas = document.querySelector('canvas');
                    if (canvas) {
                      canvas.dispatchEvent(new MouseEvent('mousemove', { bubbles: true }));
                    }
                  }, 8000);
                })();
            """.trimIndent(),
            keyFeatures = listOf(
                "Primary Choice: UptoPlay Online Android Emulator (https://www.uptoplay.net/playonline/androidemulator.php)",
                "HTML5 canvas touchscreen translation and virtual hardware controls",
                "Automated keep-alive watchdog script to prevent idle timeout",
                "Supports running multiple concurrent Android emulator streams"
            )
        ),
        IntelSourceInfo(
            id = "easyfun",
            platformName = "EasyFun.gg Android Cloud Gaming",
            feedCategory = "Cloud Phone / Android Virtualization",
            documentationSummary = "Browser-based cloud Android emulator allowing instant mobile app and game streaming via WebRTC with low latency touch virtualization.",
            officialSourceUrl = "https://easyfun.gg",
            sourceCodeRepoUrl = "https://github.com/aistudio/multistream-easyfun-emulator",
            generatedScriptSnippet = """
                // EasyFun viewport and stream keepalive injection script
                (function() {
                  console.log('[MultiStream] Initializing EasyFun Emulator session bridge');
                  window.__streamWatchdog = setInterval(function() {
                    const canvas = document.querySelector('canvas') || document.querySelector('video');
                    if (canvas) {
                      window.dispatchEvent(new MouseEvent('mousemove', { bubbles: true }));
                    }
                  }, 12000);
                })();
            """.trimIndent(),
            keyFeatures = listOf(
                "Cloud Android OS instance running inside mobile viewport",
                "Touch event translation and low-bandwidth WebRTC media stream",
                "Automated keep-alive mouse and touch signals to avoid timeout disconnection"
            )
        ),
        IntelSourceInfo(
            id = "youtube_shorts",
            platformName = "YouTube Shorts Mobile Web Stream",
            feedCategory = "Short-form Video Media Feed",
            documentationSummary = "YouTube mobile web interface optimized for portrait video playback with swipe-to-advance gesture events and HTML5 video rendering.",
            officialSourceUrl = "https://m.youtube.com/shorts",
            sourceCodeRepoUrl = "https://github.com/aistudio/multistream-youtube-shorts",
            generatedScriptSnippet = """
                // YouTube Shorts Continuous Infinite Auto-Scroll & Watchdog
                (function() {
                  const now = Date.now();
                  // 1. Advance to next short
                  window.scrollBy({ top: window.innerHeight, behavior: 'smooth' });
                  // 2. Clear any "Video paused. Continue watching?" modal popups
                  const confirmBtns = document.querySelectorAll('yt-confirm-dialog-renderer button, .yt-spec-button-shape-next--filled');
                  confirmBtns.forEach(b => { if (b && b.offsetParent !== null) b.click(); });
                  // 3. Ensure active video element is unmuted/playing
                  const videos = document.querySelectorAll('video');
                  videos.forEach(v => {
                    if (v.paused && v.readyState >= 2) { v.play().catch(()=>{}); }
                  });
                })();
            """.trimIndent(),
            keyFeatures = listOf(
                "Automated vertical viewport stepping (`window.innerHeight`)",
                "Continuous video play watchdog to prevent background feed stutter",
                "Bypass mechanism for YouTube's inactivity confirmation dialog"
            )
        ),
        IntelSourceInfo(
            id = "twitch",
            platformName = "Twitch Live Stream Interactive Hub",
            feedCategory = "Interactive Live Video Streaming",
            documentationSummary = "Twitch mobile web portal utilizing HTTP Live Streaming (HLS) with synchronized video chunk delivery and WebSocket live chat.",
            officialSourceUrl = "https://m.twitch.tv",
            sourceCodeRepoUrl = "https://github.com/aistudio/multistream-twitch-streamer",
            generatedScriptSnippet = """
                // Twitch Mobile Stream Refresh & Content Refresh
                (function() {
                  const player = document.querySelector('video');
                  if (player && player.paused) {
                    player.play().catch(e => console.log('Autoplay request: ', e));
                  }
                  window.scrollBy({ top: 300, behavior: 'smooth' });
                })();
            """.trimIndent(),
            keyFeatures = listOf(
                "Low latency HLS stream playback inside Android WebView",
                "Synchronized audio control across multi-window matrix",
                "Auto-scroll chat feed & stream recommendations"
            )
        ),
        IntelSourceInfo(
            id = "android_pip",
            platformName = "Android Picture-in-Picture & CookieManager API",
            feedCategory = "System Architecture & Authentication",
            documentationSummary = "Official Android developer specification for Picture-in-Picture window management and unified WebView CookieManager session synchronization.",
            officialSourceUrl = "https://developer.android.com/develop/ui/views/picture-in-picture",
            sourceCodeRepoUrl = "https://github.com/aistudio/multistream-pip-session-manager",
            generatedScriptSnippet = """
                // Batched Credential Form-Fill Script
                (function(user, pass) {
                  const userSelectors = ['input[type="email"]', 'input[name="identifier"]', 'input[name="username"]', 'input[type="text"]'];
                  const passSelectors = ['input[type="password"]'];
                  let userFound = false;
                  for (const sel of userSelectors) {
                    const el = document.querySelector(sel);
                    if (el && el.offsetParent !== null) {
                      el.value = user;
                      el.dispatchEvent(new Event('input', { bubbles: true }));
                      el.dispatchEvent(new Event('change', { bubbles: true }));
                      userFound = true;
                      break;
                    }
                  }
                  for (const sel of passSelectors) {
                    const el = document.querySelector(sel);
                    if (el && el.offsetParent !== null) {
                      el.value = pass;
                      el.dispatchEvent(new Event('input', { bubbles: true }));
                      el.dispatchEvent(new Event('change', { bubbles: true }));
                      break;
                    }
                  }
                  return userFound;
                })
            """.trimIndent(),
            keyFeatures = listOf(
                "Cross-window session inheritance through shared CookieManager",
                "PiP window fallback for manual verification (Captcha/2FA)",
                "Non-blocking background feed execution"
            )
        )
    )

    fun getIntelForUrl(url: String): IntelSourceInfo {
        return when {
            url.contains("uptoplay", ignoreCase = true) -> SOURCES[0]
            url.contains("easyfun", ignoreCase = true) -> SOURCES.find { it.id == "easyfun" } ?: SOURCES[0]
            url.contains("youtube.com/shorts", ignoreCase = true) -> SOURCES.find { it.id == "youtube_shorts" } ?: SOURCES[0]
            url.contains("twitch", ignoreCase = true) -> SOURCES.find { it.id == "twitch" } ?: SOURCES[0]
            url.contains("youtube", ignoreCase = true) -> SOURCES.find { it.id == "youtube_shorts" } ?: SOURCES[0]
            else -> SOURCES.find { it.id == "android_pip" } ?: SOURCES[0]
        }
    }
}
