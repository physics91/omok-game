package com.omok.domain.ai.mcts

import com.omok.domain.model.*
import com.omok.domain.ai.evaluation.PositionEvaluator
import com.omok.domain.logging.DomainLogger
import com.omok.domain.logging.NoOpLogger
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Monte Carlo Tree Search (MCTS) AI 구현
 * UCT (Upper Confidence Bound for Trees) 알고리즘 사용
 */
class MonteCarloTreeSearch(
    private val maxIterations: Int = 10000,
    private val maxThinkingTimeMs: Long = 5000,
    private val explorationConstant: Double = 1.414, // sqrt(2)
    private val evaluator: PositionEvaluator = PositionEvaluator(),
    private val logger: DomainLogger = NoOpLogger
) {
    
    /**
     * 최선의 수 찾기
     */
    fun findBestMove(board: Board, player: Player): Position? {
        val startTime = System.currentTimeMillis()
        val root = MCTSNode(board = board, player = player)
        
        var iterations = 0
        while (iterations < maxIterations && 
               System.currentTimeMillis() - startTime < maxThinkingTimeMs) {
            
            // 1. Selection - 리프 노드까지 선택
            val leaf = select(root)
            
            // 2. Expansion - 가능한 수 확장
            if (!isTerminal(leaf) && leaf.visits > 0) {
                expand(leaf)
            }
            
            // 3. Simulation - 랜덤 시뮬레이션
            val selectedChild = if (leaf.children.isEmpty()) leaf else selectRandomChild(leaf)
            val result = simulate(selectedChild)
            
            // 4. Backpropagation - 결과 역전파
            backpropagate(selectedChild, result)
            
            iterations++
        }
        
        logger.info("MCTS: Completed $iterations iterations in ${System.currentTimeMillis() - startTime}ms")
        
        // 가장 많이 방문한 자식 노드 선택
        return root.children
            .maxByOrNull { it.visits }
            ?.move
    }
    
    /**
     * Selection 단계 - UCT를 사용하여 리프 노드까지 선택
     */
    private fun select(node: MCTSNode): MCTSNode {
        var current = node
        
        while (current.children.isNotEmpty() && !isTerminal(current)) {
            current = if (current.isFullyExpanded()) {
                // UCT를 사용하여 최선의 자식 선택
                current.children.maxByOrNull { child ->
                    calculateUCT(child, current.visits)
                } ?: break
            } else {
                // 아직 확장되지 않은 노드가 있으면 확장
                return current
            }
        }
        
        return current
    }
    
    /**
     * Expansion 단계 - 새로운 자식 노드 추가
     */
    private fun expand(node: MCTSNode) {
        val possibleMoves = getPossibleMoves(node.board)
        
        for (position in possibleMoves) {
            if (!node.hasChild(position)) {
                val move = Move(position, node.player)
                val newBoard = node.board.placeStone(move)
                
                val childNode = MCTSNode(
                    board = newBoard,
                    player = node.player.opponent(),
                    parent = node,
                    move = position
                )
                
                node.addChild(childNode)
                
                // 한 번에 하나씩만 확장
                if (!node.isFullyExpanded()) {
                    break
                }
            }
        }
    }
    
    /**
     * Simulation 단계 - 게임 끝까지 랜덤 플레이
     */
    private fun simulate(node: MCTSNode): SimulationResult {
        var currentBoard = node.board
        var currentPlayer = node.player
        var moveCount = 0
        val maxMoves = 50 // 무한 루프 방지
        
        while (!isGameOver(currentBoard) && moveCount < maxMoves) {
            val possibleMoves = getPossibleMoves(currentBoard)
            if (possibleMoves.isEmpty()) break
            
            // 휴리스틱을 사용한 준-랜덤 선택
            val selectedMove = selectSimulationMove(currentBoard, currentPlayer, possibleMoves)
            currentBoard = currentBoard.placeStone(Move(selectedMove, currentPlayer))
            currentPlayer = currentPlayer.opponent()
            moveCount++
        }
        
        // 게임 결과 평가
        return evaluateResult(currentBoard, node.player)
    }
    
    /**
     * Backpropagation 단계 - 결과를 루트까지 역전파
     */
    private fun backpropagate(node: MCTSNode, result: SimulationResult) {
        var current: MCTSNode? = node
        
        while (current != null) {
            current.visits++
            
            // 현재 노드의 플레이어 관점에서 점수 업데이트
            when {
                result.winner == current.player -> current.wins += 1.0
                result.winner == null -> current.wins += 0.5 // 무승부
                // 상대가 이긴 경우는 0점
            }
            
            current = current.parent
        }
    }
    
    /**
     * UCT (Upper Confidence Bound for Trees) 계산
     */
    private fun calculateUCT(node: MCTSNode, parentVisits: Int): Double {
        if (node.visits == 0) {
            return Double.MAX_VALUE
        }
        
        val exploitation = node.wins / node.visits
        val exploration = explorationConstant * sqrt(ln(parentVisits.toDouble()) / node.visits)
        
        return exploitation + exploration
    }
    
    /**
     * 가능한 수 찾기
     */
    private fun getPossibleMoves(board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val occupied = mutableSetOf<Position>()
        
        // 기존 돌 주변의 빈 칸 찾기
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (!board.isEmpty(pos)) {
                    occupied.add(pos)
                }
            }
        }
        
        // 돌이 없으면 중앙
        if (occupied.isEmpty()) {
            return listOf(Position(7, 7))
        }
        
        // 기존 돌 주변 2칸 이내의 빈 칸
        val candidates = mutableSetOf<Position>()
        for (stone in occupied) {
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val newPos = Position(stone.row + dr, stone.col + dc)
                    if (newPos.isValid() && board.isEmpty(newPos)) {
                        candidates.add(newPos)
                    }
                }
            }
        }
        
        // 평가 점수가 높은 순으로 정렬 (상위 N개만 고려)
        return candidates
            .map { pos -> 
                pos to evaluator.evaluate(
                    board.placeStone(Move(pos, Player.BLACK)), 
                    Player.BLACK
                )
            }
            .sortedByDescending { it.second }
            .take(20) // 상위 20개만
            .map { it.first }
    }
    
    /**
     * 시뮬레이션용 수 선택 (휴리스틱 포함)
     */
    private fun selectSimulationMove(
        board: Board, 
        player: Player, 
        moves: List<Position>
    ): Position {
        // 80% 확률로 휴리스틱 기반, 20% 확률로 랜덤
        return if (Random.nextDouble() < 0.8) {
            // 간단한 휴리스틱 평가
            moves.maxByOrNull { pos ->
                val testBoard = board.placeStone(Move(pos, player))
                evaluateQuick(testBoard, pos, player)
            } ?: moves.random()
        } else {
            moves.random()
        }
    }
    
    /**
     * 빠른 평가 (시뮬레이션용)
     */
    private fun evaluateQuick(board: Board, lastMove: Position, player: Player): Int {
        // 승리 체크
        if (checkWin(board, lastMove, player)) {
            return 10000
        }
        
        // 주변 돌 개수
        var score = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val adjacent = Position(lastMove.row + dr, lastMove.col + dc)
                if (adjacent.isValid() && board.getStone(adjacent) == player) {
                    score += 10
                }
            }
        }
        
        // 중앙 보너스
        val centerDistance = kotlin.math.abs(lastMove.row - 7) + kotlin.math.abs(lastMove.col - 7)
        score += (14 - centerDistance)
        
        return score
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
     * 게임 종료 체크
     */
    private fun isGameOver(board: Board): Boolean {
        // 승리 체크
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                board.getStone(pos)?.let { player ->
                    if (checkWin(board, pos, player)) {
                        return true
                    }
                }
            }
        }
        
        // 보드가 가득 찼는지 체크
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                if (board.isEmpty(Position(row, col))) {
                    return false
                }
            }
        }
        
        return true
    }
    
    /**
     * 결과 평가
     */
    private fun evaluateResult(board: Board, originalPlayer: Player): SimulationResult {
        // 승자 찾기
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                board.getStone(pos)?.let { player ->
                    if (checkWin(board, pos, player)) {
                        return SimulationResult(winner = player)
                    }
                }
            }
        }
        
        // 무승부
        return SimulationResult(winner = null)
    }
    
    /**
     * 터미널 노드 체크
     */
    private fun isTerminal(node: MCTSNode): Boolean {
        return isGameOver(node.board)
    }
    
    /**
     * 랜덤 자식 선택
     */
    private fun selectRandomChild(node: MCTSNode): MCTSNode {
        return node.children.randomOrNull() ?: node
    }
}

/**
 * MCTS 노드
 */
class MCTSNode(
    val board: Board,
    val player: Player,
    val parent: MCTSNode? = null,
    val move: Position? = null
) {
    val children = mutableListOf<MCTSNode>()
    var visits = 0
    var wins = 0.0
    
    fun addChild(child: MCTSNode) {
        children.add(child)
    }
    
    fun hasChild(position: Position): Boolean {
        return children.any { it.move == position }
    }
    
    fun isFullyExpanded(): Boolean {
        val possibleMoves = getPossibleMovesCount()
        return children.size >= possibleMoves
    }
    
    private fun getPossibleMovesCount(): Int {
        var count = 0
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (board.isEmpty(pos) && hasAdjacentStone(pos)) {
                    count++
                }
            }
        }
        return if (count == 0 && isBoardEmpty()) 1 else count
    }
    
    private fun hasAdjacentStone(pos: Position): Boolean {
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
    
    private fun isBoardEmpty(): Boolean {
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                if (!board.isEmpty(Position(row, col))) {
                    return false
                }
            }
        }
        return true
    }
}

/**
 * 시뮬레이션 결과
 */
data class SimulationResult(
    val winner: Player?
)