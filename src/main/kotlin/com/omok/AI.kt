package com.omok

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class Move(val row: Int, val col: Int, val score: Int)

class AI(private val difficulty: Difficulty) {
    enum class Difficulty(val depth: Int, val randomFactor: Double) {
        EASY(2, 0.3),
        MEDIUM(3, 0.1),
        HARD(4, 0.0)
    }
    
    companion object {
        const val WIN_SCORE = 1000000
        const val FOUR_SCORE = 10000
        const val OPEN_THREE_SCORE = 1000
        const val THREE_SCORE = 100
        const val TWO_SCORE = 10
        const val ONE_SCORE = 1
        
        private val patterns = mapOf(
            "XXXXX" to WIN_SCORE,
            "0XXXX0" to FOUR_SCORE,
            "XXXX0" to FOUR_SCORE / 2,
            "0XXXX" to FOUR_SCORE / 2,
            "0XXX0" to OPEN_THREE_SCORE,
            "XXX0" to THREE_SCORE,
            "0XXX" to THREE_SCORE,
            "0XX0" to TWO_SCORE * 2,
            "XX0" to TWO_SCORE,
            "0XX" to TWO_SCORE,
            "0X0" to ONE_SCORE,
            "X0" to ONE_SCORE,
            "0X" to ONE_SCORE
        )
    }
    
    fun getBestMove(board: Array<Array<Stone>>, currentPlayer: Stone): Position? {
        val validMoves = getValidMoves(board, currentPlayer)
        if (validMoves.isEmpty()) return null
        
        if (difficulty.randomFactor > 0 && Random.nextDouble() < difficulty.randomFactor) {
            return validMoves.random()
        }
        
        val immediateWin = findImmediateWin(board, currentPlayer, validMoves)
        if (immediateWin != null) return immediateWin
        
        val blockOpponentWin = findImmediateWin(board, getOpponent(currentPlayer), validMoves)
        if (blockOpponentWin != null) return blockOpponentWin
        
        var bestScore = Int.MIN_VALUE
        var bestMoves = mutableListOf<Position>()
        
        for (move in validMoves) {
            board[move.row][move.col] = currentPlayer
            val score = minimax(board, difficulty.depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false, currentPlayer)
            board[move.row][move.col] = Stone.EMPTY
            
            if (score > bestScore) {
                bestScore = score
                bestMoves.clear()
                bestMoves.add(move)
            } else if (score == bestScore) {
                bestMoves.add(move)
            }
        }
        
        return if (bestMoves.isNotEmpty()) {
            bestMoves.maxByOrNull { evaluatePosition(board, it, currentPlayer) }
        } else {
            validMoves.firstOrNull()
        }
    }
    
    private fun minimax(
        board: Array<Array<Stone>>,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        player: Stone
    ): Int {
        if (depth == 0) {
            return evaluateBoard(board, player)
        }
        
        val currentPlayer = if (isMaximizing) player else getOpponent(player)
        val validMoves = getValidMoves(board, currentPlayer).take(10)
        
        if (validMoves.isEmpty()) {
            return evaluateBoard(board, player)
        }
        
        var alphaLocal = alpha
        var betaLocal = beta
        
        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in validMoves) {
                board[move.row][move.col] = currentPlayer
                val eval = minimax(board, depth - 1, alphaLocal, betaLocal, false, player)
                board[move.row][move.col] = Stone.EMPTY
                
                maxEval = max(maxEval, eval)
                alphaLocal = max(alphaLocal, eval)
                if (betaLocal <= alphaLocal) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in validMoves) {
                board[move.row][move.col] = currentPlayer
                val eval = minimax(board, depth - 1, alphaLocal, betaLocal, true, player)
                board[move.row][move.col] = Stone.EMPTY
                
                minEval = min(minEval, eval)
                betaLocal = min(betaLocal, eval)
                if (betaLocal <= alphaLocal) break
            }
            return minEval
        }
    }
    
    private fun getValidMoves(board: Array<Array<Stone>>, player: Stone): List<Position> {
        val moves = mutableListOf<Position>()
        
        for (row in 0 until RenjuRule.BOARD_SIZE) {
            for (col in 0 until RenjuRule.BOARD_SIZE) {
                if (board[row][col] == Stone.EMPTY && hasAdjacentStone(board, row, col)) {
                    if (RenjuRule.isValidMove(board, row, col, player)) {
                        moves.add(Position(row, col))
                    }
                }
            }
        }
        
        if (moves.isEmpty()) {
            val center = RenjuRule.BOARD_SIZE / 2
            if (board[center][center] == Stone.EMPTY) {
                moves.add(Position(center, center))
            }
        }
        
        return moves.sortedByDescending { evaluatePosition(board, it, player) }
    }
    
    private fun hasAdjacentStone(board: Array<Array<Stone>>, row: Int, col: Int): Boolean {
        for (dr in -2..2) {
            for (dc in -2..2) {
                if (dr == 0 && dc == 0) continue
                val r = row + dr
                val c = col + dc
                if (r in 0 until RenjuRule.BOARD_SIZE && c in 0 until RenjuRule.BOARD_SIZE) {
                    if (board[r][c] != Stone.EMPTY) return true
                }
            }
        }
        return false
    }
    
    private fun findImmediateWin(
        board: Array<Array<Stone>>,
        player: Stone,
        validMoves: List<Position>
    ): Position? {
        for (move in validMoves) {
            board[move.row][move.col] = player
            if (RenjuRule.checkWin(board, move.row, move.col, player)) {
                board[move.row][move.col] = Stone.EMPTY
                return move
            }
            board[move.row][move.col] = Stone.EMPTY
        }
        return null
    }
    
    private fun evaluateBoard(board: Array<Array<Stone>>, player: Stone): Int {
        var score = 0
        val opponent = getOpponent(player)
        
        for (row in 0 until RenjuRule.BOARD_SIZE) {
            for (col in 0 until RenjuRule.BOARD_SIZE) {
                if (board[row][col] != Stone.EMPTY) {
                    score += evaluatePoint(board, row, col, player)
                    score -= evaluatePoint(board, row, col, opponent) * 1.1.toInt()
                }
            }
        }
        
        return score
    }
    
    private fun evaluatePosition(board: Array<Array<Stone>>, pos: Position, player: Stone): Int {
        board[pos.row][pos.col] = player
        var score = evaluatePoint(board, pos.row, pos.col, player)
        board[pos.row][pos.col] = Stone.EMPTY
        
        board[pos.row][pos.col] = getOpponent(player)
        score += evaluatePoint(board, pos.row, pos.col, getOpponent(player)) * 0.9.toInt()
        board[pos.row][pos.col] = Stone.EMPTY
        
        val centerDistance = kotlin.math.abs(pos.row - 7) + kotlin.math.abs(pos.col - 7)
        score += (14 - centerDistance) * 2
        
        return score
    }
    
    private fun evaluatePoint(board: Array<Array<Stone>>, row: Int, col: Int, player: Stone): Int {
        if (board[row][col] != player) return 0
        
        var totalScore = 0
        val directions = arrayOf(
            intArrayOf(0, 1),
            intArrayOf(1, 0),
            intArrayOf(1, 1),
            intArrayOf(1, -1)
        )
        
        for (dir in directions) {
            val line = getLine(board, row, col, dir[0], dir[1], player)
            totalScore += evaluateLine(line)
        }
        
        return totalScore
    }
    
    private fun getLine(board: Array<Array<Stone>>, row: Int, col: Int, dr: Int, dc: Int, player: Stone): String {
        val sb = StringBuilder()
        
        for (i in -4..4) {
            val r = row + dr * i
            val c = col + dc * i
            
            when {
                !isValidPos(r, c) -> sb.append('B')
                board[r][c] == Stone.EMPTY -> sb.append('0')
                board[r][c] == player -> sb.append('X')
                else -> sb.append('B')
            }
        }
        
        return sb.toString()
    }
    
    private fun evaluateLine(line: String): Int {
        var score = 0
        
        for ((pattern, value) in patterns) {
            var index = line.indexOf(pattern)
            while (index != -1) {
                score += value
                index = line.indexOf(pattern, index + 1)
            }
        }
        
        return score
    }
    
    private fun isValidPos(row: Int, col: Int): Boolean {
        return row in 0 until RenjuRule.BOARD_SIZE && col in 0 until RenjuRule.BOARD_SIZE
    }
    
    private fun getOpponent(player: Stone): Stone {
        return if (player == Stone.BLACK) Stone.WHITE else Stone.BLACK
    }
}