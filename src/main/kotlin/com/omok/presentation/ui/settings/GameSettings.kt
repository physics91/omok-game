package com.omok.presentation.ui.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val SETTINGS_FILE = File(System.getProperty("user.home"), ".omok/ui_settings.json")

@Serializable
data class UIGameSettings(
    val showMoveNumbers: Boolean = false,
    val showCoordinates: Boolean = true,
    val showLastMove: Boolean = true,
    val showForbiddenMoves: Boolean = true,
    val showAIThinking: Boolean = true,
    val soundEnabled: Boolean = true,
    val animationEnabled: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC,
    val stoneTheme: StoneTheme = StoneTheme.CLASSIC,
    val isDarkMode: Boolean = false,
    val aiThinkingTime: AIThinkingTime = AIThinkingTime.NORMAL
) {
    companion object {
        private var instance: UIGameSettings? = null

        fun getInstance(): UIGameSettings {
            if (instance == null) {
                instance = SettingsRepository.load()
            }
            return instance!!
        }

        fun updateSettings(newSettings: UIGameSettings) {
            instance = newSettings
            SettingsRepository.save(newSettings)
        }
    }
}

private object SettingsRepository {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun save(settings: UIGameSettings) {
        try {
            SETTINGS_FILE.parentFile.mkdirs()
            SETTINGS_FILE.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            // 로깅 필요
            e.printStackTrace()
        }
    }

    fun load(): UIGameSettings {
        return try {
            if (SETTINGS_FILE.exists()) {
                json.decodeFromString<UIGameSettings>(SETTINGS_FILE.readText())
            } else {
                UIGameSettings()
            }
        } catch (e: Exception) {
            // 로깅 필요
            e.printStackTrace()
            UIGameSettings()
        }
    }
}

@Serializable
enum class BoardTheme(val displayName: String) {
    CLASSIC("클래식"),
    MODERN("모던"),
    DARK("다크")
}

@Serializable
enum class StoneTheme(val displayName: String) {
    CLASSIC("클래식"),
    GLASS("유리"),
    MARBLE("대리석")
}

@Serializable
enum class AIThinkingTime(
    val displayName: String,
    val milliseconds: Long,
    val description: String
) {
    INSTANT("즉시", 100L, "AI가 즉시 수를 둡니다"),
    FAST("빠름", 500L, "0.5초 사고"),
    NORMAL("보통", 1500L, "1.5초 사고"),
    SLOW("느림", 3000L, "3초 사고"),
    VERY_SLOW("매우 느림", 5000L, "5초 사고"),
    REALISTIC("현실적", 8000L, "8초 사고 (토너먼트 수준)")
}