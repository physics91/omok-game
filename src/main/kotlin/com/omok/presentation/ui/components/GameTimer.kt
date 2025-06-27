package com.omok.presentation.ui.components

import com.omok.application.dto.*
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.effects.SoundEffects
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 게임 타이머 UI 컴포넌트
 */
class GameTimer : JPanel() {
    private val blackTimeLabel = JLabel("∞")
    private val whiteTimeLabel = JLabel("∞")
    private val blackPlayerLabel = JLabel("흑돌")
    private val whitePlayerLabel = JLabel("백돌")
    private val blackPanel = JPanel()
    private val whitePanel = JPanel()
    
    private var currentTimeState: GameTimeStateDto? = null
    private var currentPlayer: PlayerDto = PlayerDto.BLACK
    private var updateTimer: Timer? = null
    
    private var onTimeUp: ((PlayerDto) -> Unit)? = null
    private var lastWarningTime = mutableMapOf<PlayerDto, Int>()
    
    init {
        initializeUI()
        startUpdateTimer()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        background = UITheme.Colors.SURFACE
        border = EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        
        // 흑돌 타이머
        blackPanel.layout = BorderLayout()
        blackPanel.background = UITheme.Colors.SURFACE
        blackPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300, 1),
            EmptyBorder(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
        )
        
        blackPlayerLabel.font = UITheme.Fonts.CAPTION_BOLD
        blackPlayerLabel.foreground = UITheme.Colors.GRAY_700
        blackPlayerLabel.horizontalAlignment = SwingConstants.CENTER
        
        blackTimeLabel.font = UITheme.Fonts.TITLE
        blackTimeLabel.foreground = UITheme.Colors.GRAY_900
        blackTimeLabel.horizontalAlignment = SwingConstants.CENTER
        
        blackPanel.add(blackPlayerLabel, BorderLayout.NORTH)
        blackPanel.add(blackTimeLabel, BorderLayout.CENTER)
        
        // 백돌 타이머
        whitePanel.layout = BorderLayout()
        whitePanel.background = UITheme.Colors.SURFACE
        whitePanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300, 1),
            EmptyBorder(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
        )
        
        whitePlayerLabel.font = UITheme.Fonts.CAPTION_BOLD
        whitePlayerLabel.foreground = UITheme.Colors.GRAY_700
        whitePlayerLabel.horizontalAlignment = SwingConstants.CENTER
        
        whiteTimeLabel.font = UITheme.Fonts.TITLE
        whiteTimeLabel.foreground = UITheme.Colors.GRAY_900
        whiteTimeLabel.horizontalAlignment = SwingConstants.CENTER
        
        whitePanel.add(whitePlayerLabel, BorderLayout.NORTH)
        whitePanel.add(whiteTimeLabel, BorderLayout.CENTER)
        
        // 레이아웃
        val timerContainer = JPanel(GridLayout(1, 2, UITheme.Spacing.SM, 0))
        timerContainer.background = UITheme.Colors.SURFACE
        timerContainer.add(blackPanel)
        timerContainer.add(whitePanel)
        
        add(timerContainer, BorderLayout.CENTER)
        
        preferredSize = Dimension(0, 60)
    }
    
    private fun startUpdateTimer() {
        updateTimer = Timer(100) { // 100ms마다 업데이트
            updateDisplay()
        }
        updateTimer?.start()
    }
    
    fun updateTimeState(timeState: GameTimeStateDto?, currentPlayer: PlayerDto) {
        this.currentTimeState = timeState
        this.currentPlayer = currentPlayer
        
        if (timeState == null) {
            // 시간 제한 없음
            blackTimeLabel.text = "∞"
            whiteTimeLabel.text = "∞"
            resetHighlight()
        } else {
            updateDisplay()
            updateHighlight()
        }
    }
    
    private fun updateDisplay() {
        val timeState = currentTimeState ?: return
        
        if (timeState.isTimerRunning) {
            // 현재 플레이어의 시간을 실시간으로 계산
            val currentTime = System.currentTimeMillis()
            val blackTime = if (currentPlayer == PlayerDto.BLACK) {
                calculateCurrentRemainingTime(timeState.blackTimeState)
            } else {
                timeState.blackTimeState.remainingTime
            }
            val whiteTime = if (currentPlayer == PlayerDto.WHITE) {
                calculateCurrentRemainingTime(timeState.whiteTimeState)
            } else {
                timeState.whiteTimeState.remainingTime
            }
            
            blackTimeLabel.text = formatTime(blackTime, timeState.blackTimeState)
            whiteTimeLabel.text = formatTime(whiteTime, timeState.whiteTimeState)
            
            // 시간 초과 체크
            if (blackTime <= 0 && !timeState.blackTimeState.isInByoyomi) {
                onTimeUp?.invoke(PlayerDto.BLACK)
            }
            if (whiteTime <= 0 && !timeState.whiteTimeState.isInByoyomi) {
                onTimeUp?.invoke(PlayerDto.WHITE)
            }
        } else {
            blackTimeLabel.text = formatTime(timeState.blackTimeState.remainingTime, timeState.blackTimeState)
            whiteTimeLabel.text = formatTime(timeState.whiteTimeState.remainingTime, timeState.whiteTimeState)
        }
    }
    
    private fun calculateCurrentRemainingTime(playerTimeState: PlayerTimeStateDto): Long {
        // 실제 시간 계산은 도메인에서 처리하므로 여기서는 단순히 현재 값 반환
        return playerTimeState.remainingTime
    }
    
    private fun formatTime(seconds: Long, playerTimeState: PlayerTimeStateDto): String {
        if (seconds < 0) return "00:00"
        
        val minutes = seconds / 60
        val secs = seconds % 60
        
        val timeStr = String.format("%02d:%02d", minutes, secs)
        
        return if (playerTimeState.isInByoyomi) {
            "$timeStr (${playerTimeState.byoyomiPeriods})"
        } else {
            timeStr
        }
    }
    
    private fun updateHighlight() {
        resetHighlight()
        
        val timeState = currentTimeState ?: return
        if (!timeState.isTimerRunning) return
        
        // 현재 플레이어 강조
        when (currentPlayer) {
            PlayerDto.BLACK -> {
                blackPanel.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.Colors.PRIMARY, 2),
                    EmptyBorder(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
                )
                blackTimeLabel.foreground = UITheme.Colors.PRIMARY
                
                // 시간 부족 경고
                val blackTime = timeState.blackTimeState.remainingTime
                if (blackTime <= 30 && blackTime > 0) {
                    blackTimeLabel.foreground = UITheme.Colors.WARNING
                    playWarningSound(PlayerDto.BLACK, blackTime.toInt())
                } else if (blackTime <= 10 && blackTime > 0) {
                    blackTimeLabel.foreground = UITheme.Colors.ERROR
                    playWarningSound(PlayerDto.BLACK, blackTime.toInt())
                }
            }
            PlayerDto.WHITE -> {
                whitePanel.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.Colors.PRIMARY, 2),
                    EmptyBorder(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
                )
                whiteTimeLabel.foreground = UITheme.Colors.PRIMARY
                
                // 시간 부족 경고
                val whiteTime = timeState.whiteTimeState.remainingTime
                if (whiteTime <= 30 && whiteTime > 0) {
                    whiteTimeLabel.foreground = UITheme.Colors.WARNING
                    playWarningSound(PlayerDto.WHITE, whiteTime.toInt())
                } else if (whiteTime <= 10 && whiteTime > 0) {
                    whiteTimeLabel.foreground = UITheme.Colors.ERROR
                    playWarningSound(PlayerDto.WHITE, whiteTime.toInt())
                }
            }
        }
    }
    
    private fun resetHighlight() {
        blackPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300, 1),
            EmptyBorder(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
        )
        whitePanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300, 1),
            EmptyBorder(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
        )
        blackTimeLabel.foreground = UITheme.Colors.GRAY_900
        whiteTimeLabel.foreground = UITheme.Colors.GRAY_900
    }
    
    fun setOnTimeUpListener(listener: (PlayerDto) -> Unit) {
        this.onTimeUp = listener
    }
    
    private fun playWarningSound(player: PlayerDto, remainingTime: Int) {
        val lastTime = lastWarningTime[player] ?: Int.MAX_VALUE
        
        // 특정 시간대에서만 사운드 재생 (30, 20, 10, 5, 3, 2, 1초)
        val warningTimes = listOf(30, 20, 10, 5, 3, 2, 1)
        
        if (remainingTime in warningTimes && remainingTime < lastTime) {
            lastWarningTime[player] = remainingTime
            
            when (remainingTime) {
                in 1..5 -> {
                    // 긴급 경고음
                    SoundEffects.playTimeWarning()
                }
                in 6..30 -> {
                    // 일반 경고음
                    SoundEffects.playTimeWarning()
                }
            }
        }
        
        // 시간 초과 시
        if (remainingTime <= 0 && lastTime > 0) {
            lastWarningTime[player] = 0
            SoundEffects.playTimeUp()
        }
    }
    
    fun cleanup() {
        updateTimer?.stop()
        updateTimer = null
    }
}