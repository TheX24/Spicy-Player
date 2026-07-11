package com.tx24.spicyplayer.ui.common

import androidx.compose.runtime.compositionLocalOf
import com.tx24.spicyplayer.ui.model.UserPreferencesUi


val LocalUserPreferences = compositionLocalOf<UserPreferencesUi> { throw IllegalArgumentException("Not provided") }