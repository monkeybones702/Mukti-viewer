package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ai.ChatbotRolePersona
import com.example.ai.GeminiTaskTier
import com.example.data.model.CloudChatMessage

@Composable
fun AiCommandAssistantDialog(
    messages: List<CloudChatMessage>,
    selectedTier: GeminiTaskTier,
    selectedPersona: ChatbotRolePersona,
    enableSearchGrounding: Boolean,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onSelectTier: (GeminiTaskTier) -> Unit,
    onSelectPersona: (ChatbotRolePersona) -> Unit,
    onToggleSearchGrounding: (Boolean) -> Unit,
    onSendPrompt: (String) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "Recommend an optimal 4-Window Focus layout",
        "Why do WebView emulator streams freeze and how does soft-refresh help?",
        "Find live public browser emulators and telemetry feeds"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_assistant_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527)),
            border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI Command Assistant",
                            tint = Color(0xFF00E5FF)
                        )
                        Column {
                            Text(
                                text = "Gemini Multi-Stream Assistant",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${selectedPersona.title} • ${selectedTier.modelId}",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close AI Assistant",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Role Persona Selector
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ChatbotRolePersona.entries) { persona ->
                        FilterChip(
                            selected = selectedPersona == persona,
                            onClick = { onSelectPersona(persona) },
                            label = { Text(persona.title, fontSize = 10.sp) }
                        )
                    }
                }

                // Gemini Model Tier Selector (3.5 Flash, 3.1 Pro, 3.1 Flash Lite)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GeminiTaskTier.entries) { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { onSelectTier(tier) },
                            label = { Text(tier.label, fontSize = 10.sp) }
                        )
                    }
                }

                if (selectedTier.supportsSearchGrounding) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Google Search Grounding",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Google Search Grounding (gemini-3.5-flash)",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = enableSearchGrounding,
                            onCheckedChange = onToggleSearchGrounding
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Chat Thread
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .testTag("ai_chat_thread")
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF131F38),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Role Instruction: ${selectedPersona.systemInstruction}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    quickPrompts.forEach { q ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable { onSendPrompt(q) }
                                        ) {
                                            Text(
                                                text = "→ $q",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val sorted = messages.sortedBy { it.createdAt?.seconds ?: 0L }
                        items(sorted, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUser) Color(0xFF1E3A8A) else Color(0xFF131F38),
                                border = BorderStroke(
                                    1.dp,
                                    if (isUser) Color(0xFF3B82F6) else Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isUser) "You" else "${msg.agentPersona} (${msg.modelId})",
                                        color = if (isUser) Color(0xFF93C5FD) else Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = msg.content,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    if (msg.sourcesSummary.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Search Grounding Sources:\n${msg.sourcesSummary}",
                                            color = Color(0xFF00E676),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Ask ${selectedPersona.badgeLabel} assistant...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_prompt_input")
                    )
                    Button(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                onSendPrompt(promptInput)
                                promptInput = ""
                            }
                        },
                        enabled = !isGenerating && promptInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF050B14)
                        ),
                        modifier = Modifier.testTag("ai_send_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF050B14),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Prompt",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
