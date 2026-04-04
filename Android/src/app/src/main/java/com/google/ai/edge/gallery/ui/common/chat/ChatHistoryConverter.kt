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

package com.google.ai.edge.gallery.ui.common.chat

import com.google.ai.edge.gallery.data.history.MessageEntity
import org.json.JSONObject

/** Converts between ChatMessage objects and Room MessageEntity for persistence. */
object ChatHistoryConverter {

  // Message types we can persist. Images, audio, and transient types are skipped.
  private val PERSISTABLE_TYPES = setOf(
    ChatMessageType.TEXT,
    ChatMessageType.THINKING,
    ChatMessageType.ERROR,
    ChatMessageType.INFO,
    ChatMessageType.WARNING,
  )

  /** Convert a ChatMessage to a MessageEntity for DB storage. Returns null for non-persistable
  types. */
  fun fromMessage(
    message: Any,
    conversationId: String,
    order: Int,
  ): MessageEntity? {
    return when (message) {
      is ChatMessageText -> {
        val meta = JSONObject().apply {
          put("latencyMs", message.latencyMs)
          put("isMarkdown", message.isMarkdown)
          put("accelerator", message.accelerator)
          put("hideSenderLabel", message.hideSenderLabel)
        }
        MessageEntity(
          conversationId = conversationId,
          order = order,
          type = ChatMessageType.TEXT.name,
          side = message.side.name,
          content = message.content,
          metadataJson = meta.toString(),
        )
      }
      is ChatMessageThinking -> {
        val meta = JSONObject().apply {
          put("inProgress", message.inProgress)
          put("accelerator", message.accelerator)
          put("hideSenderLabel", message.hideSenderLabel)
        }
        MessageEntity(
          conversationId = conversationId,
          order = order,
          type = ChatMessageType.THINKING.name,
          side = message.side.name,
          content = message.content,
          metadataJson = meta.toString(),
        )
      }
      is ChatMessageError -> MessageEntity(
        conversationId = conversationId,
        order = order,
        type = message.type.name,
        side = message.side.name,
        content = message.content,
      )
      is ChatMessageInfo -> MessageEntity(
        conversationId = conversationId,
        order = order,
        type = message.type.name,
        side = message.side.name,
        content = message.content,
      )
      is ChatMessageWarning -> MessageEntity(
        conversationId = conversationId,
        order = order,
        type = message.type.name,
        side = message.side.name,
        content = message.content,
      )
      else -> null
    }
  }

  /** Convert a MessageEntity back to a ChatMessage. */
  fun toMessage(entity: MessageEntity): ChatMessage? {
    return try {
      val type = ChatMessageType.valueOf(entity.type)
      val side = ChatSide.valueOf(entity.side)
      val meta = if (entity.metadataJson != "{}") {
        JSONObject(entity.metadataJson)
      } else {
        null
      }
      when (type) {
        ChatMessageType.TEXT -> ChatMessageText(
          content = entity.content,
          side = side,
          latencyMs = meta?.optDouble("latencyMs", 0.0)?.toFloat() ?: 0f,
          isMarkdown = meta?.optBoolean("isMarkdown", true) ?: true,
          accelerator = meta?.optString("accelerator", "") ?: "",
          hideSenderLabel = meta?.optBoolean("hideSenderLabel", false) ?: false,
        )
        ChatMessageType.THINKING -> ChatMessageThinking(
          content = entity.content,
          inProgress = meta?.optBoolean("inProgress", false) ?: false,
          side = side,
          accelerator = meta?.optString("accelerator", "") ?: "",
          hideSenderLabel = meta?.optBoolean("hideSenderLabel", false) ?: false,
        )
        ChatMessageType.ERROR -> ChatMessageError(content = entity.content)
        ChatMessageType.INFO -> ChatMessageInfo(content = entity.content)
        ChatMessageType.WARNING -> ChatMessageWarning(content = entity.content)
        else -> null
      }
    } catch (e: Exception) {
      null
    }
  }
}
