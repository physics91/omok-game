package com.omok.domain.ai.evaluation

import com.omok.domain.model.*
import com.omok.domain.ai.pattern.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 고급 포지션 평가기
 * 패턴 인식, 영향력 맵, 연결성 등을 종합적으로 평가
 */
class PositionEvaluator(
    private val patternRecognizer: PatternRecognizer = PatternRecognizer()
) {
    
    companion object {
        // 평가 가중치
        const val PATTERN_WEIGHT = 1.0
        const val INFLUENCE_WEIGHT = 0.3
        const val CONNECTIVITY_WEIGHT = 0.5
        const val TERRITORY_WEIGHT = 0.2
        const val MOBILITY_WEIGHT = 0.1
        
        // 승리 점수
        const val WIN_SCORE = 10000000
        const val DRAW_SCORE = 0
    }
    
    /**
     * 보드 전체 평가
     */
    fun evaluate(board: Board, currentPlayer: Player): Int {
        // 승리 상태 체크
        val gameState = checkGameState(board)
        if (gameState != null) {
            return when (gameState) {
                is GameState.Won -> if (gameState.winner == currentPlayer) WIN_SCORE else -WIN_SCORE
                is GameState.Draw -> DRAW_SCORE
                is GameState.Playing -> 0
                is GameState.WaitingForSwap -> 0
                is GameState.WaitingForFifthMove -> 0
                is GameState.WaitingForFifthMoveSelection -> 0
            }
        }
        
        // 각 평가 요소 계산
        val patternScore = evaluatePatterns(board, currentPlayer)
        val influenceScore = evaluateInfluence(board, currentPlayer)
        val connectivityScore = evaluateConnectivity(board, currentPlayer)
        val territoryScore = evaluateTerritory(board, currentPlayer)
        val mobilityScore = evaluateMobility(board, currentPlayer)
        
        // 가중 합계
        return (patternScore * PATTERN_WEIGHT +
                influenceScore * INFLUENCE_WEIGHT +
                connectivityScore * CONNECTIVITY_WEIGHT +
                territoryScore * TERRITORY_WEIGHT +
                mobilityScore * MOBILITY_WEIGHT).toInt()
    }
    
    /**
     * 패턴 기반 평가
     */
    private fun evaluatePatterns(board: Board, currentPlayer: Player): Int {
        var score = 0
        
        // 현재 플레이어 패턴
        val myPatterns = patternRecognizer.findAllPatterns(board, currentPlayer)
        score += myPatterns.sumOf { it.score }
        
        // 상대 플레이어 패턴
        val opponentPatterns = patternRecognizer.findAllPatterns(board, currentPlayer.opponent())
        score -= opponentPatterns.sumOf { it.score }
        
        // 위협 수준에 따른 보너스
        val myThreat = patternRecognizer.evaluateThreatLevel(board, currentPlayer)
        val opponentThreat = patternRecognizer.evaluateThreatLevel(board, currentPlayer.opponent())
        
        score += getThreatBonus(myThreat)
        score -= getThreatBonus(opponentThreat)
        
        return score
    }
    
    /**
     * 영향력 맵 평가
     */
    private fun evaluateInfluence(board: Board, currentPlayer: Player): Int {
        val influenceMap = Array(15) { IntArray(15) }
        
        // 각 돌의 영향력 계산
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                board.getStone(pos)?.let { player ->
                    val influence = if (player == currentPlayer) 1 else -1
                    spreadInfluence(influenceMap, pos, influence)
                }
            }
        }
        
        // 영향력 합계
        return influenceMap.sumOf { row -> row.sum() }
    }
    
    /**
     * 연결성 평가
     */
    private fun evaluateConnectivity(board: Board, currentPlayer: Player): Int {
        var score = 0
        
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (board.getStone(pos) == currentPlayer) {
                    // 인접한 같은 색 돌 개수
                    val connections = countConnections(board, pos, currentPlayer)
                    score += connections * connections * 10 // 제곱으로 가중치
                }
            }
        }
        
        return score
    }
    
    /**
     * 영역 제어 평가
     */
    private fun evaluateTerritory(board: Board, currentPlayer: Player): Int {
        var myTerritory = 0
        var opponentTerritory = 0
        
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (board.isEmpty(pos)) {
                    val controller = getTerritoryController(board, pos)
                    when {
                        controller == currentPlayer -> myTerritory++
                        controller == currentPlayer.opponent() -> opponentTerritory++
                        controller == null -> {} // Neutral territory
                    }
                }
            }
        }
        
        return (myTerritory - opponentTerritory) * 5
    }
    
    /**
     * 이동 가능성 평가
     */
    private fun evaluateMobility(board: Board, currentPlayer: Player): Int {
        var myMobility = 0
        var opponentMobility = 0
        
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && hasAdjacentStone(board, pos)) {
                    // 이 위치에 돌을 놓았을 때의 가치 평가
                    val myValue = patternRecognizer.evaluatePosition(board, pos, currentPlayer)
                    val opponentValue = patternRecognizer.evaluatePosition(board, pos, currentPlayer.opponent())
                    
                    if (myValue > 1000) myMobility++
                    if (opponentValue > 1000) opponentMobility++
                }
            }
        }
        
        return (myMobility - opponentMobility) * 100
    }
    
    /**
     * 영향력 전파
     */
    private fun spreadInfluence(influenceMap: Array<IntArray>, center: Position, baseInfluence: Int) {
        val maxDistance = 3
        
        for (row in max(0, center.row - maxDistance)..min(14, center.row + maxDistance)) {
            for (col in max(0, center.col - maxDistance)..min(14, center.col + maxDistance)) {
                val distance = abs(row - center.row) + abs(col - center.col)
                if (distance <= maxDistance) {
                    val influence = baseInfluence * (maxDistance - distance + 1) / (maxDistance + 1)
                    influenceMap[row][col] += influence
                }
            }
        }
    }
    
    /**
     * 연결된 돌 개수 계산
     */
    private fun countConnections(board: Board, pos: Position, player: Player): Int {
        var connections = 0
        
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val adjacent = Position(pos.row + dr, pos.col + dc)
                if (adjacent.isValid() && board.getStone(adjacent) == player) {
                    connections++
                }
            }
        }
        
        return connections
    }
    
    /**
     * 영역 제어자 판단
     */
    private fun getTerritoryController(board: Board, pos: Position): Player? {
        var blackInfluence = 0
        var whiteInfluence = 0
        
        // 주변 3칸 이내의 돌들의 영향력 계산
        for (row in max(0, pos.row - 3)..min(14, pos.row + 3)) {
            for (col in max(0, pos.col - 3)..min(14, pos.col + 3)) {
                val checkPos = Position(row, col)
                board.getStone(checkPos)?.let { player ->
                    val distance = abs(row - pos.row) + abs(col - pos.col)
                    val influence = 4 - distance
                    
                    when (player) {
                        Player.BLACK -> blackInfluence += influence
                        Player.WHITE -> whiteInfluence += influence
                    }
                }
            }
        }
        
        return when {
            blackInfluence > whiteInfluence * 2 -> Player.BLACK
            whiteInfluence > blackInfluence * 2 -> Player.WHITE
            else -> null
        }
    }
    
    /**
     * 인접한 돌이 있는지 확인
     */
    private fun hasAdjacentStone(board: Board, pos: Position): Boolean {
        for (dr in -2..2) {
            for (dc in -2..2) {
                if (dr == 0 && dc == 0) continue
                val adjacent = Position(pos.row + dr, pos.col + dc)
                if (adjacent.isValid() && !board.isEmpty(adjacent)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * 게임 상태 체크
     */
    private fun checkGameState(board: Board): GameState? {
        // 승리 체크
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                board.getStone(pos)?.let { player ->
                    val winLine = checkWinFromPosition(board, pos, player)
                    if (winLine != null) {
                        return GameState.Won(player, winLine)
                    }
                }
            }
        }
        
        // 무승부 체크
        var hasEmpty = false
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                if (board.isEmpty(Position(row, col))) {
                    hasEmpty = true
                    break
                }
            }
            if (hasEmpty) break
        }
        
        return if (!hasEmpty) GameState.Draw else null
    }
    
    /**
     * 특정 위치에서 승리 체크
     */
    private fun checkWinFromPosition(board: Board, start: Position, player: Player): List<Position>? {
        val directions = listOf(
            Pair(0, 1),   // 가로
            Pair(1, 0),   // 세로
            Pair(1, 1),   // 대각선 \
            Pair(1, -1)   // 대각선 /
        )
        
        for ((dx, dy) in directions) {
            val line = mutableListOf<Position>()
            
            // 시작점부터 한 방향으로 체크
            var pos = start
            while (pos.isValid() && board.getStone(pos) == player) {
                line.add(pos)
                pos = Position(pos.row + dx, pos.col + dy)
            }
            
            // 반대 방향으로 체크
            pos = Position(start.row - dx, start.col - dy)
            while (pos.isValid() && board.getStone(pos) == player) {
                line.add(0, pos)
                pos = Position(pos.row - dx, pos.col - dy)
            }
            
            if (line.size >= 5) {
                return line.take(5) // 5개만 반환
            }
        }
        
        return null
    }
    
    /**
     * 위협 수준에 따른 보너스 점수
     */
    private fun getThreatBonus(threat: ThreatLevel): Int = when (threat) {
        ThreatLevel.IMMEDIATE_WIN -> 100000
        ThreatLevel.DOUBLE_THREAT -> 50000
        ThreatLevel.SINGLE_THREAT -> 20000
        ThreatLevel.FORCING_SEQUENCE -> 10000
        ThreatLevel.MINOR_THREAT -> 5000
        ThreatLevel.NO_THREAT -> 0
    }
}