package com.omok.domain.achievement

/**
 * 성취도 저장소 인터페이스
 */
interface AchievementRepository {
    
    /**
     * 플레이어 진행도 저장
     */
    fun saveProgress(progress: Map<String, AchievementProgress>)
    
    /**
     * 플레이어 진행도 로드
     */
    fun loadProgress(): Map<String, AchievementProgress>
    
    /**
     * 플레이어 통계 저장
     */
    fun saveStats(stats: PlayerStats)
    
    /**
     * 플레이어 통계 로드
     */
    fun loadStats(): PlayerStats?
    
    /**
     * 커스텀 성취도 저장
     */
    fun saveCustomAchievement(achievement: Achievement)
    
    /**
     * 커스텀 성취도 로드
     */
    fun loadCustomAchievements(): List<Achievement>
    
    /**
     * 모든 데이터 삭제 (리셋)
     */
    fun reset()
}