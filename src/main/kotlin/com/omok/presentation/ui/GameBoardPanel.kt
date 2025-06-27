package com.omok.presentation.ui

import com.omok.application.dto.*
import com.omok.presentation.controller.GameController
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.theme.BoardRenderer
import com.omok.presentation.ui.theme.ThemeManager
import com.omok.presentation.ui.theme.ThemeChangeListener
import com.omok.presentation.ui.theme.GameTheme
import com.omok.presentation.ui.effects.SoundEffects
import com.omok.presentation.ui.components.ModernTooltip
import com.omok.presentation.ui.icons.IconLoader
import com.omok.presentation.ui.settings.UIGameSettings
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.math.sin

class GameBoardPanel : JPanel(), ThemeChangeListener {
    private data class AnimatedStone(
        val position: PositionDto,
        val player: PlayerDto,
        var scale: Float = 0.1f,  // Start with small scale instead of 0
        var opacity: Float = 0f
    )
    
    companion object {
        const val BOARD_SIZE = 15
        const val CELL_SIZE = 40  // 테마 렌더러와 일치
        const val MARGIN = 30  // 테마 렌더러와 일치
        const val BOARD_PIXEL_SIZE = (BOARD_SIZE - 1) * CELL_SIZE
        const val STONE_SIZE = 30
        const val MARK_SIZE = 10
        const val STAR_POINTS_SIZE = 7
        const val TOTAL_BOARD_SIZE = BOARD_PIXEL_SIZE + 2 * MARGIN
    }
    
    private var controller: GameController? = null
    private var hoverPosition: PositionDto? = null
    private var lastMovePosition: PositionDto? = null
    private var board: BoardDto = BoardDto(Array(15) { Array(15) { null } })
    private var forbiddenMoves = setOf<PositionDto>()
    private var moveHistory: List<MoveDto> = emptyList()
    private var winningLine = listOf<PositionDto>()
    private val animatedStones = mutableMapOf<PositionDto, AnimatedStone>()
    private var animationTimer: Timer? = null
    private var winAnimationProgress = 0f
    private var winAnimationTimer: Timer? = null
    private var keyboardFocusPosition: PositionDto? = null
    
    // 오픈 렌주룰 관련
    private var fifthMoveProposalMode = false
    private var proposedFifthMoves = mutableListOf<PositionDto>()
    private var fifthMoveProposalCallback: ((List<PositionDto>) -> Unit)? = null
    private var fifthMoveSelectionMode = false
    private var fifthMoveOptions = listOf<PositionDto>()
    private var fifthMoveSelectionCallback: ((PositionDto) -> Unit)? = null
    
    // AI 사고 과정 시각화
    private var aiThinkingInfo: AIThinkingInfoDto? = null
    private var aiThinkingTimer: Timer? = null
    
    // 테마 렌더러
    private val boardRenderer = BoardRenderer(CELL_SIZE, MARGIN)
    
    init {
        // 전체 보드 크기 설정
        preferredSize = Dimension(TOTAL_BOARD_SIZE, TOTAL_BOARD_SIZE)
        minimumSize = preferredSize
        maximumSize = preferredSize
        isOpaque = true
        
        // 테마 변경 리스너 등록
        ThemeManager.addThemeChangeListener(this)
        
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                handleClick(e.x, e.y)
            }
            
            override fun mouseExited(e: MouseEvent) {
                hoverPosition = null
                updateForbiddenMoves()
                ModernTooltip.hide()
                controller?.updateMouseCoordinate(null)  // 좌표 지우기
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
                    
                    // 좌표 업데이트
                    controller?.updateMouseCoordinate(newPosition)
                    
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
        forbiddenMoves = controller?.getForbiddenMoves() ?: emptySet()
    }
    
    private fun handleClick(x: Int, y: Int) {
        // 컨트롤러가 없어도 클릭 리스너는 동작하도록 수정
        
        val col = (x - MARGIN + CELL_SIZE / 2) / CELL_SIZE
        val row = (y - MARGIN + CELL_SIZE / 2) / CELL_SIZE
        
        if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
            val position = PositionDto(row, col)
            
            // 5수 제시 모드 처리
            if (fifthMoveProposalMode) {
                if (board.isEmpty(position) && !proposedFifthMoves.contains(position)) {
                    proposedFifthMoves.add(position)
                    SoundEffects.playStonePlace()
                    repaint()
                    
                    if (proposedFifthMoves.size == 2) {
                        fifthMoveProposalCallback?.invoke(proposedFifthMoves.toList())
                        fifthMoveProposalMode = false
                        proposedFifthMoves.clear()
                        fifthMoveProposalCallback = null
                    }
                }
                return
            }
            
            // 5수 선택 모드 처리
            if (fifthMoveSelectionMode) {
                if (fifthMoveOptions.contains(position)) {
                    fifthMoveSelectionCallback?.invoke(position)
                    fifthMoveSelectionMode = false
                    fifthMoveOptions = emptyList()
                    fifthMoveSelectionCallback = null
                    SoundEffects.playStonePlace()
                    repaint()
                }
                return
            }
            
            // Check if move is forbidden
            if (forbiddenMoves.contains(position)) {
                SoundEffects.playError()
                showForbiddenAnimation(position)
                return
            }
            
            // 보드 클릭 리스너 호출
            boardClickListeners.forEach { it(position) }
            
            // 컨트롤러가 있으면 이동 처리
            controller?.let { ctrl ->
                if (ctrl.makeMove(position)) {
                    SoundEffects.playStonePlace()
                    updateForbiddenMoves()
                    repaint()
                }
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
    
    fun updateBoard(newBoard: BoardDto, moveHistory: List<MoveDto>) {
        this.moveHistory = moveHistory
        updateBoard(newBoard)
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
        
        // 테마 렌더러를 사용하여 보드 그리기
        boardRenderer.drawBoard(g2d, width, height)
        
        drawForbiddenPoints(g2d)
        drawAIThinking(g2d)  // AI 사고 과정 표시
        drawStones(g2d)
        drawFifthMoveProposals(g2d)
        drawFifthMoveOptions(g2d)
        drawWinningLine(g2d)
        drawLastMoveMarker(g2d)
        drawHover(g2d)
        drawKeyboardFocus(g2d)
    }
    
    
    private fun drawForbiddenPoints(g2d: Graphics2D) {
        for (pos in forbiddenMoves) {
            val domainPos = com.omok.domain.model.Position(pos.row, pos.col)
            boardRenderer.drawForbidden(g2d, domainPos)
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
                    
                    val domainPos = com.omok.domain.model.Position(row, col)
                    val domainPlayer = if (stone == PlayerDto.BLACK) 
                        com.omok.domain.model.Player.BLACK else com.omok.domain.model.Player.WHITE
                    val isLastMove = position == lastMovePosition
                    
                    val animatedStone = animatedStones[position]
                    if (animatedStone != null) {
                        // Draw with animation
                        val oldComposite = g2d.composite
                        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, animatedStone.opacity)
                        // TODO: Add animation scale support to renderer
                        boardRenderer.drawStone(g2d, domainPos, domainPlayer, isLastMove)
                        g2d.composite = oldComposite
                    } else {
                        boardRenderer.drawStone(g2d, domainPos, domainPlayer, isLastMove, 
                            UIGameSettings.getInstance().showMoveNumbers, 
                            getMoveNumber(position))
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
    
    private fun drawMoveNumber(g2d: Graphics2D, x: Int, y: Int, position: PositionDto, player: PlayerDto) {
        // 게임 히스토리에서 이 위치의 수 번호 찾기
        val moveNumber = findMoveNumber(position)
        if (moveNumber > 0) {
            // 수 번호 텍스트 설정
            val fontSize = when {
                moveNumber < 10 -> 14
                moveNumber < 100 -> 12
                else -> 10
            }
            g2d.font = Font("맑은 고딕", Font.BOLD, fontSize)
            
            // 텍스트 색상 (돌 색과 대비되도록)
            g2d.color = if (player == PlayerDto.BLACK) Color.WHITE else Color.BLACK
            
            // 텍스트 중앙 정렬
            val fm = g2d.fontMetrics
            val text = moveNumber.toString()
            val textWidth = fm.stringWidth(text)
            val textHeight = fm.ascent
            
            val textX = x - textWidth / 2
            val textY = y + textHeight / 2 - 2
            
            // 텍스트 그리기
            g2d.drawString(text, textX, textY)
        }
    }
    
    private fun findMoveNumber(position: PositionDto): Int {
        // 게임 히스토리에서 해당 위치의 수 번호 찾기
        for (move in moveHistory) {
            if (move.position.row == position.row && move.position.col == position.col) {
                return move.moveNumber
            }
        }
        return 0
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
            g2d.color = UITheme.Colors.WIN_LINE
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
            g2d.color = Color(UITheme.Colors.LAST_MOVE_INDICATOR.red, UITheme.Colors.LAST_MOVE_INDICATOR.green, UITheme.Colors.LAST_MOVE_INDICATOR.blue, 150)
            val markSize = 12
            val halfSize = STONE_SIZE / 2
            
            val triangle = Path2D.Double()
            triangle.moveTo(x.toDouble(), (y - halfSize - markSize).toDouble())
            triangle.lineTo((x - markSize * 0.7).toDouble(), (y - halfSize).toDouble())
            triangle.lineTo((x + markSize * 0.7).toDouble(), (y - halfSize).toDouble())
            triangle.closePath()
            
            g2d.fill(triangle)
            
            g2d.color = UITheme.Colors.LAST_MOVE_INDICATOR
            g2d.draw(triangle)
        }
    }
    
    private fun drawHover(g2d: Graphics2D) {
        val pos = hoverPosition
        if (pos != null && board.isEmpty(pos) && isPlayerTurn()) {
            val domainPos = com.omok.domain.model.Position(pos.row, pos.col)
            boardRenderer.drawHover(g2d, domainPos)
        }
    }
    
    private fun isPlayerTurn(): Boolean {
        val game = controller?.getCurrentGame() ?: return false
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
    
    /**
     * 오픈 렌주룰 - 5수 제시 중인 위치 표시
     */
    private fun drawFifthMoveProposals(g2d: Graphics2D) {
        if (fifthMoveProposalMode) {
            for (pos in proposedFifthMoves) {
                val x = MARGIN + pos.col * CELL_SIZE
                val y = MARGIN + pos.row * CELL_SIZE
                
                // 제시된 위치에 특별 마커 표시
                g2d.color = UITheme.Colors.PRIMARY
                g2d.stroke = BasicStroke(3f)
                val markerSize = STONE_SIZE + 10
                g2d.drawOval(x - markerSize / 2, y - markerSize / 2, markerSize, markerSize)
                
                // 숫자 표시
                g2d.font = Font("Arial", Font.BOLD, 16)
                val number = (proposedFifthMoves.indexOf(pos) + 1).toString()
                val fm = g2d.fontMetrics
                val numberWidth = fm.stringWidth(number)
                g2d.drawString(number, x - numberWidth / 2, y + fm.height / 4)
            }
            
            // 안내 메시지
            if (proposedFifthMoves.size < 2) {
                g2d.color = UITheme.Colors.GRAY_700
                g2d.font = Font("맑은 고딕", Font.PLAIN, 14)
                val message = "${2 - proposedFifthMoves.size}개의 위치를 더 선택하세요"
                g2d.drawString(message, MARGIN, TOTAL_BOARD_SIZE - 10)
            }
        }
    }
    
    /**
     * 오픈 렌주룰 - 선택 가능한 5수 위치 표시
     */
    private fun drawFifthMoveOptions(g2d: Graphics2D) {
        if (fifthMoveSelectionMode) {
            for ((index, pos) in fifthMoveOptions.withIndex()) {
                val x = MARGIN + pos.col * CELL_SIZE
                val y = MARGIN + pos.row * CELL_SIZE
                
                // 선택 가능한 위치 강조
                g2d.color = Color(UITheme.Colors.PRIMARY.red, UITheme.Colors.PRIMARY.green, 
                                   UITheme.Colors.PRIMARY.blue, 100)
                val highlightSize = STONE_SIZE + 6
                g2d.fillOval(x - highlightSize / 2, y - highlightSize / 2, highlightSize, highlightSize)
                
                // 테두리
                g2d.color = UITheme.Colors.PRIMARY
                g2d.stroke = BasicStroke(2.5f)
                g2d.drawOval(x - highlightSize / 2, y - highlightSize / 2, highlightSize, highlightSize)
                
                // 번호 표시
                g2d.color = Color.WHITE
                g2d.font = Font("Arial", Font.BOLD, 18)
                val number = (index + 1).toString()
                val fm = g2d.fontMetrics
                val numberWidth = fm.stringWidth(number)
                g2d.drawString(number, x - numberWidth / 2, y + fm.height / 4)
            }
        }
    }
    
    /**
     * 오픈 렌주룰 - 5수 제시 모드 활성화
     */
    fun enableFifthMoveProposalMode(callback: (List<PositionDto>) -> Unit) {
        fifthMoveProposalMode = true
        proposedFifthMoves.clear()
        fifthMoveProposalCallback = callback
        repaint()
    }
    
    /**
     * 오픈 렌주룰 - 5수 선택 모드 활성화
     */
    fun enableFifthMoveSelectionMode(options: List<PositionDto>, callback: (PositionDto) -> Unit) {
        fifthMoveSelectionMode = true
        fifthMoveOptions = options
        fifthMoveSelectionCallback = callback
        repaint()
    }
    
    /**
     * 오픈 렌주룰 - 제시된 5수 위치 강조
     */
    fun highlightFifthMoveOptions(positions: List<PositionDto>) {
        fifthMoveOptions = positions
        repaint()
    }
    
    /**
     * AI 사고 과정 표시
     */
    fun showAIThinking(thinkingInfo: AIThinkingInfoDto) {
        aiThinkingInfo = thinkingInfo
        repaint()
        
        // AI 사고 종료 시 자동 제거
        if (thinkingInfo.thinkingProgress >= 1.0f) {
            aiThinkingTimer?.stop()
            aiThinkingTimer = Timer(2000) { e ->
                aiThinkingInfo = null
                repaint()
                (e.source as Timer).stop()
            }
            aiThinkingTimer?.start()
        }
    }
    
    /**
     * AI 사고 과정 그리기
     */
    private fun drawAIThinking(g2d: Graphics2D) {
        val thinkingInfo = aiThinkingInfo ?: return
        
        // 상위 5개 후보만 표시
        val topEvaluations = thinkingInfo.evaluations
            .sortedByDescending { it.score }
            .take(5)
        
        topEvaluations.forEach { eval ->
            val x = MARGIN + eval.position.col * CELL_SIZE
            val y = MARGIN + eval.position.row * CELL_SIZE
            
            // 평가 점수에 따른 색상
            val normalizedScore = (eval.score + 100000f) / 200000f // -100000 ~ 100000 범위를 0~1로 정규화
            val hue = normalizedScore * 120f / 360f // 빨강(0) ~ 초록(120)
            val color = Color.getHSBColor(hue, 0.7f, 0.9f)
            
            // 반투명 원 그리기
            val oldComposite = g2d.composite
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f)
            g2d.color = color
            val radius = 18
            g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2)
            
            // 테두리
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
            g2d.stroke = BasicStroke(2f)
            g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2)
            
            // 점수 표시
            g2d.composite = oldComposite
            g2d.color = Color.BLACK
            g2d.font = Font("맑은 고딕", Font.BOLD, 10)
            val scoreText = if (eval.score > 1000) "${eval.score/1000}k" else eval.score.toString()
            val fm = g2d.fontMetrics
            val textWidth = fm.stringWidth(scoreText)
            g2d.drawString(scoreText, x - textWidth/2, y + fm.height/4)
            
            // 이유 표시
            if (eval.reason != null) {
                g2d.font = Font("맑은 고딕", Font.PLAIN, 9)
                val reasonWidth = g2d.fontMetrics.stringWidth(eval.reason)
                g2d.color = Color(60, 60, 60)
                g2d.drawString(eval.reason, x - reasonWidth/2, y + radius + 12)
            }
        }
        
        // 현재 최고 수 강조
        thinkingInfo.currentBestMove?.let { bestMove ->
            val x = MARGIN + bestMove.col * CELL_SIZE
            val y = MARGIN + bestMove.row * CELL_SIZE
            
            // 깜빡이는 효과
            val pulse = (System.currentTimeMillis() % 1000) / 1000.0
            val alpha = 0.3f + 0.3f * Math.sin(pulse * Math.PI).toFloat()
            
            g2d.color = UITheme.Colors.PRIMARY
            g2d.stroke = BasicStroke(3f)
            val oldComposite = g2d.composite
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
            g2d.drawOval(x - 22, y - 22, 44, 44)
            g2d.composite = oldComposite
        }
        
        // 진행 상황 표시
        if (thinkingInfo.thinkingProgress < 1.0f) {
            g2d.color = UITheme.Colors.GRAY_700
            g2d.font = Font("맑은 고딕", Font.PLAIN, 12)
            val progressText = "AI 분석 중... ${(thinkingInfo.thinkingProgress * 100).toInt()}% (노드: ${thinkingInfo.nodesEvaluated})"
            g2d.drawString(progressText, MARGIN, TOTAL_BOARD_SIZE - 5)
        }
    }
    
    
    // 보드 클릭 리스너 추가 (퍼즐 모드 등에서 사용)
    private var boardClickListeners = mutableListOf<(PositionDto) -> Unit>()
    
    fun addBoardClickListener(listener: (PositionDto) -> Unit) {
        boardClickListeners.add(listener)
    }
    
    // 테마 변경 리스너 구현
    override fun onThemeChanged(oldTheme: GameTheme, newTheme: GameTheme) {
        background = newTheme.colorScheme.background
        repaint()
    }
    
    private fun getMoveNumber(position: PositionDto): Int {
        // TODO: 실제 수순 번호를 가져오는 로직 구현
        return 0
    }
}