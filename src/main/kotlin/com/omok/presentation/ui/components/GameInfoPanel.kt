package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.icons.IconLoader
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class GameInfoPanel : JPanel() {
    private val moveCountLabel = InfoCard("수", "0", IconLoader.Icon.MOVE_COUNT)
    private val timeLabel = InfoCard("시간", "00:00", IconLoader.Icon.TIMER)
    private val capturedLabel = InfoCard("포획", "0", IconLoader.Icon.FORBIDDEN)
    
    private var startTime = System.currentTimeMillis()
    private val timer = Timer(1000) { updateTime() }
    
    init {
        layout = GridLayout(1, 3, 8, 0)
        background = UITheme.Colors.BACKGROUND
        preferredSize = Dimension(400, 80)
        maximumSize = Dimension(500, 80)
        
        add(moveCountLabel)
        add(timeLabel)
        add(capturedLabel)
    }
    
    fun startTimer() {
        startTime = System.currentTimeMillis()
        timer.start()
    }
    
    fun stopTimer() {
        timer.stop()
    }
    
    fun resetInfo() {
        moveCountLabel.setValue("0")
        timeLabel.setValue("00:00")
        capturedLabel.setValue("0")
        startTime = System.currentTimeMillis()
    }
    
    fun updateMoveCount(count: Int) {
        moveCountLabel.setValue(count.toString())
    }
    
    fun updateCaptured(count: Int) {
        capturedLabel.setValue(count.toString())
    }
    
    private fun updateTime() {
        val elapsed = System.currentTimeMillis() - startTime
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / 1000) / 60
        timeLabel.setValue(String.format("%02d:%02d", minutes, seconds))
    }
    
    private class InfoCard(title: String, initialValue: String, icon: IconLoader.Icon) : JPanel() {
        private val valueLabel = JLabel(initialValue)
        
        init {
            layout = BorderLayout()
            background = UITheme.Colors.SURFACE
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_200, 1),
                EmptyBorder(8, 12, 8, 12)
            )
            
            // Icon and title
            val topPanel = JPanel(FlowLayout(FlowLayout.LEFT, UITheme.Spacing.XS, 0))
            topPanel.background = UITheme.Colors.SURFACE
            
            val iconLabel = JLabel()
            iconLabel.icon = IconLoader.getIcon(icon, 16, 16)
            topPanel.add(iconLabel)
            
            val titleLabel = JLabel(title)
            titleLabel.font = UITheme.Fonts.CAPTION
            titleLabel.foreground = UITheme.Colors.GRAY_600
            topPanel.add(titleLabel)
            
            add(topPanel, BorderLayout.NORTH)
            
            // Value
            valueLabel.font = Font(UITheme.Fonts.HEADING.family, Font.BOLD, 20)
            valueLabel.foreground = UITheme.Colors.GRAY_900
            valueLabel.horizontalAlignment = SwingConstants.CENTER
            add(valueLabel, BorderLayout.CENTER)
        }
        
        fun setValue(value: String) {
            valueLabel.text = value
        }
        
        override fun paintComponent(g: Graphics) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.color = background
            g2d.fillRoundRect(0, 0, width, height, UITheme.BorderRadius.MD, UITheme.BorderRadius.MD)
        }
    }
}