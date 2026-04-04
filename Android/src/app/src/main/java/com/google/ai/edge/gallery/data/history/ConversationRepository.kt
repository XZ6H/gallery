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

package com.google.ai.edge.gallery.data.history

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing conversation history persistence.
 * Bridges Room database operations with higher-level semantics.
 */
@Singleton
class ConversationRepository @Inject constructor(
  private val database: ConversationHistoryDatabase,
) {
  private val dao: ConversationHistoryDao = database.conversationHistoryDao()

  /** Get all conversations, most recent first. */
  fun getAllConversations(): Flow<List<ConversationEntity>> {
    return dao.getAllConversations()
  }

  /** Get conversations for a specific task, most recent first. */
  fun getConversationsByTask(taskId: String): Flow<List<ConversationEntity>> {
    return dao.getConversationsByTask(taskId)
  }

  /** Get a single conversation by ID. */
  suspend fun getConversation(conversationId: String): ConversationEntity? {
    return dao.getConversation(conversationId)
  }

  /** Get a conversation with all its messages. */
  suspend fun getConversationWithMessages(conversationId: String): ConversationWithMessages? {
    return try {
      dao.getConversationWithMessages(conversationId)
    } catch (e: Exception) {
      null
    }
  }

  /** Save or update a conversation. */
  suspend fun saveConversation(conversation: ConversationEntity) {
    dao.insertConversation(conversation)
  }

  /** Save messages for a conversation. */
  suspend fun saveMessages(messages: List<MessageEntity>) {
    if (messages.isNotEmpty()) {
      dao.insertMessages(messages)
    }
  }

  /** Delete a conversation and all its messages (cascade). */
  suspend fun deleteConversationById(conversationId: String) {
    dao.deleteConversationById(conversationId)
  }

  /** Get raw message entities for a conversation. */
  suspend fun getMessagesForConversation(conversationId: String): List<MessageEntity> {
    return dao.getMessagesForConversation(conversationId)
  }
}
