package com.omok.presentation.ui.icons

import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import com.omok.infrastructure.logging.Logger

/**
 * SVG 아이콘 로더 유틸리티
 */
object IconLoader {
    
    /**
     * 사용 가능한 아이콘들
     */
    enum class Icon(val filename: String) {
        BLACK_STONE("black-stone.svg"),
        WHITE_STONE("white-stone.svg"),
        NEW_GAME("new-game.svg"),
        UNDO("undo.svg"),
        SETTINGS("settings.svg"),
        AI_THINKING("ai-thinking.svg"),
        FORBIDDEN("forbidden.svg"),
        WIN("win.svg"),
        TIMER("timer.svg"),
        HELP("help.svg"),
        SWAP("swap.svg"),
        FIFTH_MOVE("fifth-move.svg"),
        PLAYER_VS_PLAYER("player-vs-player.svg"),
        PLAYER_VS_AI("player-vs-ai.svg"),
        DIFFICULTY_EASY("difficulty-easy.svg"),
        DIFFICULTY_MEDIUM("difficulty-medium.svg"),
        DIFFICULTY_HARD("difficulty-hard.svg"),
        MOVE_COUNT("move-count.svg"),
        LAST_MOVE("last-move.svg"),
        SAVE("save.svg"),
        LOAD("load.svg"),
        LOGO("logo.svg")
    }
    
    private val iconCache = mutableMapOf<String, ImageIcon>()
    
    /**
     * 아이콘 로드
     */
    fun getIcon(icon: Icon, width: Int = 24, height: Int = 24): ImageIcon? {
        val cacheKey = "${icon.name}_${width}x${height}"
        
        return iconCache.getOrPut(cacheKey) {
            try {
                // SVG 로딩 대신 프로그래밍 방식으로 아이콘 렌더링
                Logger.debug("IconLoader", "Rendering icon: ${icon.name} (${width}x${height})")
                IconRenderer.renderIcon(icon, width, height)
            } catch (e: Exception) {
                Logger.error("IconLoader", "Failed to render icon: ${icon.name}", e)
                createDefaultIcon(width, height)
            }
        }
    }
    
    /**
     * 기본 아이콘 생성 (SVG 로드 실패 시)
     */
    private fun createDefaultIcon(width: Int, height: Int): ImageIcon {
        val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        
        // 간단한 기본 아이콘 그리기
        g.color = java.awt.Color.GRAY
        g.fillOval(2, 2, width - 4, height - 4)
        
        g.dispose()
        return ImageIcon(image)
    }
    
    /**
     * SVG 경로 문자열 반환 (직접 렌더링용)
     */
    fun getSvgPath(icon: Icon): String {
        return "/icons/${icon.filename}"
    }
    
    /**
     * 리소스 스트림 반환
     */
    fun getIconStream(icon: Icon): java.io.InputStream? {
        return IconLoader::class.java.getResourceAsStream("/icons/${icon.filename}")
    }
}