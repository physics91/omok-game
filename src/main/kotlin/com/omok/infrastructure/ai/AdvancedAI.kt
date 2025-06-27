package com.omok.infrastructure.ai

import com.omok.domain.model.*
import com.omok.domain.service.AIStrategy
import com.omok.domain.service.RuleValidator
import com.omok.infrastructure.logging.Logger
import kotlin.math.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 고급 AI 전략 - 매우 어려운 난이도를 위한 강화된 알고리즘
 * - 패턴 인식 강화
 * - 전략적 사고 (공격과 방어 균형)
 * - 위협 분석 및 연속 수 계산
 * - 트랜스포지션 테이블 사용
 * - 병렬 처리로 성능 향상
 */
class AdvancedAI(
    private val difficulty: AIDifficulty,
    private val ruleValidator: RuleValidator = RuleValidator()
) : AIStrategy {
    
    // 트랜스포지션 테이블 (이미 계산한 보드 상태 저장)
    private val transpositionTable = ConcurrentHashMap<Long, TranspositionEntry>()
    
    // AI 사고 과정 콜백
    private var thinkingProgressCallback: ((AIThinkingInfo) -> Unit)? = null
    
    // 평가 정보 수집
    private val currentEvaluations = mutableListOf<AIEvaluation>()
    private var nodesEvaluated = 0
    private var currentBestMove: Position? = null
    
    companion object {
        // 점수 체계 - 더 세밀하게 조정
        const val WIN_SCORE = 10000000
        const val FIVE_IN_ROW = 1000000
        const val OPEN_FOUR = 100000      // 양쪽이 열린 4
        const val FOUR = 10000            // 한쪽이 막힌 4
        const val DOUBLE_OPEN_THREE = 50000  // 열린 3-3
        const val OPEN_THREE = 5000      // 양쪽이 열린 3
        const val BLOCKED_THREE = 1000    // 한쪽이 막힌 3
        const val DOUBLE_TWO = 2000       // 2-2 패턴
        const val OPEN_TWO = 200          // 열린 2
        const val BLOCKED_TWO = 50        // 막힌 2
        const val ONE = 10
        
        // 위협 레벨
        const val CRITICAL_THREAT = 100000  // 즉시 대응 필요
        const val HIGH_THREAT = 10000       // 높은 위협
        const val MEDIUM_THREAT = 1000      // 중간 위협
        const val LOW_THREAT = 100          // 낮은 위협
        
        // 고급 패턴 정의
        private val advancedPatterns = listOf(
            // 5개 연속
            Pattern("11111", FIVE_IN_ROW, PatternType.WIN),
            
            // 열린 4 (양쪽이 비어있음)
            Pattern("011110", OPEN_FOUR, PatternType.CRITICAL),
            
            // 한쪽 막힌 4
            Pattern("11110", FOUR, PatternType.HIGH),
            Pattern("01111", FOUR, PatternType.HIGH),
            Pattern("11011", FOUR, PatternType.HIGH),
            Pattern("11101", FOUR, PatternType.HIGH),
            Pattern("10111", FOUR, PatternType.HIGH),
            
            // 열린 3 (공격적)
            Pattern("01110", OPEN_THREE, PatternType.MEDIUM),
            Pattern("010110", OPEN_THREE, PatternType.MEDIUM),
            Pattern("011010", OPEN_THREE, PatternType.MEDIUM),
            
            // 한쪽 막힌 3
            Pattern("1110", BLOCKED_THREE, PatternType.LOW),
            Pattern("0111", BLOCKED_THREE, PatternType.LOW),
            Pattern("1011", BLOCKED_THREE, PatternType.LOW),
            Pattern("1101", BLOCKED_THREE, PatternType.LOW),
            
            // 열린 2
            Pattern("00110", OPEN_TWO, PatternType.LOW),
            Pattern("01100", OPEN_TWO, PatternType.LOW),
            Pattern("01010", OPEN_TWO, PatternType.LOW),
            Pattern("010010", OPEN_TWO, PatternType.LOW),
            
            // 기타 패턴
            Pattern("110", BLOCKED_TWO, PatternType.MINIMAL),
            Pattern("011", BLOCKED_TWO, PatternType.MINIMAL),
            Pattern("101", BLOCKED_TWO, PatternType.MINIMAL),
            Pattern("10", ONE, PatternType.MINIMAL),
            Pattern("01", ONE, PatternType.MINIMAL)
        )
    }
    
    data class Pattern(
        val pattern: String,
        val score: Int,
        val type: PatternType
    )
    
    enum class PatternType {
        WIN, CRITICAL, HIGH, MEDIUM, LOW, MINIMAL
    }
    
    data class TranspositionEntry(
        val depth: Int,
        val score: Int,
        val bestMove: Position?,
        val flag: EntryFlag
    )
    
    enum class EntryFlag {
        EXACT, LOWER_BOUND, UPPER_BOUND
    }
    
    data class ThreatAnalysis(
        val position: Position,
        val threatLevel: Int,
        val isOffensive: Boolean,
        val patterns: List<Pattern>
    )
    
    override fun getBestMove(board: Board, player: Player): Position? {
        return try {
            Logger.info("AdvancedAI", "Getting best move for player: $player")

            // 평가 정보 초기화
            currentEvaluations.clear()
            nodesEvaluated = 0
            currentBestMove = null

            val moveCount = countStones(board)
            val center = Position.center()

            // 오프닝 룰 처리
            when (moveCount) {
                0 -> { // 1수 (흑)
                    Logger.debug("AdvancedAI", "Opening Rule: 1st move, returning center")
                    return center
                }
                1 -> { // 2수 (백)
                    Logger.debug("AdvancedAI", "Opening Rule: 2nd move")
                    val validMoves = getOpeningMoves(board, player, 1)
                    return validMoves.maxByOrNull { evaluatePositionAdvanced(board, it, player) }
                }
                2 -> { // 3수 (흑)
                    Logger.debug("AdvancedAI", "Opening Rule: 3rd move")
                    val validMoves = getOpeningMoves(board, player, 2)
                    return validMoves.maxByOrNull { evaluatePositionAdvanced(board, it, player) }
                }
            }

            // 즉시 승리 가능한 수 확인
            val immediateWin = findWinningMove(board, player)
            if (immediateWin != null) {
                Logger.info("AdvancedAI", "Found immediate winning move at (${immediateWin.row}, ${immediateWin.col})")
                return immediateWin
            }

            // 상대방의 승리를 막아야 하는 수 확인
            val mustBlock = findWinningMove(board, player.opponent())
            if (mustBlock != null) {
                Logger.info("AdvancedAI", "Must block opponent winning move at (${mustBlock.row}, ${mustBlock.col})")
                return mustBlock
            }

            // 위협 분석
            val threats = analyzeThreats(board, player)
            val criticalThreat = threats.firstOrNull { it.threatLevel >= CRITICAL_THREAT }
            if (criticalThreat != null) {
                Logger.info("AdvancedAI", "Found critical threat at (${criticalThreat.position.row}, ${criticalThreat.position.col})")
                return criticalThreat.position
            }

            // 고급 미니맥스 알고리즘 실행
            Logger.debug("AdvancedAI", "Running advanced minimax algorithm")
            runAdvancedMinimax(board, player)
        } catch (e: Exception) {
            Logger.error("AdvancedAI", "Error in getBestMove, falling back to simple strategy", e)
            // 폴백: 간단한 전략으로 안전한 수 찾기
            findSafeMove(board, player)
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
    
    override fun setThinkingProgressCallback(callback: (AIThinkingInfo) -> Unit) {
        thinkingProgressCallback = callback
    }
    
    private fun runAdvancedMinimax(board: Board, player: Player): Position? {
        return try {
            val candidateMoves = getCandidateMoves(board, player)
            if (candidateMoves.isEmpty()) {
                Logger.error("AdvancedAI", "No candidate moves found")
                return null
            }
            
            Logger.debug("AdvancedAI", "Found ${candidateMoves.size} candidate moves")
            
            var bestScore = Int.MIN_VALUE
            var bestMoves = mutableListOf<Position>()
            val depth = when (difficulty) {
                AIDifficulty.EASY -> 3
                AIDifficulty.MEDIUM -> 5
                AIDifficulty.HARD -> 7
            }
            
            Logger.info("AdvancedAI", "Running minimax with depth $depth for difficulty $difficulty")
        
        // 병렬 처리로 각 후보수 평가
        runBlocking {
            val totalMoves = candidateMoves.size
            var evaluatedMoves = 0
            
            val scores = candidateMoves.map { move ->
                async(Dispatchers.Default) {
                    val newBoard = board.placeStone(Move(move, player))
                    val score = alphaBeta(
                        newBoard, 
                        depth - 1, 
                        Int.MIN_VALUE, 
                        Int.MAX_VALUE, 
                        false, 
                        player,
                        move
                    )
                    
                    // 평가 정보 수집
                    synchronized(currentEvaluations) {
                        evaluatedMoves++
                        val evaluation = AIEvaluation(
                            position = move,
                            score = score,
                            depth = depth,
                            reason = getEvaluationReason(score)
                        )
                        currentEvaluations.add(evaluation)
                        
                        // 현재 최고 수 업데이트
                        if (currentBestMove == null || score > currentEvaluations.maxOf { it.score }) {
                            currentBestMove = move
                        }
                        
                        // 진행 상황 콜백
                        thinkingProgressCallback?.invoke(
                            AIThinkingInfo(
                                evaluations = currentEvaluations.toList(),
                                currentBestMove = currentBestMove,
                                thinkingProgress = evaluatedMoves.toFloat() / totalMoves,
                                nodesEvaluated = nodesEvaluated,
                                currentDepth = depth
                            )
                        )
                    }
                    
                    move to score
                }
            }.awaitAll()
            
            scores.forEach { (move, score) ->
                when {
                    score > bestScore -> {
                        bestScore = score
                        bestMoves.clear()
                        bestMoves.add(move)
                    }
                    score == bestScore -> {
                        bestMoves.add(move)
                    }
                }
            }
        }
        
        // 동점인 경우 추가 평가로 최선의 수 선택
        val finalMove = if (bestMoves.size > 1) {
            Logger.debug("AdvancedAI", "Multiple best moves found (${bestMoves.size}), evaluating advanced positions")
            bestMoves.maxByOrNull { evaluatePositionAdvanced(board, it, player) }
        } else {
            bestMoves.firstOrNull()
        }
        
        if (finalMove != null) {
            Logger.info("AdvancedAI", "Selected move: (${finalMove.row}, ${finalMove.col}) with score $bestScore")
        } else {
            Logger.error("AdvancedAI", "Failed to select a move")
        }
        
        finalMove
        } catch (e: Exception) {
            Logger.error("AdvancedAI", "Error in runAdvancedMinimax", e)
            null
        }
    }
    
    private fun alphaBeta(
        board: Board,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        player: Player,
        lastMove: Position
    ): Int {
        // 트랜스포지션 테이블 확인
        val boardHash = board.hashCode().toLong()
        val ttEntry = transpositionTable[boardHash]
        if (ttEntry != null && ttEntry.depth >= depth) {
            when (ttEntry.flag) {
                EntryFlag.EXACT -> return ttEntry.score
                EntryFlag.LOWER_BOUND -> if (ttEntry.score >= beta) return ttEntry.score
                EntryFlag.UPPER_BOUND -> if (ttEntry.score <= alpha) return ttEntry.score
            }
        }
        
        // 종료 조건 확인
        val gameState = ruleValidator.checkWin(board, lastMove, if (isMaximizing) player.opponent() else player)
        if (gameState is GameState.Won || depth == 0) {
            val score = evaluateBoardAdvanced(board, player, depth)
            return score
        }
        
        val currentPlayer = if (isMaximizing) player else player.opponent()
        val moves = getCandidateMoves(board, currentPlayer).take(15) // 상위 15개만 탐색
        
        var bestScore = if (isMaximizing) Int.MIN_VALUE else Int.MAX_VALUE
        var localAlpha = alpha
        var localBeta = beta
        var bestMove: Position? = null
        
        for (move in moves) {
            val newBoard = board.placeStone(Move(move, currentPlayer))
            nodesEvaluated++  // 노드 평가 수 증가
            val score = alphaBeta(newBoard, depth - 1, localAlpha, localBeta, !isMaximizing, player, move)
            
            if (isMaximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
                localAlpha = max(localAlpha, score)
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = move
                }
                localBeta = min(localBeta, score)
            }
            
            if (localBeta <= localAlpha) break // 가지치기
        }
        
        // 트랜스포지션 테이블에 저장
        val flag = when {
            bestScore <= alpha -> EntryFlag.UPPER_BOUND
            bestScore >= beta -> EntryFlag.LOWER_BOUND
            else -> EntryFlag.EXACT
        }
        transpositionTable[boardHash] = TranspositionEntry(depth, bestScore, bestMove, flag)
        
        return bestScore
    }
    
    private fun getCandidateMoves(board: Board, player: Player): List<Position> {
        val moves = mutableListOf<PositionScore>()
        val opponent = player.opponent()
        
        // 모든 빈 칸 중 주변에 돌이 있는 위치만 고려
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && board.hasAdjacentStone(pos, 2)) {
                    // 규칙 검증
                    if (!ruleValidator.isValidMove(board, pos, player)) continue
                    
                    // 각 위치의 점수 계산
                    var score = 0
                    
                    // 공격 점수
                    val attackScore = evaluateMove(board, pos, player)
                    score += attackScore
                    
                    // 방어 점수 (더 높은 가중치)
                    val defenseScore = evaluateMove(board, pos, opponent)
                    score += (defenseScore * 1.2).toInt()
                    
                    // 중앙 선호
                    val centerDistance = abs(pos.row - 7) + abs(pos.col - 7)
                    score += (14 - centerDistance) * 10
                    
                    // 연결성 보너스
                    score += calculateConnectivity(board, pos, player) * 50
                    
                    moves.add(PositionScore(pos, score))
                }
            }
        }
        
        // 점수 순으로 정렬
        return moves.sortedByDescending { it.score }.map { it.position }
    }
    
    private fun evaluateMove(board: Board, position: Position, player: Player): Int {
        val testBoard = board.placeStone(Move(position, player))
        var totalScore = 0
        
        // 4방향 평가
        val directions = listOf(
            Pair(0, 1),   // 가로
            Pair(1, 0),   // 세로
            Pair(1, 1),   // 대각선 \
            Pair(1, -1)   // 대각선 /
        )
        
        for ((dr, dc) in directions) {
            val line = buildLine(testBoard, position, dr, dc, player)
            val patterns = findPatterns(line)
            
            for (pattern in patterns) {
                totalScore += pattern.score
                
                // 특수 패턴 보너스
                if (pattern.type == PatternType.CRITICAL) {
                    totalScore += CRITICAL_THREAT
                } else if (pattern.type == PatternType.HIGH) {
                    totalScore += HIGH_THREAT
                }
            }
        }
        
        // 복합 위협 보너스 (여러 방향에서 동시 위협)
        val threatCount = countThreats(testBoard, position, player)
        if (threatCount >= 2) {
            totalScore += threatCount * MEDIUM_THREAT
        }
        
        return totalScore
    }
    
    private fun buildLine(board: Board, center: Position, dr: Int, dc: Int, player: Player): String {
        val sb = StringBuilder()
        
        // 중심에서 양방향으로 최대 4칸씩
        for (i in -4..4) {
            val row = center.row + dr * i
            val col = center.col + dc * i
            
            if (row !in 0 until Position.BOARD_SIZE || col !in 0 until Position.BOARD_SIZE) {
                sb.append('2') // 벽
            } else {
                val stone = board.getStone(Position(row, col))
                when (stone) {
                    player -> sb.append('1')
                    null -> sb.append('0')
                    else -> sb.append('2') // 상대 돌
                }
            }
        }
        
        return sb.toString()
    }
    
    private fun findPatterns(line: String): List<Pattern> {
        val foundPatterns = mutableListOf<Pattern>()
        
        for (pattern in advancedPatterns) {
            var index = line.indexOf(pattern.pattern)
            while (index != -1) {
                foundPatterns.add(pattern)
                index = line.indexOf(pattern.pattern, index + 1)
            }
        }
        
        return foundPatterns
    }
    
    private fun countThreats(board: Board, position: Position, player: Player): Int {
        var threatCount = 0
        
        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1)
        )
        
        for ((dr, dc) in directions) {
            val line = buildLine(board, position, dr, dc, player)
            val patterns = findPatterns(line)
            
            if (patterns.any { it.type in listOf(PatternType.HIGH, PatternType.CRITICAL) }) {
                threatCount++
            }
        }
        
        return threatCount
    }
    
    private fun calculateConnectivity(board: Board, position: Position, player: Player): Int {
        var connectivity = 0
        
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                
                val adjacent = Position(position.row + dr, position.col + dc)
                if (adjacent.isValid() && board.getStone(adjacent) == player) {
                    connectivity++
                }
            }
        }
        
        return connectivity
    }
    
    private fun evaluateBoardAdvanced(board: Board, player: Player, depth: Int): Int {
        var score = 0
        
        // 각 돌의 위치별 점수 계산
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val pos = Position(row, col)
                val stone = board.getStone(pos)
                
                when (stone) {
                    player -> score += evaluateStonePosition(board, pos, player)
                    null -> {} // 빈 칸은 무시
                    else -> score -= evaluateStonePosition(board, pos, stone) * 1.1.toInt()
                }
            }
        }
        
        // 깊이 보너스 (빠른 승리 선호)
        score += depth * 100
        
        return score
    }
    
    private fun evaluateStonePosition(board: Board, position: Position, player: Player): Int {
        var score = 0
        
        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1)
        )
        
        for ((dr, dc) in directions) {
            val lineScore = evaluateDirection(board, position, dr, dc, player)
            score += lineScore
        }
        
        // 위치 가치
        val centerDistance = abs(position.row - 7) + abs(position.col - 7)
        score += (14 - centerDistance) * 5
        
        return score
    }
    
    private fun evaluateDirection(board: Board, start: Position, dr: Int, dc: Int, player: Player): Int {
        var count = 1
        var openEnds = 0
        var gaps = 0
        
        // 정방향 탐색
        var i = 1
        while (i <= 4) {
            val pos = Position(start.row + dr * i, start.col + dc * i)
            if (!pos.isValid()) break
            
            val stone = board.getStone(pos)
            when (stone) {
                player -> count++
                null -> {
                    val prevPos = Position(start.row + dr * (i - 1), start.col + dc * (i - 1))
                    if (i == 1 || (prevPos.isValid() && board.getStone(prevPos) == player)) {
                        openEnds++
                    }
                    val nextPos = Position(start.row + dr * (i + 1), start.col + dc * (i + 1))
                    if (i < 4 && nextPos.isValid() && board.getStone(nextPos) == player) {
                        gaps++
                        i++ // 갭 건너뛰기
                    } else {
                        break
                    }
                }
                else -> break
            }
            i++
        }
        
        // 역방향 탐색
        i = 1
        while (i <= 4) {
            val pos = Position(start.row - dr * i, start.col - dc * i)
            if (!pos.isValid()) break
            
            val stone = board.getStone(pos)
            when (stone) {
                player -> count++
                null -> {
                    val prevPos = Position(start.row - dr * (i - 1), start.col - dc * (i - 1))
                    if (i == 1 || (prevPos.isValid() && board.getStone(prevPos) == player)) {
                        openEnds++
                    }
                    break
                }
                else -> break
            }
            i++
        }
        
        // 점수 계산
        return when {
            count >= 5 -> WIN_SCORE
            count == 4 && openEnds == 2 -> OPEN_FOUR
            count == 4 && openEnds == 1 -> FOUR
            count == 3 && openEnds == 2 && gaps == 0 -> OPEN_THREE
            count == 3 && openEnds == 1 -> BLOCKED_THREE
            count == 2 && openEnds == 2 -> OPEN_TWO
            count == 2 && openEnds == 1 -> BLOCKED_TWO
            else -> count * ONE
        }
    }
    
    private fun findWinningMove(board: Board, player: Player): Position? {
        Logger.debug("AdvancedAI", "Searching for winning move for player: $player")
        
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && ruleValidator.isValidMove(board, pos, player)) {
                    val testBoard = board.placeStone(Move(pos, player))
                    val gameState = ruleValidator.checkWin(testBoard, pos, player)
                    if (gameState is GameState.Won) {
                        Logger.debug("AdvancedAI", "Found winning move at (${pos.row}, ${pos.col}) for $player")
                        return pos
                    }
                }
            }
        }
        
        Logger.debug("AdvancedAI", "No winning move found for $player")
        return null
    }
    
    private fun analyzeThreats(board: Board, player: Player): List<ThreatAnalysis> {
        Logger.debug("AdvancedAI", "Analyzing threats for player: $player")
        
        val threats = mutableListOf<ThreatAnalysis>()
        
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val pos = Position(row, col)
                if (board.isEmpty(pos)) {
                    // 공격적 위협
                    val offensiveThreat = analyzeThreatAt(board, pos, player, true)
                    if (offensiveThreat.threatLevel > 0) {
                        threats.add(offensiveThreat)
                    }
                    
                    // 방어적 위협
                    val defensiveThreat = analyzeThreatAt(board, pos, player.opponent(), false)
                    if (defensiveThreat.threatLevel > 0) {
                        threats.add(defensiveThreat)
                    }
                }
            }
        }
        
        val sortedThreats = threats.sortedByDescending { it.threatLevel }
        Logger.debug("AdvancedAI", "Found ${sortedThreats.size} threats, highest threat level: ${sortedThreats.firstOrNull()?.threatLevel ?: 0}")
        
        return sortedThreats
    }
    
    private fun analyzeThreatAt(board: Board, position: Position, player: Player, isOffensive: Boolean): ThreatAnalysis {
        if (!ruleValidator.isValidMove(board, position, player)) {
            return ThreatAnalysis(position, 0, isOffensive, emptyList())
        }
        
        val testBoard = board.placeStone(Move(position, player))
        val patterns = mutableListOf<Pattern>()
        var threatLevel = 0
        
        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1)
        )
        
        for ((dr, dc) in directions) {
            val line = buildLine(testBoard, position, dr, dc, player)
            val foundPatterns = findPatterns(line)
            patterns.addAll(foundPatterns)
            
            foundPatterns.forEach { pattern ->
                when (pattern.type) {
                    PatternType.WIN -> threatLevel += WIN_SCORE
                    PatternType.CRITICAL -> threatLevel += CRITICAL_THREAT
                    PatternType.HIGH -> threatLevel += HIGH_THREAT
                    PatternType.MEDIUM -> threatLevel += MEDIUM_THREAT
                    PatternType.LOW -> threatLevel += LOW_THREAT
                    PatternType.MINIMAL -> threatLevel += pattern.score
                }
            }
        }
        
        // 방어는 더 높은 우선순위
        if (!isOffensive) {
            threatLevel = (threatLevel * 1.3).toInt()
        }
        
        return ThreatAnalysis(position, threatLevel, isOffensive, patterns)
    }
    
    private fun evaluatePositionAdvanced(board: Board, position: Position, player: Player): Int {
        var score = evaluateMove(board, position, player)
        
        // 추가 고려사항
        val futureThreats = analyzeFutureThreats(board, position, player)
        score += futureThreats * 100
        
        // 포크 가능성
        val forkPotential = calculateForkPotential(board, position, player)
        score += forkPotential * 500
        
        return score
    }
    
    private fun analyzeFutureThreats(board: Board, position: Position, player: Player): Int {
        val testBoard = board.placeStone(Move(position, player))
        var futureThreats = 0
        
        // 이 수를 둔 후 만들어질 수 있는 위협들
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val pos = Position(row, col)
                if (testBoard.isEmpty(pos)) {
                    val threat = analyzeThreatAt(testBoard, pos, player, true)
                    if (threat.threatLevel >= MEDIUM_THREAT) {
                        futureThreats++
                    }
                }
            }
        }
        
        return futureThreats
    }
    
    private fun calculateForkPotential(board: Board, position: Position, player: Player): Int {
        val testBoard = board.placeStone(Move(position, player))
        var openThrees = 0
        
        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1)
        )
        
        for ((dr, dc) in directions) {
            val line = buildLine(testBoard, position, dr, dc, player)
            if (line.contains("01110") || line.contains("010110") || line.contains("011010")) {
                openThrees++
            }
        }
        
        // 2개 이상의 열린 3이 만들어지면 포크
        return if (openThrees >= 2) openThrees else 0
    }
    
    /**
     * 안전한 수를 찾는 폴백 메서드
     */
    private fun findSafeMove(board: Board, player: Player): Position? {
        Logger.debug("AdvancedAI", "Using safe move fallback strategy")
        
        // 중앙 근처의 유효한 수 찾기
        val center = Position.center()
        for (radius in 1..7) {
            for (dr in -radius..radius) {
                for (dc in -radius..radius) {
                    if (kotlin.math.abs(dr) == radius || kotlin.math.abs(dc) == radius) {
                        val pos = Position(center.row + dr, center.col + dc)
                        if (pos.isValid() && board.isEmpty(pos) && 
                            ruleValidator.isValidMove(board, pos, player)) {
                            Logger.debug("AdvancedAI", "Found safe move at (${pos.row}, ${pos.col})")
                            return pos
                        }
                    }
                }
            }
        }
        
        // 마지막 수단: 첫 번째 유효한 빈 칸
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && ruleValidator.isValidMove(board, pos, player)) {
                    Logger.debug("AdvancedAI", "Found fallback move at (${pos.row}, ${pos.col})")
                    return pos
                }
            }
        }
        
        Logger.error("AdvancedAI", "No valid moves found")
        return null
    }
    
    data class PositionScore(val position: Position, val score: Int)
    
    /**
     * AI가 스왑(흑백 교체)를 할지 결정 - 고급 전략
     */
    override fun shouldSwap(board: Board, thirdMove: Position): Boolean {
        Logger.info("AdvancedAI", "Evaluating swap decision for third move at (${thirdMove.row}, ${thirdMove.col})")
        
        // 현재 보드 상태를 정밀 분석
        val blackAdvantage = analyzeOpeningAdvantage(board)
        
        // 난이도별 스왑 전략
        val swapProbability = when (difficulty) {
            AIDifficulty.EASY -> {
                // 쉬운 난이도는 기본적인 평가
                if (blackAdvantage > 150) 0.3 else 0.1
            }
            AIDifficulty.MEDIUM -> {
                // 중간 난이도는 중간 수준 평가
                if (blackAdvantage > 100) 0.6 else 0.3
            }
            AIDifficulty.HARD -> {
                // 하드 난이도는 정밀한 평가
                when {
                    blackAdvantage > 200 -> 0.95
                    blackAdvantage > 100 -> 0.8
                    blackAdvantage > 0 -> 0.6
                    else -> 0.3
                }
            }
        }
        
        val shouldSwap = kotlin.random.Random.nextDouble() < swapProbability
        Logger.info("AdvancedAI", "Black advantage: $blackAdvantage, swap probability: $swapProbability, decision: $shouldSwap")
        
        return shouldSwap
    }
    
    /**
     * 오프닝 우위 분석
     */
    private fun analyzeOpeningAdvantage(board: Board): Int {
        val stones = mutableListOf<Position>()
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val position = Position(row, col)
                if (!board.isEmpty(position)) {
                    stones.add(position)
                }
            }
        }
        
        if (stones.size != 3) return 0
        
        val center = Position.center()
        var advantage = 0
        
        // 중앙 제어 평가
        stones.forEach { stone ->
            val distance = kotlin.math.abs(stone.row - center.row) + kotlin.math.abs(stone.col - center.col)
            advantage += when (distance) {
                0 -> 100  // 천원
                1 -> 50   // 천원 인접
                2 -> 30   // 근거리
                else -> 10
            }
        }
        
        // 패턴 형태 평가
        if (stones.size == 3) {
            val pattern = analyzeThreeStonePattern(stones)
            advantage += pattern.advantageScore
        }
        
        return advantage
    }
    
    private data class ThreeStonePattern(
        val type: String,
        val advantageScore: Int
    )
    
    private fun analyzeThreeStonePattern(stones: List<Position>): ThreeStonePattern {
        // 3개 돌의 상대적 위치로 패턴 분석
        val sorted = stones.sortedWith(compareBy({ it.row }, { it.col }))
        
        // 직선 패턴 확인
        val isHorizontal = sorted.all { it.row == sorted[0].row }
        val isVertical = sorted.all { it.col == sorted[0].col }
        val isDiagonal = sorted.zipWithNext().all { (a, b) -> 
            b.row - a.row == b.col - a.col || b.row - a.row == -(b.col - a.col)
        }
        
        return when {
            isHorizontal || isVertical -> ThreeStonePattern("직선", 150)
            isDiagonal -> ThreeStonePattern("대각선", 120)
            else -> ThreeStonePattern("분산", 80)
        }
    }
    
    /**
     * AI가 5수로 제시할 두 위치를 선택 - 고급 전략
     */
    override fun proposeFifthMoves(board: Board): List<Position> {
        Logger.info("AdvancedAI", "Proposing fifth moves")
        
        val candidates = findFifthMoveCandidates(board)
        
        if (candidates.size < 2) {
            Logger.warn("AdvancedAI", "Not enough candidates for fifth moves")
            return candidates.take(2)
        }
        
        // 난이도별 전략
        return when (difficulty) {
            AIDifficulty.EASY -> {
                // 쉬운 난이도: 무작위 선택
                candidates.shuffled().take(2)
            }
            AIDifficulty.MEDIUM -> {
                // 중간 난이도: 균형잡힌 선택
                selectBalancedFifthMoves(candidates)
            }
            AIDifficulty.HARD -> {
                // 하드 난이도: 전략적 선택 (흑에게 불리한 수)
                selectStrategicFifthMoves(board, candidates)
            }
        }
    }
    
    private fun findFifthMoveCandidates(board: Board): List<Position> {
        val candidates = mutableListOf<Position>()
        val center = Position.center()
        
        // 중앙에서 가까운 순으로 후보 탐색
        for (distance in 1..5) {
            for (row in (center.row - distance)..(center.row + distance)) {
                for (col in (center.col - distance)..(center.col + distance)) {
                    val pos = Position(row, col)
                    if (pos.isValid() && board.isEmpty(pos) && 
                        ruleValidator.isValidMove(board, pos, Player.BLACK)) {
                        candidates.add(pos)
                    }
                }
            }
            if (candidates.size >= 20) break
        }
        
        return candidates
    }
    
    private fun selectBalancedFifthMoves(candidates: List<Position>): List<Position> {
        if (candidates.size < 2) return candidates
        
        val first = candidates.random()
        val second = candidates.filter { pos ->
            val distance = kotlin.math.abs(pos.row - first.row) + kotlin.math.abs(pos.col - first.col)
            distance in 3..5  // 적당한 거리
        }.randomOrNull() ?: candidates.filter { it != first }.random()
        
        return listOf(first, second)
    }
    
    private fun selectStrategicFifthMoves(board: Board, candidates: List<Position>): List<Position> {
        // 각 후보의 점수를 계산 (낮을수록 흑에게 불리)
        val scored = candidates.map { pos ->
            val testBoard = board.placeStone(Move(pos, Player.BLACK))
            // 간단한 평가: 주변 연결 가능성 계산
            val score = evaluateMoveStrength(testBoard, pos, Player.BLACK)
            pos to score
        }.sortedBy { it.second }
        
        // 가장 불리한 2개 선택
        return scored.take(2).map { it.first }
    }
    
    private fun evaluateMoveStrength(board: Board, position: Position, player: Player): Int {
        var score = 0
        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1)
        )
        
        for ((dr, dc) in directions) {
            val line = buildLine(board, position, dr, dc, player)
            val patterns = findPatterns(line)
            
            for (pattern in patterns) {
                score += pattern.score
            }
        }
        
        return score
    }
    
    /**
     * AI가 제시된 5수 중 하나를 선택 - 고급 전략
     */
    override fun selectFifthMove(board: Board, proposedMoves: List<Position>): Position? {
        if (proposedMoves.isEmpty()) return null
        if (proposedMoves.size == 1) return proposedMoves[0]
        
        Logger.info("AdvancedAI", "Selecting from proposed fifth moves: ${proposedMoves.map { "(${it.row}, ${it.col})" }}")
        
        // 각 수의 평가 (백의 관점에서)
        val evaluations = proposedMoves.map { pos ->
            val testBoard = board.placeStone(Move(pos, Player.BLACK))
            // 간단한 평가: 흑의 공격력 vs 백의 방어 기회
            val blackStrength = evaluateMoveStrength(testBoard, pos, Player.BLACK)
            val whiteOpportunity = evaluateDefenseOpportunity(testBoard, Player.WHITE)
            val advantage = whiteOpportunity - blackStrength  // 백의 우위
            
            Logger.debug("AdvancedAI", "Position (${pos.row}, ${pos.col}): advantage=$advantage")
            pos to advantage
        }
        
        // 난이도별 선택
        return when (difficulty) {
            AIDifficulty.EASY -> {
                // 쉬운 난이도: 무작위 선택
                proposedMoves.random()
            }
            AIDifficulty.MEDIUM -> {
                // 중간 난이도: 중간 정도 유리한 수 선택
                evaluations.sortedByDescending { it.second }[evaluations.size / 2].first
            }
            AIDifficulty.HARD -> {
                // 하드 난이도: 백에게 가장 유리한 수 선택
                evaluations.maxByOrNull { it.second }?.first ?: proposedMoves[0]
            }
        }
    }
    
    private fun evaluateDefenseOpportunity(board: Board, player: Player): Int {
        var opportunity = 0
        
        // 보드 전체에서 백이 놓을 수 있는 좋은 위치 계산
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && board.hasAdjacentStone(pos)) {
                    val testBoard = board.placeStone(Move(pos, player))
                    opportunity += countThreats(testBoard, pos, player) * 100
                }
            }
        }
        
        return opportunity
    }
    
    /**
     * 평가 점수에 따른 이유 설명
     */
    private fun getEvaluationReason(score: Int): String {
        return when {
            score >= WIN_SCORE -> "필승수"
            score >= FIVE_IN_ROW -> "5리늑 달성"
            score >= OPEN_FOUR -> "양끝 열린 4"
            score >= DOUBLE_OPEN_THREE -> "열린 3-3"
            score >= FOUR -> "4 만들기"
            score >= OPEN_THREE -> "열린 3"
            score >= DOUBLE_TWO -> "2-2 패턴"
            score >= BLOCKED_THREE -> "막힌 3"
            score >= OPEN_TWO -> "열린 2"
            score <= -WIN_SCORE -> "필패 방어"
            score <= -OPEN_FOUR -> "상대 4 방어"
            score <= -OPEN_THREE -> "상대 3 방어"
            else -> "일반 수"
        }
    }
}