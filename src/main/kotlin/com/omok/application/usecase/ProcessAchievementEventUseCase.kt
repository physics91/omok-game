package com.omok.application.usecase

import com.omok.domain.achievement.AchievementEvent
import com.omok.domain.achievement.AchievementManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 성취도 이벤트 처리 유스케이스
 */
class ProcessAchievementEventUseCase(
    private val achievementManager: AchievementManager
) {
    
    /**
     * 성취도 이벤트 처리
     */
    suspend fun execute(event: AchievementEvent) = withContext(Dispatchers.Default) {
        try {
            achievementManager.processEvent(event)
        } catch (e: Exception) {
            // 성취도 처리 오류는 게임 플레이에 영향을 주지 않도록 로깅만 수행
            println("Achievement event processing failed: ${e.message}")
        }
    }
    
    /**
     * 게임 시작 이벤트
     */
    suspend fun onGameStarted() {
        execute(AchievementEvent.GameStarted)
    }
    
    /**
     * 게임 종료 이벤트
     */
    suspend fun onGameEnded(
        won: Boolean,
        duration: Long,
        moveCount: Int,
        aiDifficulty: String? = null,
        hadForbiddenMoves: Boolean = false
    ) {
        execute(AchievementEvent.GameEnded(
            won = won,
            duration = duration,
            moveCount = moveCount,
            aiDifficulty = aiDifficulty,
            hadForbiddenMoves = hadForbiddenMoves
        ))
    }
    
    /**
     * AI 격파 이벤트
     */
    suspend fun onAIDefeated(difficulty: String) {
        execute(AchievementEvent.AIDefeated(difficulty))
    }
    
    /**
     * 패턴 생성 이벤트
     */
    suspend fun onPatternCreated(patternName: String) {
        execute(AchievementEvent.PatternCreated(patternName))
    }
    
    /**
     * 퍼즐 해결 이벤트
     */
    suspend fun onPuzzleSolved(difficulty: String) {
        execute(AchievementEvent.PuzzleSolved(difficulty))
    }
    
    /**
     * 토너먼트 우승 이벤트
     */
    suspend fun onTournamentWon(participantCount: Int) {
        execute(AchievementEvent.TournamentWon(participantCount))
    }
    
    /**
     * 게임 분석 이벤트
     */
    suspend fun onGameAnalyzed(moveQuality: String) {
        execute(AchievementEvent.GameAnalyzed(moveQuality))
    }
    
    /**
     * 테마 생성 이벤트
     */
    suspend fun onThemeCreated(themeName: String) {
        execute(AchievementEvent.ThemeCreated(themeName))
    }
    
    /**
     * 테마 사용 이벤트
     */
    suspend fun onThemeUsed(themeId: String) {
        execute(AchievementEvent.ThemeUsed(themeId))
    }
    
    /**
     * 명수 발견 이벤트
     */
    suspend fun onBrilliantMoveFound(evaluation: Float) {
        execute(AchievementEvent.BrilliantMoveFound(evaluation))
    }
    
    /**
     * 연승 이벤트
     */
    suspend fun onWinStreak(count: Int) {
        execute(AchievementEvent.WinStreak(count))
    }
    
    /**
     * 플레이 시간 누적 이벤트
     */
    suspend fun onPlayTimeAccumulated(totalMinutes: Long) {
        execute(AchievementEvent.PlayTimeAccumulated(totalMinutes))
    }
}