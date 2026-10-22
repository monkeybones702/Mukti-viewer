package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StreamWindow

@Composable
fun WindowZoomDimensionDialog(
    targetWindow: StreamWindow?,
    initialCols: Int,
    initialRows: Int,
    initialFitPhone: Boolean,
    initialProportional: Boolean,
    onDismiss: () -> Unit,
    onApply: (cols: Int, rows: Int, fitPhone: Boolean, proportional: Boolean, applyToAll: Boolean) -> Unit
) {
    var selectedCols by remember { mutableIntStateOf(initialCols.coerceIn(1, 5)) }
    var selectedRows by remember { mutableIntStateOf(initialRows.coerceIn(1, 10)) }
    var fitPhone by remember { mutableStateOf(initialFitPhone) }
    var proportional by remember { mutableStateOf(initialProportional) }
    var applyToAll by remember { mutableStateOf(targetWindow == null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (targetWindow != null) "Window Zoom: ${targetWindow.title}" else "Global 5x10 Zoom Matrix",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Current Scale: ${selectedCols}×${selectedRows} (${(100 / selectedCols)}% × ${(100 / selectedRows)}%)",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Zoom Dialog",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive 5x10 Matrix Grid Picker
                Text(
                    text = "Tap Matrix Cell (Cols 1–5 × Rows 1–10):",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (r in 1..10) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (c in 1..5) {
                                val isActive = c <= selectedCols && r <= selectedRows
                                val isExact = c == selectedCols && r == selectedRows
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(22.dp)
                                        .background(
                                            color = when {
                                                isExact -> Color(0xFF00E5FF)
                                                isActive -> Color(0xFF00E5FF).copy(alpha = 0.28f)
                                                else -> Color(0xFF1E293B)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isActive) Color(0xFF00E5FF) else Color(0xFF334155),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            selectedCols = c
                                            selectedRows = r
                                        }
                                        .testTag("zoom_matrix_${c}x${r}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${c}×${r}",
                                        color = if (isExact) Color(0xFF050B14) else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = if (isExact) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Width Columns: $selectedCols",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Slider(
                    value = selectedCols.toFloat(),
                    onValueChange = { selectedCols = it.toInt().coerceIn(1, 5) },
                    valueRange = 1f..5f,
                    steps = 3
                )

                Text(
                    text = "Height Rows: $selectedRows",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Slider(
                    value = selectedRows.toFloat(),
                    onValueChange = { selectedRows = it.toInt().coerceIn(1, 10) },
                    valueRange = 1f..10f,
                    steps = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fit Entire Phone Screen (1×1 Fit)", color = Color.White, fontSize = 12.sp)
                    Switch(checked = fitPhone, onCheckedChange = { fitPhone = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Proportional Aspect Lock", color = Color.White, fontSize = 12.sp)
                    Switch(checked = proportional, onCheckedChange = { proportional = it })
                }

                if (targetWindow != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Apply to ALL Active Windows", color = Color(0xFF00E5FF), fontSize = 12.sp)
                        Switch(checked = applyToAll, onCheckedChange = { applyToAll = it })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedCols = 1
                            selectedRows = 1
                            fitPhone = true
                            proportional = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitScreen,
                            contentDescription = "1x1 Fit",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("1×1 Fit", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onApply(selectedCols, selectedRows, fitPhone, proportional, applyToAll)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF050B14)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_zoom_button")
                    ) {
                        Text("Apply ${selectedCols}×${selectedRows}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
