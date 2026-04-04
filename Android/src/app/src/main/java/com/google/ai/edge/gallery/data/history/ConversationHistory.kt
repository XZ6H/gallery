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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Represents a saved conversation session.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val taskId: String,
  val modelName: String,
  val title: String,
  val lastMessageTimestamp: Long,
  val createdAt: Long,
)

/**
 * Represents a single message within a conversation.
 * Images and audio clips are not stored — only text-based messages for history replay.
 */
@Entity(
  tableName = "messages",
  foreignKeys = [
    ForeignKey(
      entity = ConversationEntity::class,
      parentColumns = ["id"],
      childColumns = ["conversationId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index(value = ["conversationId"])],
)
data class MessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val conversationId: String,
  val order: Int,
  val type: String,
  val side: String,
  val content: String,
  val metadataJson: String = "{}",
)

/**
 * Joined result for loading a conversation with all its messages.
 */
data class ConversationWithMessages(
  val conversation: ConversationEntity,
  val messages: List<MessageEntity>,
)

@TypeConverters
@Dao
interface ConversationHistoryDao {

  @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
  fun getAllConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE taskId = :taskId ORDER BY lastMessageTimestamp DESC")
  fun getConversationsByTask(taskId: String): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE id = :conversationId")
  suspend fun getConversation(conversationId: String): ConversationEntity?

  @androidx.room.Transaction
  @Query("SELECT * FROM conversations WHERE id = :conversationId")
  suspend fun getConversationWithMessages(conversationId: String): ConversationWithMessages?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conversation: ConversationEntity)

  @Delete
  suspend fun deleteConversation(conversation: ConversationEntity)

  @Query("DELETE FROM conversations WHERE id = :conversationId")
  suspend fun deleteConversationById(conversationId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: MessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessages(messages: List<MessageEntity>)

  @Query("DELETE FROM messages WHERE conversationId = :conversationId")
  suspend fun deleteMessagesForConversation(conversationId: String)

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY `order` ASC")
  suspend fun getMessagesForConversation(conversationId: String): List<MessageEntity>
}

@androidx.room.Database(
  entities = [ConversationEntity::class, MessageEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class ConversationHistoryDatabase : RoomDatabase() {
  abstract fun conversationHistoryDao(): ConversationHistoryDao
}
