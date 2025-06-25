package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JButton
import javax.swing.Timer

class ModernButton(
    text: String,
    private val style: ButtonStyle = ButtonStyle.PRIMARY,
    private val size: ButtonSize = ButtonSize.MEDIUM
) : JButton(text) {
    
    enum class ButtonStyle(
        val backgroundColor: Color,
        val hoverColor: Color,
        val pressedColor: Color,
        val textColor: Color
    ) {
        PRIMARY(
            UITheme.Colors.PRIMARY,
            UITheme.Colors.PRIMARY_DARK,
            UITheme.Colors.SECONDARY_DARK,
            Color.WHITE
        ),
        SECONDARY(
            UITheme.Colors.GRAY_200,
            UITheme.Colors.GRAY_300,
            UITheme.Colors.GRAY_400,
            UITheme.Colors.GRAY_800
        ),
        SUCCESS(
            UITheme.Colors.SUCCESS,
            UITheme.Colors.SUCCESS_DARK,
            UITheme.Colors.SUCCESS_DARK.darker(),
            Color.WHITE
        ),
        DANGER(
            UITheme.Colors.DANGER,
            UITheme.Colors.DANGER_DARK,
            UITheme.Colors.DANGER_DARK.darker(),
            Color.WHITE
        ),
        GHOST(
            Color(0, 0, 0, 0),
            UITheme.Colors.GRAY_100,
            UITheme.Colors.GRAY_200,
            UITheme.Colors.GRAY_700
        )
    }
    
    enum class ButtonSize(
        val padding: Insets,
        val fontSize: Int,
        val borderRadius: Int
    ) {
        SMALL(Insets(8, 20, 8, 20), 14, UITheme.BorderRadius.SM),
        MEDIUM(Insets(12, 30, 12, 30), 16, UITheme.BorderRadius.MD),
        LARGE(Insets(16, 40, 16, 40), 18, UITheme.BorderRadius.LG)
    }
    
    private var currentBackgroundColor = style.backgroundColor
    private var animationTimer: Timer? = null
    private var animationProgress = 0f
    private var targetColor = style.backgroundColor
    private var startColor = style.backgroundColor
    
    init {
        isContentAreaFilled = false
        isFocusPainted = false
        isBorderPainted = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        
        font = Font(UITheme.Fonts.BUTTON.family, Font.BOLD, size.fontSize)
        foreground = style.textColor
        
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                if (isEnabled) {
                    animateColorTransition(style.hoverColor)
                }
            }
            
            override fun mouseExited(e: MouseEvent) {
                if (isEnabled) {
                    animateColorTransition(style.backgroundColor)
                }
            }
            
            override fun mousePressed(e: MouseEvent) {
                if (isEnabled) {
                    currentBackgroundColor = style.pressedColor
                    repaint()
                }
            }
            
            override fun mouseReleased(e: MouseEvent) {
                if (isEnabled) {
                    currentBackgroundColor = if (contains(e.point)) {
                        style.hoverColor
                    } else {
                        style.backgroundColor
                    }
                    repaint()
                }
            }
        })
    }
    
    private fun animateColorTransition(toColor: Color) {
        animationTimer?.stop()
        
        startColor = currentBackgroundColor
        targetColor = toColor
        animationProgress = 0f
        
        animationTimer = Timer(10) { e ->
            animationProgress += 0.1f
            if (animationProgress >= 1f) {
                animationProgress = 1f
                currentBackgroundColor = targetColor
                (e.source as Timer).stop()
            } else {
                currentBackgroundColor = interpolateColor(startColor, targetColor, animationProgress)
            }
            repaint()
        }
        animationTimer?.start()
    }
    
    private fun interpolateColor(start: Color, end: Color, progress: Float): Color {
        val smoothProgress = easeInOutCubic(progress)
        return Color(
            (start.red + (end.red - start.red) * smoothProgress).toInt(),
            (start.green + (end.green - start.green) * smoothProgress).toInt(),
            (start.blue + (end.blue - start.blue) * smoothProgress).toInt(),
            (start.alpha + (end.alpha - start.alpha) * smoothProgress).toInt()
        )
    }
    
    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4 * t * t * t
        } else {
            1 - Math.pow((-2 * t + 2).toDouble(), 3.0).toFloat() / 2
        }
    }
    
    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        if (style != ButtonStyle.GHOST) {
            g2d.color = UITheme.Colors.SHADOW
            val shadowShape = RoundRectangle2D.Double(
                2.0, 2.0,
                (width - 4).toDouble(), (height - 2).toDouble(),
                size.borderRadius.toDouble(), size.borderRadius.toDouble()
            )
            g2d.fill(shadowShape)
        }
        
        g2d.color = if (isEnabled) currentBackgroundColor else UITheme.Colors.GRAY_300
        val buttonShape = RoundRectangle2D.Double(
            0.0, 0.0,
            width.toDouble(), (height - 2).toDouble(),
            size.borderRadius.toDouble(), size.borderRadius.toDouble()
        )
        g2d.fill(buttonShape)
        
        g2d.color = if (isEnabled) foreground else UITheme.Colors.GRAY_500
        val fm = g2d.fontMetrics
        val textBounds = fm.getStringBounds(text, g2d)
        val x = (width - textBounds.width) / 2
        val y = (height - textBounds.height) / 2 + fm.ascent - 1
        g2d.drawString(text, x.toInt(), y.toInt())
    }
    
    override fun getInsets(): Insets {
        return size.padding
    }
    
    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(font)
        val textBounds = fm.getStringBounds(text, graphics)
        return Dimension(
            textBounds.width.toInt() + size.padding.left + size.padding.right,
            textBounds.height.toInt() + size.padding.top + size.padding.bottom
        )
    }
}