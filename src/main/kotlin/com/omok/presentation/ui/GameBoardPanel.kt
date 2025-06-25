package com.omok.presentation.ui

import com.omok.application.dto.*
import com.omok.presentation.controller.GameController
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.effects.SoundEffects
import com.omok.presentation.ui.components.ModernTooltip
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import javax.swing.JPanel
import javax.swing.Timer

class GameBoardPanel : JPanel() {
    private data class AnimatedStone(
        val position: PositionDto,
        val player: PlayerDto,
        var scale: Float = 0.1f,  // Start with small scale instead of 0
        var opacity: Float = 0f
    )
    
    companion object {
        const val BOARD_SIZE = 15
        const val CELL_SIZE = 35  // 적절한 크기
        const val MARGIN = 45  // 충분한 여백 확보 (좌표 라벨 공간 포함)
        const val BOARD_PIXEL_SIZE = (BOARD_SIZE - 1) * CELL_SIZE  // 14 * 35 = 490px
        const val STONE_SIZE = 30  // 셀 크기의 85%
        const val MARK_SIZE = 10
        const val STAR_POINTS_SIZE = 7
        const val TOTAL_BOARD_SIZE = BOARD_PIXEL_SIZE + 2 * MARGIN  // 490 + 90 = 580px
        
        val BOARD_COLOR = UITheme.Colors.BOARD_BACKGROUND
        val LINE_COLOR = UITheme.Colors.BOARD_LINE
        val BLACK_STONE_COLOR = UITheme.Colors.BLACK_STONE
        val WHITE_STONE_COLOR = UITheme.Colors.WHITE_STONE
        val LAST_MOVE_COLOR = UITheme.Colors.LAST_MOVE_INDICATOR
        val HOVER_COLOR = UITheme.Colors.HOVER_OVERLAY
        val FORBIDDEN_COLOR = UITheme.Colors.FORBIDDEN_OVERLAY
        val WIN_LINE_COLOR = UITheme.Colors.WIN_LINE
    }
    
    private lateinit var controller: GameController
    private var hoverPosition: PositionDto? = null
    private var lastMovePosition: PositionDto? = null
    private var board: BoardDto = BoardDto(Array(15) { Array(15) { null } })
    private var forbiddenMoves = setOf<PositionDto>()
    private var winningLine = listOf<PositionDto>()
    private val animatedStones = mutableMapOf<PositionDto, AnimatedStone>()
    private var animationTimer: Timer? = null
    private var winAnimationProgress = 0f
    private var winAnimationTimer: Timer? = null
    private var keyboardFocusPosition: PositionDto? = null
    
    init {
        // 전체 보드 크기 설정 (660x660)
        preferredSize = Dimension(TOTAL_BOARD_SIZE, TOTAL_BOARD_SIZE)
        minimumSize = preferredSize
        maximumSize = preferredSize
        background = BOARD_COLOR
        isOpaque = true
        
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                handleClick(e.x, e.y)
            }
            
            override fun mouseExited(e: MouseEvent) {
                hoverPosition = null
                updateForbiddenMoves()
                ModernTooltip.hide()
                repaint()
            }
        })
        
        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val col = (e.x - MARGIN + CELL_SIZE / 2) / CELL_SIZE
                val row = (e.y - MARGIN + CELL_SIZE / 2) / CELL_SIZE
                
                val newPosition = if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
                    PositionDto(row, col)
                } else {
                    null
                }
                
                if (newPosition != hoverPosition) {
                    hoverPosition = newPosition
                    updateForbiddenMoves()
                    
                    // Show tooltip for forbidden moves
                    if (newPosition != null && forbiddenMoves.contains(newPosition)) {
                        val forbiddenType = getForbiddenType(newPosition)
                        ModernTooltip.show(this@GameBoardPanel, forbiddenType, ModernTooltip.TooltipPosition.TOP)
                    } else {
                        ModernTooltip.hide()
                    }
                    
                    repaint()
                }
            }
        })
    }
    
    fun setController(controller: GameController) {
        this.controller = controller
    }
    
    private fun updateForbiddenMoves() {
        forbiddenMoves = if (::controller.isInitialized) {
            controller.getForbiddenMoves()
        } else {
            emptySet()
        }
    }
    
    private fun handleClick(x: Int, y: Int) {
        if (!::controller.isInitialized) return
        
        val col = (x - MARGIN + CELL_SIZE / 2) / CELL_SIZE
        val row = (y - MARGIN + CELL_SIZE / 2) / CELL_SIZE
        
        if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
            val position = PositionDto(row, col)
            
            // Check if move is forbidden
            if (forbiddenMoves.contains(position)) {
                SoundEffects.playError()
                showForbiddenAnimation(position)
                return
            }
            
            if (controller.makeMove(position)) {
                SoundEffects.playStonePlace()
                updateForbiddenMoves()
                repaint()
            }
        }
    }
    
    private fun showForbiddenAnimation(position: PositionDto) {
        // Add shake animation or red flash
        Timer(50) { e ->
            repaint()
            (e.source as Timer).stop()
        }.start()
    }
    
    fun updateBoard(newBoard: BoardDto) {
        // Find new stones
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val position = PositionDto(row, col)
                val oldStone = board.getStone(position)
                val newStone = newBoard.getStone(position)
                
                if (oldStone == null && newStone != null) {
                    // New stone placed, animate it
                    animatedStones[position] = AnimatedStone(position, newStone, 0.1f, 0f)
                    startStoneAnimation(position)
                }
            }
        }
        
        board = newBoard
        updateForbiddenMoves()
        repaint()
    }
    
    private fun startStoneAnimation(position: PositionDto) {
        animationTimer?.stop()
        
        animationTimer = Timer(16) { e ->
            var allComplete = true
            
            animatedStones.forEach { (pos, stone) ->
                if (stone.scale < 1f) {
                    stone.scale = minOf(1f, stone.scale + 0.15f)
                    stone.opacity = minOf(1f, stone.opacity + 0.1f)
                    allComplete = false
                }
            }
            
            repaint()
            
            if (allComplete) {
                (e.source as Timer).stop()
                animatedStones.clear()
            }
        }
        animationTimer?.start()
    }
    
    fun setLastMove(position: PositionDto) {
        lastMovePosition = position
        repaint()
    }
    
    fun clearLastMove() {
        lastMovePosition = null
        repaint()
    }
    
    fun clearWinAnimation() {
        winAnimationTimer?.stop()
        winAnimationTimer = null
        winningLine = emptyList()
        winAnimationProgress = 0f
        repaint()
    }
    
    fun showWinAnimation(winningPositions: List<PositionDto>) {
        winningLine = winningPositions
        winAnimationProgress = 0f
        SoundEffects.playWin()
        
        winAnimationTimer?.stop()
        winAnimationTimer = Timer(16) { e ->
            winAnimationProgress += 0.02f
            if (winAnimationProgress >= 1f) {
                winAnimationProgress = 1f
            }
            repaint()
        }
        winAnimationTimer?.start()
    }
    
    private fun getForbiddenType(position: PositionDto): String {
        // This would need actual logic from the game engine
        return "금수: 삼삼"  // Example
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        // 배경 명시적으로 그리기
        g2d.color = BOARD_COLOR
        g2d.fillRect(0, 0, width, height)
        
        drawBoard(g2d)
        drawStarPoints(g2d)
        drawForbiddenPoints(g2d)
        drawStones(g2d)
        drawWinningLine(g2d)
        drawLastMoveMarker(g2d)
        drawHover(g2d)
        drawKeyboardFocus(g2d)
    }
    
    private fun drawBoard(g2d: Graphics2D) {
        // 모든 격자선 그리기
        g2d.color = LINE_COLOR
        g2d.stroke = BasicStroke(1f)
        
        // 가로선
        for (i in 0 until BOARD_SIZE) {
            val y = MARGIN + i * CELL_SIZE
            g2d.drawLine(MARGIN, y, MARGIN + BOARD_PIXEL_SIZE, y)
        }
        
        // 세로선
        for (i in 0 until BOARD_SIZE) {
            val x = MARGIN + i * CELL_SIZE
            g2d.drawLine(x, MARGIN, x, MARGIN + BOARD_PIXEL_SIZE)
        }
        
        // 테두리 강조
        g2d.stroke = BasicStroke(2f)
        g2d.drawRect(MARGIN - 1, MARGIN - 1, BOARD_PIXEL_SIZE + 2, BOARD_PIXEL_SIZE + 2)
        
        // 좌표 라벨 그리기
        g2d.font = Font("Arial", Font.BOLD, 14)
        g2d.color = UITheme.Colors.GRAY_700
        
        for (i in 0 until BOARD_SIZE) {
            val pos = MARGIN + i * CELL_SIZE
            val label = ('A' + i).toString()
            val number = (BOARD_SIZE - i).toString()
            
            val fm = g2d.fontMetrics
            val labelWidth = fm.stringWidth(label)
            val numberWidth = fm.stringWidth(number)
            
            // 상하 라벨
            g2d.drawString(label, pos - labelWidth / 2, MARGIN - 20)
            g2d.drawString(label, pos - labelWidth / 2, MARGIN + BOARD_PIXEL_SIZE + 30)
            
            // 좌우 숫자
            g2d.drawString(number, MARGIN - numberWidth - 15, pos + fm.height / 4)
            g2d.drawString(number, MARGIN + BOARD_PIXEL_SIZE + 15, pos + fm.height / 4)
        }
    }
    
    private fun drawStarPoints(g2d: Graphics2D) {
        g2d.color = LINE_COLOR
        // 오목판의 표준 화점 위치
        val starPoints = listOf(
            PositionDto(3, 3), PositionDto(3, 11), PositionDto(11, 3), PositionDto(11, 11),
            PositionDto(7, 7)  // 중앙 화점
        )
        
        for (point in starPoints) {
            val x = MARGIN + point.col * CELL_SIZE
            val y = MARGIN + point.row * CELL_SIZE
            g2d.fillOval(x - STAR_POINTS_SIZE / 2, y - STAR_POINTS_SIZE / 2, 
                        STAR_POINTS_SIZE, STAR_POINTS_SIZE)
        }
    }
    
    private fun drawForbiddenPoints(g2d: Graphics2D) {
        for (pos in forbiddenMoves) {
            val x = MARGIN + pos.col * CELL_SIZE
            val y = MARGIN + pos.row * CELL_SIZE
            
            g2d.color = Color(FORBIDDEN_COLOR.red, FORBIDDEN_COLOR.green, FORBIDDEN_COLOR.blue, 60)
            g2d.fillOval(x - 12, y - 12, 24, 24)
            
            g2d.color = FORBIDDEN_COLOR
            g2d.stroke = BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2d.drawLine(x - 8, y - 8, x + 8, y + 8)
            g2d.drawLine(x - 8, y + 8, x + 8, y - 8)
        }
    }
    
    private fun drawStones(g2d: Graphics2D) {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val position = PositionDto(row, col)
                val stone = board.getStone(position)
                
                if (stone != null) {
                    val x = MARGIN + col * CELL_SIZE
                    val y = MARGIN + row * CELL_SIZE
                    
                    val animatedStone = animatedStones[position]
                    if (animatedStone != null) {
                        // Draw with animation
                        val oldComposite = g2d.composite
                        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, animatedStone.opacity)
                        drawStone(g2d, x, y, stone, animatedStone.scale)
                        g2d.composite = oldComposite
                    } else {
                        drawStone(g2d, x, y, stone)
                    }
                }
            }
        }
    }
    
    private fun drawStone(g2d: Graphics2D, x: Int, y: Int, player: PlayerDto, scale: Float = 1f) {
        val scaledSize = Math.max(4, (STONE_SIZE * scale).toInt())
        val halfSize = scaledSize / 2
        
        // 그림자 효과
        g2d.color = Color(0, 0, 0, 60)
        g2d.fillOval(x - halfSize + 3, y - halfSize + 3, scaledSize, scaledSize)
        
        if (player == PlayerDto.BLACK) {
            // 검은돌
            g2d.color = Color(20, 20, 20)
            g2d.fillOval(x - halfSize, y - halfSize, scaledSize, scaledSize)
            
            // 하이라이트
            val gradient = RadialGradientPaint(
                x.toFloat() - halfSize / 3, y.toFloat() - halfSize / 3,
                scaledSize * 0.7f,
                floatArrayOf(0f, 0.8f, 1f),
                arrayOf(Color(60, 60, 60), Color(20, 20, 20), Color(0, 0, 0))
            )
            g2d.paint = gradient
            g2d.fillOval(x - halfSize, y - halfSize, scaledSize, scaledSize)
        } else {
            // 흰돌
            g2d.color = Color(250, 250, 250)
            g2d.fillOval(x - halfSize, y - halfSize, scaledSize, scaledSize)
            
            // 그라데이션
            val gradient = RadialGradientPaint(
                x.toFloat() - halfSize / 3, y.toFloat() - halfSize / 3,
                scaledSize * 0.8f,
                floatArrayOf(0f, 0.7f, 1f),
                arrayOf(Color.WHITE, Color(245, 245, 245), Color(220, 220, 220))
            )
            g2d.paint = gradient
            g2d.fillOval(x - halfSize, y - halfSize, scaledSize, scaledSize)
            
            // 테두리
            g2d.color = Color(180, 180, 180)
            g2d.stroke = BasicStroke(1f)
            g2d.drawOval(x - halfSize, y - halfSize, scaledSize, scaledSize)
        }
    }
    
    private fun drawWinningLine(g2d: Graphics2D) {
        if (winningLine.isNotEmpty() && winAnimationProgress > 0) {
            val startPos = winningLine.first()
            val endPos = winningLine.last()
            
            val startX = MARGIN + startPos.col * CELL_SIZE
            val startY = MARGIN + startPos.row * CELL_SIZE
            val endX = MARGIN + endPos.col * CELL_SIZE
            val endY = MARGIN + endPos.row * CELL_SIZE
            
            // Calculate animated end point
            val animX = startX + ((endX - startX) * winAnimationProgress).toInt()
            val animY = startY + ((endY - startY) * winAnimationProgress).toInt()
            
            // Pulsing effect
            val pulse = Math.sin(winAnimationProgress * Math.PI * 4).toFloat() * 0.2f + 1f
            
            // Glow effect
            val oldComposite = g2d.composite
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f * winAnimationProgress)
            g2d.color = UITheme.Colors.WIN_LINE_GLOW
            g2d.stroke = BasicStroke(20f * pulse, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2d.drawLine(startX, startY, animX, animY)
            
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f * winAnimationProgress)
            g2d.color = WIN_LINE_COLOR
            g2d.stroke = BasicStroke(8f * pulse, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2d.drawLine(startX, startY, animX, animY)
            
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, winAnimationProgress)
            g2d.color = Color(255, 255, 255, 200)
            g2d.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2d.drawLine(startX, startY, animX, animY)
            
            g2d.composite = oldComposite
            
            // Draw sparkles around winning stones
            if (winAnimationProgress > 0.5f) {
                drawSparkles(g2d, winningLine, (winAnimationProgress - 0.5f) * 2f)
            }
        }
    }
    
    private fun drawSparkles(g2d: Graphics2D, positions: List<PositionDto>, intensity: Float) {
        val oldComposite = g2d.composite
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity)
        g2d.color = Color(255, 215, 0)
        
        positions.forEach { pos ->
            val x = MARGIN + pos.col * CELL_SIZE
            val y = MARGIN + pos.row * CELL_SIZE
            
            for (i in 0..3) {
                val angle = (i * 90 + intensity * 360).toDouble()
                val distance = 25 + intensity * 10
                val sparkleX = x + (Math.cos(Math.toRadians(angle)) * distance).toInt()
                val sparkleY = y + (Math.sin(Math.toRadians(angle)) * distance).toInt()
                
                g2d.fillOval(sparkleX - 2, sparkleY - 2, 4, 4)
            }
        }
        
        g2d.composite = oldComposite
    }
    
    private fun drawLastMoveMarker(g2d: Graphics2D) {
        lastMovePosition?.let { position ->
            val x = MARGIN + position.col * CELL_SIZE
            val y = MARGIN + position.row * CELL_SIZE
            
            g2d.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2d.color = Color(LAST_MOVE_COLOR.red, LAST_MOVE_COLOR.green, LAST_MOVE_COLOR.blue, 150)
            val markSize = 12
            val halfSize = STONE_SIZE / 2
            
            val triangle = Path2D.Double()
            triangle.moveTo(x.toDouble(), (y - halfSize - markSize).toDouble())
            triangle.lineTo((x - markSize * 0.7).toDouble(), (y - halfSize).toDouble())
            triangle.lineTo((x + markSize * 0.7).toDouble(), (y - halfSize).toDouble())
            triangle.closePath()
            
            g2d.fill(triangle)
            
            g2d.color = LAST_MOVE_COLOR
            g2d.draw(triangle)
        }
    }
    
    private fun drawHover(g2d: Graphics2D) {
        val pos = hoverPosition
        if (pos != null && board.isEmpty(pos) && isPlayerTurn()) {
            val x = MARGIN + pos.col * CELL_SIZE
            val y = MARGIN + pos.row * CELL_SIZE
            
            val game = controller.getCurrentGame()
            val currentPlayer = game?.currentPlayer
            
            if (currentPlayer != null) {
                val halfSize = STONE_SIZE / 2
                
                g2d.color = if (currentPlayer == PlayerDto.BLACK) {
                    Color(0, 0, 0, 50)
                } else {
                    Color(255, 255, 255, 100)
                }
                g2d.fillOval(x - halfSize, y - halfSize, STONE_SIZE, STONE_SIZE)
                
                g2d.color = if (currentPlayer == PlayerDto.BLACK) {
                    UITheme.Colors.BLACK_STONE
                } else {
                    UITheme.Colors.GRAY_400
                }
                g2d.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, floatArrayOf(5f, 5f), 0f)
                g2d.drawOval(x - halfSize, y - halfSize, STONE_SIZE, STONE_SIZE)
            }
        }
    }
    
    private fun isPlayerTurn(): Boolean {
        if (!::controller.isInitialized) return false
        val game = controller.getCurrentGame() ?: return false
        return game.isPlayerTurn
    }
    
    fun setKeyboardFocus(position: PositionDto) {
        keyboardFocusPosition = position
        repaint()
    }
    
    private fun drawKeyboardFocus(g2d: Graphics2D) {
        keyboardFocusPosition?.let { pos ->
            val x = MARGIN + pos.col * CELL_SIZE
            val y = MARGIN + pos.row * CELL_SIZE
            
            g2d.color = UITheme.Colors.PRIMARY
            g2d.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1.0f, floatArrayOf(8f, 4f), 0f)
            val focusSize = STONE_SIZE + 8
            g2d.drawOval(x - focusSize / 2, y - focusSize / 2, focusSize, focusSize)
        }
    }
}