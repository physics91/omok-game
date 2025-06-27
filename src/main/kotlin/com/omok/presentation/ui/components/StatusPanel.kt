package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.icons.IconLoader
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class StatusPanel : JPanel() {
    private val statusIcon = JLabel()
    private val statusText = JLabel()
    private val turnIndicator = TurnIndicator()
    private val coordinateLabel = JLabel()  // 마우스 좌표 표시
    
    init {
        layout = BorderLayout()
        background = UITheme.Colors.SURFACE
        border = EmptyBorder(8, 16, 8, 16)
        preferredSize = Dimension(0, 50)
        
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, UITheme.Spacing.SM, 0))
        leftPanel.background = UITheme.Colors.SURFACE
        
        statusIcon.font = UITheme.Fonts.BODY
        statusIcon.foreground = UITheme.Colors.PRIMARY
        leftPanel.add(statusIcon)
        
        statusText.font = UITheme.Fonts.BODY
        statusText.foreground = UITheme.Colors.GRAY_700
        leftPanel.add(statusText)
        
        add(leftPanel, BorderLayout.WEST)
        
        // 중앙 패널에 좌표 표시
        val centerPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0))
        centerPanel.background = UITheme.Colors.SURFACE
        
        coordinateLabel.font = UITheme.Fonts.CAPTION
        coordinateLabel.foreground = UITheme.Colors.GRAY_500
        centerPanel.add(coordinateLabel)
        
        add(centerPanel, BorderLayout.CENTER)
        add(turnIndicator, BorderLayout.EAST)
    }
    
    fun updateStatus(text: String, isPlaying: Boolean = false) {
        statusText.text = text
        statusIcon.icon = when {
            text.contains("승리") -> IconLoader.getIcon(IconLoader.Icon.WIN, 20, 20)
            text.contains("무승부") -> IconLoader.getIcon(IconLoader.Icon.TIMER, 20, 20)
            text.contains("AI") -> IconLoader.getIcon(IconLoader.Icon.AI_THINKING, 20, 20)
            isPlaying -> IconLoader.getIcon(IconLoader.Icon.TIMER, 20, 20)
            else -> null
        }
    }
    
    fun setCurrentTurn(isBlackTurn: Boolean) {
        turnIndicator.setTurn(isBlackTurn)
    }
    
    fun updateCoordinate(coordinate: String?) {
        coordinateLabel.text = coordinate ?: ""
    }
    
    private class TurnIndicator : JPanel() {
        private var isBlackTurn = true
        
        init {
            preferredSize = Dimension(120, 45)
            background = UITheme.Colors.SURFACE
            isOpaque = false
        }
        
        fun setTurn(isBlack: Boolean) {
            isBlackTurn = isBlack
            repaint()
        }
        
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            val label = if (isBlackTurn) "흑돌 차례" else "백돌 차례"
            g2d.font = UITheme.Fonts.BODY_SMALL
            val fm = g2d.fontMetrics
            val textWidth = fm.stringWidth(label)
            
            val stoneSize = 20
            val totalWidth = stoneSize + UITheme.Spacing.SM + textWidth
            val startX = (width - totalWidth) / 2
            val centerY = height / 2
            
            // Draw stone
            if (isBlackTurn) {
                g2d.color = UITheme.Colors.BLACK_STONE
                g2d.fillOval(startX, centerY - stoneSize / 2, stoneSize, stoneSize)
            } else {
                g2d.color = UITheme.Colors.WHITE_STONE
                g2d.fillOval(startX, centerY - stoneSize / 2, stoneSize, stoneSize)
                g2d.color = UITheme.Colors.GRAY_400
                g2d.drawOval(startX, centerY - stoneSize / 2, stoneSize, stoneSize)
            }
            
            // Draw text
            g2d.color = UITheme.Colors.GRAY_700
            g2d.drawString(label, startX + stoneSize + UITheme.Spacing.SM, centerY + fm.height / 4)
        }
    }
}