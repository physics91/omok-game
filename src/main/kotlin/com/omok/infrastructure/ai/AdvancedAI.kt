package com.omok.infrastructure.ai

import com.omok.domain.model.*
import com.omok.domain.service.AIStrategy
import com.omok.domain.service.RuleValidator
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
        // 첫 수는 중앙에
        if (board.isEmpty(Position.center())) {
            return Position.center()
        }
        
        // 즉시 승리 가능한 수 확인
        val immediateWin = findWinningMove(board, player)
        if (immediateWin != null) return immediateWin
        
        // 상대방의 승리를 막아야 하는 수 확인
        val mustBlock = findWinningMove(board, player.opponent())
        if (mustBlock != null) return mustBlock
        
        // 위협 분석
        val threats = analyzeThreats(board, player)
        val criticalThreat = threats.firstOrNull { it.threatLevel >= CRITICAL_THREAT }
        if (criticalThreat != null) return criticalThreat.position
        
        // 고급 미니맥스 알고리즘 실행
        return runAdvancedMinimax(board, player)
    }
    
    private fun runAdvancedMinimax(board: Board, player: Player): Position? {
        val candidateMoves = getCandidateMoves(board, player)
        if (candidateMoves.isEmpty()) return null
        
        var bestScore = Int.MIN_VALUE
        var bestMoves = mutableListOf<Position>()
        val depth = when (difficulty) {
            AIDifficulty.EASY -> 3
            AIDifficulty.MEDIUM -> 5
            AIDifficulty.HARD -> 7
        }
        
        // 병렬 처리로 각 후보수 평가
        runBlocking {
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
        return if (bestMoves.size > 1) {
            bestMoves.maxByOrNull { evaluatePositionAdvanced(board, it, player) }
        } else {
            bestMoves.firstOrNull()
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
                    if (i == 1 || board.getStone(Position(start.row + dr * (i - 1), start.col + dc * (i - 1))) == player) {
                        openEnds++
                    }
                    if (i < 4 && board.getStone(Position(start.row + dr * (i + 1), start.col + dc * (i + 1))) == player) {
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
                    if (i == 1 || board.getStone(Position(start.row - dr * (i - 1), start.col - dc * (i - 1))) == player) {
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
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && ruleValidator.isValidMove(board, pos, player)) {
                    val testBoard = board.placeStone(Move(pos, player))
                    val gameState = ruleValidator.checkWin(testBoard, pos, player)
                    if (gameState is GameState.Won) {
                        return pos
                    }
                }
            }
        }
        return null
    }
    
    private fun analyzeThreats(board: Board, player: Player): List<ThreatAnalysis> {
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
        
        return threats.sortedByDescending { it.threatLevel }
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
    
    data class PositionScore(val position: Position, val score: Int)
}