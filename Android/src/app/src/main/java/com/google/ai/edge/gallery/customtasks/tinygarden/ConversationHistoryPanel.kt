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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.History
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

// null = current session, non-null = viewing a saved conversation by ID
private sealed class PanelView {
  data object CurrentSession : PanelView()
  data object PastConversationsList : PanelView()
  data class SavedConversation(val id: String) : PanelView()
}

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

  var panelView: PanelView by remember { mutableStateOf(PanelView.CurrentSession) }
  var detailMessages: List<ChatMessage> by remember { mutableStateOf(emptyList()) }
  val scope = rememberCoroutineScope()

  // Load messages when a saved conversation is selected.
  LaunchedEffect(panelView) {
    val view = panelView
    if (view is PanelView.SavedConversation) {
      val loaded = historyViewModel.loadConversation(view.id)
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
      when (panelView) {
        is PanelView.CurrentSession -> {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.padding(start = 8.dp)) {
              Text(
                stringResource(R.string.conversation_history),
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }
        }
        is PanelView.PastConversationsList -> {
          IconButton(onClick = { panelView = PanelView.CurrentSession }) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
            )
          }
          Text("Past Conversations", style = MaterialTheme.typography.titleMedium)
        }
        is PanelView.SavedConversation -> {
          IconButton(onClick = { panelView = PanelView.PastConversationsList }) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
            )
          }
        }
      }
      Row {
        // Show past conversations button only from the current session view.
        if (panelView is PanelView.CurrentSession && savedConversations.isNotEmpty()) {
          IconButton(onClick = { panelView = PanelView.PastConversationsList }) {
            Icon(
              imageVector = Icons.Outlined.History,
              contentDescription = "Past conversations",
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
    }

    when (panelView) {
      is PanelView.CurrentSession -> {
        // Show current session messages directly.
        MessageList(
          messages = uiState.messages,
          task = task,
          emptyText = "No messages yet",
          modifier = Modifier.weight(1f),
        )
      }

      is PanelView.PastConversationsList -> {
        // Show list of saved conversations.
        val scrollState = rememberScrollState()
        Column(
          modifier = Modifier.weight(1f).verticalScroll(scrollState),
        ) {
          for (conversation in savedConversations) {
            ConversationListItem(
              title = conversation.title,
              subtitle = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(conversation.lastMessageTimestamp)),
              onClick = { panelView = PanelView.SavedConversation(conversation.id) },
              onDelete = {
                scope.launch { historyViewModel.deleteConversation(conversation.id) }
              },
            )
            HorizontalDivider()
          }
        }
      }

      is PanelView.SavedConversation -> {
        MessageList(
          messages = detailMessages,
          task = task,
          emptyText = "No messages",
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun MessageList(
  messages: List<ChatMessage>,
  task: Task,
  emptyText: String,
  modifier: Modifier = Modifier,
) {
  val listState = rememberScrollState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollTo(listState.maxValue)
    }
  }

  if (messages.isEmpty()) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      Text(
        emptyText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  } else {
    Column(
      modifier = modifier.padding(horizontal = 16.dp).verticalScroll(state = listState)
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
