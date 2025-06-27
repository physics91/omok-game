package com.omok.domain.achievement

import java.io.Serializable
import java.time.LocalDateTime

/**
 * 성취도 도메인 모델
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val type: AchievementType,
    val requirement: AchievementRequirement,
    val iconPath: String = "default_achievement.png",
    val hidden: Boolean = false,
    val points: Int = 10
) : Serializable

/**
 * 성취도 달성 기록
 */
data class AchievementProgress(
    val achievementId: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: LocalDateTime? = null,
    val currentValue: Int = 0,
    val targetValue: Int = 1,
    val bestValue: Int = 0
) : Serializable {
    
    val progressPercentage: Float
        get() = if (targetValue == 0) 100f else (currentValue.toFloat() / targetValue * 100f).coerceAtMost(100f)
    
    val isCompleted: Boolean
        get() = currentValue >= targetValue
}

/**
 * 성취도 카테고리
 */
enum class AchievementCategory(val displayName: String, val color: java.awt.Color) {
    GAMEPLAY("게임 플레이", java.awt.Color(76, 175, 80)),
    WINS("승리", java.awt.Color(255, 193, 7)),
    SPECIAL("특별", java.awt.Color(156, 39, 176)),
    STRATEGY("전략", java.awt.Color(33, 150, 243)),
    TIME("시간", java.awt.Color(255, 87, 34)),
    EXPLORATION("탐험", java.awt.Color(96, 125, 139)),
    MILESTONE("마일스톤", java.awt.Color(244, 67, 54))
}

/**
 * 성취도 타입
 */
sealed class AchievementType {
    object OneTime : AchievementType()
    data class Cumulative(val target: Int) : AchievementType()
    data class Streak(val required: Int) : AchievementType()
    data class Conditional(val condition: String) : AchievementType()
}

/**
 * 성취도 요구사항
 */
sealed class AchievementRequirement(val description: String) {
    // 게임 플레이 관련
    data class PlayGames(val count: Int) : AchievementRequirement("${count}게임 플레이")
    data class WinGames(val count: Int) : AchievementRequirement("${count}승 달성")
    data class WinStreak(val count: Int) : AchievementRequirement("${count}연승 달성")
    
    // 시간 관련
    data class FastWin(val seconds: Int) : AchievementRequirement("${seconds}초 내 승리")
    data class LongGame(val minutes: Int) : AchievementRequirement("${minutes}분 이상 대국")
    data class TotalPlayTime(val hours: Int) : AchievementRequirement("총 ${hours}시간 플레이")
    
    // 특별한 승리 조건
    object WinWithoutForbidden : AchievementRequirement("금수 없이 승리")
    object WinAgainstHardAI : AchievementRequirement("어려움 AI 격파")
    object PerfectGame : AchievementRequirement("완벽한 게임")
    object ComebackVictory : AchievementRequirement("역전승")
    
    // AI 관련
    data class DefeatAIDifficulty(val difficulty: String, val count: Int) : 
        AchievementRequirement("${difficulty} AI ${count}번 격파")
    
    // 패턴 관련
    data class MakePattern(val patternName: String, val count: Int) : 
        AchievementRequirement("${patternName} 패턴 ${count}번 생성")
    
    // 퍼즐/문제 관련
    data class SolvePuzzles(val count: Int) : AchievementRequirement("퍼즐 ${count}개 해결")
    data class SolveDifficultPuzzles(val difficulty: String, val count: Int) : 
        AchievementRequirement("${difficulty} 퍼즐 ${count}개 해결")
    
    // 토너먼트 관련
    data class WinTournaments(val count: Int) : AchievementRequirement("토너먼트 ${count}회 우승")
    object FirstTournamentWin : AchievementRequirement("첫 토너먼트 우승")
    
    // 분석 관련
    data class AnalyzeGames(val count: Int) : AchievementRequirement("게임 ${count}개 분석")
    object FindBrilliantMove : AchievementRequirement("명수 발견")
    
    // 설정/커스터마이징
    object CreateCustomTheme : AchievementRequirement("커스텀 테마 생성")
    object TryAllThemes : AchievementRequirement("모든 테마 사용")
    
    // 마일스톤
    data class ReachRating(val rating: Int) : AchievementRequirement("레이팅 ${rating} 달성")
    data class PlayDays(val days: Int) : AchievementRequirement("${days}일 동안 플레이")
}

/**
 * 성취도 이벤트 (게임 상황에서 발생하는 이벤트)
 */
sealed class AchievementEvent {
    object GameStarted : AchievementEvent()
    data class GameEnded(
        val won: Boolean,
        val duration: Long,
        val moveCount: Int,
        val aiDifficulty: String?,
        val hadForbiddenMoves: Boolean
    ) : AchievementEvent()
    
    data class WinStreak(val count: Int) : AchievementEvent()
    data class AIDefeated(val difficulty: String) : AchievementEvent()
    data class PatternCreated(val patternName: String) : AchievementEvent()
    data class PuzzleSolved(val difficulty: String) : AchievementEvent()
    data class TournamentWon(val participantCount: Int) : AchievementEvent()
    data class GameAnalyzed(val moveQuality: String) : AchievementEvent()
    data class ThemeCreated(val themeName: String) : AchievementEvent()
    data class ThemeUsed(val themeId: String) : AchievementEvent()
    data class PlayTimeAccumulated(val totalMinutes: Long) : AchievementEvent()
    data class BrilliantMoveFound(val evaluation: Float) : AchievementEvent()
}