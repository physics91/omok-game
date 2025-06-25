package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class ModernTooltip private constructor() {
    companion object {
        private var currentTooltip: JWindow? = null
        private var hideTimer: Timer? = null
        
        fun show(component: JComponent, text: String, position: TooltipPosition = TooltipPosition.TOP) {
            hide()
            
            val tooltip = JWindow(SwingUtilities.getWindowAncestor(component))
            tooltip.type = Window.Type.POPUP
            
            val panel = TooltipPanel(text)
            tooltip.contentPane = panel
            tooltip.pack()
            
            // Calculate position
            val componentLocation = component.locationOnScreen
            val x: Int
            val y: Int
            
            when (position) {
                TooltipPosition.TOP -> {
                    x = componentLocation.x + (component.width - tooltip.width) / 2
                    y = componentLocation.y - tooltip.height - 5
                }
                TooltipPosition.BOTTOM -> {
                    x = componentLocation.x + (component.width - tooltip.width) / 2
                    y = componentLocation.y + component.height + 5
                }
                TooltipPosition.LEFT -> {
                    x = componentLocation.x - tooltip.width - 5
                    y = componentLocation.y + (component.height - tooltip.height) / 2
                }
                TooltipPosition.RIGHT -> {
                    x = componentLocation.x + component.width + 5
                    y = componentLocation.y + (component.height - tooltip.height) / 2
                }
            }
            
            tooltip.location = Point(x, y)
            tooltip.isVisible = true
            
            currentTooltip = tooltip
            
            // Auto-hide after delay
            hideTimer?.stop()
            hideTimer = Timer(3000) {
                hide()
            }
            hideTimer?.isRepeats = false
            hideTimer?.start()
        }
        
        fun hide() {
            hideTimer?.stop()
            currentTooltip?.isVisible = false
            currentTooltip?.dispose()
            currentTooltip = null
        }
    }
    
    enum class TooltipPosition {
        TOP, BOTTOM, LEFT, RIGHT
    }
    
    private class TooltipPanel(text: String) : JPanel() {
        init {
            layout = BorderLayout()
            background = UITheme.Colors.GRAY_900
            border = EmptyBorder(8, 12, 8, 12)
            
            val label = JLabel(text)
            label.font = UITheme.Fonts.BODY_SMALL
            label.foreground = Color.WHITE
            add(label)
        }
        
        override fun paintComponent(g: Graphics) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            // Shadow
            g2d.color = Color(0, 0, 0, 50)
            g2d.fillRoundRect(2, 2, width - 2, height - 2, 8, 8)
            
            // Background
            g2d.color = background
            g2d.fillRoundRect(0, 0, width - 2, height - 2, 8, 8)
        }
    }
}