package com.omok.domain.analysis

import com.omok.domain.model.*
import com.omok.domain.service.RuleValidator
import kotlin.math.abs
import kotlin.math.max

/**
 * 게임 분석 엔진
 * 각 수의 품질을 평가하고 형세를 판단
 */
class GameAnalyzer(
    private val ruleValidator: RuleValidator = RuleValidator()
) {
    
    /**
     * 수의 품질 평가
     */
    enum class MoveQuality(val displayName: String, val color: java.awt.Color) {
        BRILLIANT("명수!", java.awt.Color(0, 200, 83)),      // 최고의 수
        EXCELLENT("호수", java.awt.Color(34, 197, 94)),     // 매우 좋은 수
        GOOD("좋은 수", java.awt.Color(99, 102, 241)),      // 좋은 수
        NORMAL("보통", java.awt.Color(156, 163, 175)),      // 평범한 수
        DUBIOUS("의문수", java.awt.Color(251, 146, 60)),    // 의심스러운 수
        MISTAKE("실수", java.awt.Color(239, 68, 68)),       // 실수
        BLUNDER("대실수!", java.awt.Color(220, 38, 38))     // 치명적 실수
    }
    
    /**
     * 게임 분석 결과
     */
    data class GameAnalysisResult(
        val moveAnalyses: List<MoveAnalysis>,
        val positionEvaluations: List<PositionEvaluation>,
        val turningPoints: List<TurningPoint>,
        val summary: GameSummary
    )
    
    /**
     * 각 수의 분석 결과
     */
    data class MoveAnalysis(
        val moveNumber: Int,
        val move: Move,
        val quality: MoveQuality,
        val evaluation: Int,           // 평가 점수
        val bestMove: Position?,       // 추천 수
        val explanation: String,       // 설명
        val threats: List<Threat>,     // 위협 요소
        val opportunities: List<Position> // 기회 요소
    )
    
    /**
     * 포지션 평가
     */
    data class PositionEvaluation(
        val moveNumber: Int,
        val blackAdvantage: Int,  // 흑의 이득 (양수면 흑 유리, 음수면 백 유리)
        val criticalPoints: List<Position>, // 중요 지점
        val blackThreats: Int,    // 흑의 위협 수
        val whiteThreats: Int     // 백의 위협 수
    )
    
    /**
     * 전환점
     */
    data class TurningPoint(
        val moveNumber: Int,
        val description: String,
        val evaluationChange: Int  // 평가 변화량
    )
    
    /**
     * 게임 요약
     */
    data class GameSummary(
        val totalMoves: Int,
        val blackMistakes: Int,
        val whiteMistakes: Int,
        val blackBlunders: Int,
        val whiteBlunders: Int,
        val accuracy: Map<Player, Double>, // 정확도 (%)
        val averageTimePerMove: Map<Player, Long>, // 평균 사고시간
        val keyMoments: List<Int> // 중요한 순간들 (수 번호)
    )
    
    /**
     * 위협 요소
     */
    data class Threat(
        val type: ThreatType,
        val positions: List<Position>,
        val severity: Int  // 1-10
    )
    
    enum class ThreatType {
        WIN_IN_1,      // 1수 승리
        WIN_IN_3,      // 3수 내 승리
        FOUR,          // 4목
        OPEN_THREE,    // 열린 3
        DOUBLE_THREE,  // 33
        COMPLEX        // 복합 위협
    }
    
    /**
     * 전체 게임 분석
     */
    fun analyzeGame(game: Game): GameAnalysisResult {
        val moveAnalyses = mutableListOf<MoveAnalysis>()
        val positionEvaluations = mutableListOf<PositionEvaluation>()
        val turningPoints = mutableListOf<TurningPoint>()
        
        // 빈 보드부터 시작하여 수를 하나씩 놓으며 분석
        var currentBoard = Board()
        var previousEval = 0
        
        game.getMoveHistory().forEachIndexed { index, move ->
            // 현재 보드 상태 평가
            val positionEval = evaluatePosition(currentBoard, move.player)
            positionEvaluations.add(
                PositionEvaluation(
                    moveNumber = index + 1,
                    blackAdvantage = positionEval,
                    criticalPoints = findCriticalPoints(currentBoard, move.player),
                    blackThreats = countThreats(currentBoard, Player.BLACK),
                    whiteThreats = countThreats(currentBoard, Player.WHITE)
                )
            )
            
            // 수 품질 평가
            val moveAnalysis = analyzeMove(currentBoard, move, previousEval, positionEval)
            moveAnalyses.add(moveAnalysis)
            
            // 전환점 감지
            val evalChange = abs(positionEval - previousEval)
            if (evalChange > 5000) { // 큰 평가 변화
                turningPoints.add(
                    TurningPoint(
                        moveNumber = index + 1,
                        description = describeTurningPoint(move, evalChange, positionEval),
                        evaluationChange = evalChange
                    )
                )
            }
            
            // 보드 업데이트
            currentBoard = currentBoard.placeStone(move)
            previousEval = positionEval
        }
        
        // 게임 요약 생성
        val summary = createGameSummary(game, moveAnalyses)
        
        return GameAnalysisResult(
            moveAnalyses = moveAnalyses,
            positionEvaluations = positionEvaluations,
            turningPoints = turningPoints,
            summary = summary
        )
    }
    
    /**
     * 단일 수 분석
     */
    private fun analyzeMove(
        board: Board, 
        move: Move, 
        previousEval: Int, 
        currentEval: Int
    ): MoveAnalysis {
        // 최선의 수 찾기
        val bestMove = findBestMove(board, move.player)
        val bestMoveEval = if (bestMove != null) {
            val testBoard = board.placeStone(Move(bestMove, move.player))
            evaluatePosition(testBoard, move.player.opponent())
        } else currentEval
        
        // 수의 품질 결정
        val quality = determineMoveQuality(move, bestMove, currentEval, bestMoveEval, previousEval)
        
        // 위협 요소 찾기
        val threats = findThreats(board.placeStone(move), move.player.opponent())
        
        // 기회 요소 찾기
        val opportunities = findOpportunities(board, move.player)
        
        return MoveAnalysis(
            moveNumber = move.moveNumber,
            move = move,
            quality = quality,
            evaluation = currentEval,
            bestMove = bestMove,
            explanation = explainMove(move, quality, threats, opportunities),
            threats = threats,
            opportunities = opportunities
        )
    }
    
    /**
     * 포지션 평가 (간단한 휴리스틱)
     */
    private fun evaluatePosition(board: Board, nextPlayer: Player): Int {
        var blackScore = 0
        var whiteScore = 0
        
        // 각 플레이어의 패턴 평가
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val position = Position(row, col)
                board.getStone(position)?.let { player ->
                    val score = evaluateStonePosition(board, position, player)
                    if (player == Player.BLACK) {
                        blackScore += score
                    } else {
                        whiteScore += score
                    }
                }
            }
        }
        
        // 다음 차례 플레이어에게 약간의 보너스
        val turnBonus = 1000
        return if (nextPlayer == Player.BLACK) {
            blackScore - whiteScore + turnBonus
        } else {
            blackScore - whiteScore - turnBonus
        }
    }
    
    /**
     * 돌 하나의 위치 평가
     */
    private fun evaluateStonePosition(board: Board, position: Position, player: Player): Int {
        var score = 0
        val directions = listOf(
            Pair(0, 1),   // 가로
            Pair(1, 0),   // 세로
            Pair(1, 1),   // 대각선 \
            Pair(1, -1)   // 대각선 /
        )
        
        for ((dx, dy) in directions) {
            val pattern = getPattern(board, position, dx, dy, player)
            score += evaluatePattern(pattern)
        }
        
        // 중앙 보너스
        val centerDistance = abs(position.row - 7) + abs(position.col - 7)
        score += (14 - centerDistance) * 10
        
        return score
    }
    
    /**
     * 패턴 추출
     */
    private fun getPattern(board: Board, start: Position, dx: Int, dy: Int, player: Player): String {
        val pattern = StringBuilder()
        
        // 역방향 확인
        var pos = Position(start.row - dx * 4, start.col - dy * 4)
        for (i in -4..4) {
            if (i == 0) {
                pattern.append('X') // 현재 위치
            } else {
                pos = Position(start.row + dx * i, start.col + dy * i)
                if (!pos.isValid()) {
                    pattern.append('#') // 벽
                } else {
                    when (board.getStone(pos)) {
                        player -> pattern.append('X')
                        null -> pattern.append('.')
                        else -> pattern.append('O')
                    }
                }
            }
        }
        
        return pattern.toString()
    }
    
    /**
     * 패턴 평가
     */
    private fun evaluatePattern(pattern: String): Int {
        return when {
            pattern.contains("XXXXX") -> 1000000  // 오목
            pattern.contains(".XXXX.") -> 50000   // 열린 4
            pattern.contains("XXXX.") || pattern.contains(".XXXX") -> 10000  // 4
            pattern.contains(".XXX.") -> 5000     // 열린 3
            pattern.contains("XXX.") || pattern.contains(".XXX") -> 1000    // 3
            pattern.contains(".XX.") -> 500       // 열린 2
            pattern.contains("XX.") || pattern.contains(".XX") -> 100       // 2
            pattern.contains(".X.") -> 50         // 열린 1
            else -> 10
        }
    }
    
    /**
     * 최선의 수 찾기 (간단한 버전)
     */
    private fun findBestMove(board: Board, player: Player): Position? {
        var bestMove: Position? = null
        var bestScore = Int.MIN_VALUE
        
        // 기존 돌 주변의 빈 칸들만 검토
        val candidates = mutableSetOf<Position>()
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && hasAdjacentStone(board, pos)) {
                    candidates.add(pos)
                }
            }
        }
        
        // 각 후보 수 평가
        for (candidate in candidates) {
            val testMove = Move(candidate, player)
            val testBoard = board.placeStone(testMove)
            val score = evaluatePosition(testBoard, player.opponent())
            
            if (player == Player.BLACK && score > bestScore) {
                bestScore = score
                bestMove = candidate
            } else if (player == Player.WHITE && -score > bestScore) {
                bestScore = -score
                bestMove = candidate
            }
        }
        
        return bestMove
    }
    
    /**
     * 인접한 돌이 있는지 확인
     */
    private fun hasAdjacentStone(board: Board, position: Position): Boolean {
        for (dr in -2..2) {
            for (dc in -2..2) {
                if (dr == 0 && dc == 0) continue
                val adjacent = Position(position.row + dr, position.col + dc)
                if (adjacent.isValid() && !board.isEmpty(adjacent)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * 수의 품질 결정
     */
    private fun determineMoveQuality(
        move: Move,
        bestMove: Position?,
        currentEval: Int,
        bestMoveEval: Int,
        previousEval: Int
    ): MoveQuality {
        val evalDiff = abs(currentEval - bestMoveEval)
        val evalChange = currentEval - previousEval
        
        // 플레이어 관점에서 평가 변화 계산
        val playerEvalChange = if (move.player == Player.BLACK) evalChange else -evalChange
        
        return when {
            move.position == bestMove -> MoveQuality.EXCELLENT
            evalDiff < 1000 -> MoveQuality.GOOD
            evalDiff < 5000 -> MoveQuality.NORMAL
            evalDiff < 10000 -> MoveQuality.DUBIOUS
            playerEvalChange < -20000 -> MoveQuality.BLUNDER
            playerEvalChange < -10000 -> MoveQuality.MISTAKE
            else -> MoveQuality.NORMAL
        }
    }
    
    /**
     * 위협 요소 찾기
     */
    private fun findThreats(board: Board, player: Player): List<Threat> {
        val threats = mutableListOf<Threat>()
        
        // 승리 위협 찾기
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (board.isEmpty(pos)) {
                    val testBoard = board.placeStone(Move(pos, player))
                    if (checkWin(testBoard, pos, player)) {
                        threats.add(
                            Threat(
                                type = ThreatType.WIN_IN_1,
                                positions = listOf(pos),
                                severity = 10
                            )
                        )
                    }
                }
            }
        }
        
        return threats
    }
    
    /**
     * 기회 요소 찾기
     */
    private fun findOpportunities(board: Board, player: Player): List<Position> {
        val opportunities = mutableListOf<Position>()
        
        // 좋은 수들 찾기
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && hasAdjacentStone(board, pos)) {
                    val testBoard = board.placeStone(Move(pos, player))
                    val eval = evaluatePosition(testBoard, player.opponent())
                    if ((player == Player.BLACK && eval > 5000) || 
                        (player == Player.WHITE && eval < -5000)) {
                        opportunities.add(pos)
                    }
                }
            }
        }
        
        return opportunities
    }
    
    /**
     * 중요 지점 찾기
     */
    private fun findCriticalPoints(board: Board, nextPlayer: Player): List<Position> {
        val criticalPoints = mutableListOf<Position>()
        
        // 양 플레이어의 위협 지점 모두 포함
        val blackThreats = findThreats(board, Player.BLACK)
        val whiteThreats = findThreats(board, Player.WHITE)
        
        criticalPoints.addAll(blackThreats.flatMap { it.positions })
        criticalPoints.addAll(whiteThreats.flatMap { it.positions })
        
        return criticalPoints.distinct()
    }
    
    /**
     * 위협 수 계산
     */
    private fun countThreats(board: Board, player: Player): Int {
        return findThreats(board, player).size
    }
    
    /**
     * 승리 체크
     */
    private fun checkWin(board: Board, lastMove: Position, player: Player): Boolean {
        val directions = listOf(
            Pair(0, 1),   // 가로
            Pair(1, 0),   // 세로
            Pair(1, 1),   // 대각선 \
            Pair(1, -1)   // 대각선 /
        )
        
        for ((dx, dy) in directions) {
            var count = 1
            
            // 정방향
            var pos = Position(lastMove.row + dx, lastMove.col + dy)
            while (pos.isValid() && board.getStone(pos) == player) {
                count++
                pos = Position(pos.row + dx, pos.col + dy)
            }
            
            // 역방향
            pos = Position(lastMove.row - dx, lastMove.col - dy)
            while (pos.isValid() && board.getStone(pos) == player) {
                count++
                pos = Position(pos.row - dx, pos.col - dy)
            }
            
            if (count >= 5) return true
        }
        
        return false
    }
    
    /**
     * 전환점 설명
     */
    private fun describeTurningPoint(move: Move, evalChange: Int, currentEval: Int): String {
        val player = if (move.player == Player.BLACK) "흑" else "백"
        return when {
            evalChange > 20000 -> "$player${move.moveNumber}수로 결정적 우위 확보"
            evalChange > 10000 -> "$player${move.moveNumber}수로 큰 이득"
            currentEval > 50000 -> "흑이 승리에 근접"
            currentEval < -50000 -> "백이 승리에 근접"
            else -> "${move.moveNumber}수에서 형세 급변"
        }
    }
    
    /**
     * 수 설명
     */
    private fun explainMove(
        move: Move, 
        quality: MoveQuality, 
        threats: List<Threat>,
        opportunities: List<Position>
    ): String {
        val explanations = mutableListOf<String>()
        
        // 품질 설명
        when (quality) {
            MoveQuality.BRILLIANT -> explanations.add("최고의 선택!")
            MoveQuality.EXCELLENT -> explanations.add("매우 좋은 수")
            MoveQuality.BLUNDER -> explanations.add("치명적인 실수")
            MoveQuality.MISTAKE -> explanations.add("실수")
            else -> {}
        }
        
        // 위협 설명
        threats.forEach { threat ->
            when (threat.type) {
                ThreatType.WIN_IN_1 -> explanations.add("상대 승리 위협 차단")
                ThreatType.FOUR -> explanations.add("4목 생성")
                ThreatType.OPEN_THREE -> explanations.add("열린 3 생성")
                else -> {}
            }
        }
        
        // 기회 설명
        if (opportunities.isNotEmpty()) {
            explanations.add("${opportunities.size}개의 좋은 후속수 확보")
        }
        
        return explanations.joinToString(". ")
    }
    
    /**
     * 게임 요약 생성
     */
    private fun createGameSummary(game: Game, moveAnalyses: List<MoveAnalysis>): GameSummary {
        val blackMoves = moveAnalyses.filter { it.move.player == Player.BLACK }
        val whiteMoves = moveAnalyses.filter { it.move.player == Player.WHITE }
        
        val blackMistakes = blackMoves.count { it.quality == MoveQuality.MISTAKE }
        val whiteMistakes = whiteMoves.count { it.quality == MoveQuality.MISTAKE }
        val blackBlunders = blackMoves.count { it.quality == MoveQuality.BLUNDER }
        val whiteBlunders = whiteMoves.count { it.quality == MoveQuality.BLUNDER }
        
        // 정확도 계산 (실수와 대실수가 없을수록 높음)
        val blackAccuracy = if (blackMoves.isNotEmpty()) {
            100.0 * (blackMoves.size - blackMistakes - blackBlunders * 2) / blackMoves.size
        } else 0.0
        
        val whiteAccuracy = if (whiteMoves.isNotEmpty()) {
            100.0 * (whiteMoves.size - whiteMistakes - whiteBlunders * 2) / whiteMoves.size
        } else 0.0
        
        // 중요한 순간들 (품질이 극단적인 수들)
        val keyMoments = moveAnalyses
            .filter { 
                it.quality == MoveQuality.BRILLIANT || 
                it.quality == MoveQuality.BLUNDER ||
                it.threats.any { threat -> threat.type == ThreatType.WIN_IN_1 }
            }
            .map { it.moveNumber }
        
        return GameSummary(
            totalMoves = game.getMoveHistory().size,
            blackMistakes = blackMistakes,
            whiteMistakes = whiteMistakes,
            blackBlunders = blackBlunders,
            whiteBlunders = whiteBlunders,
            accuracy = mapOf(
                Player.BLACK to blackAccuracy.coerceIn(0.0, 100.0),
                Player.WHITE to whiteAccuracy.coerceIn(0.0, 100.0)
            ),
            averageTimePerMove = emptyMap(), // 시간 정보가 없으므로 비움
            keyMoments = keyMoments
        )
    }
}