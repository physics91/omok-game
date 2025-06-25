package com.omok.infrastructure.ai

import com.omok.domain.model.*
import com.omok.domain.service.AIStrategy
import com.omok.domain.service.RuleValidator
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MinimaxAI(
    private val difficulty: AIDifficulty,
    private val ruleValidator: RuleValidator = RuleValidator()
) : AIStrategy {
    
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
    
    override fun getBestMove(board: Board, player: Player): Position? {
        val validMoves = getValidMoves(board, player)
        if (validMoves.isEmpty()) return null
        
        if (difficulty.randomness > 0 && Random.nextDouble() < difficulty.randomness) {
            return validMoves.random()
        }
        
        val immediateWin = findImmediateWin(board, player, validMoves)
        if (immediateWin != null) return immediateWin
        
        val blockOpponentWin = findImmediateWin(board, player.opponent(), validMoves)
        if (blockOpponentWin != null) return blockOpponentWin
        
        var bestScore = Int.MIN_VALUE
        var bestMoves = mutableListOf<Position>()
        
        for (move in validMoves) {
            val newBoard = board.placeStone(Move(move, player))
            val score = minimax(newBoard, difficulty.depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false, player)
            
            if (score > bestScore) {
                bestScore = score
                bestMoves.clear()
                bestMoves.add(move)
            } else if (score == bestScore) {
                bestMoves.add(move)
            }
        }
        
        return if (bestMoves.isNotEmpty()) {
            bestMoves.maxByOrNull { evaluatePosition(board, it, player) }
        } else {
            validMoves.firstOrNull()
        }
    }
    
    private fun minimax(
        board: Board,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        player: Player
    ): Int {
        if (depth == 0) {
            return evaluateBoard(board, player)
        }
        
        val currentPlayer = if (isMaximizing) player else player.opponent()
        val validMoves = getValidMoves(board, currentPlayer).take(10)
        
        if (validMoves.isEmpty()) {
            return evaluateBoard(board, player)
        }
        
        var alphaLocal = alpha
        var betaLocal = beta
        
        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in validMoves) {
                val newBoard = board.placeStone(Move(move, currentPlayer))
                val eval = minimax(newBoard, depth - 1, alphaLocal, betaLocal, false, player)
                
                maxEval = max(maxEval, eval)
                alphaLocal = max(alphaLocal, eval)
                if (betaLocal <= alphaLocal) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in validMoves) {
                val newBoard = board.placeStone(Move(move, currentPlayer))
                val eval = minimax(newBoard, depth - 1, alphaLocal, betaLocal, true, player)
                
                minEval = min(minEval, eval)
                betaLocal = min(betaLocal, eval)
                if (betaLocal <= alphaLocal) break
            }
            return minEval
        }
    }
    
    private fun getValidMoves(board: Board, player: Player): List<Position> {
        val moves = mutableListOf<Position>()
        
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val position = Position(row, col)
                if (board.isEmpty(position) && board.hasAdjacentStone(position)) {
                    if (ruleValidator.isValidMove(board, position, player)) {
                        moves.add(position)
                    }
                }
            }
        }
        
        if (moves.isEmpty() && board.isEmpty(Position.center())) {
            moves.add(Position.center())
        }
        
        return moves.sortedByDescending { evaluatePosition(board, it, player) }
    }
    
    private fun findImmediateWin(
        board: Board,
        player: Player,
        validMoves: List<Position>
    ): Position? {
        for (move in validMoves) {
            val testBoard = board.placeStone(Move(move, player))
            val result = ruleValidator.checkWin(testBoard, move, player)
            if (result is GameState.Won) {
                return move
            }
        }
        return null
    }
    
    private fun evaluateBoard(board: Board, player: Player): Int {
        var score = 0
        val opponent = player.opponent()
        
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val position = Position(row, col)
                val stone = board.getStone(position)
                
                if (stone != null) {
                    if (stone == player) {
                        score += evaluatePoint(board, position, player)
                    } else {
                        score -= (evaluatePoint(board, position, opponent) * 1.1).toInt()
                    }
                }
            }
        }
        
        return score
    }
    
    private fun evaluatePosition(board: Board, position: Position, player: Player): Int {
        val testBoard = board.placeStone(Move(position, player))
        var score = evaluatePoint(testBoard, position, player)
        
        val opponentTestBoard = board.placeStone(Move(position, player.opponent()))
        score += (evaluatePoint(opponentTestBoard, position, player.opponent()) * 0.9).toInt()
        
        val centerDistance = kotlin.math.abs(position.row - 7) + kotlin.math.abs(position.col - 7)
        score += (14 - centerDistance) * 2
        
        return score
    }
    
    private fun evaluatePoint(board: Board, position: Position, player: Player): Int {
        if (board.getStone(position) != player) return 0
        
        var totalScore = 0
        
        val directions = arrayOf(
            intArrayOf(0, 1),   // 가로
            intArrayOf(1, 0),   // 세로  
            intArrayOf(1, 1),   // 대각선 \
            intArrayOf(1, -1)   // 대각선 /
        )
        
        for (dir in directions) {
            val line = getLine(board, position, dir[0], dir[1], player)
            totalScore += evaluateLine(line)
        }
        
        return totalScore
    }
    
    private fun getLine(board: Board, position: Position, dr: Int, dc: Int, player: Player): String {
        val sb = StringBuilder()
        
        for (i in -4..4) {
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
}