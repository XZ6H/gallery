/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.history.ConversationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Screen that displays the conversation history list.
 * Each conversation can be tapped to load/resume, or swiped to delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
  taskId: String? = null,
  onConversationClicked: (String, String) -> Unit,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ConversationHistoryViewModel = hiltViewModel(),
) {
  val conversations by (
    if (taskId != null) {
      viewModel.getConversationsByTask(taskId)
    } else {
      viewModel.getAllConversations()
    }
  ).collectAsState(initial = emptyList())

  var conversationToDelete by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text("Conversation History") },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
  ) { innerPadding ->
    if (conversations.isEmpty()) {
      EmptyHistoryContent(modifier = Modifier.padding(innerPadding))
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(conversations, key = { it.id }) { conversation ->
          HistoryCard(
            conversation = conversation,
            onClick = { onConversationClicked(conversation.id, conversation.modelName) },
            onDelete = { conversationToDelete = conversation.id },
          )
        }
      }
    }
  }

  // Confirmation dialog for deletion.
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
        TextButton(onClick = { conversationToDelete = null }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun HistoryCard(
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
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.Chat,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 2.dp),
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

@Composable
private fun EmptyHistoryContent(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.Chat,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    )
    Text(
      text = "No conversations yet",
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 16.dp),
    )
    Text(
      text = "Your conversation history will appear here.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      modifier = Modifier.padding(top = 8.dp),
    )
  }
}

private fun formatTimestamp(timestamp: Long): String {
  val now = System.currentTimeMillis()
  val diffMs = now - timestamp

  return when {
    diffMs < 60_000L -> "Just now"
    diffMs < 3_600_000L -> "${diffMs / 60_000L}m ago"
    diffMs < 86_400_000L -> "${diffMs / 3_600_000L}h ago"
    diffMs < 604_800_000L -> {
      val format = SimpleDateFormat("EEEE", Locale.getDefault())
      format.format(Date(timestamp))
    }
    else -> {
      val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
      format.format(Date(timestamp))
    }
  }
}
