package com.omok.presentation.ui.theme

import com.omok.domain.model.Player
import com.omok.domain.model.Position
import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.*

/**
 * 테마 기반 보드 렌더러
 */
class BoardRenderer(
    private val cellSize: Int = 40,
    private val padding: Int = 30
) {
    private var theme: GameTheme = ThemeManager.getCurrentTheme()
    private val boardCache = mutableMapOf<String, BufferedImage>()
    
    init {
        ThemeManager.addThemeChangeListener(object : ThemeChangeListener {
            override fun onThemeChanged(oldTheme: GameTheme, newTheme: GameTheme) {
                theme = newTheme
                boardCache.clear()
            }
        })
    }
    
    /**
     * 보드 배경 그리기
     */
    fun drawBoard(g: Graphics2D, width: Int, height: Int) {
        val cacheKey = "${theme.id}_${width}_${height}"
        val cachedBoard = boardCache[cacheKey]
        
        if (cachedBoard != null) {
            g.drawImage(cachedBoard, 0, 0, null)
        } else {
            val boardImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val bg = boardImage.createGraphics()
            bg.setRenderingHints(getRenderingHints())
            
            // 배경 그리기
            drawBoardBackground(bg, width, height)
            
            // 보드 선 그리기
            drawBoardLines(bg)
            
            // 화점 그리기
            drawStarPoints(bg)
            
            // 좌표 그리기
            drawCoordinates(bg)
            
            bg.dispose()
            boardCache[cacheKey] = boardImage
            g.drawImage(boardImage, 0, 0, null)
        }
    }
    
    /**
     * 보드 배경 그리기
     */
    private fun drawBoardBackground(g: Graphics2D, width: Int, height: Int) {
        when (theme.boardStyle) {
            BoardStyle.WOOD_GRAIN -> drawWoodGrainBackground(g, width, height)
            BoardStyle.MINIMAL -> drawMinimalBackground(g, width, height)
            BoardStyle.DARK_WOOD -> drawDarkWoodBackground(g, width, height)
            BoardStyle.GRID -> drawGridBackground(g, width, height)
            BoardStyle.BAMBOO -> drawBambooBackground(g, width, height)
            BoardStyle.MARBLE -> drawMarbleBackground(g, width, height)
            BoardStyle.PAPER -> drawPaperBackground(g, width, height)
            BoardStyle.HIGH_CONTRAST -> drawHighContrastBackground(g, width, height)
        }
    }
    
    /**
     * 나무결 배경
     */
    private fun drawWoodGrainBackground(g: Graphics2D, width: Int, height: Int) {
        g.color = theme.colorScheme.boardBackground
        g.fillRect(0, 0, width, height)
        
        // 나무결 효과
        val grainColor = Color(
            theme.colorScheme.boardBackground.red - 20,
            theme.colorScheme.boardBackground.green - 20,
            theme.colorScheme.boardBackground.blue - 20
        )
        g.color = grainColor
        g.stroke = BasicStroke(0.5f)
        
        for (i in 0 until height step 3) {
            val path = Path2D.Float()
            path.moveTo(0f, i.toFloat())
            
            var x = 0f
            while (x < width) {
                val y = i + sin(x * 0.01) * 2
                path.lineTo(x, y.toFloat())
                x += 5
            }
            g.draw(path)
        }
    }
    
    /**
     * 미니멀 배경
     */
    private fun drawMinimalBackground(g: Graphics2D, width: Int, height: Int) {
        g.color = theme.colorScheme.boardBackground
        g.fillRect(0, 0, width, height)
    }
    
    /**
     * 검은 나무 배경
     */
    private fun drawDarkWoodBackground(g: Graphics2D, width: Int, height: Int) {
        // 그라디언트 배경
        val gradient = GradientPaint(
            0f, 0f, theme.colorScheme.boardBackground,
            width.toFloat(), height.toFloat(), 
            Color(
                theme.colorScheme.boardBackground.red - 30,
                theme.colorScheme.boardBackground.green - 30,
                theme.colorScheme.boardBackground.blue - 30
            )
        )
        g.paint = gradient
        g.fillRect(0, 0, width, height)
    }
    
    /**
     * 그리드 배경
     */
    private fun drawGridBackground(g: Graphics2D, width: Int, height: Int) {
        g.color = theme.colorScheme.boardBackground
        g.fillRect(0, 0, width, height)
        
        // 점선 그리드
        g.color = Color(theme.colorScheme.boardLines.red, theme.colorScheme.boardLines.green, 
                       theme.colorScheme.boardLines.blue, 30)
        g.stroke = BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                              10.0f, floatArrayOf(2.0f, 4.0f), 0.0f)
        
        for (i in 0 until width step 10) {
            g.drawLine(i, 0, i, height)
        }
        for (i in 0 until height step 10) {
            g.drawLine(0, i, width, i)
        }
    }
    
    /**
     * 대나무 배경
     */
    private fun drawBambooBackground(g: Graphics2D, width: Int, height: Int) {
        g.color = theme.colorScheme.boardBackground
        g.fillRect(0, 0, width, height)
        
        // 대나무 무늬
        val bambooColor = Color(
            theme.colorScheme.boardBackground.red - 15,
            theme.colorScheme.boardBackground.green - 10,
            theme.colorScheme.boardBackground.blue - 5
        )
        g.color = bambooColor
        
        for (x in 0 until width step 30) {
            g.fillRect(x, 0, 2, height)
            
            // 마디
            for (y in 0 until height step 60) {
                g.fillOval(x - 2, y, 6, 4)
            }
        }
    }
    
    /**
     * 대리석 배경
     */
    private fun drawMarbleBackground(g: Graphics2D, width: Int, height: Int) {
        // 대리석 패턴 (노이즈 효과)
        val base = theme.colorScheme.boardBackground
        for (x in 0 until width step 2) {
            for (y in 0 until height step 2) {
                val noise = (Math.random() * 20 - 10).toInt()
                g.color = Color(
                    (base.red + noise).coerceIn(0, 255),
                    (base.green + noise).coerceIn(0, 255),
                    (base.blue + noise).coerceIn(0, 255)
                )
                g.fillRect(x, y, 2, 2)
            }
        }
    }
    
    /**
     * 종이 배경
     */
    private fun drawPaperBackground(g: Graphics2D, width: Int, height: Int) {
        g.color = theme.colorScheme.boardBackground
        g.fillRect(0, 0, width, height)
        
        // 종이 질감
        g.color = Color(0, 0, 0, 5)
        for (i in 0..50) {
            val x1 = (Math.random() * width).toInt()
            val y1 = (Math.random() * height).toInt()
            val x2 = x1 + (Math.random() * 50 - 25).toInt()
            val y2 = y1 + (Math.random() * 50 - 25).toInt()
            g.drawLine(x1, y1, x2, y2)
        }
    }
    
    /**
     * 고대비 배경
     */
    private fun drawHighContrastBackground(g: Graphics2D, width: Int, height: Int) {
        g.color = theme.colorScheme.boardBackground
        g.fillRect(0, 0, width, height)
    }
    
    /**
     * 보드 선 그리기
     */
    private fun drawBoardLines(g: Graphics2D) {
        g.color = theme.colorScheme.boardLines
        g.stroke = BasicStroke(1f)
        
        // 세로선
        for (i in 0 until 15) {
            val x = padding + i * cellSize
            g.drawLine(x, padding, x, padding + 14 * cellSize)
        }
        
        // 가로선
        for (i in 0 until 15) {
            val y = padding + i * cellSize
            g.drawLine(padding, y, padding + 14 * cellSize, y)
        }
    }
    
    /**
     * 화점 그리기
     */
    private fun drawStarPoints(g: Graphics2D) {
        g.color = theme.colorScheme.boardStarPoints
        val starPoints = listOf(
            Position(3, 3), Position(3, 11), Position(11, 3), Position(11, 11),
            Position(7, 7), Position(3, 7), Position(7, 3), Position(11, 7), Position(7, 11)
        )
        
        starPoints.forEach { pos ->
            val x = padding + pos.col * cellSize
            val y = padding + pos.row * cellSize
            g.fillOval(x - 3, y - 3, 6, 6)
        }
    }
    
    /**
     * 좌표 그리기
     */
    private fun drawCoordinates(g: Graphics2D) {
        g.color = theme.colorScheme.boardCoordinates
        g.font = Font("Arial", Font.PLAIN, 12)
        
        // 열 좌표 (A-O)
        for (i in 0 until 15) {
            val x = padding + i * cellSize
            val letter = ('A' + i).toString()
            val fm = g.fontMetrics
            g.drawString(letter, x - fm.stringWidth(letter) / 2, padding - 10)
            g.drawString(letter, x - fm.stringWidth(letter) / 2, padding + 14 * cellSize + 20)
        }
        
        // 행 좌표 (1-15)
        for (i in 0 until 15) {
            val y = padding + i * cellSize
            val number = (15 - i).toString()
            val fm = g.fontMetrics
            g.drawString(number, padding - 20 - fm.stringWidth(number), y + fm.height / 3)
            g.drawString(number, padding + 14 * cellSize + 10, y + fm.height / 3)
        }
    }
    
    /**
     * 돌 그리기
     */
    fun drawStone(g: Graphics2D, position: Position, player: Player, isLastMove: Boolean, 
                  showNumber: Boolean = false, number: Int = 0) {
        val x = padding + position.col * cellSize
        val y = padding + position.row * cellSize
        
        when (theme.stoneStyle) {
            StoneStyle.TRADITIONAL -> drawTraditionalStone(g, x, y, player, isLastMove)
            StoneStyle.FLAT -> drawFlatStone(g, x, y, player, isLastMove)
            StoneStyle.GLOWING -> drawGlowingStone(g, x, y, player, isLastMove)
            StoneStyle.NEON -> drawNeonStone(g, x, y, player, isLastMove)
            StoneStyle.NATURAL -> drawNaturalStone(g, x, y, player, isLastMove)
            StoneStyle.GLASS -> drawGlassStone(g, x, y, player, isLastMove)
            StoneStyle.HIGH_CONTRAST -> drawHighContrastStone(g, x, y, player, isLastMove)
        }
        
        // 수순 번호 표시
        if (showNumber && number > 0) {
            g.font = Font("Arial", Font.BOLD, 12)
            g.color = theme.colorScheme.stoneNumber
            val str = number.toString()
            val fm = g.fontMetrics
            g.drawString(str, x - fm.stringWidth(str) / 2, y + fm.height / 3)
        }
    }
    
    /**
     * 전통적인 돌
     */
    private fun drawTraditionalStone(g: Graphics2D, x: Int, y: Int, player: Player, isLastMove: Boolean) {
        val radius = cellSize / 2 - 2
        
        // 그림자
        g.color = Color(0, 0, 0, 80)
        g.fillOval(x - radius + 2, y - radius + 2, radius * 2, radius * 2)
        
        // 돌 본체
        val stoneColor = if (player == Player.BLACK) theme.colorScheme.blackStone else theme.colorScheme.whiteStone
        val gradient = RadialGradientPaint(
            Point2D.Float((x - radius / 3).toFloat(), (y - radius / 3).toFloat()),
            radius.toFloat(),
            floatArrayOf(0f, 1f),
            arrayOf(
                Color(min(255, stoneColor.red + 30), min(255, stoneColor.green + 30), min(255, stoneColor.blue + 30)),
                stoneColor
            )
        )
        g.paint = gradient
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2)
        
        // 외곽선
        g.color = if (player == Player.BLACK) theme.colorScheme.blackStoneOutline else theme.colorScheme.whiteStoneOutline
        g.stroke = BasicStroke(1f)
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2)
        
        // 마지막 수 표시
        if (isLastMove) {
            g.color = theme.colorScheme.boardLastMove
            g.stroke = BasicStroke(2f)
            g.drawOval(x - radius - 2, y - radius - 2, radius * 2 + 4, radius * 2 + 4)
        }
    }
    
    /**
     * 플랫 돌
     */
    private fun drawFlatStone(g: Graphics2D, x: Int, y: Int, player: Player, isLastMove: Boolean) {
        val radius = cellSize / 2 - 2
        
        // 돌 본체
        g.color = if (player == Player.BLACK) theme.colorScheme.blackStone else theme.colorScheme.whiteStone
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2)
        
        // 마지막 수 표시
        if (isLastMove) {
            g.color = theme.colorScheme.boardLastMove
            g.stroke = BasicStroke(3f)
            g.drawOval(x - radius - 2, y - radius - 2, radius * 2 + 4, radius * 2 + 4)
        }
    }
    
    /**
     * 발광 돌
     */
    private fun drawGlowingStone(g: Graphics2D, x: Int, y: Int, player: Player, isLastMove: Boolean) {
        val radius = cellSize / 2 - 2
        
        // 발광 효과
        val glowColor = if (player == Player.BLACK) 
            Color(100, 100, 100, 100) else Color(255, 255, 200, 100)
        for (i in 3 downTo 1) {
            g.color = Color(glowColor.red, glowColor.green, glowColor.blue, 30 * i)
            g.fillOval(x - radius - i * 2, y - radius - i * 2, 
                      radius * 2 + i * 4, radius * 2 + i * 4)
        }
        
        // 돌 본체
        drawTraditionalStone(g, x, y, player, isLastMove)
    }
    
    /**
     * 네온 돌
     */
    private fun drawNeonStone(g: Graphics2D, x: Int, y: Int, player: Player, isLastMove: Boolean) {
        val radius = cellSize / 2 - 2
        
        // 네온 발광
        val neonColor = if (player == Player.BLACK) theme.colorScheme.primary else theme.colorScheme.secondary
        g.color = Color(neonColor.red, neonColor.green, neonColor.blue, 150)
        g.stroke = BasicStroke(3f)
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2)
        
        // 돌 본체
        g.color = if (player == Player.BLACK) Color.BLACK else Color.WHITE
        g.fillOval(x - radius + 3, y - radius + 3, radius * 2 - 6, radius * 2 - 6)
        
        // 마지막 수 표시
        if (isLastMove) {
            g.color = theme.colorScheme.boardLastMove
            g.stroke = BasicStroke(4f)
            g.drawOval(x - radius - 3, y - radius - 3, radius * 2 + 6, radius * 2 + 6)
        }
    }
    
    /**
     * 자연석
     */
    private fun drawNaturalStone(g: Graphics2D, x: Int, y: Int, player: Player, isLastMove: Boolean) {
        val radius = cellSize / 2 - 2
        
        // 불규칙한 모양
        val path = Path2D.Float()
        for (i in 0..7) {
            val angle = i * Math.PI / 4
            val r = radius + (Math.random() * 4 - 2)
            val px = x + r * cos(angle)
            val py = y + r * sin(angle)
            
            if (i == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.closePath()
        
        // 그림자
        g.color = Color(0, 0, 0, 60)
        g.translate(2, 2)
        g.fill(path)
        g.translate(-2, -2)
        
        // 돌 본체
        g.color = if (player == Player.BLACK) theme.colorScheme.blackStone else theme.colorScheme.whiteStone
        g.fill(path)
        
        // 외곽선
        g.color = if (player == Player.BLACK) theme.colorScheme.blackStoneOutline else theme.colorScheme.whiteStoneOutline
        g.stroke = BasicStroke(1f)
        g.draw(path)
        
        // 마지막 수 표시
        if (isLastMove) {
            g.color = theme.colorScheme.boardLastMove
            g.stroke = BasicStroke(2f)
            g.drawOval(x - radius - 2, y - radius - 2, radius * 2 + 4, radius * 2 + 4)
        }
    }
    
    /**
     * 유리 돌
     */
    private fun drawGlassStone(g: Graphics2D, x: Int, y: Int, player: Player, isLastMove: Boolean) {
        val radius = cellSize / 2 - 2
        
        // 투명도가 있는 돌
        val baseColor = if (player == Player.BLACK) theme.colorScheme.blackStone else theme.colorScheme.whiteStone
        g.color = Color(baseColor.red, baseColor.green, baseColor.blue, 200)
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2)
        
        // 하이라이트
        g.color = Color(255, 255, 255, 100)
        g.fillOval(x - radius / 2, y - radius + 2, radius, radius / 2)
        
        // 외곽선
        g.color = if (player == Player.BLACK) theme.colorScheme.blackStoneOutline else theme.colorScheme.whiteStoneOutline
        g.stroke = BasicStroke(2f)
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2)
        
        // 마지막 수 표시
        if (isLastMove) {
            g.color = theme.colorScheme.boardLastMove
            g.stroke = BasicStroke(3f)
            g.drawOval(x - radius - 2, y - radius - 2, radius * 2 + 4, radius * 2 + 4)
        }
    }
    
    /**
     * 고대비 돌
     */
    private fun drawHighContrastStone(g: Graphics2D, x: Int, y: Int, player: Player, isLastMove: Boolean) {
        val radius = cellSize / 2 - 2
        
        // 돌 본체
        g.color = if (player == Player.BLACK) Color.BLACK else Color.WHITE
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2)
        
        // 외곽선 (반대색)
        g.color = if (player == Player.BLACK) Color.WHITE else Color.BLACK
        g.stroke = BasicStroke(2f)
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2)
        
        // 마지막 수 표시
        if (isLastMove) {
            g.color = Color.RED
            g.stroke = BasicStroke(4f)
            g.drawOval(x - radius - 3, y - radius - 3, radius * 2 + 6, radius * 2 + 6)
        }
    }
    
    /**
     * 하이라이트 그리기
     */
    fun drawHighlight(g: Graphics2D, position: Position) {
        val x = padding + position.col * cellSize
        val y = padding + position.row * cellSize
        val radius = cellSize / 2 - 2
        
        g.color = theme.colorScheme.boardHighlight
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2)
    }
    
    /**
     * 호버 효과 그리기
     */
    fun drawHover(g: Graphics2D, position: Position) {
        val x = padding + position.col * cellSize
        val y = padding + position.row * cellSize
        val radius = cellSize / 2 - 2
        
        g.color = theme.colorScheme.boardHover
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2)
    }
    
    /**
     * 금수 위치 표시
     */
    fun drawForbidden(g: Graphics2D, position: Position) {
        val x = padding + position.col * cellSize
        val y = padding + position.row * cellSize
        
        g.color = theme.colorScheme.boardForbidden
        g.stroke = BasicStroke(2f)
        
        // X 표시
        val size = cellSize / 3
        g.drawLine(x - size, y - size, x + size, y + size)
        g.drawLine(x - size, y + size, x + size, y - size)
    }
    
    /**
     * 렌더링 힌트 설정
     */
    private fun getRenderingHints(): RenderingHints {
        return RenderingHints(mapOf(
            RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON,
            RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY,
            RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC,
            RenderingHints.KEY_ALPHA_INTERPOLATION to RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY,
            RenderingHints.KEY_COLOR_RENDERING to RenderingHints.VALUE_COLOR_RENDER_QUALITY,
            RenderingHints.KEY_STROKE_CONTROL to RenderingHints.VALUE_STROKE_PURE
        ))
    }
    
    /**
     * 마우스 위치를 보드 좌표로 변환
     */
    fun getBoardPosition(mouseX: Int, mouseY: Int): Position? {
        val col = ((mouseX - padding + cellSize / 2) / cellSize)
        val row = ((mouseY - padding + cellSize / 2) / cellSize)
        
        return if (col in 0..14 && row in 0..14) {
            Position(row, col)
        } else {
            null
        }
    }
}