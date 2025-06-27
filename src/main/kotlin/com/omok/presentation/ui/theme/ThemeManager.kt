package com.omok.presentation.ui.theme

import com.omok.infrastructure.logging.Logger
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities

/**
 * 테마 관리자 - 싱글톤
 */
object ThemeManager {
    private val themes = ConcurrentHashMap<String, GameTheme>()
    private var currentTheme: GameTheme = GameTheme.CLASSIC
    private val listeners = mutableListOf<ThemeChangeListener>()
    
    private const val THEME_FILE = "theme_settings.dat"
    private const val CUSTOM_THEMES_DIR = "custom_themes"
    
    init {
        // 기본 테마 로드
        GameTheme.DEFAULT_THEMES.forEach { theme ->
            themes[theme.id] = theme
        }
        
        // 저장된 테마 설정 로드
        loadThemeSettings()
        
        // 커스텀 테마 로드
        loadCustomThemes()
    }
    
    /**
     * 현재 테마 가져오기
     */
    fun getCurrentTheme(): GameTheme = currentTheme
    
    /**
     * 테마 변경
     */
    fun setTheme(themeId: String) {
        val theme = themes[themeId]
        if (theme != null && theme != currentTheme) {
            val oldTheme = currentTheme
            currentTheme = theme
            saveThemeSettings()
            notifyListeners(oldTheme, theme)
            Logger.info("ThemeManager", "Theme changed to: ${theme.name}")
        }
    }
    
    /**
     * 테마 변경 (테마 객체로)
     */
    fun setTheme(theme: GameTheme) {
        if (!themes.containsKey(theme.id)) {
            themes[theme.id] = theme
        }
        setTheme(theme.id)
    }
    
    /**
     * 모든 사용 가능한 테마 가져오기
     */
    fun getAllThemes(): List<GameTheme> {
        return themes.values.toList().sortedBy { it.name }
    }
    
    /**
     * 기본 테마만 가져오기
     */
    fun getDefaultThemes(): List<GameTheme> {
        return GameTheme.DEFAULT_THEMES
    }
    
    /**
     * 커스텀 테마만 가져오기
     */
    fun getCustomThemes(): List<GameTheme> {
        return themes.values.filter { theme ->
            GameTheme.DEFAULT_THEMES.none { it.id == theme.id }
        }.sortedBy { it.name }
    }
    
    /**
     * 테마 ID로 테마 가져오기
     */
    fun getTheme(themeId: String): GameTheme? {
        return themes[themeId]
    }
    
    /**
     * 커스텀 테마 추가
     */
    fun addCustomTheme(theme: GameTheme): Boolean {
        if (themes.containsKey(theme.id)) {
            Logger.warn("ThemeManager", "Theme with id ${theme.id} already exists")
            return false
        }
        
        themes[theme.id] = theme
        saveCustomTheme(theme)
        Logger.info("ThemeManager", "Added custom theme: ${theme.name}")
        return true
    }
    
    /**
     * 커스텀 테마 삭제
     */
    fun removeCustomTheme(themeId: String): Boolean {
        val theme = themes[themeId]
        if (theme == null || GameTheme.DEFAULT_THEMES.any { it.id == themeId }) {
            return false
        }
        
        themes.remove(themeId)
        deleteCustomThemeFile(themeId)
        
        // 현재 테마가 삭제된 경우 기본 테마로 변경
        if (currentTheme.id == themeId) {
            setTheme(GameTheme.CLASSIC.id)
        }
        
        Logger.info("ThemeManager", "Removed custom theme: ${theme.name}")
        return true
    }
    
    /**
     * 테마 변경 리스너 추가
     */
    fun addThemeChangeListener(listener: ThemeChangeListener) {
        listeners.add(listener)
    }
    
    /**
     * 테마 변경 리스너 제거
     */
    fun removeThemeChangeListener(listener: ThemeChangeListener) {
        listeners.remove(listener)
    }
    
    /**
     * 테마 설정 저장
     */
    private fun saveThemeSettings() {
        try {
            ObjectOutputStream(FileOutputStream(THEME_FILE)).use { out ->
                out.writeObject(currentTheme.id)
            }
        } catch (e: Exception) {
            Logger.error("ThemeManager", "Failed to save theme settings", e)
        }
    }
    
    /**
     * 테마 설정 로드
     */
    private fun loadThemeSettings() {
        val file = File(THEME_FILE)
        if (!file.exists()) return
        
        try {
            ObjectInputStream(FileInputStream(file)).use { input ->
                val themeId = input.readObject() as String
                themes[themeId]?.let { currentTheme = it }
            }
        } catch (e: Exception) {
            Logger.error("ThemeManager", "Failed to load theme settings", e)
        }
    }
    
    /**
     * 커스텀 테마 저장
     */
    private fun saveCustomTheme(theme: GameTheme) {
        val dir = File(CUSTOM_THEMES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        val file = File(dir, "${theme.id}.theme")
        try {
            ObjectOutputStream(FileOutputStream(file)).use { out ->
                out.writeObject(theme)
            }
        } catch (e: Exception) {
            Logger.error("ThemeManager", "Failed to save custom theme", e)
        }
    }
    
    /**
     * 커스텀 테마 파일 삭제
     */
    private fun deleteCustomThemeFile(themeId: String) {
        val file = File(CUSTOM_THEMES_DIR, "$themeId.theme")
        if (file.exists()) {
            file.delete()
        }
    }
    
    /**
     * 커스텀 테마 로드
     */
    private fun loadCustomThemes() {
        val dir = File(CUSTOM_THEMES_DIR)
        if (!dir.exists()) return
        
        dir.listFiles { file -> file.extension == "theme" }?.forEach { file ->
            try {
                ObjectInputStream(FileInputStream(file)).use { input ->
                    val theme = input.readObject() as GameTheme
                    themes[theme.id] = theme
                }
            } catch (e: Exception) {
                Logger.error("ThemeManager", "Failed to load custom theme from ${file.name}", e)
            }
        }
    }
    
    /**
     * 리스너들에게 테마 변경 알림
     */
    private fun notifyListeners(oldTheme: GameTheme, newTheme: GameTheme) {
        SwingUtilities.invokeLater {
            listeners.forEach { listener ->
                listener.onThemeChanged(oldTheme, newTheme)
            }
        }
    }
    
    /**
     * 테마 빌더를 사용하여 커스텀 테마 생성
     */
    fun createCustomTheme(
        name: String,
        baseTheme: GameTheme,
        customizer: ThemeCustomizer.() -> Unit
    ): GameTheme {
        val customizer = ThemeCustomizer(baseTheme)
        customizer.customizer()
        return customizer.build(name)
    }
}

/**
 * 테마 변경 리스너
 */
interface ThemeChangeListener {
    fun onThemeChanged(oldTheme: GameTheme, newTheme: GameTheme)
}

/**
 * 테마 커스터마이저
 */
class ThemeCustomizer(private val baseTheme: GameTheme) {
    private var colorScheme: ColorScheme = baseTheme.colorScheme
    private var boardStyle: BoardStyle = baseTheme.boardStyle
    private var stoneStyle: StoneStyle = baseTheme.stoneStyle
    private var fontScheme: FontScheme = baseTheme.fontScheme
    private var soundScheme: SoundScheme? = baseTheme.soundScheme
    
    fun colors(builder: ColorSchemeBuilder.() -> Unit) {
        val colorBuilder = ColorSchemeBuilder(colorScheme)
        colorBuilder.builder()
        colorScheme = colorBuilder.build()
    }
    
    fun board(style: BoardStyle) {
        boardStyle = style
    }
    
    fun stones(style: StoneStyle) {
        stoneStyle = style
    }
    
    fun fonts(scheme: FontScheme) {
        fontScheme = scheme
    }
    
    fun sounds(scheme: SoundScheme?) {
        soundScheme = scheme
    }
    
    fun build(name: String): GameTheme {
        val id = "custom_${name.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}"
        return GameTheme(
            id = id,
            name = name,
            description = "커스텀 테마 - $name",
            colorScheme = colorScheme,
            boardStyle = boardStyle,
            stoneStyle = stoneStyle,
            fontScheme = fontScheme,
            soundScheme = soundScheme
        )
    }
}

/**
 * 색상 구성 빌더
 */
class ColorSchemeBuilder(private val base: ColorScheme) {
    private val colors = mutableMapOf<String, java.awt.Color>()
    
    fun background(color: java.awt.Color) { colors["background"] = color }
    fun surface(color: java.awt.Color) { colors["surface"] = color }
    fun boardBackground(color: java.awt.Color) { colors["boardBackground"] = color }
    fun boardLines(color: java.awt.Color) { colors["boardLines"] = color }
    fun blackStone(color: java.awt.Color) { colors["blackStone"] = color }
    fun whiteStone(color: java.awt.Color) { colors["whiteStone"] = color }
    fun primary(color: java.awt.Color) { colors["primary"] = color }
    fun secondary(color: java.awt.Color) { colors["secondary"] = color }
    fun textPrimary(color: java.awt.Color) { colors["textPrimary"] = color }
    fun textSecondary(color: java.awt.Color) { colors["textSecondary"] = color }
    
    fun build(): ColorScheme {
        return ColorScheme(
            background = colors["background"] ?: base.background,
            surface = colors["surface"] ?: base.surface,
            surfaceVariant = base.surfaceVariant,
            boardBackground = colors["boardBackground"] ?: base.boardBackground,
            boardLines = colors["boardLines"] ?: base.boardLines,
            boardStarPoints = base.boardStarPoints,
            boardCoordinates = base.boardCoordinates,
            boardHighlight = base.boardHighlight,
            boardLastMove = base.boardLastMove,
            boardHover = base.boardHover,
            boardForbidden = base.boardForbidden,
            blackStone = colors["blackStone"] ?: base.blackStone,
            whiteStone = colors["whiteStone"] ?: base.whiteStone,
            blackStoneOutline = base.blackStoneOutline,
            whiteStoneOutline = base.whiteStoneOutline,
            stoneNumber = base.stoneNumber,
            primary = colors["primary"] ?: base.primary,
            primaryVariant = base.primaryVariant,
            secondary = colors["secondary"] ?: base.secondary,
            secondaryVariant = base.secondaryVariant,
            error = base.error,
            warning = base.warning,
            success = base.success,
            info = base.info,
            textPrimary = colors["textPrimary"] ?: base.textPrimary,
            textSecondary = colors["textSecondary"] ?: base.textSecondary,
            textDisabled = base.textDisabled,
            textOnPrimary = base.textOnPrimary,
            textOnSecondary = base.textOnSecondary
        )
    }
}