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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.history.ConversationEntity
import com.google.ai.edge.gallery.data.history.ConversationHistoryDao
import com.google.ai.edge.gallery.data.history.ConversationRepository
import com.google.ai.edge.gallery.data.history.ConversationWithMessages
import com.google.ai.edge.gallery.data.history.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for browsing conversation history.
 */
@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
  private val repository: ConversationRepository,
) : ViewModel() {

  /** All conversations, most recent first, as a Flow. */
  fun getAllConversations(): Flow<List<ConversationEntity>> {
    return repository.getAllConversations()
  }

  /** Conversations filtered by task ID. */
  fun getConversationsByTask(taskId: String): Flow<List<ConversationEntity>> {
    return repository.getConversationsByTask(taskId)
  }

  /** Delete a conversation by ID. */
  fun deleteConversation(conversationId: String) {
    viewModelScope.launch {
      repository.deleteConversationById(conversationId)
    }
  }

  /** Load a full conversation with its messages. */
  suspend fun loadConversation(conversationId: String): ConversationWithMessages? {
    return repository.getConversationWithMessages(conversationId)
  }

  /** Save a conversation entity. */
  suspend fun saveConversation(conversation: ConversationEntity) {
    repository.saveConversation(conversation)
  }

  /** Save message entities for a conversation. */
  suspend fun saveMessages(messages: List<MessageEntity>) {
    repository.saveMessages(messages)
  }

  /** Get an existing conversation entity (for preserving createdAt on re-save). */
  suspend fun getConversation(conversationId: String): ConversationEntity? {
    return repository.getConversation(conversationId)
  }
}
