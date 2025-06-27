package com.omok.presentation.ui.replay

import com.omok.application.dto.*
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.icons.IconLoader
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.math.max
import kotlin.math.min

/**
 * 기보 재생 패널
 * 게임 수순을 단계별로 재생할 수 있는 컨트롤 제공
 */
class GameReplayPanel : JPanel() {
    
    private var gameHistory: List<MoveDto> = emptyList()
    private var currentMoveIndex: Int = -1 // -1은 시작 전 상태
    private var isReplayMode: Boolean = false
    private var autoPlayTimer: Timer? = null
    private var autoPlaySpeed: Int = 1000 // 밀리초
    
    // UI 컴포넌트
    private val moveCountLabel = JLabel("0 / 0")
    private val currentMoveLabel = JLabel("게임 시작")
    private val playerLabel = JLabel("흑돌 차례")
    
    private val firstButton = ModernButton("⏮", ModernButton.ButtonStyle.GHOST)
    private val prevButton = ModernButton("⏪", ModernButton.ButtonStyle.GHOST)
    private val playPauseButton = ModernButton("▶", ModernButton.ButtonStyle.PRIMARY)
    private val nextButton = ModernButton("⏩", ModernButton.ButtonStyle.GHOST)
    private val lastButton = ModernButton("⏭", ModernButton.ButtonStyle.GHOST)
    
    private val speedSlider = JSlider(100, 3000, 1000) // 0.1초 ~ 3초
    private val speedLabel = JLabel("1.0초")
    
    // 콜백
    private var onMoveChanged: ((Int) -> Unit)? = null
    private var onReplayModeChanged: ((Boolean) -> Unit)? = null
    
    init {
        initializeUI()
        setupEventHandlers()
        updateButtonStates()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        background = UITheme.Colors.SURFACE
        border = EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        preferredSize = Dimension(0, 100)
        
        // 상단: 현재 수 정보
        val infoPanel = JPanel(BorderLayout())
        infoPanel.background = UITheme.Colors.SURFACE
        
        val leftInfoPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        leftInfoPanel.background = UITheme.Colors.SURFACE
        
        moveCountLabel.font = UITheme.Fonts.BUTTON
        moveCountLabel.foreground = UITheme.Colors.PRIMARY
        
        currentMoveLabel.font = UITheme.Fonts.BODY
        currentMoveLabel.foreground = UITheme.Colors.GRAY_700
        currentMoveLabel.border = EmptyBorder(0, UITheme.Spacing.SM, 0, 0)
        
        playerLabel.font = UITheme.Fonts.BODY
        playerLabel.foreground = UITheme.Colors.GRAY_600
        playerLabel.border = EmptyBorder(0, UITheme.Spacing.SM, 0, 0)
        
        leftInfoPanel.add(moveCountLabel)
        leftInfoPanel.add(currentMoveLabel)
        leftInfoPanel.add(playerLabel)
        
        infoPanel.add(leftInfoPanel, BorderLayout.WEST)
        
        // 재생 속도 컨트롤
        val speedPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.XS, 0))
        speedPanel.background = UITheme.Colors.SURFACE
        
        val speedTitleLabel = JLabel("속도:")
        speedTitleLabel.font = UITheme.Fonts.CAPTION
        speedTitleLabel.foreground = UITheme.Colors.GRAY_600
        
        speedSlider.preferredSize = Dimension(80, 20)
        speedSlider.background = UITheme.Colors.SURFACE
        speedSlider.addChangeListener {
            autoPlaySpeed = speedSlider.value
            val seconds = autoPlaySpeed / 1000.0
            speedLabel.text = "${String.format("%.1f", seconds)}초"
        }
        
        speedLabel.font = UITheme.Fonts.CAPTION
        speedLabel.foreground = UITheme.Colors.GRAY_600
        speedLabel.preferredSize = Dimension(40, 20)
        
        speedPanel.add(speedTitleLabel)
        speedPanel.add(speedSlider)
        speedPanel.add(speedLabel)
        
        infoPanel.add(speedPanel, BorderLayout.EAST)
        
        add(infoPanel, BorderLayout.NORTH)
        
        // 하단: 재생 컨트롤
        val controlPanel = JPanel(FlowLayout(FlowLayout.CENTER, UITheme.Spacing.XS, 0))
        controlPanel.background = UITheme.Colors.SURFACE
        controlPanel.border = EmptyBorder(UITheme.Spacing.XS, 0, 0, 0)
        
        // 버튼 스타일 설정
        listOf(firstButton, prevButton, playPauseButton, nextButton, lastButton).forEach { button ->
            button.preferredSize = Dimension(40, 32)
            button.font = Font(button.font.name, Font.BOLD, 14)
        }
        
        // 툴팁 설정
        firstButton.toolTipText = "처음으로"
        prevButton.toolTipText = "이전 수"
        playPauseButton.toolTipText = "자동 재생 / 일시정지"
        nextButton.toolTipText = "다음 수"
        lastButton.toolTipText = "마지막으로"
        
        controlPanel.add(firstButton)
        controlPanel.add(prevButton)
        controlPanel.add(playPauseButton)
        controlPanel.add(nextButton)
        controlPanel.add(lastButton)
        
        add(controlPanel, BorderLayout.CENTER)
    }
    
    private fun setupEventHandlers() {
        firstButton.addActionListener { goToFirst() }
        prevButton.addActionListener { goToPrevious() }
        playPauseButton.addActionListener { toggleAutoPlay() }
        nextButton.addActionListener { goToNext() }
        lastButton.addActionListener { goToLast() }
    }
    
    /**
     * 재생할 게임 기록 설정
     */
    fun setGameHistory(moves: List<MoveDto>) {
        gameHistory = moves
        currentMoveIndex = -1
        updateDisplay()
        updateButtonStates()
        
        // 기록이 있으면 자동으로 재생 모드 활성화
        setReplayMode(moves.isNotEmpty())
    }
    
    /**
     * 재생 모드 활성화/비활성화
     */
    fun setReplayMode(enabled: Boolean) {
        isReplayMode = enabled
        isVisible = enabled
        
        if (!enabled) {
            stopAutoPlay()
            currentMoveIndex = -1
        }
        
        onReplayModeChanged?.invoke(enabled)
        updateDisplay()
        updateButtonStates()
    }
    
    /**
     * 현재 수 인덱스 가져오기
     */
    fun getCurrentMoveIndex(): Int = currentMoveIndex
    
    /**
     * 재생 모드 여부 확인
     */
    fun isInReplayMode(): Boolean = isReplayMode
    
    /**
     * 콜백 설정
     */
    fun setOnMoveChangedListener(callback: (Int) -> Unit) {
        onMoveChanged = callback
    }
    
    fun setOnReplayModeChangedListener(callback: (Boolean) -> Unit) {
        onReplayModeChanged = callback
    }
    
    private fun goToFirst() {
        if (gameHistory.isEmpty()) return
        
        currentMoveIndex = -1
        updateDisplay()
        updateButtonStates()
        onMoveChanged?.invoke(currentMoveIndex)
    }
    
    private fun goToPrevious() {
        if (gameHistory.isEmpty()) return
        
        currentMoveIndex = max(-1, currentMoveIndex - 1)
        updateDisplay()
        updateButtonStates()
        onMoveChanged?.invoke(currentMoveIndex)
    }
    
    private fun goToNext() {
        if (gameHistory.isEmpty()) return
        
        currentMoveIndex = min(gameHistory.size - 1, currentMoveIndex + 1)
        updateDisplay()
        updateButtonStates()
        onMoveChanged?.invoke(currentMoveIndex)
    }
    
    private fun goToLast() {
        if (gameHistory.isEmpty()) return
        
        currentMoveIndex = gameHistory.size - 1
        updateDisplay()
        updateButtonStates()
        onMoveChanged?.invoke(currentMoveIndex)
    }
    
    private fun toggleAutoPlay() {
        if (autoPlayTimer?.isRunning == true) {
            stopAutoPlay()
        } else {
            startAutoPlay()
        }
    }
    
    private fun startAutoPlay() {
        if (gameHistory.isEmpty() || currentMoveIndex >= gameHistory.size - 1) return
        
        autoPlayTimer = Timer(autoPlaySpeed) {
            if (currentMoveIndex < gameHistory.size - 1) {
                goToNext()
            } else {
                stopAutoPlay()
            }
        }
        autoPlayTimer?.start()
        
        playPauseButton.text = "⏸"
        playPauseButton.toolTipText = "일시정지"
    }
    
    private fun stopAutoPlay() {
        autoPlayTimer?.stop()
        autoPlayTimer = null
        
        playPauseButton.text = "▶"
        playPauseButton.toolTipText = "자동 재생"
    }
    
    private fun updateDisplay() {
        if (gameHistory.isEmpty()) {
            moveCountLabel.text = "0 / 0"
            currentMoveLabel.text = "기록 없음"
            playerLabel.text = ""
            return
        }
        
        val totalMoves = gameHistory.size
        val currentDisplayIndex = currentMoveIndex + 1 // 사용자에게는 1부터 표시
        
        moveCountLabel.text = "$currentDisplayIndex / $totalMoves"
        
        if (currentMoveIndex < 0) {
            currentMoveLabel.text = "게임 시작"
            playerLabel.text = "흑돌 차례"
        } else {
            val move = gameHistory[currentMoveIndex]
            val position = move.position
            val coordinate = "${'A' + position.col}${15 - position.row}"
            
            currentMoveLabel.text = "${move.moveNumber}수: $coordinate"
            
            val nextPlayerText = if (currentMoveIndex < gameHistory.size - 1) {
                val nextMove = gameHistory[currentMoveIndex + 1]
                when (nextMove.player) {
                    PlayerDto.BLACK -> "흑돌 차례"
                    PlayerDto.WHITE -> "백돌 차례"
                }
            } else {
                "게임 종료"
            }
            playerLabel.text = nextPlayerText
        }
    }
    
    private fun updateButtonStates() {
        val hasHistory = gameHistory.isNotEmpty()
        val isAtStart = currentMoveIndex <= -1
        val isAtEnd = currentMoveIndex >= gameHistory.size - 1
        val isAutoPlaying = autoPlayTimer?.isRunning == true
        
        firstButton.isEnabled = hasHistory && !isAtStart && !isAutoPlaying
        prevButton.isEnabled = hasHistory && !isAtStart && !isAutoPlaying
        nextButton.isEnabled = hasHistory && !isAtEnd && !isAutoPlaying
        lastButton.isEnabled = hasHistory && !isAtEnd && !isAutoPlaying
        
        playPauseButton.isEnabled = hasHistory && !isAtEnd
    }
    
    fun cleanup() {
        stopAutoPlay()
    }
}