package com.omok.presentation.ui.theme

import java.awt.Color
import java.awt.Font
import java.awt.Image
import javax.swing.ImageIcon

/**
 * 게임 테마 정의
 */
data class GameTheme(
    val id: String,
    val name: String,
    val description: String,
    val colorScheme: ColorScheme,
    val boardStyle: BoardStyle,
    val stoneStyle: StoneStyle,
    val fontScheme: FontScheme,
    val soundScheme: SoundScheme? = null,
    val customAssets: Map<String, Any> = emptyMap()
) {
    companion object {
        // 기본 테마들
        val CLASSIC = GameTheme(
            id = "classic",
            name = "클래식",
            description = "전통적인 바둑판 스타일",
            colorScheme = ColorScheme.CLASSIC,
            boardStyle = BoardStyle.WOOD_GRAIN,
            stoneStyle = StoneStyle.TRADITIONAL,
            fontScheme = FontScheme.DEFAULT
        )
        
        val MODERN = GameTheme(
            id = "modern",
            name = "모던",
            description = "깔끔한 현대적 디자인",
            colorScheme = ColorScheme.MODERN,
            boardStyle = BoardStyle.MINIMAL,
            stoneStyle = StoneStyle.FLAT,
            fontScheme = FontScheme.MODERN
        )
        
        val DARK = GameTheme(
            id = "dark",
            name = "다크 모드",
            description = "어두운 배경의 눈 편한 테마",
            colorScheme = ColorScheme.DARK,
            boardStyle = BoardStyle.DARK_WOOD,
            stoneStyle = StoneStyle.GLOWING,
            fontScheme = FontScheme.MODERN
        )
        
        val NEON = GameTheme(
            id = "neon",
            name = "네온",
            description = "사이버펑크 스타일의 형광 테마",
            colorScheme = ColorScheme.NEON,
            boardStyle = BoardStyle.GRID,
            stoneStyle = StoneStyle.NEON,
            fontScheme = FontScheme.FUTURISTIC
        )
        
        val NATURE = GameTheme(
            id = "nature",
            name = "자연",
            description = "자연에서 영감을 받은 편안한 테마",
            colorScheme = ColorScheme.NATURE,
            boardStyle = BoardStyle.BAMBOO,
            stoneStyle = StoneStyle.NATURAL,
            fontScheme = FontScheme.ORGANIC
        )
        
        val HIGH_CONTRAST = GameTheme(
            id = "high_contrast",
            name = "고대비",
            description = "시각적 접근성을 위한 고대비 테마",
            colorScheme = ColorScheme.HIGH_CONTRAST,
            boardStyle = BoardStyle.HIGH_CONTRAST,
            stoneStyle = StoneStyle.HIGH_CONTRAST,
            fontScheme = FontScheme.BOLD
        )
        
        // 모든 기본 테마 목록
        val DEFAULT_THEMES = listOf(
            CLASSIC, MODERN, DARK, NEON, NATURE, HIGH_CONTRAST
        )
    }
}

/**
 * 색상 구성
 */
data class ColorScheme(
    // 배경 색상
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    
    // 보드 색상
    val boardBackground: Color,
    val boardLines: Color,
    val boardStarPoints: Color,
    val boardCoordinates: Color,
    val boardHighlight: Color,
    val boardLastMove: Color,
    val boardHover: Color,
    val boardForbidden: Color,
    
    // 돌 색상
    val blackStone: Color,
    val whiteStone: Color,
    val blackStoneOutline: Color,
    val whiteStoneOutline: Color,
    val stoneNumber: Color,
    
    // UI 색상
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val error: Color,
    val warning: Color,
    val success: Color,
    val info: Color,
    
    // 텍스트 색상
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val textOnPrimary: Color,
    val textOnSecondary: Color
) {
    companion object {
        val CLASSIC = ColorScheme(
            background = Color(245, 245, 240),
            surface = Color.WHITE,
            surfaceVariant = Color(250, 250, 245),
            
            boardBackground = Color(220, 179, 92),
            boardLines = Color(50, 50, 50),
            boardStarPoints = Color.BLACK,
            boardCoordinates = Color(100, 100, 100),
            boardHighlight = Color(255, 255, 0, 80),
            boardLastMove = Color(255, 0, 0, 100),
            boardHover = Color(0, 255, 0, 50),
            boardForbidden = Color(255, 0, 0, 120),
            
            blackStone = Color(20, 20, 20),
            whiteStone = Color(240, 240, 240),
            blackStoneOutline = Color(40, 40, 40),
            whiteStoneOutline = Color(200, 200, 200),
            stoneNumber = Color.RED,
            
            primary = Color(33, 150, 243),
            primaryVariant = Color(25, 118, 210),
            secondary = Color(255, 152, 0),
            secondaryVariant = Color(245, 124, 0),
            error = Color(244, 67, 54),
            warning = Color(255, 193, 7),
            success = Color(76, 175, 80),
            info = Color(33, 150, 243),
            
            textPrimary = Color(33, 33, 33),
            textSecondary = Color(117, 117, 117),
            textDisabled = Color(189, 189, 189),
            textOnPrimary = Color.WHITE,
            textOnSecondary = Color.WHITE
        )
        
        val MODERN = ColorScheme(
            background = Color(250, 250, 250),
            surface = Color.WHITE,
            surfaceVariant = Color(245, 245, 245),
            
            boardBackground = Color(240, 240, 240),
            boardLines = Color(200, 200, 200),
            boardStarPoints = Color(150, 150, 150),
            boardCoordinates = Color(180, 180, 180),
            boardHighlight = Color(100, 200, 255, 100),
            boardLastMove = Color(255, 100, 100, 100),
            boardHover = Color(100, 255, 100, 80),
            boardForbidden = Color(255, 50, 50, 150),
            
            blackStone = Color(30, 30, 30),
            whiteStone = Color(250, 250, 250),
            blackStoneOutline = Color(0, 0, 0),
            whiteStoneOutline = Color(220, 220, 220),
            stoneNumber = Color(255, 87, 34),
            
            primary = Color(63, 81, 181),
            primaryVariant = Color(48, 63, 159),
            secondary = Color(255, 64, 129),
            secondaryVariant = Color(245, 0, 87),
            error = Color(244, 67, 54),
            warning = Color(255, 152, 0),
            success = Color(76, 175, 80),
            info = Color(3, 169, 244),
            
            textPrimary = Color(33, 33, 33),
            textSecondary = Color(97, 97, 97),
            textDisabled = Color(158, 158, 158),
            textOnPrimary = Color.WHITE,
            textOnSecondary = Color.WHITE
        )
        
        val DARK = ColorScheme(
            background = Color(18, 18, 18),
            surface = Color(33, 33, 33),
            surfaceVariant = Color(48, 48, 48),
            
            boardBackground = Color(48, 48, 48),
            boardLines = Color(100, 100, 100),
            boardStarPoints = Color(150, 150, 150),
            boardCoordinates = Color(120, 120, 120),
            boardHighlight = Color(255, 255, 100, 100),
            boardLastMove = Color(255, 100, 100, 120),
            boardHover = Color(100, 255, 100, 80),
            boardForbidden = Color(255, 50, 50, 180),
            
            blackStone = Color(10, 10, 10),
            whiteStone = Color(230, 230, 230),
            blackStoneOutline = Color(80, 80, 80),
            whiteStoneOutline = Color(180, 180, 180),
            stoneNumber = Color(255, 152, 0),
            
            primary = Color(187, 134, 252),
            primaryVariant = Color(149, 117, 205),
            secondary = Color(3, 218, 198),
            secondaryVariant = Color(0, 188, 170),
            error = Color(207, 102, 121),
            warning = Color(255, 193, 7),
            success = Color(129, 199, 132),
            info = Color(79, 195, 247),
            
            textPrimary = Color(255, 255, 255),
            textSecondary = Color(189, 189, 189),
            textDisabled = Color(97, 97, 97),
            textOnPrimary = Color.BLACK,
            textOnSecondary = Color.BLACK
        )
        
        val NEON = ColorScheme(
            background = Color(10, 10, 30),
            surface = Color(20, 20, 40),
            surfaceVariant = Color(30, 30, 50),
            
            boardBackground = Color(15, 15, 35),
            boardLines = Color(0, 255, 255, 150),
            boardStarPoints = Color(255, 0, 255),
            boardCoordinates = Color(0, 200, 200),
            boardHighlight = Color(255, 255, 0, 200),
            boardLastMove = Color(255, 0, 255, 200),
            boardHover = Color(0, 255, 0, 150),
            boardForbidden = Color(255, 0, 0, 255),
            
            blackStone = Color(0, 0, 0),
            whiteStone = Color(255, 255, 255),
            blackStoneOutline = Color(0, 255, 255),
            whiteStoneOutline = Color(255, 0, 255),
            stoneNumber = Color(255, 255, 0),
            
            primary = Color(0, 255, 255),
            primaryVariant = Color(0, 200, 200),
            secondary = Color(255, 0, 255),
            secondaryVariant = Color(200, 0, 200),
            error = Color(255, 0, 100),
            warning = Color(255, 255, 0),
            success = Color(0, 255, 100),
            info = Color(100, 100, 255),
            
            textPrimary = Color(255, 255, 255),
            textSecondary = Color(200, 200, 255),
            textDisabled = Color(100, 100, 150),
            textOnPrimary = Color.BLACK,
            textOnSecondary = Color.BLACK
        )
        
        val NATURE = ColorScheme(
            background = Color(245, 242, 235),
            surface = Color(250, 248, 240),
            surfaceVariant = Color(240, 235, 225),
            
            boardBackground = Color(210, 180, 140),
            boardLines = Color(101, 67, 33),
            boardStarPoints = Color(139, 90, 43),
            boardCoordinates = Color(139, 90, 43),
            boardHighlight = Color(255, 223, 0, 100),
            boardLastMove = Color(205, 92, 92, 120),
            boardHover = Color(154, 205, 50, 80),
            boardForbidden = Color(220, 20, 60, 150),
            
            blackStone = Color(47, 47, 47),
            whiteStone = Color(255, 248, 220),
            blackStoneOutline = Color(30, 30, 30),
            whiteStoneOutline = Color(222, 184, 135),
            stoneNumber = Color(178, 34, 34),
            
            primary = Color(76, 175, 80),
            primaryVariant = Color(56, 142, 60),
            secondary = Color(139, 195, 74),
            secondaryVariant = Color(104, 159, 56),
            error = Color(183, 28, 28),
            warning = Color(245, 127, 23),
            success = Color(46, 125, 50),
            info = Color(13, 71, 161),
            
            textPrimary = Color(62, 39, 35),
            textSecondary = Color(121, 85, 72),
            textDisabled = Color(161, 136, 127),
            textOnPrimary = Color.WHITE,
            textOnSecondary = Color.WHITE
        )
        
        val HIGH_CONTRAST = ColorScheme(
            background = Color.WHITE,
            surface = Color.WHITE,
            surfaceVariant = Color(240, 240, 240),
            
            boardBackground = Color.WHITE,
            boardLines = Color.BLACK,
            boardStarPoints = Color.BLACK,
            boardCoordinates = Color.BLACK,
            boardHighlight = Color(255, 255, 0),
            boardLastMove = Color(255, 0, 0),
            boardHover = Color(0, 255, 0),
            boardForbidden = Color(255, 0, 0),
            
            blackStone = Color.BLACK,
            whiteStone = Color.WHITE,
            blackStoneOutline = Color.WHITE,
            whiteStoneOutline = Color.BLACK,
            stoneNumber = Color.RED,
            
            primary = Color.BLACK,
            primaryVariant = Color(50, 50, 50),
            secondary = Color(0, 0, 255),
            secondaryVariant = Color(0, 0, 200),
            error = Color(255, 0, 0),
            warning = Color(255, 140, 0),
            success = Color(0, 128, 0),
            info = Color(0, 0, 255),
            
            textPrimary = Color.BLACK,
            textSecondary = Color(64, 64, 64),
            textDisabled = Color(128, 128, 128),
            textOnPrimary = Color.WHITE,
            textOnSecondary = Color.WHITE
        )
    }
}

/**
 * 보드 스타일
 */
enum class BoardStyle(val displayName: String) {
    WOOD_GRAIN("나무결"),
    MINIMAL("미니멀"),
    DARK_WOOD("검은 나무"),
    GRID("그리드"),
    BAMBOO("대나무"),
    MARBLE("대리석"),
    PAPER("종이"),
    HIGH_CONTRAST("고대비")
}

/**
 * 돌 스타일
 */
enum class StoneStyle(val displayName: String) {
    TRADITIONAL("전통"),
    FLAT("플랫"),
    GLOWING("발광"),
    NEON("네온"),
    NATURAL("자연석"),
    GLASS("유리"),
    HIGH_CONTRAST("고대비")
}

/**
 * 폰트 구성
 */
data class FontScheme(
    val title: Font,
    val heading: Font,
    val body: Font,
    val button: Font,
    val small: Font,
    val code: Font
) {
    companion object {
        val DEFAULT = FontScheme(
            title = Font("맑은 고딕", Font.BOLD, 24),
            heading = Font("맑은 고딕", Font.BOLD, 18),
            body = Font("맑은 고딕", Font.PLAIN, 14),
            button = Font("맑은 고딕", Font.BOLD, 14),
            small = Font("맑은 고딕", Font.PLAIN, 12),
            code = Font("Consolas", Font.PLAIN, 13)
        )
        
        val MODERN = FontScheme(
            title = Font("Segoe UI", Font.BOLD, 24),
            heading = Font("Segoe UI", Font.BOLD, 18),
            body = Font("Segoe UI", Font.PLAIN, 14),
            button = Font("Segoe UI", Font.BOLD, 14),
            small = Font("Segoe UI", Font.PLAIN, 12),
            code = Font("Cascadia Code", Font.PLAIN, 13)
        )
        
        val FUTURISTIC = FontScheme(
            title = Font("Arial", Font.BOLD, 26),
            heading = Font("Arial", Font.BOLD, 20),
            body = Font("Arial", Font.PLAIN, 14),
            button = Font("Arial", Font.BOLD, 14),
            small = Font("Arial", Font.PLAIN, 12),
            code = Font("Courier New", Font.PLAIN, 13)
        )
        
        val ORGANIC = FontScheme(
            title = Font("Georgia", Font.BOLD, 24),
            heading = Font("Georgia", Font.BOLD, 18),
            body = Font("Georgia", Font.PLAIN, 14),
            button = Font("Georgia", Font.BOLD, 14),
            small = Font("Georgia", Font.PLAIN, 12),
            code = Font("Monaco", Font.PLAIN, 13)
        )
        
        val BOLD = FontScheme(
            title = Font("Arial Black", Font.BOLD, 26),
            heading = Font("Arial Black", Font.BOLD, 20),
            body = Font("Arial", Font.BOLD, 16),
            button = Font("Arial", Font.BOLD, 16),
            small = Font("Arial", Font.BOLD, 14),
            code = Font("Consolas", Font.BOLD, 14)
        )
    }
}

/**
 * 사운드 구성
 */
data class SoundScheme(
    val enabled: Boolean = true,
    val volume: Float = 0.8f,
    val stonePlaceSound: String? = null,
    val gameStartSound: String? = null,
    val gameEndSound: String? = null,
    val moveSound: String? = null,
    val errorSound: String? = null
)