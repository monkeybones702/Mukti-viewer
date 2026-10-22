package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.LoginState
import com.example.data.model.StreamWindow
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PhoneBezel
import com.example.ui.theme.PhoneBezelBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.AutoScrollScripts
import com.example.util.WebConfig
import com.example.viewmodel.StreamUiEvent
import kotlinx.coroutines.flow.SharedFlow

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PhoneEmulatorFrame(
    window: StreamWindow,
    uiEvents: SharedFlow<StreamUiEvent>,
    onToggleMute: () -> Unit,
    onToggleAutoScroll: () -> Unit,
    onTriggerScrollNow: () -> Unit,
    onReload: () -> Unit,
    onLaunchPipLogin: () -> Unit,
    onInspectIntel: () -> Unit,
    onInspectSourceCode: () -> Unit,
    onDelete: () -> Unit,
    isMaximized: Boolean = false,
    onToggleMaximize: () -> Unit = {},
    onOpenZoomDimensionDialog: () -> Unit = {},
    onStepZoomOut: () -> Unit = {},
    onStepZoomIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var scrollCount by remember { mutableIntStateOf(0) }

    // Pulsing animation for active auto-scroll and PiP attention
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Automatically re-apply window zoom and audio mute whenever window properties change
    LaunchedEffect(
        window.zoomWidthCols,
        window.zoomHeightRows,
        window.fitEntirePhone,
        window.proportionalZoom,
        webViewInstance
    ) {
        WebConfig.applyWindowZoom(
            webView = webViewInstance,
            zoomCols = window.zoomWidthCols,
            zoomRows = window.zoomHeightRows,
            fitEntirePhone = window.fitEntirePhone,
            proportionalZoom = window.proportionalZoom
        )
    }

    LaunchedEffect(window.isMuted, webViewInstance) {
        WebConfig.applyAudioMute(
            webView = webViewInstance,
            isMuted = window.isMuted
        )
    }

    // Handle incoming events like Auto-Scroll trigger, Zoom updates, Master Audio, or Batch Login execution
    LaunchedEffect(window.id) {
        uiEvents.collect { event ->
            when (event) {
                is StreamUiEvent.ExecuteAutoScroll -> {
                    if (event.windowId == window.id && window.autoScrollEnabled) {
                        val script = AutoScrollScripts.buildAutoScrollScript(
                            feedType = event.feedType,
                            intervalSeconds = event.intervalSeconds,
                            zoomCols = event.zoomCols,
                            zoomRows = event.zoomRows
                        )
                        webViewInstance?.evaluateJavascript(script) {
                            scrollCount++
                        }
                    }
                }
                is StreamUiEvent.ExecuteBatchLoginScript -> {
                    if (event.windowId == window.id) {
                        webViewInstance?.evaluateJavascript(event.script, null)
                    }
                }
                is StreamUiEvent.ApplyWindowZoom -> {
                    if (event.windowId == window.id) {
                        WebConfig.applyWindowZoom(
                            webView = webViewInstance,
                            zoomCols = event.zoomCols,
                            zoomRows = event.zoomRows,
                            fitEntirePhone = event.fitEntirePhone,
                            proportionalZoom = event.proportionalZoom
                        )
                    }
                }
                is StreamUiEvent.ApplyAllWindowsZoom -> {
                    WebConfig.applyWindowZoom(
                        webView = webViewInstance,
                        zoomCols = event.zoomCols,
                        zoomRows = event.zoomRows,
                        fitEntirePhone = event.fitEntirePhone,
                        proportionalZoom = event.proportionalZoom
                    )
                }
                is StreamUiEvent.ApplyWindowAudioMute -> {
                    if (event.windowId == window.id) {
                        WebConfig.applyAudioMute(
                            webView = webViewInstance,
                            isMuted = event.isMuted
                        )
                    }
                }
                is StreamUiEvent.ApplyAllAudioMute -> {
                    WebConfig.applyAudioMute(
                        webView = webViewInstance,
                        isMuted = event.isMuted
                    )
                }
                is StreamUiEvent.ReloadWindow -> {
                    if (event.windowId == window.id) {
                        webViewInstance?.reload()
                    }
                }
                is StreamUiEvent.ReloadAllWindows -> {
                    webViewInstance?.reload()
                }
                else -> {}
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    // Outer Phone Chassis styling
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        PhoneBezelBorder,
                        if (window.loginState == LoginState.FAILED_REQUIRES_PIP.name) WarningAmber else NeonCyan.copy(alpha = 0.4f),
                        PhoneBezelBorder
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("emulator_frame_${window.id}"),
        color = PhoneBezel,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Simulated Phone Status & Camera Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF030712))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EMULATOR • ${window.id.takeLast(4).uppercase()}",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                // Simulated Punch-hole Camera
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(0.5.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (window.autoScrollEnabled) "AUTO-SCROLL ON" else "PAUSED",
                        color = if (window.autoScrollEnabled) NeonCyan else TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (window.autoScrollEnabled) NeonCyan.copy(alpha = pulseAlpha) else Color.Gray)
                    )
                }
            }

            // Window Header Control Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (window.currentUrl.contains("uptoplay", ignoreCase = true) || window.feedType == "UPTOPLAY") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonCyan,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "★ MAIN CHOICE",
                                    color = CyberBlack,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = window.title,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = window.currentUrl,
                        color = TextMuted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Full Phone Fit / Maximize Window Toggle
                IconButton(
                    onClick = onToggleMaximize,
                    modifier = Modifier.size(30.dp).testTag("maximize_button_${window.id}")
                ) {
                    Icon(
                        imageVector = if (isMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isMaximized) "Exit Full Phone View" else "Maximize to Full Phone Size",
                        tint = if (isMaximized) NeonCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Mute / Unmute
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(30.dp).testTag("mute_button_${window.id}")
                ) {
                    Icon(
                        imageVector = if (window.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = if (window.isMuted) "Unmute Feed" else "Mute Feed",
                        tint = if (window.isMuted) TextMuted else NeonCyan,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Intel Source Link Action
                IconButton(
                    onClick = onInspectIntel,
                    modifier = Modifier.size(30.dp).testTag("intel_button_${window.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Source Intelligence and Documentation Link",
                        tint = NeonPurple,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Generated Page Source Code Link Action
                IconButton(
                    onClick = onInspectSourceCode,
                    modifier = Modifier.size(30.dp).testTag("code_button_${window.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Page Source Code & Generation Script",
                        tint = NeonCyan,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Overflow Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(30.dp).testTag("more_button_${window.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Window Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(SurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Custom Zoom Dimension (${window.zoomDimensionLabel})",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                showMenu = false
                                onOpenZoomDimensionDialog()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ZoomOutMap, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isMaximized) "Restore Grid View" else "Fit Entire Phone Screen",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleMaximize()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reload Feed", color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                showMenu = false
                                onReload()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Launch PiP Login", color = WarningAmber, fontSize = 12.sp) },
                            onClick = {
                                showMenu = false
                                onLaunchPipLogin()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PictureInPicture, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open in Device Browser", color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                showMenu = false
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(window.currentUrl))
                                context.startActivity(intent)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from Matrix", color = ErrorRed, fontSize = 12.sp) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // PiP Login Required Attention Banner
            if (window.loginState == LoginState.FAILED_REQUIRES_PIP.name) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarningAmber.copy(alpha = 0.2f))
                        .border(1.dp, WarningAmber.copy(alpha = pulseAlpha))
                        .clickable { onLaunchPipLogin() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Login Challenge",
                            tint = WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Batch Login Paused: Tap to Open PiP Login",
                            color = WarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "Launch PiP",
                        tint = WarningAmber,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Live Mobile Web Screen (WebView viewport)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            WebConfig.configureWebView(
                                webView = this,
                                onPageFinished = {
                                    WebConfig.applyWindowZoom(
                                        webView = this,
                                        zoomCols = window.zoomWidthCols,
                                        zoomRows = window.zoomHeightRows,
                                        fitEntirePhone = window.fitEntirePhone,
                                        proportionalZoom = window.proportionalZoom
                                    )
                                    WebConfig.applyAudioMute(
                                        webView = this,
                                        isMuted = window.isMuted
                                    )
                                }
                            )
                            loadUrl(window.currentUrl)
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        WebConfig.applyWindowZoom(
                            webView = view,
                            zoomCols = window.zoomWidthCols,
                            zoomRows = window.zoomHeightRows,
                            fitEntirePhone = window.fitEntirePhone,
                            proportionalZoom = window.proportionalZoom
                        )
                        WebConfig.applyAudioMute(
                            webView = view,
                            isMuted = window.isMuted
                        )
                    }
                )

                // Subtle Auto-Scroll & Zoom Dimension Indicator Pill floating in lower right
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clickable { onOpenZoomDimensionDialog() },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "ZOOM ${window.zoomDimensionLabel}",
                            color = NeonPurple,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (window.autoScrollEnabled) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.South,
                                    contentDescription = "Auto-scrolling",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${window.autoScrollIntervalSeconds}s #${scrollCount}",
                                    color = NeonCyan,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Phone Frame Navigation Bar & Quick Controls (including Per-Window 1x1..5x10 Zoom Controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF030712))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Toggle Auto-Scroll Button
                Surface(
                    modifier = Modifier
                        .clickable { onToggleAutoScroll() }
                        .testTag("toggle_autoscroll_${window.id}"),
                    shape = RoundedCornerShape(7.dp),
                    color = if (window.autoScrollEnabled) NeonCyan.copy(alpha = 0.15f) else SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (window.autoScrollEnabled) NeonCyan else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (window.autoScrollEnabled) "Auto: ON" else "Auto: OFF",
                            color = if (window.autoScrollEnabled) NeonCyan else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Per-Window Custom Zoom-Out Dimension Control (- | 1x1..5x10 | +)
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (window.zoomWidthCols > 1 || window.zoomHeightRows > 1) NeonPurple else NeonCyan.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                    ) {
                        // Step Zoom Out (towards 5x10)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .clickable { onStepZoomOut() }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                                .testTag("zoom_out_step_${window.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom Out Custom Dimension",
                                tint = NeonCyan,
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        // Custom WxH Dimension Picker Badge (opens 5x10 dialog)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .clickable { onOpenZoomDimensionDialog() }
                                .background(CyberBlack)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .testTag("zoom_dimension_button_${window.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = window.zoomDimensionLabel,
                                color = if (window.zoomWidthCols > 1 || window.zoomHeightRows > 1) NeonPurple else NeonCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Step Zoom In (back towards 1x1 Fit)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .clickable { onStepZoomIn() }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                                .testTag("zoom_in_step_${window.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom In Toward 1x1 Fit",
                                tint = NeonCyan,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Scroll Now Action
                Surface(
                    modifier = Modifier
                        .clickable { onTriggerScrollNow() }
                        .testTag("scroll_now_${window.id}"),
                    shape = RoundedCornerShape(7.dp),
                    color = SurfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.South,
                            contentDescription = "Scroll Now",
                            tint = TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Scroll",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }

                // Interactive PiP Quick Launcher
                Surface(
                    modifier = Modifier
                        .clickable { onLaunchPipLogin() }
                        .testTag("pip_button_${window.id}"),
                    shape = RoundedCornerShape(7.dp),
                    color = if (window.loginState == LoginState.FAILED_REQUIRES_PIP.name)
                        WarningAmber.copy(alpha = 0.2f)
                    else
                        SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (window.loginState == LoginState.FAILED_REQUIRES_PIP.name) WarningAmber else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = "PiP Window",
                            tint = if (window.loginState == LoginState.FAILED_REQUIRES_PIP.name) WarningAmber else TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (window.loginState == LoginState.FAILED_REQUIRES_PIP.name) "PiP Login" else "PiP",
                            color = if (window.loginState == LoginState.FAILED_REQUIRES_PIP.name) WarningAmber else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = if (window.loginState == LoginState.FAILED_REQUIRES_PIP.name) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
