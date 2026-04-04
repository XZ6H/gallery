# Plan: Persistent Conversation History

## Overview
Add Room-based persistence for LLM chat conversations, a history screen, and save/load hooks into the existing ChatViewModel system.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer                              │
│  ConversationHistoryScreen (LazyColumn of cards)          │
│  └── tap → navigate to existing chat with loaded messages │
│  └── swipe to delete conversation                        │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│               ViewModel Layer                             │
│  ConversationHistoryViewModel                             │
│  ├── loadConversations() → Flow<List<Conversation>>      │
│  ├── deleteConversation(id)                               │
│  ├── loadConversation(id) → List<DbChatMessage>           │
│  └── saveConversation(taskId, model, messages)            │
│                                                            │
│  ChatViewModel (existing) → add save hooks                │
│  ├── on message added → save to DB (debounced)            │
│  └── on session reset → new conversation ID               │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              Repository Layer                             │
│  ConversationRepository                                  │
│  ├── getConversationsByTask(taskId) → Flow              │
│  ├── getConversationWithMessages(convId)                 │
│  ├── saveConversation/Message                            │
│  ├── deleteConversation(convId)                          │
│  └── loadMessagesForResume(convId) → ChatMessage[]       │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              Room Database                                │
│  ConversationHistoryDatabase (v1)                        │
│  ├── ConversationDao                                     │
│  └── Tables: conversations, messages                     │
└─────────────────────────────────────────────────────────┘
```

## Data Model

### Room Entities

**ConversationEntity**
- `id: String` (UUID, PK)
- `taskId: String` (LLM_CHAT, LLM_ASK_IMAGE, LLM_ASK_AUDIO)
- `modelName: String`
- `title: String` (auto-generated from first user message)
- `lastMessageTimestamp: Long`
- `createdAt: Long`

**MessageEntity**
- `id: Long` (auto, PK)
- `conversationId: String` (FK → ConversationEntity.id, cascade delete)
- `order: Int`
- `type: String` (TEXT, THINKING, ERROR, IMAGE, etc.)
- `side: String` (USER, AGENT, SYSTEM)
- `content: String`
- `metadataJson: String` (for extra fields)

### Serialization Approach
Only persist TEXT, THINKING, ERROR types (the ones that matter for history). Images/audio are not persisted — those conversations show only text history when loaded. On resume, only text/thinking/error messages are restored.

## Files to Create
1. `data/history/ConversationHistory.kt` — Room entities + DAO + Database
2. `data/history/ConversationRepository.kt` — Repository
3. `di/DatabaseModule.kt` — Hilt provides for DB + Repository
4. `ui/history/ConversationHistoryScreen.kt` — History list screen
5. `ui/history/ConversationHistoryViewModel.kt` — History VM

## Files to Modify
1. `build.gradle.kts` — Add Room dependencies
2. `libs.versions.toml` — Add Room version entries
3. `ui/common/chat/ChatViewModel.kt` — Add save hooks (optional parameter)
4. `ui/navigation/GalleryNavGraph.kt` — Add history route
5. `ui/common/chat/ChatView.kt` — Add "view history" access point

## Implementation Steps (sequential)
1. Add Room deps to gradle files
2. Create Room entities, DAO, Database
3. Create ConversationRepository
4. Create Hilt DatabaseModule
5. Create ConversationHistoryViewModel
6. Create ConversationHistoryScreen
7. Hook save into ChatViewModel
8. Add navigation for history screen

