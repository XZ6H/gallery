/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.google.ai.edge.gallery.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.ai.edge.gallery.data.history.ConversationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Modal dialog variant of [ConversationHistoryScreen] — matches the look of the
 * config dialog so the history list feels like an inline popup rather than a
 * separate screen.
 */
@Composable
fun ConversationHistoryDialog(
  taskId: String?,
  viewModel: ConversationHistoryViewModel,
  onDismissed: () -> Unit,
  onConversationClicked: (ConversationEntity) -> Unit,
) {
  val conversations by (
    if (taskId != null) viewModel.getConversationsByTask(taskId)
    else viewModel.getAllConversations()
  ).collectAsState(initial = emptyList())

  var conversationToDelete by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  Dialog(onDismissRequest = onDismissed) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
          "Conversation History",
          style = MaterialTheme.typography.titleLarge,
        )

        if (conversations.isEmpty()) {
          Text(
            "No conversations yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp),
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(conversations, key = { it.id }) { conversation ->
              HistoryRow(
                conversation = conversation,
                onClick = {
                  onConversationClicked(conversation)
                  onDismissed()
                },
                onDelete = { conversationToDelete = conversation.id },
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismissed) { Text("Close") }
        }
      }
    }
  }

  conversationToDelete?.let { convId ->
    AlertDialog(
      onDismissRequest = { conversationToDelete = null },
      title = { Text("Delete conversation?") },
      text = { Text("This conversation will be permanently deleted.") },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              viewModel.deleteConversation(convId)
              conversationToDelete = null
            }
          },
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { conversationToDelete = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun HistoryRow(
  conversation: ConversationEntity,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.Chat,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = conversation.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Medium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = formatTimestamp(conversation.lastMessageTimestamp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete) {
        Icon(
          Icons.Rounded.Delete,
          contentDescription = "Delete conversation",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

private fun formatTimestamp(timestamp: Long): String {
  val now = System.currentTimeMillis()
  val diffMs = now - timestamp
  return when {
    diffMs < 60_000L -> "Just now"
    diffMs < 3_600_000L -> "${diffMs / 60_000L}m ago"
    diffMs < 86_400_000L -> "${diffMs / 3_600_000L}h ago"
    diffMs < 604_800_000L -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
    else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
  }
}
