package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class GameHeader : JPanel() {
    private val logoLabel = JLabel()
    private val titleLabel = JLabel("렌주룰 오목")
    private val subtitleLabel = JLabel("Renju Gomoku")
    private val gameInfoPanel = JPanel()
    private var currentMode = ""
    private var difficulty = ""
    
    init {
        layout = BorderLayout()
        background = UITheme.Colors.BACKGROUND
        // 텍스트 짤림 방지를 위해 여백 증가
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, UITheme.Colors.PRIMARY_LIGHT),
            EmptyBorder(12, UITheme.Spacing.MD, 12, UITheme.Spacing.MD)
        )
        
        // 최소 높이 설정
        preferredSize = Dimension(0, 75)
        
        // Left side - Logo and title
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, UITheme.Spacing.MD, 0))
        leftPanel.background = UITheme.Colors.BACKGROUND
        
        // Logo
        logoLabel.text = "●"
        logoLabel.font = Font("맑은 고딕", Font.PLAIN, 36)
        logoLabel.foreground = UITheme.Colors.PRIMARY
        leftPanel.add(logoLabel)
        
        // Title section - 세로 정렬 개선
        val titleSection = JPanel(GridBagLayout())
        titleSection.background = UITheme.Colors.BACKGROUND
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        
        titleLabel.font = UITheme.Fonts.TITLE
        titleLabel.foreground = UITheme.Colors.GRAY_900
        titleSection.add(titleLabel, gbc)
        
        subtitleLabel.font = UITheme.Fonts.CAPTION
        subtitleLabel.foreground = UITheme.Colors.GRAY_500
        gbc.gridy = 1
        gbc.insets = Insets(2, 0, 0, 0)
        titleSection.add(subtitleLabel, gbc)
        
        leftPanel.add(titleSection)
        add(leftPanel, BorderLayout.WEST)
        
        // Right side - Game info
        gameInfoPanel.background = UITheme.Colors.BACKGROUND
        gameInfoPanel.layout = FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0)
        updateGameInfo()
        add(gameInfoPanel, BorderLayout.EAST)
    }
    
    fun setGameMode(mode: String, difficulty: String? = null) {
        this.currentMode = mode
        this.difficulty = difficulty ?: ""
        updateGameInfo()
    }
    
    private fun updateGameInfo() {
        gameInfoPanel.removeAll()
        
        if (currentMode.isNotEmpty()) {
            val modeChip = createInfoChip(currentMode, UITheme.Colors.PRIMARY_LIGHT, UITheme.Colors.PRIMARY)
            gameInfoPanel.add(modeChip)
            
            if (difficulty.isNotEmpty()) {
                val difficultyChip = createInfoChip(difficulty, UITheme.Colors.SECONDARY.brighter(), UITheme.Colors.SECONDARY)
                gameInfoPanel.add(difficultyChip)
            }
        }
        
        gameInfoPanel.revalidate()
        gameInfoPanel.repaint()
    }
    
    private fun createInfoChip(text: String, bgColor: Color, textColor: Color): JPanel {
        return object : JPanel() {
            init {
                background = bgColor
                border = EmptyBorder(4, 10, 4, 10)
                layout = FlowLayout(FlowLayout.CENTER, 0, 0)
                isOpaque = false
                
                val label = JLabel(text)
                label.font = UITheme.Fonts.CAPTION
                label.foreground = textColor
                add(label)
            }
            
            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.color = background
                g2d.fillRoundRect(0, 0, width, height, 12, 12)
                super.paintComponent(g)
            }
        }
    }
}