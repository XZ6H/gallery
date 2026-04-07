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
package com.google.ai.edge.gallery.customtasks.tinygarden

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.history.ConversationEntity
import com.google.ai.edge.gallery.ui.common.chat.ChatHistoryConverter
import com.google.ai.edge.gallery.ui.common.chat.ChatMessage
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageError
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning
import com.google.ai.edge.gallery.ui.common.chat.ChatSide
import com.google.ai.edge.gallery.ui.common.chat.MessageBodyError
import com.google.ai.edge.gallery.ui.common.chat.MessageBodyText
import com.google.ai.edge.gallery.ui.common.chat.MessageBodyWarning
import com.google.ai.edge.gallery.ui.common.chat.MessageBubbleShape
import com.google.ai.edge.gallery.ui.common.chat.MessageSender
import com.google.ai.edge.gallery.ui.history.ConversationHistoryViewModel
import com.google.ai.edge.gallery.ui.theme.customColors
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** A panel to show the conversation history. */
@Composable
fun ConversationHistoryPanel(
  task: Task,
  bottomPadding: Dp,
  viewModel: TinyGardenViewModel,
  historyViewModel: ConversationHistoryViewModel,
  onDismiss: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val savedConversations by historyViewModel
    .getConversationsByTask(BuiltInTaskId.LLM_TINY_GARDEN)
    .collectAsState(initial = emptyList())

  // null = show conversation list; non-null = show messages for that conversation ID.
  // Empty string is a sentinel meaning "show current session".
  var selectedConversationId: String? by remember { mutableStateOf(null) }
  var detailMessages: List<ChatMessage> by remember { mutableStateOf(emptyList()) }
  val scope = rememberCoroutineScope()

  // Load messages when a saved conversation is selected.
  LaunchedEffect(selectedConversationId) {
    val id = selectedConversationId
    if (id != null && id.isNotEmpty()) {
      val loaded = historyViewModel.loadConversation(id)
      detailMessages = loaded?.messages
        ?.sortedBy { it.order }
        ?.mapNotNull { ChatHistoryConverter.toMessage(it) }
        ?: emptyList()
    }
  }

  Column(
    modifier =
      Modifier.background(color = MaterialTheme.colorScheme.surface)
        .fillMaxSize()
        .padding(bottom = bottomPadding)
  ) {
    // Title bar.
    Row(
      modifier =
        Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerHighest)
          .fillMaxWidth()
          .padding(start = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (selectedConversationId != null) {
        IconButton(onClick = { selectedConversationId = null }) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_close_icon),
          )
        }
      } else {
        Box(modifier = Modifier.padding(start = 8.dp)) {
          Text(
            stringResource(R.string.conversation_history),
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
      IconButton(onClick = { onDismiss() }) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = stringResource(R.string.cd_close_icon),
        )
      }
    }

    if (selectedConversationId == null) {
      // --- Conversation list ---
      val currentMessages = uiState.messages
      if (currentMessages.isEmpty() && savedConversations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            "No conversation history yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          // Current session entry.
          if (currentMessages.isNotEmpty()) {
            item {
              ConversationListItem(
                title = "Current Session",
                subtitle = "${currentMessages.size} message${if (currentMessages.size == 1) "" else "s"}",
                onClick = {
                  detailMessages = currentMessages
                  selectedConversationId = "" // sentinel for current session
                },
                onDelete = null,
              )
              HorizontalDivider()
            }
          }

          // Saved conversations.
          items(savedConversations, key = { it.id }) { conversation ->
            ConversationListItem(
              title = conversation.title,
              subtitle = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(conversation.lastMessageTimestamp)),
              onClick = { selectedConversationId = conversation.id },
              onDelete = {
                scope.launch { historyViewModel.deleteConversation(conversation.id) }
              },
            )
            HorizontalDivider()
          }
        }
      }
    } else {
      // --- Message detail view ---
      val messages = if (selectedConversationId!!.isEmpty()) uiState.messages else detailMessages
      val listState = rememberScrollState()

      LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollTo(Int.MAX_VALUE)
      }

      if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            "No messages",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        Column(
          modifier =
            Modifier.weight(1f).padding(horizontal = 16.dp).verticalScroll(state = listState)
        ) {
          for (message in messages) {
            var hAlign: Alignment.Horizontal = Alignment.End
            var backgroundColor: Color = MaterialTheme.customColors.userBubbleBgColor
            var hardCornerAtLeftOrRight = false
            var extraPaddingStart = 48.dp
            var extraPaddingEnd = 0.dp
            if (message.side == ChatSide.AGENT) {
              hAlign = Alignment.Start
              backgroundColor = MaterialTheme.customColors.agentBubbleBgColor
              hardCornerAtLeftOrRight = true
              extraPaddingStart = 0.dp
              extraPaddingEnd = 48.dp
            } else if (message.side == ChatSide.SYSTEM) {
              extraPaddingStart = 24.dp
              extraPaddingEnd = 24.dp
            }
            val bubbleBorderRadius = dimensionResource(R.dimen.chat_bubble_corner_radius)

            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(
                    start = extraPaddingStart,
                    end = extraPaddingEnd,
                    top = 6.dp,
                    bottom = 6.dp,
                  ),
              horizontalAlignment = hAlign,
            ) messageColumn@{
              var agentName = stringResource(task.agentNameRes)
              if (message.accelerator.isNotEmpty()) {
                agentName = "$agentName on ${message.accelerator}"
              }
              MessageSender(message = message, agentName = agentName)

              when (message) {
                is ChatMessageWarning -> MessageBodyWarning(message = message)
                is ChatMessageError -> MessageBodyError(message = message)
                else -> {
                  when (message) {
                    is ChatMessageText -> {
                      Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        Box(
                          modifier =
                            Modifier.clip(
                                MessageBubbleShape(
                                  radius = bubbleBorderRadius,
                                  hardCornerAtLeftOrRight = hardCornerAtLeftOrRight,
                                )
                              )
                              .background(backgroundColor)
                        ) {
                          MessageBodyText(message = message, inProgress = false)
                        }
                      }
                    }
                    else -> {}
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ConversationListItem(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  onDelete: (() -> Unit)?,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable { onClick() }
        .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        title,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (onDelete != null) {
      IconButton(onClick = onDelete) {
        Icon(
          imageVector = Icons.Rounded.Delete,
          contentDescription = "Delete conversation",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
