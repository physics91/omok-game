package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class StatusPanel : JPanel() {
    private val statusIcon = JLabel()
    private val statusText = JLabel()
    private val turnIndicator = TurnIndicator()
    
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
        add(turnIndicator, BorderLayout.EAST)
    }
    
    fun updateStatus(text: String, isPlaying: Boolean = false) {
        statusText.text = text
        statusIcon.text = when {
            text.contains("승리") -> "[승리]"
            text.contains("무승부") -> "[무승부]"
            isPlaying -> "[진행중]"
            else -> "[대기]"
        }
    }
    
    fun setCurrentTurn(isBlackTurn: Boolean) {
        turnIndicator.setTurn(isBlackTurn)
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