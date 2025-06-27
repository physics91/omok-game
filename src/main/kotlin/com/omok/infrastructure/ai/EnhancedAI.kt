package com.omok.infrastructure.ai

import com.omok.domain.model.*
import com.omok.domain.service.AIStrategy
import com.omok.domain.service.RuleValidator
import com.omok.domain.ai.mcts.MonteCarloTreeSearch
import com.omok.domain.ai.pattern.PatternRecognizer
import com.omok.domain.ai.evaluation.PositionEvaluator
import com.omok.infrastructure.logging.Logger
import kotlinx.coroutines.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * 향상된 AI 전략
 * MCTS, 패턴 인식, 고급 평가 함수를 결합
 */
class EnhancedAI(
    private val difficulty: AIDifficulty,
    private val ruleValidator: RuleValidator = RuleValidator()
) : AIStrategy {
    
    // AI 컴포넌트
    private val patternRecognizer = PatternRecognizer()
    private val positionEvaluator = PositionEvaluator(patternRecognizer)
    private val mcts = MonteCarloTreeSearch(
        maxIterations = when (difficulty) {
            AIDifficulty.EASY -> 2000
            AIDifficulty.MEDIUM -> 10000
            AIDifficulty.HARD -> 20000
        },
        maxThinkingTimeMs = difficulty.thinkingTimeMs
    )
    
    // MinimaxAI 인스턴스 (폴백용)
    private val minimaxAI = MinimaxAI(difficulty, ruleValidator)
    
    // AI 사고 과정 콜백
    private var thinkingProgressCallback: ((AIThinkingInfo) -> Unit)? = null
    
    override fun getBestMove(board: Board, player: Player): Position? {
        val startTime = System.currentTimeMillis()

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

        // 즉시 승리 또는 방어 수 확인
        val immediateMove = findImmediateMove(board, player)
        if (immediateMove != null) {
            val thinkingTime = System.currentTimeMillis() - startTime
            Logger.info("EnhancedAI", "Immediate move found: $immediateMove in ${thinkingTime}ms")
            return immediateMove
        }

        // 난이도에 따라 알고리즘 선택 (MEDIUM 이상은 항상 고급 알고리즘 사용)
        val useAdvancedAlgorithm = when (difficulty) {
            AIDifficulty.EASY -> false
            else -> true
        }

        return if (useAdvancedAlgorithm) {
            runBlocking {
                getBestMoveAdvanced(board, player, startTime)
            }
        } else {
            // 기존 Minimax 사용
            minimaxAI.getBestMove(board, player)
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
    
    /**
     * 고급 알고리즘을 사용한 최선의 수 계산
     */
    private suspend fun getBestMoveAdvanced(
        board: Board,
        player: Player,
        startTime: Long
    ): Position? = coroutineScope {
        
        // 후보 수 생성
        val candidateMoves = getCandidateMoves(board, player)
        if (candidateMoves.isEmpty()) return@coroutineScope null
        
        // 진행 상황 콜백
        thinkingProgressCallback?.invoke(
            AIThinkingInfo(
                evaluations = emptyList(),
                currentBestMove = null,
                thinkingProgress = 0.0f,
                nodesEvaluated = 0,
                currentDepth = 0
            )
        )
        
        // 병렬로 각 후보 수 평가
        val evaluationJobs = candidateMoves.map { move ->
            async(Dispatchers.Default) {
                evaluateMove(board, move, player)
            }
        }
        
        val evaluations = evaluationJobs.awaitAll()
        
        // MCTS로 추가 분석 (Hard 난이도만)
        val bestMove = if (difficulty == AIDifficulty.HARD && candidateMoves.size > 1) {
            // 상위 5개 후보에 대해 MCTS 실행
            val topCandidates = evaluations
                .sortedByDescending { it.score }
                .take(5)
                .map { it.position }
            
            val mctsEvaluations = topCandidates.map { candidate ->
                async(Dispatchers.Default) {
                    val testBoard = board.placeStone(Move(candidate, player))
                    val mctsResult = mcts.findBestMove(testBoard, player.opponent())
                    candidate to (if (mctsResult != null) evaluatePosition(board, candidate, player) else 0)
                }
            }.awaitAll()
            
            mctsEvaluations.maxByOrNull { it.second }?.first
        } else {
            evaluations.maxByOrNull { it.score }?.position
        }
        
        val thinkingTime = System.currentTimeMillis() - startTime
        
        // 최종 진행 상황 콜백
        thinkingProgressCallback?.invoke(
            AIThinkingInfo(
                evaluations = evaluations.sortedByDescending { it.score }.take(5),
                currentBestMove = bestMove,
                thinkingProgress = 1.0f,
                nodesEvaluated = evaluations.size,
                currentDepth = difficulty.depth
            )
        )
        
        Logger.info("EnhancedAI", "Best move: $bestMove, thinking time: ${thinkingTime}ms")
        return@coroutineScope bestMove
    }
    
    /**
     * 수 평가
     */
    private fun evaluateMove(board: Board, position: Position, player: Player): AIEvaluation {
        val testBoard = board.placeStone(Move(position, player))
        
        // 다양한 평가 요소 결합 (패턴 가중치 상향)
        val patternScore = patternRecognizer.evaluatePosition(board, position, player)
        val positionScore = positionEvaluator.evaluate(testBoard, player)
        val threatScore = evaluateThreat(testBoard, position, player)
        
        // 전체 점수 계산
        val totalScore = (patternScore * 0.5 + positionScore * 0.3 + threatScore * 0.2).toInt()
        
        // 평가 정보 생성
        val description = when {
            totalScore > 100000 -> "승리수"
            totalScore > 50000 -> "매우 유리"
            totalScore > 10000 -> "유리"
            totalScore > 1000 -> "약간 유리"
            totalScore > -1000 -> "균형"
            totalScore > -10000 -> "약간 불리"
            else -> "불리"
        }
        
        return AIEvaluation(
            position = position,
            score = totalScore,
            depth = 0,
            isCandidate = true,
            reason = description
        )
    }
    
    /**
     * 위협 평가
     */
    private fun evaluateThreat(board: Board, lastMove: Position, player: Player): Int {
        var threatScore = 0
        
        // 자신의 위협
        val myThreat = patternRecognizer.evaluateThreatLevel(board, player)
        threatScore += when (myThreat) {
            com.omok.domain.ai.pattern.ThreatLevel.IMMEDIATE_WIN -> 100000
            com.omok.domain.ai.pattern.ThreatLevel.DOUBLE_THREAT -> 50000
            com.omok.domain.ai.pattern.ThreatLevel.SINGLE_THREAT -> 20000
            com.omok.domain.ai.pattern.ThreatLevel.FORCING_SEQUENCE -> 10000
            com.omok.domain.ai.pattern.ThreatLevel.MINOR_THREAT -> 5000
            com.omok.domain.ai.pattern.ThreatLevel.NO_THREAT -> 0
        }
        
        // 상대방 위협 차단
        val opponentThreat = patternRecognizer.evaluateThreatLevel(board, player.opponent())
        threatScore += when (opponentThreat) {
            com.omok.domain.ai.pattern.ThreatLevel.IMMEDIATE_WIN -> -90000
            com.omok.domain.ai.pattern.ThreatLevel.DOUBLE_THREAT -> -45000
            com.omok.domain.ai.pattern.ThreatLevel.SINGLE_THREAT -> -18000
            com.omok.domain.ai.pattern.ThreatLevel.FORCING_SEQUENCE -> -9000
            com.omok.domain.ai.pattern.ThreatLevel.MINOR_THREAT -> -4500
            com.omok.domain.ai.pattern.ThreatLevel.NO_THREAT -> 0
        }
        
        return threatScore
    }
    
    /**
     * 즉시 수를 둬야 하는 상황 확인
     */
    private fun findImmediateMove(board: Board, player: Player): Position? {
        val validMoves = getCandidateMoves(board, player).take(20)
        
        // 즉시 승리 확인
        for (move in validMoves) {
            val testBoard = board.placeStone(Move(move, player))
            val result = ruleValidator.checkWin(testBoard, move, player)
            if (result is GameState.Won) {
                return move
            }
        }
        
        // 상대 승리 차단
        for (move in validMoves) {
            val testBoard = board.placeStone(Move(move, player.opponent()))
            val result = ruleValidator.checkWin(testBoard, move, player.opponent())
            if (result is GameState.Won) {
                return move
            }
        }
        
        return null
    }
    
    /**
     * 후보 수 생성
     */
    private fun getCandidateMoves(board: Board, player: Player): List<Position> {
        val moves = mutableListOf<Position>()
        val occupied = mutableSetOf<Position>()
        
        // 기존 돌 위치 수집
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val position = Position(row, col)
                if (!board.isEmpty(position)) {
                    occupied.add(position)
                }
            }
        }
        
        // 첫 수는 중앙
        if (occupied.isEmpty()) {
            return listOf(Position.center())
        }
        
        // 기존 돌 주변의 빈 칸 찾기
        val candidates = mutableSetOf<Position>()
        for (stone in occupied) {
            for (dr in -2..2) {
                for (dc in -2..2) {
                    if (dr == 0 && dc == 0) continue
                    val newPos = Position(stone.row + dr, stone.col + dc)
                    if (newPos.isValid() && board.isEmpty(newPos)) {
                        if (ruleValidator.isValidMove(board, newPos, player)) {
                            candidates.add(newPos)
                        }
                    }
                }
            }
        }
        
        // 패턴 기반 정렬
        return candidates.sortedByDescending { pos ->
            patternRecognizer.evaluatePosition(board, pos, player)
        }
    }
    
    /**
     * 수 개수 계산
     */
    private fun countMoves(board: Board): Int {
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
    
    /**
     * 위치 평가 (간단 버전)
     */
    private fun evaluatePosition(board: Board, position: Position, player: Player): Int {
        return patternRecognizer.evaluatePosition(board, position, player)
    }
    
    // 스왑 관련 메서드들은 MinimaxAI에 위임
    override fun shouldSwap(board: Board, thirdMove: Position): Boolean {
        return minimaxAI.shouldSwap(board, thirdMove)
    }
    
    override fun proposeFifthMoves(board: Board): List<Position> {
        return minimaxAI.proposeFifthMoves(board)
    }
    
    override fun selectFifthMove(board: Board, proposedMoves: List<Position>): Position? {
        return minimaxAI.selectFifthMove(board, proposedMoves)
    }
    
    override fun setThinkingProgressCallback(callback: (AIThinkingInfo) -> Unit) {
        thinkingProgressCallback = callback
        minimaxAI.setThinkingProgressCallback(callback)
    }
}