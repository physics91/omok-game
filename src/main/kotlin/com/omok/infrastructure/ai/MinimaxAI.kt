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
    
    // AI 사고 과정 콜백
    private var thinkingProgressCallback: ((AIThinkingInfo) -> Unit)? = null
    
    // 평가 정보 수집
    private val currentEvaluations = mutableListOf<AIEvaluation>()
    private var nodesEvaluated = 0
    private var currentBestMove: Position? = null
    
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
        val moveCount = countStones(board)
        val center = Position.center()

        // 오프닝 룰 처리
        when (moveCount) {
            0 -> return center // 1수 (흑)
            1 -> { // 2수 (백)
                val validMoves = getOpeningMoves(board, player, 1)
                return validMoves.maxByOrNull { evaluatePosition(board, it, player) }
            }
            2 -> { // 3수 (흑)
                val validMoves = getOpeningMoves(board, player, 2)
                return validMoves.maxByOrNull { evaluatePosition(board, it, player) }
            }
        }

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

    private fun countStones(board: Board): Int {
        var count = 0
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                if (!board.isEmpty(Position(row, col))) {
                    count++
                }
            }
        }
        return count
    }

    private fun getOpeningMoves(board: Board, player: Player, radius: Int): List<Position> {
        val moves = mutableListOf<Position>()
        val center = Position.center()
        for (row in (center.row - radius)..(center.row + radius)) {
            for (col in (center.col - radius)..(center.col + radius)) {
                val pos = Position(row, col)
                if (pos.isValid() && board.isEmpty(pos) && pos != center) {
                    if (ruleValidator.isValidMove(board, pos, player)) {
                        moves.add(pos)
                    }
                }
            }
        }
        return moves
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
    
    /**
     * AI가 스왑(흑백 교체)를 할지 결정
     * 현재 보드 상태를 평가하여 백이 더 유리하다고 판단되면 스왑
     */
    override fun shouldSwap(board: Board, thirdMove: Position): Boolean {
        // 보드의 현재 상태를 평가
        val blackScore = evaluateBoard(board, Player.BLACK)
        val whiteScore = evaluateBoard(board, Player.WHITE)
        
        // 3수까지의 패턴을 분석
        val stones = mutableListOf<Position>()
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val position = Position(row, col)
                if (!board.isEmpty(position)) {
                    stones.add(position)
                }
            }
        }
        
        // 중앙에서의 거리와 패턴 분석
        val center = Position.center()
        val pattern = analyzeOpeningPattern(stones)
        
        // 난이도에 따른 스왑 결정
        return when (difficulty) {
            AIDifficulty.EASY -> {
                // 쉬운 난이도는 거의 스왑하지 않음
                Random.nextDouble() < 0.1
            }
            AIDifficulty.MEDIUM -> {
                // 중간 난이도는 패턴 기반으로 결정
                pattern.isAdvantageousForBlack && Random.nextDouble() < 0.7
            }
            AIDifficulty.HARD -> {
                // 어려운 난이도는 정확한 평가 기반
                pattern.isAdvantageousForBlack || blackScore > whiteScore + 50
            }
        }
    }
    
    /**
     * AI가 5수로 제시할 두 위치를 선택
     */
    override fun proposeFifthMoves(board: Board): List<Position> {
        val validMoves = getValidMovesForFifthMove(board)
        
        if (validMoves.size < 2) {
            // 유효한 수가 부족하면 가능한 수 반환
            return validMoves.take(2)
        }
        
        // 난이도에 따른 5수 제시 전략
        return when (difficulty) {
            AIDifficulty.EASY -> {
                // 쉬운 난이도는 무작위로 선택
                validMoves.shuffled().take(2)
            }
            AIDifficulty.MEDIUM -> {
                // 중간 난이도는 균형잡힌 선택
                val balanced = selectBalancedFifthMoves(board, validMoves)
                if (balanced.size >= 2) balanced else validMoves.take(2)
            }
            AIDifficulty.HARD -> {
                // 어려운 난이도는 전략적 선택
                val strategic = selectStrategicFifthMoves(board, validMoves)
                if (strategic.size >= 2) strategic else validMoves.take(2)
            }
        }
    }
    
    /**
     * AI가 제시된 5수 중 하나를 선택
     */
    override fun selectFifthMove(board: Board, proposedMoves: List<Position>): Position? {
        if (proposedMoves.isEmpty()) return null
        if (proposedMoves.size == 1) return proposedMoves[0]
        
        // 각 수의 평가점수 계산
        val scores = proposedMoves.map { move ->
            move to evaluatePosition(board, move, Player.BLACK)
        }
        
        // 난이도에 따른 선택
        return when (difficulty) {
            AIDifficulty.EASY -> {
                // 쉬운 난이도는 무작위 선택
                proposedMoves.random()
            }
            AIDifficulty.MEDIUM -> {
                // 중간 난이도는 점수가 낮은 수를 선호
                scores.minByOrNull { it.second }?.first ?: proposedMoves[0]
            }
            AIDifficulty.HARD -> {
                // 어려운 난이도는 점수가 낮은 수를 선택 (흑에게 불리한 수)
                scores.minByOrNull { it.second }?.first ?: proposedMoves[0]
            }
        }
    }
    
    /**
     * 오프닝 패턴 분석
     */
    private data class OpeningPattern(
        val isAdvantageousForBlack: Boolean,
        val symmetry: Double,
        val centerControl: Double
    )
    
    private fun analyzeOpeningPattern(stones: List<Position>): OpeningPattern {
        if (stones.size != 3) {
            return OpeningPattern(false, 0.0, 0.0)
        }
        
        val center = Position.center()
        
        // 대칭성 분석
        var symmetry = 0.0
        for (stone in stones) {
            val mirrorH = Position(stone.row, 14 - stone.col)
            val mirrorV = Position(14 - stone.row, stone.col)
            val mirrorD = Position(14 - stone.row, 14 - stone.col)
            
            if (stones.any { it == mirrorH || it == mirrorV || it == mirrorD }) {
                symmetry += 0.33
            }
        }
        
        // 중앙 제어력 분석
        val centerControl = stones.map { stone ->
            val distance = kotlin.math.abs(stone.row - center.row) + kotlin.math.abs(stone.col - center.col)
            1.0 / (distance + 1)
        }.average()
        
        // 흑에게 유리한 패턴인지 판단
        val isAdvantageousForBlack = centerControl > 0.3 && symmetry < 0.5
        
        return OpeningPattern(isAdvantageousForBlack, symmetry, centerControl)
    }
    
    /**
     * 5수를 위한 유효한 수 찾기
     */
    private fun getValidMovesForFifthMove(board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val center = Position.center()
        
        // 중앙 근처의 빈 칸들을 우선 고려
        for (distance in 1..7) {
            for (row in (center.row - distance)..(center.row + distance)) {
                for (col in (center.col - distance)..(center.col + distance)) {
                    val position = Position(row, col)
                    if (position.isValid() && board.isEmpty(position)) {
                        // 5수는 흑이 두는 것이므로 흑 기준으로 검증
                        if (ruleValidator.isValidMove(board, position, Player.BLACK)) {
                            moves.add(position)
                        }
                    }
                }
            }
            
            if (moves.size >= 10) break // 충분한 후보가 있으면 중단
        }
        
        return moves
    }
    
    /**
     * 균형잡힌 5수 선택 (중간 난이도)
     */
    private fun selectBalancedFifthMoves(board: Board, validMoves: List<Position>): List<Position> {
        // 너무 가깝지도, 너무 멀지도 않은 두 수를 선택
        val selected = mutableListOf<Position>()
        
        if (validMoves.isNotEmpty()) {
            selected.add(validMoves.random())
            
            // 첫 번째 수와 적당한 거리의 두 번째 수 선택
            val secondMove = validMoves.filter { move ->
                val distance = kotlin.math.abs(move.row - selected[0].row) + 
                             kotlin.math.abs(move.col - selected[0].col)
                distance in 2..4
            }.randomOrNull() ?: validMoves.filter { it != selected[0] }.randomOrNull()
            
            if (secondMove != null) {
                selected.add(secondMove)
            }
        }
        
        return selected
    }
    
    /**
     * 전략적 5수 선택 (어려운 난이도)
     */
    private fun selectStrategicFifthMoves(board: Board, validMoves: List<Position>): List<Position> {
        // 흑에게 가장 불리한 두 수를 선택
        val evaluated = validMoves.map { move ->
            move to evaluatePosition(board, move, Player.BLACK)
        }.sortedBy { it.second }
        
        return evaluated.take(2).map { it.first }
    }
    
    override fun setThinkingProgressCallback(callback: (AIThinkingInfo) -> Unit) {
        thinkingProgressCallback = callback
    }
}