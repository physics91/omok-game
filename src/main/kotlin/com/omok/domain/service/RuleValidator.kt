package com.omok.domain.service

import com.omok.domain.model.*
import com.omok.domain.model.Position.Companion.BOARD_SIZE

open class RuleValidator {
    
    open fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        if (!position.isValid() || !board.isEmpty(position)) {
            return false
        }
        
        return if (player == Player.BLACK) {
            !isRenjuForbidden(board, position, player)
        } else {
            true
        }
    }
    
    protected open fun isRenjuForbidden(board: Board, position: Position, player: Player): Boolean {
        val testBoard = board.placeStone(Move(position, player))
        
        return isDoubleThree(testBoard, position, player) ||
               isDoubleFour(testBoard, position, player) ||
               isOverline(testBoard, position, player)
    }
    
    open fun isDoubleThree(board: Board, position: Position, player: Player): Boolean {
        var openThreeCount = 0
        
        val directions = arrayOf(
            intArrayOf(0, 1),   // 가로
            intArrayOf(1, 0),   // 세로
            intArrayOf(1, 1),   // 대각선 \
            intArrayOf(1, -1)   // 대각선 /
        )
        
        for (dir in directions) {
            if (isOpenThree(board, position, dir[0], dir[1], player)) {
                openThreeCount++
            }
        }
        
        return openThreeCount >= 2
    }
    
    private fun isOpenThree(board: Board, position: Position, dr: Int, dc: Int, player: Player): Boolean {
        val line = extractLine(board, position, dr, dc, player)
        
        val openThreePatterns = listOf(
            "0XXX0",
            "0X0XX0", "0XX0X0",
            "00XXX0", "0XXX00",
            "0X00XX0", "0XX00X0", "0X0X0X0"
        )
        
        return openThreePatterns.any { pattern -> line.contains(pattern) }
    }
    
    open fun isDoubleFour(board: Board, position: Position, player: Player): Boolean {
        var fourCount = 0
        
        val directions = arrayOf(
            intArrayOf(0, 1),
            intArrayOf(1, 0),
            intArrayOf(1, 1),
            intArrayOf(1, -1)
        )
        
        for (dir in directions) {
            if (isFour(board, position, dir[0], dir[1], player)) {
                fourCount++
            }
        }
        
        return fourCount >= 2
    }
    
    private fun isFour(board: Board, position: Position, dr: Int, dc: Int, player: Player): Boolean {
        val count = countConsecutive(board, position, dr, dc, player) +
                   countConsecutive(board, position, -dr, -dc, player) + 1
        
        if (count == 4) {
            val end1 = getEndPosition(board, position, dr, dc, player)
            val end2 = getEndPosition(board, position, -dr, -dc, player)
            
            val openEnds = listOfNotNull(end1, end2).count { board.isEmpty(it) }
            return openEnds > 0
        }
        
        return false
    }
    
    open fun isOverline(board: Board, position: Position, player: Player): Boolean {
        val directions = arrayOf(
            intArrayOf(0, 1),
            intArrayOf(1, 0),
            intArrayOf(1, 1),
            intArrayOf(1, -1)
        )
        
        for (dir in directions) {
            val count = countConsecutive(board, position, dir[0], dir[1], player) +
                       countConsecutive(board, position, -dir[0], -dir[1], player) + 1
            if (count > 5) return true
        }
        
        return false
    }
    
    open fun checkWin(board: Board, position: Position, player: Player): GameState {
        val directions = arrayOf(
            intArrayOf(0, 1),
            intArrayOf(1, 0),
            intArrayOf(1, 1),
            intArrayOf(1, -1)
        )
        
        for (dir in directions) {
            val count = countConsecutive(board, position, dir[0], dir[1], player) +
                       countConsecutive(board, position, -dir[0], -dir[1], player) + 1
            
            val isWin = when (player) {
                Player.BLACK -> count == 5
                Player.WHITE -> count >= 5
            }
            
            if (isWin) {
                val winningLine = getWinningLine(board, position, dir[0], dir[1], player, count)
                return GameState.Won(player, winningLine)
            }
        }
        
        return if (board.isFull()) GameState.Draw else GameState.Playing
    }
    
    /**
     * 게임 상태 체크 - 기본적으로 checkWin과 동일, 특수 룰에서 오버라이드
     */
    open fun checkGameState(board: Board, lastMove: Move): GameState {
        return checkWin(board, lastMove.position, lastMove.player)
    }
    
    protected fun getWinningLine(
        board: Board, 
        position: Position, 
        dr: Int,
        dc: Int,
        player: Player, 
        count: Int
    ): List<Position> {
        val line = mutableListOf<Position>()
        
        // Start from the furthest position in the opposite direction
        val backCount = countConsecutive(board, position, -dr, -dc, player)
        var current = Position(
            position.row - dr * backCount,
            position.col - dc * backCount
        )
        
        // Collect winning line positions
        for (i in 0 until minOf(count, 5)) {
            if (current.isValid() && board.getStone(current) == player) {
                line.add(current)
            }
            current = Position(
                current.row + dr,
                current.col + dc
            )
        }
        
        return line
    }
    
    protected fun countConsecutive(
        board: Board, 
        position: Position, 
        dr: Int,
        dc: Int,
        player: Player
    ): Int {
        var count = 0
        var current = Position(
            position.row + dr,
            position.col + dc
        )
        
        while (current.isValid() && board.getStone(current) == player) {
            count++
            current = Position(
                current.row + dr,
                current.col + dc
            )
        }
        
        return count
    }
    
    private fun getEndPosition(
        board: Board, 
        position: Position, 
        dr: Int,
        dc: Int,
        player: Player
    ): Position? {
        val consecutiveCount = countConsecutive(board, position, dr, dc, player)
        val endPos = Position(
            position.row + dr * (consecutiveCount + 1),
            position.col + dc * (consecutiveCount + 1)
        )
        
        return if (endPos.isValid()) endPos else null
    }
    
    private fun extractLine(
        board: Board, 
        position: Position, 
        dr: Int,
        dc: Int,
        player: Player
    ): String {
        val sb = StringBuilder()
        
        for (i in -5..5) {
            val pos = Position(
                position.row + dr * i,
                position.col + dc * i
            )
            
            when {
                !pos.isValid() -> sb.append('B')
                board.isEmpty(pos) -> sb.append('0')
                board.getStone(pos) == player -> sb.append('X')
                else -> sb.append('B')
            }
        }
        
        return sb.toString()
    }
}

enum class Direction(val deltaRow: Int, val deltaCol: Int) {
    HORIZONTAL(0, 1),
    VERTICAL(1, 0),
    DIAGONAL_RIGHT(1, 1),
    DIAGONAL_LEFT(1, -1)
}