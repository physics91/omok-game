package com.omok.application.usecase.puzzle

import com.omok.domain.model.Position
import com.omok.domain.puzzle.*
import com.omok.domain.event.GameEventBus
import com.omok.infrastructure.logging.Logger

/**
 * 퍼즐 풀기 유스케이스
 */
class SolvePuzzleUseCase(
    private val puzzleRepository: PuzzleRepository,
    private val eventBus: GameEventBus
) {
    
    private var currentProgress: PuzzleProgress? = null
    
    /**
     * 퍼즐 시작
     */
    fun startPuzzle(puzzleId: String): PuzzleProgress? {
        val puzzle = puzzleRepository.getPuzzleById(puzzleId)
        if (puzzle == null) {
            Logger.warn("SolvePuzzleUseCase", "Puzzle not found: $puzzleId")
            return null
        }
        
        currentProgress = PuzzleProgress(puzzle = puzzle)
        Logger.info("SolvePuzzleUseCase", "Started puzzle: ${puzzle.title}")
        
        return currentProgress
    }
    
    /**
     * 수 두기
     */
    fun makeMove(position: Position): MakeMoveResult {
        val progress = currentProgress ?: return MakeMoveResult.NoPuzzleActive
        
        // 이미 돌이 있는 위치인지 확인
        if (!progress.currentBoard.isEmpty(position)) {
            return MakeMoveResult.InvalidMove("이미 돌이 놓여있습니다.")
        }
        
        // 수 평가
        val evaluation = progress.puzzle.getMoveEvaluation(position)
        
        // 진행 상태 업데이트
        val newProgress = progress.makeMove(position)
        currentProgress = newProgress
        
        // 퍼즐 완료 체크
        if (newProgress.completed) {
            puzzleRepository.recordCompletion(progress.puzzle.id, newProgress)
            Logger.info("SolvePuzzleUseCase", "Puzzle completed: ${progress.puzzle.title}")
        }
        
        return MakeMoveResult.Success(
            evaluation = evaluation,
            progress = newProgress,
            completed = newProgress.completed
        )
    }
    
    /**
     * 힌트 요청
     */
    fun requestHint(): HintResult {
        val progress = currentProgress ?: return HintResult.NoPuzzleActive
        
        val nextHint = progress.getNextHint()
        if (nextHint == null) {
            return HintResult.NoMoreHints
        }
        
        val newProgress = progress.useHint()
        currentProgress = newProgress
        
        return HintResult.Success(hint = nextHint, hintsRemaining = progress.puzzle.hints.size - newProgress.hintsUsed)
    }
    
    /**
     * 퍼즐 리셋
     */
    fun resetPuzzle(): PuzzleProgress? {
        val progress = currentProgress ?: return null
        
        currentProgress = PuzzleProgress(puzzle = progress.puzzle)
        Logger.info("SolvePuzzleUseCase", "Puzzle reset: ${progress.puzzle.title}")
        
        return currentProgress
    }
    
    /**
     * 해답 보기
     */
    fun showSolution(): SolutionResult {
        val progress = currentProgress ?: return SolutionResult.NoPuzzleActive
        
        val mainSolution = progress.puzzle.solutions.firstOrNull()
            ?: return SolutionResult.NoSolution
        
        return SolutionResult.Success(
            mainLine = mainSolution.mainLine,
            explanation = mainSolution.explanation
        )
    }
    
    /**
     * 현재 진행 상태 조회
     */
    fun getCurrentProgress(): PuzzleProgress? = currentProgress
    
    /**
     * 퍼즐 종료
     */
    fun endPuzzle() {
        currentProgress?.let { progress ->
            if (progress.attempts > 0 && !progress.completed) {
                // 미완료 상태로 기록
                puzzleRepository.recordCompletion(progress.puzzle.id, progress)
            }
        }
        currentProgress = null
    }
}

/**
 * 수 두기 결과
 */
sealed class MakeMoveResult {
    object NoPuzzleActive : MakeMoveResult()
    data class InvalidMove(val reason: String) : MakeMoveResult()
    data class Success(
        val evaluation: MoveEvaluation,
        val progress: PuzzleProgress,
        val completed: Boolean
    ) : MakeMoveResult()
}

/**
 * 힌트 결과
 */
sealed class HintResult {
    object NoPuzzleActive : HintResult()
    object NoMoreHints : HintResult()
    data class Success(val hint: String, val hintsRemaining: Int) : HintResult()
}

/**
 * 해답 결과
 */
sealed class SolutionResult {
    object NoPuzzleActive : SolutionResult()
    object NoSolution : SolutionResult()
    data class Success(
        val mainLine: List<Position>,
        val explanation: String
    ) : SolutionResult()
}