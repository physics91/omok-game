package com.omok.presentation.ui.icons

import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

/**
 * 프로그래밍 방식으로 아이콘을 렌더링하는 유틸리티
 */
object IconRenderer {
    
    /**
     * 아이콘 타입별로 렌더링
     */
    fun renderIcon(icon: IconLoader.Icon, width: Int, height: Int): ImageIcon {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        
        // 안티앨리어싱 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        
        // 아이콘별 렌더링
        when (icon) {
            IconLoader.Icon.BLACK_STONE -> drawBlackStone(g2d, width, height)
            IconLoader.Icon.WHITE_STONE -> drawWhiteStone(g2d, width, height)
            IconLoader.Icon.NEW_GAME -> drawNewGame(g2d, width, height)
            IconLoader.Icon.UNDO -> drawUndo(g2d, width, height)
            IconLoader.Icon.SETTINGS -> drawSettings(g2d, width, height)
            IconLoader.Icon.AI_THINKING -> drawAIThinking(g2d, width, height)
            IconLoader.Icon.FORBIDDEN -> drawForbidden(g2d, width, height)
            IconLoader.Icon.WIN -> drawWin(g2d, width, height)
            IconLoader.Icon.TIMER -> drawTimer(g2d, width, height)
            IconLoader.Icon.HELP -> drawHelp(g2d, width, height)
            IconLoader.Icon.SWAP -> drawSwap(g2d, width, height)
            IconLoader.Icon.FIFTH_MOVE -> drawFifthMove(g2d, width, height)
            IconLoader.Icon.PLAYER_VS_PLAYER -> drawPlayerVsPlayer(g2d, width, height)
            IconLoader.Icon.PLAYER_VS_AI -> drawPlayerVsAI(g2d, width, height)
            IconLoader.Icon.DIFFICULTY_EASY -> drawDifficultyEasy(g2d, width, height)
            IconLoader.Icon.DIFFICULTY_MEDIUM -> drawDifficultyMedium(g2d, width, height)
            IconLoader.Icon.DIFFICULTY_HARD -> drawDifficultyHard(g2d, width, height)
            IconLoader.Icon.MOVE_COUNT -> drawMoveCount(g2d, width, height)
            IconLoader.Icon.LAST_MOVE -> drawLastMove(g2d, width, height)
            IconLoader.Icon.SAVE -> drawSave(g2d, width, height)
            IconLoader.Icon.LOAD -> drawLoad(g2d, width, height)
            IconLoader.Icon.LOGO -> drawLogo(g2d, width, height)
        }
        
        g2d.dispose()
        return ImageIcon(image)
    }
    
    private fun drawBlackStone(g: Graphics2D, w: Int, h: Int) {
        val size = minOf(w, h) - 4
        val x = (w - size) / 2
        val y = (h - size) / 2
        
        // 그라디언트
        val gradient = RadialGradientPaint(
            x + size * 0.3f, y + size * 0.3f, size * 0.7f,
            floatArrayOf(0f, 0.5f, 1f),
            arrayOf(Color(70, 70, 70), Color(30, 30, 30), Color.BLACK)
        )
        g.paint = gradient
        g.fillOval(x, y, size, size)
        
        // 하이라이트
        g.color = Color(255, 255, 255, 40)
        g.fillOval(x + size/4, y + size/4, size/3, size/4)
    }
    
    private fun drawWhiteStone(g: Graphics2D, w: Int, h: Int) {
        val size = minOf(w, h) - 4
        val x = (w - size) / 2
        val y = (h - size) / 2
        
        // 그라디언트
        val gradient = RadialGradientPaint(
            x + size * 0.3f, y + size * 0.3f, size * 0.7f,
            floatArrayOf(0f, 0.7f, 1f),
            arrayOf(Color.WHITE, Color(240, 240, 240), Color(200, 200, 200))
        )
        g.paint = gradient
        g.fillOval(x, y, size, size)
        
        // 테두리
        g.color = Color(150, 150, 150)
        g.stroke = BasicStroke(0.5f)
        g.drawOval(x, y, size, size)
        
        // 하이라이트
        g.color = Color(255, 255, 255, 150)
        g.fillOval(x + size/4, y + size/4, size/3, size/4)
    }
    
    private fun drawNewGame(g: Graphics2D, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        
        // 보드 그리기
        g.color = UITheme.Colors.PRIMARY
        g.stroke = BasicStroke(2f)
        g.drawRect(4, 8, w-8, h-12)
        
        // 격자선
        g.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(2f, 2f), 0f)
        g.drawLine(4, cy, w-4, cy)
        g.drawLine(cx, 8, cx, h-4)
        
        // 더하기 기호
        g.stroke = BasicStroke(2f)
        g.drawLine(cx-4, 4, cx+4, 4)
        g.drawLine(cx, 0, cx, 8)
    }
    
    private fun drawUndo(g: Graphics2D, w: Int, h: Int) {
        val path = Path2D.Float()
        path.moveTo(w * 0.5, h * 0.3)
        path.curveTo(w * 0.2, h * 0.3, w * 0.1, h * 0.5, w * 0.1, h * 0.7)
        path.lineTo(w * 0.1, h * 0.9)
        
        g.color = UITheme.Colors.GRAY_600
        g.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(path)
        
        // 화살표
        val arrow = Path2D.Float()
        arrow.moveTo(w * 0.1, h * 0.9)
        arrow.lineTo(w * 0.2, h * 0.8)
        arrow.moveTo(w * 0.1, h * 0.9)
        arrow.lineTo(w * 0.0, h * 0.8)
        g.draw(arrow)
    }
    
    private fun drawSettings(g: Graphics2D, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        val r = minOf(w, h) / 4
        
        // 기어 톱니
        g.color = UITheme.Colors.GRAY_600
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4
            val x1 = cx + (r * 1.2 * Math.cos(angle)).toInt()
            val y1 = cy + (r * 1.2 * Math.sin(angle)).toInt()
            g.fillOval(x1 - 2, y1 - 2, 4, 4)
        }
        
        // 중앙 원
        g.fillOval(cx - r, cy - r, r * 2, r * 2)
        g.color = UITheme.Colors.BACKGROUND
        g.fillOval(cx - r/2, cy - r/2, r, r)
    }
    
    private fun drawAIThinking(g: Graphics2D, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        
        // 외부 원
        g.color = UITheme.Colors.PRIMARY
        g.stroke = BasicStroke(2f)
        g.drawOval(2, 2, w-4, h-4)
        
        // 중앙 원
        g.fillOval(cx-3, cy-3, 6, 6)
        
        // 방사선
        g.stroke = BasicStroke(1f)
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4
            val x1 = cx + (8 * Math.cos(angle)).toInt()
            val y1 = cy + (8 * Math.sin(angle)).toInt()
            val x2 = cx + (10 * Math.cos(angle)).toInt()
            val y2 = cy + (10 * Math.sin(angle)).toInt()
            g.drawLine(x1, y1, x2, y2)
        }
    }
    
    private fun drawForbidden(g: Graphics2D, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        val r = minOf(w, h) / 2 - 2
        
        // 배경 원
        g.color = Color(239, 68, 68, 50)
        g.fillOval(cx - r, cy - r, r * 2, r * 2)
        
        // 테두리
        g.color = Color(239, 68, 68)
        g.stroke = BasicStroke(2f)
        g.drawOval(cx - r, cy - r, r * 2, r * 2)
        
        // X 표시
        g.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val offset = r / 2
        g.drawLine(cx - offset, cy - offset, cx + offset, cy + offset)
        g.drawLine(cx - offset, cy + offset, cx + offset, cy - offset)
    }
    
    private fun drawWin(g: Graphics2D, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        
        // 별 그리기
        val star = createStar(cx, cy, minOf(w, h) / 2 - 2, 5)
        g.color = Color(255, 215, 0)
        g.fill(star)
        g.color = Color(255, 140, 0)
        g.stroke = BasicStroke(1f)
        g.draw(star)
        
        // 중앙 원
        g.color = Color(255, 255, 255, 200)
        g.fillOval(cx - 3, cy - 3, 6, 6)
    }
    
    private fun drawTimer(g: Graphics2D, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2 + 1
        val r = minOf(w, h) / 2 - 3
        
        // 시계 본체
        g.color = UITheme.Colors.GRAY_600
        g.stroke = BasicStroke(2f)
        g.drawOval(cx - r, cy - r, r * 2, r * 2)
        
        // 시계 머리
        g.fillRect(cx - 3, 2, 6, 3)
        
        // 시계 바늘
        g.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.drawLine(cx, cy, cx, cy - r + 4)
        g.drawLine(cx, cy, cx + r/2, cy + 2)
        
        // 중심점
        g.fillOval(cx - 1, cy - 1, 2, 2)
    }
    
    private fun drawHelp(g: Graphics2D, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        val r = minOf(w, h) / 2 - 2
        
        // 원
        g.color = UITheme.Colors.PRIMARY
        g.stroke = BasicStroke(2f)
        g.drawOval(cx - r, cy - r, r * 2, r * 2)
        
        // 물음표
        g.font = Font("Arial", Font.BOLD, h * 2 / 3)
        val fm = g.fontMetrics
        val text = "?"
        val textWidth = fm.stringWidth(text)
        g.drawString(text, cx - textWidth/2, cy + fm.ascent/3)
    }
    
    private fun drawSwap(g: Graphics2D, w: Int, h: Int) {
        // 흑돌
        g.color = Color.BLACK
        g.fillOval(2, 2, 10, 10)
        
        // 백돌
        g.color = Color.WHITE
        g.fillOval(w-12, h-12, 10, 10)
        g.color = Color.GRAY
        g.stroke = BasicStroke(1f)
        g.drawOval(w-12, h-12, 10, 10)
        
        // 교체 화살표
        g.color = UITheme.Colors.PRIMARY
        g.stroke = BasicStroke(2f)
        val arrow1 = Path2D.Float()
        arrow1.moveTo(13f, 5f)
        arrow1.quadTo(w/2f, 0f, w-5f, 8f)
        g.draw(arrow1)
        
        val arrow2 = Path2D.Float()
        arrow2.moveTo(w-13f, h-5f)
        arrow2.quadTo(w/2f, h.toFloat(), 5f, h-8f)
        g.draw(arrow2)
    }
    
    private fun drawFifthMove(g: Graphics2D, w: Int, h: Int) {
        // 점선 원 1
        g.color = Color.BLACK
        g.stroke = BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(3f, 2f), 0f)
        g.drawOval(2, 2, 10, 10)
        g.font = Font("Arial", Font.BOLD, 8)
        g.drawString("1", 5, 10)
        
        // 점선 원 2
        g.drawOval(w-12, h-12, 10, 10)
        g.drawString("2", w-9, h-4)
        
        // 5 표시
        g.color = UITheme.Colors.PRIMARY
        g.font = Font("Arial", Font.BOLD, 10)
        g.drawString("5", w/2-2, 8)
    }
    
    private fun drawPlayerVsPlayer(g: Graphics2D, w: Int, h: Int) {
        // 플레이어 1
        g.color = UITheme.Colors.GRAY_700
        g.fillOval(3, 3, 6, 6)
        g.fillRect(2, 9, 8, 8)
        
        // 플레이어 2
        g.color = UITheme.Colors.GRAY_600
        g.fillOval(w-9, 3, 6, 6)
        g.fillRect(w-10, 9, 8, 8)
    }
    
    private fun drawPlayerVsAI(g: Graphics2D, w: Int, h: Int) {
        // 플레이어
        g.color = UITheme.Colors.GRAY_700
        g.fillOval(3, 3, 6, 6)
        g.fillRect(2, 9, 8, 8)
        
        // AI (로봇)
        g.color = UITheme.Colors.PRIMARY
        g.fillRect(w-10, 2, 8, 6)
        g.color = Color.WHITE
        g.fillOval(w-9, 3, 2, 2)
        g.fillOval(w-5, 3, 2, 2)
        g.color = UITheme.Colors.PRIMARY
        g.fillRect(w-10, 9, 8, 8)
    }
    
    private fun drawDifficultyEasy(g: Graphics2D, w: Int, h: Int) {
        val barWidth = (w - 4) / 3
        val barHeight = h - 8
        
        // 1개 막대 (초록색)
        g.color = Color(16, 185, 129)
        g.fillRect(2, 8, barWidth - 1, barHeight)
        
        // 나머지 막대 (회색)
        g.color = UITheme.Colors.GRAY_200
        g.fillRect(2 + barWidth, 8, barWidth - 1, barHeight)
        g.fillRect(2 + barWidth * 2, 8, barWidth - 1, barHeight)
    }
    
    private fun drawDifficultyMedium(g: Graphics2D, w: Int, h: Int) {
        val barWidth = (w - 4) / 3
        val barHeight = h - 8
        
        // 2개 막대 (주황색)
        g.color = Color(245, 158, 11)
        g.fillRect(2, 8, barWidth - 1, barHeight)
        g.fillRect(2 + barWidth, 8, barWidth - 1, barHeight)
        
        // 나머지 막대 (회색)
        g.color = UITheme.Colors.GRAY_200
        g.fillRect(2 + barWidth * 2, 8, barWidth - 1, barHeight)
    }
    
    private fun drawDifficultyHard(g: Graphics2D, w: Int, h: Int) {
        val barWidth = (w - 4) / 3
        val barHeight = h - 8
        
        // 3개 막대 (빨간색)
        g.color = Color(239, 68, 68)
        g.fillRect(2, 8, barWidth - 1, barHeight)
        g.fillRect(2 + barWidth, 8, barWidth - 1, barHeight)
        g.fillRect(2 + barWidth * 2, 8, barWidth - 1, barHeight)
    }
    
    private fun drawMoveCount(g: Graphics2D, w: Int, h: Int) {
        // 흑돌
        g.color = Color.BLACK
        g.fillOval(2, 2, 10, 10)
        
        // 백돌
        g.color = Color.WHITE
        g.fillOval(w-12, h-12, 10, 10)
        g.color = Color.GRAY
        g.stroke = BasicStroke(1f)
        g.drawOval(w-12, h-12, 10, 10)
        
        // 숫자
        g.color = UITheme.Colors.PRIMARY
        g.font = Font("Arial", Font.BOLD, 10)
        g.drawString("#", w/2-3, 8)
    }
    
    private fun drawLastMove(g: Graphics2D, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        
        // 점선 원
        g.color = UITheme.Colors.PRIMARY
        g.stroke = BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(2f, 2f), 0f)
        g.drawOval(4, 4, w-8, h-8)
        
        // 화살표
        val arrow = Path2D.Float()
        arrow.moveTo(cx.toFloat(), 6f)
        arrow.lineTo(cx - 4f, 10f)
        arrow.lineTo(cx - 1f, 10f)
        arrow.lineTo(cx - 1f, cy + 2f)
        arrow.lineTo(cx + 1f, cy + 2f)
        arrow.lineTo(cx + 1f, 10f)
        arrow.lineTo(cx + 4f, 10f)
        arrow.closePath()
        g.fill(arrow)
    }
    
    private fun drawLogo(g: Graphics2D, w: Int, h: Int) {
        // 보드 배경
        val gradient = LinearGradientPaint(
            0f, 0f, w.toFloat(), h.toFloat(),
            floatArrayOf(0f, 1f),
            arrayOf(Color(254, 243, 199), Color(245, 158, 11))
        )
        g.paint = gradient
        g.fillRoundRect(4, 4, w-8, h-8, 4, 4)
        
        // 격자선
        g.color = Color(146, 64, 14, 128)
        g.stroke = BasicStroke(1f)
        val step = (w - 8) / 4
        for (i in 1 until 4) {
            g.drawLine(4 + i * step, 8, 4 + i * step, h-8)
            g.drawLine(8, 4 + i * step, w-8, 4 + i * step)
        }
        
        // 돌
        g.color = Color.BLACK
        g.fillOval(w/3 - 4, h/3 - 4, 8, 8)
        g.color = Color.WHITE
        g.fillOval(w*2/3 - 4, h*2/3 - 4, 8, 8)
        g.color = Color.GRAY
        g.drawOval(w*2/3 - 4, h*2/3 - 4, 8, 8)
    }
    
    private fun createStar(cx: Int, cy: Int, radius: Int, points: Int): Path2D {
        val path = Path2D.Float()
        val innerRadius = radius * 0.4
        
        for (i in 0 until points * 2) {
            val angle = Math.PI * i / points - Math.PI / 2
            val r = if (i % 2 == 0) radius.toDouble() else innerRadius
            val x = cx + r * Math.cos(angle)
            val y = cy + r * Math.sin(angle)
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.closePath()
        return path
    }
    
    private fun drawSave(g: Graphics2D, w: Int, h: Int) {
        val stroke = BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.stroke = stroke
        g.color = UITheme.Colors.PRIMARY
        
        // 디스켓 모양
        val rect = Rectangle2D.Float(w/6f, h/8f, w*2/3f, h*3/4f)
        g.draw(rect)
        g.color = Color(UITheme.Colors.PRIMARY.red, UITheme.Colors.PRIMARY.green, UITheme.Colors.PRIMARY.blue, 30)
        g.fill(rect)
        
        // 라벨 부분
        g.color = UITheme.Colors.PRIMARY
        val labelRect = Rectangle2D.Float(w/4f, h/8f, w/2f, h/4f)
        g.draw(labelRect)
        
        // 중앙 아래 사각형
        val centerRect = Rectangle2D.Float(w/3f, h/2f, w/3f, h/3f)
        g.draw(centerRect)
    }
    
    private fun drawLoad(g: Graphics2D, w: Int, h: Int) {
        val stroke = BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.stroke = stroke
        g.color = UITheme.Colors.PRIMARY
        
        // 폴더 모양
        val path = Path2D.Float()
        path.moveTo(w/6f, h/3f)
        path.lineTo(w/3f, h/3f)
        path.lineTo(w*2/5f, h/6f)
        path.lineTo(w*5/6f, h/6f)
        path.lineTo(w*5/6f, h*5/6f)
        path.lineTo(w/6f, h*5/6f)
        path.closePath()
        
        g.draw(path)
        g.color = Color(UITheme.Colors.PRIMARY.red, UITheme.Colors.PRIMARY.green, UITheme.Colors.PRIMARY.blue, 30)
        g.fill(path)
        
        // 화살표
        g.color = UITheme.Colors.PRIMARY
        val arrowPath = Path2D.Float()
        arrowPath.moveTo(w/2f, h*3/5f)
        arrowPath.lineTo(w*3/8f, h/2f)
        arrowPath.lineTo(w*5/8f, h/2f)
        arrowPath.closePath()
        g.fill(arrowPath)
    }
}