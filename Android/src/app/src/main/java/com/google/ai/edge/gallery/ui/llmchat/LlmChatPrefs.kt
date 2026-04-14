/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.google.ai.edge.gallery.ui.llmchat

import android.content.Context

/** SharedPreferences-backed store for LLM Chat user settings (e.g. custom system prompt). */
object LlmChatPrefs {
  private const val PREFS_NAME = "llm_chat_prefs"
  private const val KEY_SYSTEM_PROMPT_PREFIX = "system_prompt_"

  private fun prefs(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private fun key(taskId: String) = "$KEY_SYSTEM_PROMPT_PREFIX$taskId"

  /** Returns the user's custom system prompt for this task, or empty if unset. */
  fun getSystemPrompt(context: Context, taskId: String): String =
    prefs(context).getString(key(taskId), null) ?: ""

  fun setSystemPrompt(context: Context, taskId: String, prompt: String) {
    val p = prefs(context)
    if (prompt.isEmpty()) {
      p.edit().remove(key(taskId)).apply()
    } else {
      p.edit().putString(key(taskId), prompt).apply()
    }
  }
}
