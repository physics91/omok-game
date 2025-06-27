package com.omok.domain.puzzle

/**
 * 퍼즐 리포지토리 인터페이스
 */
interface PuzzleRepository {
    /**
     * 모든 퍼즐 조회
     */
    fun getAllPuzzles(): List<OmokPuzzle>
    
    /**
     * ID로 퍼즐 조회
     */
    fun getPuzzleById(id: String): OmokPuzzle?
    
    /**
     * 카테고리별 퍼즐 조회
     */
    fun getPuzzlesByCategory(category: PuzzleCategory): List<OmokPuzzle>
    
    /**
     * 난이도별 퍼즐 조회
     */
    fun getPuzzlesByDifficulty(difficulty: PuzzleDifficulty): List<OmokPuzzle>
    
    /**
     * 퍼즐 완료 기록 저장
     */
    fun recordCompletion(puzzleId: String, progress: PuzzleProgress)
    
    /**
     * 사용자의 퍼즐 완료 기록 조회
     */
    fun getCompletionRecords(): List<PuzzleCompletionRecord>
    
    /**
     * 추천 퍼즐 조회 (사용자 실력 기반)
     */
    fun getRecommendedPuzzles(limit: Int = 5): List<OmokPuzzle>
}

/**
 * 퍼즐 완료 기록
 */
data class PuzzleCompletionRecord(
    val puzzleId: String,
    val completedAt: java.time.LocalDateTime,
    val success: Boolean,
    val attempts: Int,
    val hintsUsed: Int,
    val solvingTime: Long // 밀리초
)