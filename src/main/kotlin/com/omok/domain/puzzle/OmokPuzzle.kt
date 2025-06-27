package com.omok.domain.puzzle

import com.omok.domain.model.*
import java.time.LocalDateTime

/**
 * 오목 퍼즐 도메인 모델
 */
data class OmokPuzzle(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: PuzzleDifficulty,
    val category: PuzzleCategory,
    val objective: PuzzleObjective,
    val initialBoard: Board,
    val currentPlayer: Player,
    val solutions: List<PuzzleSolution>,
    val hints: List<String>,
    val timeLimit: Long? = null, // 밀리초
    val author: String = "System",
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 수가 올바른 해법인지 검증
     */
    fun isCorrectMove(position: Position): Boolean {
        return solutions.any { solution ->
            solution.mainLine.firstOrNull() == position
        }
    }
    
    /**
     * 수에 대한 피드백 제공
     */
    fun getMoveEvaluation(position: Position): MoveEvaluation {
        for (solution in solutions) {
            if (solution.mainLine.firstOrNull() == position) {
                return MoveEvaluation.CORRECT
            }
            if (solution.variations.contains(position)) {
                return MoveEvaluation.GOOD
            }
        }
        
        // 완전히 틀린 수인지 검사
        val testBoard = initialBoard.placeStone(Move(position, currentPlayer))
        
        // 상대방이 바로 이길 수 있는 수인지 확인
        if (canOpponentWinImmediately(testBoard, currentPlayer.opponent())) {
            return MoveEvaluation.BLUNDER
        }
        
        return MoveEvaluation.INCORRECT
    }
    
    private fun canOpponentWinImmediately(board: Board, player: Player): Boolean {
        // 간단한 승리 체크 로직
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val pos = Position(row, col)
                if (board.isEmpty(pos)) {
                    val testMove = Move(pos, player)
                    val testBoard = board.placeStone(testMove)
                    if (checkWin(testBoard, pos, player)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
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
}

/**
 * 퍼즐 난이도
 */
enum class PuzzleDifficulty(val displayName: String, val rating: Int) {
    BEGINNER("초급", 1),
    INTERMEDIATE("중급", 2),
    ADVANCED("상급", 3),
    EXPERT("고수", 4),
    MASTER("달인", 5)
}

/**
 * 퍼즐 카테고리
 */
enum class PuzzleCategory(val displayName: String, val description: String) {
    TACTICS("전술", "기본 전술 훈련"),
    ENDGAME("끝내기", "승리로 이끄는 마무리"),
    DEFENSE("수비", "상대의 위협 방어"),
    OPENING("포석", "초반 전략"),
    RENJU("렌주룰", "렌주룰 특수 상황"),
    COMBINATION("연계", "복잡한 수의 조합")
}

/**
 * 퍼즐 목표
 */
enum class PuzzleObjective(val displayName: String) {
    WIN_IN_1("1수 승리"),
    WIN_IN_3("3수 내 승리"),
    WIN_IN_5("5수 내 승리"),
    DEFEND("방어하기"),
    FIND_BEST_MOVE("최선의 수 찾기"),
    AVOID_FORBIDDEN("금수 피하기")
}

/**
 * 퍼즐 해법
 */
data class PuzzleSolution(
    val mainLine: List<Position>,      // 주요 해법 수순
    val variations: List<Position>,     // 대안
    val explanation: String            // 설명
)

/**
 * 수 평가
 */
enum class MoveEvaluation(val displayName: String, val message: String) {
    CORRECT("정답!", "완벽합니다! 정확한 수입니다."),
    GOOD("좋은 수", "좋은 수지만 더 좋은 수가 있습니다."),
    INCORRECT("틀림", "다시 생각해보세요."),
    BLUNDER("치명적 실수", "이 수는 상대방이 즉시 승리합니다!")
}

/**
 * 퍼즐 진행 상태
 */
data class PuzzleProgress(
    val puzzle: OmokPuzzle,
    val moveHistory: List<Move> = emptyList(),
    val currentBoard: Board = puzzle.initialBoard,
    val hintsUsed: Int = 0,
    val attempts: Int = 0,
    val startTime: LocalDateTime = LocalDateTime.now(),
    val completed: Boolean = false,
    val success: Boolean = false
) {
    fun makeMove(position: Position): PuzzleProgress {
        val move = Move(position, puzzle.currentPlayer, moveHistory.size + 1)
        val newBoard = currentBoard.placeStone(move)
        val newMoveHistory = moveHistory + move
        
        // 정답 확인
        val isCorrect = puzzle.isCorrectMove(position)
        val isComplete = isCorrect && newMoveHistory.size >= puzzle.solutions.minOf { it.mainLine.size }
        
        return copy(
            moveHistory = newMoveHistory,
            currentBoard = newBoard,
            attempts = attempts + 1,
            completed = isComplete,
            success = isComplete && isCorrect
        )
    }
    
    fun useHint(): PuzzleProgress {
        return copy(hintsUsed = hintsUsed + 1)
    }
    
    fun getNextHint(): String? {
        return puzzle.hints.getOrNull(hintsUsed)
    }
    
    fun getSolvedTime(): Long {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis()
    }
}