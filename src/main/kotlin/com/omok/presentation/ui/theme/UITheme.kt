package com.omok.presentation.ui.theme

import java.awt.Color
import java.awt.Font
import javax.swing.UIManager
import javax.swing.BorderFactory
import javax.swing.border.Border

object UITheme {
    object Colors {
        val PRIMARY = Color(99, 102, 241)
        val PRIMARY_DARK = Color(79, 70, 229)
        val PRIMARY_LIGHT = Color(165, 180, 252)
        
        val SECONDARY = Color(168, 85, 247)
        val SECONDARY_DARK = Color(147, 51, 234)
        
        val SUCCESS = Color(34, 197, 94)
        val SUCCESS_DARK = Color(22, 163, 74)
        
        val WARNING = Color(251, 146, 60)
        val WARNING_DARK = Color(234, 88, 12)
        
        val DANGER = Color(239, 68, 68)
        val DANGER_DARK = Color(220, 38, 38)
        
        val GRAY_900 = Color(17, 24, 39)
        val GRAY_800 = Color(31, 41, 55)
        val GRAY_700 = Color(55, 65, 81)
        val GRAY_600 = Color(75, 85, 99)
        val GRAY_500 = Color(107, 114, 128)
        val GRAY_400 = Color(156, 163, 175)
        val GRAY_300 = Color(209, 213, 219)
        val GRAY_200 = Color(229, 231, 235)
        val GRAY_100 = Color(243, 244, 246)
        val GRAY_50 = Color(249, 250, 251)
        
        val BACKGROUND = Color(255, 255, 255)
        val SURFACE = Color(249, 250, 251)
        val SURFACE_DARK = Color(243, 244, 246)
        
        val BOARD_BACKGROUND = Color(220, 179, 92)  // 나무색
        val BOARD_LINE = Color(0, 0, 0)  // 검은색 선
        val BOARD_LINE_LIGHT = Color(0, 0, 0, 50)  // 연한 검은색
        
        val BLACK_STONE = Color(24, 24, 27)
        val BLACK_STONE_HIGHLIGHT = Color(39, 39, 42)
        val WHITE_STONE = Color(250, 250, 250)
        val WHITE_STONE_SHADOW = Color(228, 228, 231)
        
        val HOVER_OVERLAY = Color(0, 0, 0, 40)
        val FORBIDDEN_OVERLAY = Color(239, 68, 68, 180)
        val WIN_LINE = Color(255, 215, 0)
        val WIN_LINE_GLOW = Color(255, 215, 0, 100)
        
        val LAST_MOVE_INDICATOR = Color(99, 102, 241)
        val SHADOW = Color(0, 0, 0, 20)
        val SHADOW_DARK = Color(0, 0, 0, 40)
    }
    
    object Fonts {
        // 시스템 폰트 감지
        private fun getSystemFont(): String {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                osName.contains("win") -> "맑은 고딕"  // Windows
                osName.contains("mac") -> "AppleGothic"    // macOS
                else -> "Noto Sans CJK KR"                  // Linux
            }
        }
        
        private val SYSTEM_FONT = getSystemFont()
        
        // 폰트 크기 조정 - 전체적으로 확대
        val TITLE = Font(SYSTEM_FONT, Font.BOLD, 28)
        val HEADING = Font(SYSTEM_FONT, Font.BOLD, 22)
        val SUBHEADING = Font(SYSTEM_FONT, Font.PLAIN, 18)
        val BODY = Font(SYSTEM_FONT, Font.PLAIN, 16)
        val BODY_SMALL = Font(SYSTEM_FONT, Font.PLAIN, 14)
        val BUTTON = Font(SYSTEM_FONT, Font.BOLD, 16)
        val LABEL = Font(SYSTEM_FONT, Font.PLAIN, 14)
        val CAPTION = Font(SYSTEM_FONT, Font.PLAIN, 13)
    }
    
    object Spacing {
        const val XS = 4
        const val SM = 8
        const val MD = 16
        const val LG = 24
        const val XL = 32
        const val XXL = 48
    }
    
    object BorderRadius {
        const val SM = 6
        const val MD = 8
        const val LG = 12
        const val XL = 16
        const val FULL = 9999
    }
    
    object Elevation {
        fun getElevation(level: Int): Border {
            return when (level) {
                1 -> BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(0, 0, 0, 10), 1),
                    BorderFactory.createEmptyBorder(1, 1, 2, 1)
                )
                2 -> BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(0, 0, 0, 15), 1),
                    BorderFactory.createEmptyBorder(2, 2, 4, 2)
                )
                3 -> BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(0, 0, 0, 20), 1),
                    BorderFactory.createEmptyBorder(3, 3, 6, 3)
                )
                else -> BorderFactory.createEmptyBorder()
            }
        }
    }
    
    fun applyTheme() {
        UIManager.put("Panel.background", Colors.BACKGROUND)
        UIManager.put("Button.font", Fonts.BUTTON)
        UIManager.put("Label.font", Fonts.BODY)
        UIManager.put("TextField.font", Fonts.BODY)
        UIManager.put("TextArea.font", Fonts.BODY)
        UIManager.put("ComboBox.font", Fonts.BODY)
        UIManager.put("List.font", Fonts.BODY)
        UIManager.put("Table.font", Fonts.BODY)
        UIManager.put("Menu.font", Fonts.BODY_SMALL)
        UIManager.put("MenuItem.font", Fonts.BODY_SMALL)
        
        UIManager.put("Button.background", Colors.PRIMARY)
        UIManager.put("Button.foreground", Color.WHITE)
        UIManager.put("Button.select", Colors.PRIMARY_DARK)
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(10, 20, 10, 20))
        
        UIManager.put("OptionPane.background", Colors.BACKGROUND)
        UIManager.put("OptionPane.messageForeground", Colors.GRAY_800)
        UIManager.put("OptionPane.messageFont", Fonts.BODY)
        
        UIManager.put("MenuBar.background", Colors.SURFACE)
        UIManager.put("Menu.background", Colors.SURFACE)
        UIManager.put("MenuItem.background", Colors.SURFACE)
        UIManager.put("Menu.foreground", Colors.GRAY_700)
        UIManager.put("MenuItem.foreground", Colors.GRAY_700)
        UIManager.put("MenuItem.selectionBackground", Colors.PRIMARY_LIGHT)
        UIManager.put("MenuItem.selectionForeground", Colors.GRAY_900)
    }
}