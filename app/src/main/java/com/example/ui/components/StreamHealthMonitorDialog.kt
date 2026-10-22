package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FeedHealthStatus
import com.example.data.model.StreamHealthIncident
import com.example.data.model.StreamWindow
import com.example.service.HealthMonitorSummary

@Composable
fun StreamHealthMonitorDialog(
    summary: HealthMonitorSummary,
    windows: List<StreamWindow>,
    cloudIncidents: List<StreamHealthIncident>,
    onDismiss: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onToggleAutoSoftRefresh: (Boolean) -> Unit,
    onChangeThresholdSec: (Int) -> Unit,
    onScanNow: () -> Unit,
    onSimulateFreeze: (StreamWindow) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("health_monitor_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527)),
            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "Stream Health Watchdog",
                            tint = Color(0xFF00E676)
                        )
                        Column {
                            Text(
                                text = "Background Feed Health Monitor",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Monitors individual feeds & auto soft-refreshes frozen streams",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Health Monitor",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Service Controls
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF131F38),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Background Watchdog Service",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Healthy: ${summary.healthyCount} • Frozen/Dropped: ${summary.frozenOrDroppedCount} • Auto-Recoveries: ${summary.totalAutoRecoveries}",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = summary.isServiceRunning,
                                onCheckedChange = onToggleService,
                                modifier = Modifier.testTag("health_service_toggle")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto Soft-Refresh on Freeze / Drop",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Switch(
                                checked = summary.isAutoSoftRefreshEnabled,
                                onCheckedChange = onToggleAutoSoftRefresh,
                                modifier = Modifier.testTag("auto_soft_refresh_toggle")
                            )
                        }

                        Text(
                            text = "Freeze Timeout Threshold: ${summary.freezeThresholdSec}s",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Slider(
                            value = summary.freezeThresholdSec.toFloat(),
                            onValueChange = { onChangeThresholdSec(it.toInt()) },
                            valueRange = 5f..60f,
                            steps = 10
                        )

                        Button(
                            onClick = onScanNow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color(0xFF050B14)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("health_scan_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan Feeds Now",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Run Immediate Feed Health Check", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Active Emulator Feeds (${windows.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    items(windows, key = { it.id }) { win ->
                        val snap = summary.feedSnapshots[win.id]
                        val status = snap?.status ?: FeedHealthStatus.fromString(win.healthStatus)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF131F38),
                            border = BorderStroke(1.dp, Color(status.badgeHex).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = win.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(status.badgeHex).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = status.label,
                                                color = Color(status.badgeHex),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = snap?.lastDiagnosis
                                            ?: win.lastRecoveryReason.ifBlank { "Heartbeat active • Soft-refreshes: ${win.softRefreshCount}" },
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onSimulateFreeze(win) },
                                    modifier = Modifier.testTag("simulate_freeze_${win.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BugReport,
                                        contentDescription = "Test Soft Refresh",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Test Freeze", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (cloudIncidents.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Recent Cloud Recovery Logs (${cloudIncidents.size})",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        items(cloudIncidents.take(5), key = { it.id }) { incident ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0A1122),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "${incident.windowTitle} • ${incident.status} (Attempt #${incident.attemptCount})",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${incident.reason} -> ${incident.recoveryAction}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
