package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedType
import com.example.ui.components.AddEditWindowDialog
import com.example.ui.components.BatchLoginDialog
import com.example.ui.components.IntelSourcesDialog
import com.example.ui.components.PhoneEmulatorFrame
import com.example.ui.components.PiPLoginDialog
import com.example.ui.components.PresetManagerDialog
import com.example.ui.components.WindowZoomDimensionDialog
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.MultiStreamViewModel
import com.example.viewmodel.StreamUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiStreamScreen(
    viewModel: MultiStreamViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windows by viewModel.windows.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val activePresetId by viewModel.activePresetId.collectAsState()
    val gridMode by viewModel.gridMode.collectAsState()
    val gridRows by viewModel.gridRows.collectAsState()
    val fitEntirePhoneScreen by viewModel.fitEntirePhoneScreen.collectAsState()
    val maximizedWindowId by viewModel.maximizedWindowId.collectAsState()
    val globalAutoScroll by viewModel.globalAutoScroll.collectAsState()
    val globalAudioMuted by viewModel.globalAudioMuted.collectAsState()
    val isBatchLoginRunning by viewModel.isBatchLoginRunning.collectAsState()
    val batchLoginStatusText by viewModel.batchLoginStatusText.collectAsState()

    val selectedWindowForPip by viewModel.selectedWindowForPip.collectAsState()
    val selectedWindowForIntel by viewModel.selectedWindowForIntel.collectAsState()
    val selectedWindowForSourceCode by viewModel.selectedWindowForSourceCode.collectAsState()
    val selectedWindowForZoom by viewModel.selectedWindowForZoom.collectAsState()
    val showGlobalZoomDialog by viewModel.showGlobalZoomDialog.collectAsState()
    val showPresetManagerDialog by viewModel.showPresetManagerDialog.collectAsState()
    val showBatchLoginDialog by viewModel.showBatchLoginDialog.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showAllIntelDirectory by viewModel.showAllIntelDirectory.collectAsState()

    val unmutedCount = windows.count { !it.isMuted }
    val allAudioMuted = windows.isEmpty() || unmutedCount == 0

    // Allow back gesture to exit single-window full-phone maximize mode
    if (maximizedWindowId != null) {
        BackHandler {
            viewModel.toggleMaximizeWindow(maximizedWindowId!!)
        }
    }

    // Toast listener
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            if (event is StreamUiEvent.ShowToast) {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark)) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (globalAutoScroll) NeonCyan else WarningAmber)
                            )
                            Column {
                                Text(
                                    text = "MultiStream Grid",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${windows.size} Feeds (${gridMode}×${gridRows}) • Audio ${if (allAudioMuted) "Muted" else "$unmutedCount ON"}",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceDark
                    ),
                    actions = {
                        // Master Audio Control Button (Mutes or Enables Audio for All Active Emulator Instances Simultaneously)
                        IconButton(
                            onClick = { viewModel.toggleMasterAudio() },
                            modifier = Modifier.testTag("master_audio_toggle")
                        ) {
                            Icon(
                                imageVector = if (allAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (allAudioMuted) "Enable Audio for All Emulators" else "Mute All Emulators",
                                tint = if (allAudioMuted) WarningAmber else NeonCyan
                            )
                        }

                        // Preset Manager Dialog Button ('4-Window Focus', '10-Window Overview', Custom Save/Load)
                        IconButton(
                            onClick = { viewModel.openPresetManagerDialog() },
                            modifier = Modifier.testTag("open_preset_manager_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = "Grid Preset Manager",
                                tint = NeonCyan
                            )
                        }

                        // Fit Entire Phone Screen Toggle
                        IconButton(
                            onClick = { viewModel.toggleFitEntirePhoneScreen() },
                            modifier = Modifier.testTag("fit_phone_screen_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitScreen,
                                contentDescription = "Fit Entire Phone Screen",
                                tint = if (fitEntirePhoneScreen) NeonCyan else TextMuted
                            )
                        }

                        // Custom 5x10 Zoom Dimension Matrix Dialog
                        IconButton(
                            onClick = { viewModel.openGlobalZoomDialog() },
                            modifier = Modifier.testTag("open_global_zoom_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "Custom Zoom Dimension up to 5x10",
                                tint = NeonPurple
                            )
                        }

                        // Reload All Feeds
                        IconButton(
                            onClick = { viewModel.reloadAllWindows() },
                            modifier = Modifier.testTag("reload_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload All Feeds",
                                tint = NeonCyan
                            )
                        }

                        // Global Auto-Scroll Master Toggle
                        IconButton(
                            onClick = { viewModel.toggleGlobalAutoScroll() },
                            modifier = Modifier.testTag("master_autoscroll_toggle")
                        ) {
                            Icon(
                                imageVector = if (globalAutoScroll) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Master Auto-Scroll",
                                tint = if (globalAutoScroll) NeonCyan else TextMuted
                            )
                        }

                        // Batched Login Engine Action
                        IconButton(
                            onClick = { viewModel.openBatchLoginDialog() },
                            modifier = Modifier.testTag("open_batch_login_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Batch Login Media Accounts",
                                tint = NeonPurple
                            )
                        }

                        // Intel & Source Code Directory Action
                        IconButton(
                            onClick = { viewModel.openIntelDirectory() },
                            modifier = Modifier.testTag("open_intel_directory_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Intel Sources & Code Generation Links",
                                tint = NeonCyan
                            )
                        }

                        // System Picture-in-Picture trigger
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val activity = context as? Activity
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                } else {
                                    Toast.makeText(context, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("system_pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Enter Android PiP Mode",
                                tint = TextSecondary
                            )
                        }
                    }
                )

                // Quick Session Presets & Master Audio Strip ('4-Window Focus', '10-Window Overview', Custom Save/Load)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040812))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Master Audio Quick Control Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.toggleMasterAudio() }
                            .testTag("master_audio_pill"),
                        color = if (allAudioMuted) WarningAmber.copy(alpha = 0.18f) else NeonCyan.copy(alpha = 0.22f),
                        border = BorderStroke(
                            1.dp,
                            if (allAudioMuted) WarningAmber else NeonCyan
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (allAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (allAudioMuted) WarningAmber else NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (allAudioMuted) "MASTER AUDIO: MUTED" else "MASTER AUDIO: ON ($unmutedCount/${windows.size})",
                                color = if (allAudioMuted) WarningAmber else NeonCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "PRESETS:",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    presets.forEach { preset ->
                        val isSelected = activePresetId == preset.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.loadGridPreset(preset) }
                                .testTag("quick_preset_${preset.id}"),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else Color.Transparent
                            )
                        ) {
                            Text(
                                text = "${preset.name} (${preset.dimensionLabel})",
                                color = if (isSelected) NeonCyan else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.openPresetManagerDialog() }
                            .testTag("open_preset_manager_chip"),
                        color = NeonPurple.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "+ Save / Manage Presets",
                            color = NeonPurple,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Quick Full-Phone Fit & Custom Dimension Bar (1x1 Fit up to 5x10)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF060B16))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "WIN SIZE & ZOOM:",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    val quickDimensions = listOf(
                        Triple(1, 1, "1×1 Full Phone"),
                        Triple(2, 2, "2×2 Grid"),
                        Triple(2, 4, "2×4"),
                        Triple(3, 6, "3×6"),
                        Triple(4, 8, "4×8"),
                        Triple(5, 10, "5×10 Max")
                    )

                    quickDimensions.forEach { (cols, rows, label) ->
                        val isSelected = gridMode == cols && gridRows == rows && maximizedWindowId == null
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.setMatrixGridDimensions(cols, rows, fitPhoneScreen = true)
                                    viewModel.updateAllWindowsZoomDimension(cols, rows, fitEntirePhone = true)
                                }
                                .testTag("quick_dim_${cols}x${rows}"),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else Color.Transparent
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.openGlobalZoomDialog() }
                            .testTag("custom_5x10_matrix_button"),
                        color = NeonPurple.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "Custom 1×1..5×10 ▾",
                            color = NeonPurple,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddWindowDialog() },
                containerColor = NeonCyan,
                contentColor = CyberBlack,
                shape = CircleShape,
                modifier = Modifier.testTag("add_feed_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Feed Window")
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val availableHeight = maxHeight
            val maximizedWindow = windows.find { it.id == maximizedWindowId }

            if (windows.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No Streaming Windows Active",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add UptoPlay Android emulator, YouTube Shorts, EasyFun, or Twitch streams to begin multi-watching.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.resetPresets() },
                            color = NeonCyan
                        ) {
                            Text(
                                text = "Load Quad Emulator Preset (UptoPlay Main)",
                                color = CyberBlack,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            } else if (maximizedWindow != null) {
                // Single Window Full-Phone Fit Mode (100% of available phone screen)
                PhoneEmulatorFrame(
                    window = maximizedWindow,
                    uiEvents = viewModel.uiEvents,
                    onToggleMute = { viewModel.toggleMute(maximizedWindow) },
                    onToggleAutoScroll = { viewModel.toggleWindowAutoScroll(maximizedWindow) },
                    onTriggerScrollNow = { viewModel.triggerImmediateScroll(maximizedWindow) },
                    onReload = { viewModel.reloadWindow(maximizedWindow.id) },
                    onLaunchPipLogin = { viewModel.launchPipLogin(maximizedWindow) },
                    onInspectIntel = { viewModel.inspectIntel(maximizedWindow) },
                    onInspectSourceCode = { viewModel.inspectSourceCode(maximizedWindow) },
                    onDelete = {
                        viewModel.toggleMaximizeWindow(maximizedWindow.id)
                        viewModel.deleteWindow(maximizedWindow.id)
                    },
                    isMaximized = true,
                    onToggleMaximize = { viewModel.toggleMaximizeWindow(maximizedWindow.id) },
                    onOpenZoomDimensionDialog = { viewModel.openWindowZoomDialog(maximizedWindow) },
                    onStepZoomOut = { viewModel.stepWindowZoomOut(maximizedWindow) },
                    onStepZoomIn = { viewModel.stepWindowZoomIn(maximizedWindow) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            } else {
                // Dynamic Full-Phone Sizing Calculation
                val gridPadding = if (fitEntirePhoneScreen && gridMode == 1 && gridRows == 1) 4.dp else 6.dp
                val verticalSpacing = if (fitEntirePhoneScreen && gridMode == 1 && gridRows == 1) 6.dp else 6.dp
                val targetWindowHeight = if (fitEntirePhoneScreen) {
                    if (gridRows <= 1) {
                        // Each window fits 100% of the phone viewport height
                        (availableHeight - (gridPadding * 2)).coerceAtLeast(320.dp)
                    } else {
                        val totalSpacing = (gridPadding * 2) + (verticalSpacing * (gridRows - 1))
                        ((availableHeight - totalSpacing) / gridRows).coerceAtLeast(180.dp)
                    }
                } else {
                    if (gridMode == 1) (availableHeight - 16.dp).coerceAtLeast(520.dp) else 420.dp
                }

                // Grid of Simultaneous Emulator Feeds (1..5 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridMode.coerceIn(1, 5)),
                    contentPadding = PaddingValues(gridPadding),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("stream_windows_grid")
                ) {
                    items(windows, key = { it.id }) { window ->
                        PhoneEmulatorFrame(
                            window = window,
                            uiEvents = viewModel.uiEvents,
                            onToggleMute = { viewModel.toggleMute(window) },
                            onToggleAutoScroll = { viewModel.toggleWindowAutoScroll(window) },
                            onTriggerScrollNow = { viewModel.triggerImmediateScroll(window) },
                            onReload = { viewModel.reloadWindow(window.id) },
                            onLaunchPipLogin = { viewModel.launchPipLogin(window) },
                            onInspectIntel = { viewModel.inspectIntel(window) },
                            onInspectSourceCode = { viewModel.inspectSourceCode(window) },
                            onDelete = { viewModel.deleteWindow(window.id) },
                            isMaximized = false,
                            onToggleMaximize = { viewModel.toggleMaximizeWindow(window.id) },
                            onOpenZoomDimensionDialog = { viewModel.openWindowZoomDialog(window) },
                            onStepZoomOut = { viewModel.stepWindowZoomOut(window) },
                            onStepZoomIn = { viewModel.stepWindowZoomIn(window) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(targetWindowHeight)
                        )
                    }
                }
            }
        }
    }

    // Custom Per-Window or Global Zoom-Out Dimension Dialog (1x1 Fit up to 5x10)
    if (selectedWindowForZoom != null || showGlobalZoomDialog) {
        WindowZoomDimensionDialog(
            targetWindow = selectedWindowForZoom,
            currentMatrixCols = gridMode,
            currentMatrixRows = gridRows,
            fitEntirePhoneScreen = fitEntirePhoneScreen,
            onDismiss = { viewModel.closeZoomDialog() },
            onApplyToWindow = { winId, cols, rows, fitPhone, proportional ->
                viewModel.updateWindowZoomDimension(
                    windowId = winId,
                    zoomCols = cols,
                    zoomRows = rows,
                    fitEntirePhone = fitPhone,
                    proportionalZoom = proportional
                )
            },
            onApplyToAllWindows = { cols, rows, fitPhone, proportional ->
                viewModel.updateAllWindowsZoomDimension(
                    zoomCols = cols,
                    zoomRows = rows,
                    fitEntirePhone = fitPhone,
                    proportionalZoom = proportional
                )
            },
            onUpdateMatrixGrid = { cols, rows, fitPhoneScreen ->
                viewModel.setMatrixGridDimensions(cols, rows, fitPhoneScreen)
            }
        )
    }

    // Active PiP Interactive Login Window
    selectedWindowForPip?.let { win ->
        PiPLoginDialog(
            window = win,
            onDismiss = { viewModel.closePipLogin() },
            onLoginCompleted = { winId -> viewModel.confirmPipLoginSuccess(winId) }
        )
    }

    // Intel & Source Code Documentation Dialog (for specific window or global)
    if (selectedWindowForIntel != null || selectedWindowForSourceCode != null || showAllIntelDirectory) {
        val target = selectedWindowForIntel ?: selectedWindowForSourceCode
        IntelSourcesDialog(
            targetWindow = target,
            onDismiss = {
                viewModel.closeIntelDialog()
                viewModel.closeSourceCodeDialog()
                viewModel.closeIntelDirectory()
            }
        )
    }

    // Batch Login Dialog
    if (showBatchLoginDialog) {
        BatchLoginDialog(
            onDismiss = { viewModel.closeBatchLoginDialog() },
            onExecuteBatchLogin = { user, pass ->
                viewModel.executeBatchLogin(user, pass)
            }
        )
    }

    // Add Stream Window Dialog
    if (showAddDialog) {
        AddEditWindowDialog(
            onDismiss = { viewModel.closeAddWindowDialog() },
            onAddWindow = { title, url, type, interval, zoomCols, zoomRows ->
                viewModel.addNewWindow(title, url, type, interval, zoomCols, zoomRows)
            }
        )
    }

    // Grid Preset Manager Dialog ('4-Window Focus', '10-Window Overview', Custom Save/Load)
    if (showPresetManagerDialog) {
        PresetManagerDialog(
            presets = presets,
            activePresetId = activePresetId,
            currentWindowCount = windows.size,
            currentGridCols = gridMode,
            currentGridRows = gridRows,
            fitEntirePhone = fitEntirePhoneScreen,
            globalAudioMuted = allAudioMuted,
            onDismiss = { viewModel.closePresetManagerDialog() },
            onSaveCurrentPreset = { name, desc, existingId ->
                viewModel.saveCurrentGridAsPreset(name, desc, existingId)
            },
            onLoadPreset = { preset ->
                viewModel.loadGridPreset(preset)
            },
            onDeletePreset = { preset ->
                viewModel.deleteGridPreset(preset)
            },
            onRestoreBuiltInPresets = {
                viewModel.restoreBuiltInPresets()
            },
            onToggleMasterAudio = {
                viewModel.toggleMasterAudio()
            }
        )
    }
}
