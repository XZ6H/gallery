/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.google.ai.edge.gallery.customtasks.tinygarden

import android.content.Context

/** Simple SharedPreferences-backed store for TinyGarden user settings. */
object TinyGardenPrefs {
  private const val PREFS_NAME = "tiny_garden_prefs"
  private const val KEY_CUSTOM_SYSTEM_PROMPT = "custom_system_prompt"

  private fun prefs(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /** Returns the user's custom system prompt, or null if they haven't set one. */
  fun getCustomSystemPrompt(context: Context): String? =
    prefs(context).getString(KEY_CUSTOM_SYSTEM_PROMPT, null)

  /** Returns the effective base prompt: custom if set, otherwise the default. */
  fun getEffectiveBasePrompt(context: Context): String =
    getCustomSystemPrompt(context) ?: DEFAULT_TINY_GARDEN_SYSTEM_PROMPT

  fun setCustomSystemPrompt(context: Context, prompt: String) {
    prefs(context).edit().putString(KEY_CUSTOM_SYSTEM_PROMPT, prompt).apply()
  }

  fun clearCustomSystemPrompt(context: Context) {
    prefs(context).edit().remove(KEY_CUSTOM_SYSTEM_PROMPT).apply()
  }
}
