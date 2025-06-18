package com.omok

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import javax.swing.JPanel
import kotlin.math.abs

class GameBoard(private val gameController: GameController) : JPanel() {
    companion object {
        const val BOARD_SIZE = 15
        const val CELL_SIZE = 40
        const val MARGIN = 40
        const val BOARD_PIXEL_SIZE = (BOARD_SIZE - 1) * CELL_SIZE
        const val STONE_SIZE = 34
        const val MARK_SIZE = 10
        const val STAR_POINTS_SIZE = 6
        
        val BOARD_COLOR = Color(220, 180, 100)
        val LINE_COLOR = Color(30, 30, 30)
        val BLACK_STONE_COLOR = Color(20, 20, 20)
        val WHITE_STONE_COLOR = Color(240, 240, 240)
        val LAST_MOVE_COLOR = Color(255, 80, 80)
        val HOVER_COLOR = Color(100, 100, 100, 100)
        val FORBIDDEN_COLOR = Color(255, 0, 0, 150)
    }
    
    private var hoverRow = -1
    private var hoverCol = -1
    private var lastMoveRow = -1
    private var lastMoveCol = -1
    private val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { Stone.EMPTY } }
    private val forbiddenMoves = mutableSetOf<Position>()
    
    init {
        preferredSize = Dimension(
            BOARD_PIXEL_SIZE + 2 * MARGIN,
            BOARD_PIXEL_SIZE + 2 * MARGIN
        )
        background = BOARD_COLOR
        
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                handleClick(e.x, e.y)
            }
            
            override fun mouseExited(e: MouseEvent) {
                hoverRow = -1
                hoverCol = -1
                updateForbiddenMoves()
                repaint()
            }
        })
        
        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val col = (e.x - MARGIN + CELL_SIZE / 2) / CELL_SIZE
                val row = (e.y - MARGIN + CELL_SIZE / 2) / CELL_SIZE
                
                if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE && 
                    (row != hoverRow || col != hoverCol)) {
                    hoverRow = row
                    hoverCol = col
                    updateForbiddenMoves()
                    repaint()
                }
            }
        })
    }
    
    private fun updateForbiddenMoves() {
        forbiddenMoves.clear()
        if (gameController.getCurrentPlayer() == Stone.BLACK) {
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    if (board[row][col] == Stone.EMPTY && 
                        !RenjuRule.isValidMove(board, row, col, Stone.BLACK)) {
                        forbiddenMoves.add(Position(row, col))
                    }
                }
            }
        }
    }
    
    private fun handleClick(x: Int, y: Int) {
        val col = (x - MARGIN + CELL_SIZE / 2) / CELL_SIZE
        val row = (y - MARGIN + CELL_SIZE / 2) / CELL_SIZE
        
        if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
            if (gameController.makeMove(row, col)) {
                lastMoveRow = row
                lastMoveCol = col
                updateForbiddenMoves()
                repaint()
            }
        }
    }
    
    fun updateBoard(newBoard: Array<Array<Stone>>) {
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                board[i][j] = newBoard[i][j]
            }
        }
        updateForbiddenMoves()
        repaint()
    }
    
    fun setLastMove(row: Int, col: Int) {
        lastMoveRow = row
        lastMoveCol = col
    }
    
    fun reset() {
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                board[i][j] = Stone.EMPTY
            }
        }
        lastMoveRow = -1
        lastMoveCol = -1
        hoverRow = -1
        hoverCol = -1
        forbiddenMoves.clear()
        repaint()
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        drawBoard(g2d)
        drawStarPoints(g2d)
        drawForbiddenPoints(g2d)
        drawStones(g2d)
        drawLastMoveMarker(g2d)
        drawHover(g2d)
    }
    
    private fun drawBoard(g2d: Graphics2D) {
        g2d.color = LINE_COLOR
        g2d.stroke = BasicStroke(1f)
        
        for (i in 0 until BOARD_SIZE) {
            val pos = MARGIN + i * CELL_SIZE
            
            g2d.drawLine(MARGIN, pos, MARGIN + BOARD_PIXEL_SIZE, pos)
            g2d.drawLine(pos, MARGIN, pos, MARGIN + BOARD_PIXEL_SIZE)
            
            g2d.font = Font("Arial", Font.PLAIN, 12)
            val label = ('A' + i).toString()
            val number = (BOARD_SIZE - i).toString()
            
            g2d.drawString(label, pos - 4, MARGIN - 10)
            g2d.drawString(label, pos - 4, MARGIN + BOARD_PIXEL_SIZE + 20)
            g2d.drawString(number, MARGIN - 25, pos + 4)
            g2d.drawString(number, MARGIN + BOARD_PIXEL_SIZE + 10, pos + 4)
        }
        
        g2d.stroke = BasicStroke(2f)
        g2d.drawRect(MARGIN, MARGIN, BOARD_PIXEL_SIZE, BOARD_PIXEL_SIZE)
    }
    
    private fun drawStarPoints(g2d: Graphics2D) {
        g2d.color = LINE_COLOR
        val starPoints = listOf(
            Position(3, 3), Position(3, 11), Position(11, 3), Position(11, 11),
            Position(7, 7),
            Position(3, 7), Position(7, 3), Position(7, 11), Position(11, 7)
        )
        
        for (point in starPoints) {
            val x = MARGIN + point.col * CELL_SIZE
            val y = MARGIN + point.row * CELL_SIZE
            g2d.fillOval(x - STAR_POINTS_SIZE / 2, y - STAR_POINTS_SIZE / 2, 
                        STAR_POINTS_SIZE, STAR_POINTS_SIZE)
        }
    }
    
    private fun drawForbiddenPoints(g2d: Graphics2D) {
        g2d.color = FORBIDDEN_COLOR
        for (pos in forbiddenMoves) {
            val x = MARGIN + pos.col * CELL_SIZE
            val y = MARGIN + pos.row * CELL_SIZE
            
            g2d.stroke = BasicStroke(2f)
            g2d.drawLine(x - 8, y - 8, x + 8, y + 8)
            g2d.drawLine(x - 8, y + 8, x + 8, y - 8)
        }
    }
    
    private fun drawStones(g2d: Graphics2D) {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (board[row][col] != Stone.EMPTY) {
                    val x = MARGIN + col * CELL_SIZE
                    val y = MARGIN + row * CELL_SIZE
                    
                    drawStone(g2d, x, y, board[row][col])
                }
            }
        }
    }
    
    private fun drawStone(g2d: Graphics2D, x: Int, y: Int, stone: Stone) {
        val halfSize = STONE_SIZE / 2
        
        if (stone == Stone.BLACK) {
            val gradient = RadialGradientPaint(
                x.toFloat() - halfSize / 3, y.toFloat() - halfSize / 3,
                STONE_SIZE.toFloat(),
                floatArrayOf(0f, 1f),
                arrayOf(Color(60, 60, 60), BLACK_STONE_COLOR)
            )
            g2d.paint = gradient
        } else {
            val gradient = RadialGradientPaint(
                x.toFloat() - halfSize / 3, y.toFloat() - halfSize / 3,
                STONE_SIZE.toFloat(),
                floatArrayOf(0f, 0.8f, 1f),
                arrayOf(Color.WHITE, WHITE_STONE_COLOR, Color(200, 200, 200))
            )
            g2d.paint = gradient
        }
        
        g2d.fillOval(x - halfSize, y - halfSize, STONE_SIZE, STONE_SIZE)
        
        g2d.color = Color(50, 50, 50)
        g2d.stroke = BasicStroke(1f)
        g2d.drawOval(x - halfSize, y - halfSize, STONE_SIZE, STONE_SIZE)
    }
    
    private fun drawLastMoveMarker(g2d: Graphics2D) {
        if (lastMoveRow >= 0 && lastMoveCol >= 0) {
            val x = MARGIN + lastMoveCol * CELL_SIZE
            val y = MARGIN + lastMoveRow * CELL_SIZE
            
            g2d.color = LAST_MOVE_COLOR
            g2d.stroke = BasicStroke(2f)
            val markHalfSize = MARK_SIZE / 2
            g2d.drawRect(x - markHalfSize, y - markHalfSize, MARK_SIZE, MARK_SIZE)
        }
    }
    
    private fun drawHover(g2d: Graphics2D) {
        if (hoverRow >= 0 && hoverCol >= 0 && board[hoverRow][hoverCol] == Stone.EMPTY &&
            gameController.isPlayerTurn()) {
            val x = MARGIN + hoverCol * CELL_SIZE
            val y = MARGIN + hoverRow * CELL_SIZE
            
            g2d.color = HOVER_COLOR
            val halfSize = STONE_SIZE / 2
            g2d.fillOval(x - halfSize, y - halfSize, STONE_SIZE, STONE_SIZE)
        }
    }
}