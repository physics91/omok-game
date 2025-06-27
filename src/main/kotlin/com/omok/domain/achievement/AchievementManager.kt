package com.omok.domain.achievement

import com.omok.domain.event.GameEvent
import com.omok.domain.event.GameEventBus
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 성취도 관리자 - 도메인 서비스
 */
class AchievementManager(
    private val eventBus: GameEventBus,
    private val repository: AchievementRepository
) {
    
    private val achievements = ConcurrentHashMap<String, Achievement>()
    private val playerProgress = ConcurrentHashMap<String, AchievementProgress>()
    private val listeners = mutableListOf<AchievementListener>()
    
    // 통계 추적
    private var gamesPlayed = 0
    private var gamesWon = 0
    private var currentWinStreak = 0
    private var bestWinStreak = 0
    private var totalPlayTimeMinutes = 0L
    private var puzzlesSolved = 0
    private var tournamentsWon = 0
    private var gamesAnalyzed = 0
    private var themesUsed = mutableSetOf<String>()
    private var hasCreatedTheme = false
    private var aiDefeats = mutableMapOf<String, Int>()
    private var patternsCreated = mutableMapOf<String, Int>()
    private var brilliantMovesFound = 0
    
    init {
        loadAchievements()
        loadPlayerProgress()
        setupEventListeners()
    }
    
    /**
     * 게임 이벤트 처리
     */
    private fun setupEventListeners() {
        // 이벤트 리스너는 직접 이벤트 처리 대신 UseCase를 통해 처리됨
        // 게임 컨트롤러에서 직접 achievementEventUseCase 호출
    }
    
    /**
     * 성취도 이벤트 처리
     */
    fun processEvent(event: AchievementEvent) {
        updateStatistics(event)
        checkAchievements(event)
    }
    
    /**
     * 통계 업데이트
     */
    private fun updateStatistics(event: AchievementEvent) {
        when (event) {
            is AchievementEvent.GameStarted -> {
                gamesPlayed++
            }
            
            is AchievementEvent.GameEnded -> {
                if (event.won) {
                    gamesWon++
                    currentWinStreak++
                    if (currentWinStreak > bestWinStreak) {
                        bestWinStreak = currentWinStreak
                    }
                    processEvent(AchievementEvent.WinStreak(currentWinStreak))
                } else {
                    currentWinStreak = 0
                }
                
                totalPlayTimeMinutes += event.duration / (1000 * 60)
                processEvent(AchievementEvent.PlayTimeAccumulated(totalPlayTimeMinutes))
                
                event.aiDifficulty?.let { difficulty ->
                    if (event.won) {
                        aiDefeats[difficulty] = aiDefeats.getOrDefault(difficulty, 0) + 1
                        processEvent(AchievementEvent.AIDefeated(difficulty))
                    }
                }
            }
            
            is AchievementEvent.PuzzleSolved -> {
                puzzlesSolved++
            }
            
            is AchievementEvent.TournamentWon -> {
                tournamentsWon++
            }
            
            is AchievementEvent.GameAnalyzed -> {
                gamesAnalyzed++
            }
            
            is AchievementEvent.ThemeCreated -> {
                hasCreatedTheme = true
            }
            
            is AchievementEvent.ThemeUsed -> {
                themesUsed.add(event.themeId)
            }
            
            is AchievementEvent.PatternCreated -> {
                patternsCreated[event.patternName] = patternsCreated.getOrDefault(event.patternName, 0) + 1
            }
            
            is AchievementEvent.BrilliantMoveFound -> {
                brilliantMovesFound++
            }
            
            else -> { /* 다른 이벤트들 */ }
        }
        
        saveProgress()
    }
    
    /**
     * 성취도 확인 및 해제
     */
    private fun checkAchievements(event: AchievementEvent) {
        achievements.values.forEach { achievement ->
            val progress = playerProgress[achievement.id] ?: createInitialProgress(achievement)
            
            if (!progress.isUnlocked && shouldUnlockAchievement(achievement, progress, event)) {
                unlockAchievement(achievement.id)
            } else {
                updateProgress(achievement, progress, event)
            }
        }
    }
    
    /**
     * 성취도 해제 조건 확인
     */
    private fun shouldUnlockAchievement(
        achievement: Achievement, 
        progress: AchievementProgress, 
        event: AchievementEvent
    ): Boolean {
        return when (achievement.requirement) {
            is AchievementRequirement.PlayGames -> gamesPlayed >= achievement.requirement.count
            is AchievementRequirement.WinGames -> gamesWon >= achievement.requirement.count
            is AchievementRequirement.WinStreak -> bestWinStreak >= achievement.requirement.count
            is AchievementRequirement.TotalPlayTime -> totalPlayTimeMinutes >= achievement.requirement.hours * 60
            is AchievementRequirement.DefeatAIDifficulty -> {
                aiDefeats.getOrDefault(achievement.requirement.difficulty, 0) >= achievement.requirement.count
            }
            is AchievementRequirement.SolvePuzzles -> puzzlesSolved >= achievement.requirement.count
            is AchievementRequirement.WinTournaments -> tournamentsWon >= achievement.requirement.count
            is AchievementRequirement.AnalyzeGames -> gamesAnalyzed >= achievement.requirement.count
            is AchievementRequirement.CreateCustomTheme -> hasCreatedTheme
            is AchievementRequirement.TryAllThemes -> themesUsed.size >= getDefaultThemeCount()
            is AchievementRequirement.FindBrilliantMove -> brilliantMovesFound > 0
            
            is AchievementRequirement.FastWin -> {
                event is AchievementEvent.GameEnded && 
                event.won && 
                event.duration <= achievement.requirement.seconds * 1000
            }
            
            is AchievementRequirement.WinWithoutForbidden -> {
                event is AchievementEvent.GameEnded && 
                event.won && 
                !event.hadForbiddenMoves
            }
            
            is AchievementRequirement.WinAgainstHardAI -> {
                event is AchievementEvent.AIDefeated && 
                event.difficulty == "HARD"
            }
            
            else -> false
        }
    }
    
    /**
     * 진행도 업데이트
     */
    private fun updateProgress(achievement: Achievement, progress: AchievementProgress, event: AchievementEvent) {
        val newValue = calculateNewProgress(achievement, progress, event)
        if (newValue != progress.currentValue) {
            val updatedProgress = progress.copy(
                currentValue = newValue,
                bestValue = maxOf(progress.bestValue, newValue)
            )
            playerProgress[achievement.id] = updatedProgress
            
            notifyProgressUpdate(achievement, updatedProgress)
            
            if (updatedProgress.isCompleted && !updatedProgress.isUnlocked) {
                unlockAchievement(achievement.id)
            }
        }
    }
    
    /**
     * 새로운 진행도 계산
     */
    private fun calculateNewProgress(
        achievement: Achievement, 
        progress: AchievementProgress, 
        event: AchievementEvent
    ): Int {
        return when (achievement.requirement) {
            is AchievementRequirement.PlayGames -> gamesPlayed
            is AchievementRequirement.WinGames -> gamesWon
            is AchievementRequirement.WinStreak -> currentWinStreak
            is AchievementRequirement.TotalPlayTime -> (totalPlayTimeMinutes / 60).toInt()
            is AchievementRequirement.SolvePuzzles -> puzzlesSolved
            is AchievementRequirement.WinTournaments -> tournamentsWon
            is AchievementRequirement.AnalyzeGames -> gamesAnalyzed
            is AchievementRequirement.DefeatAIDifficulty -> 
                aiDefeats.getOrDefault(achievement.requirement.difficulty, 0)
            is AchievementRequirement.TryAllThemes -> themesUsed.size
            is AchievementRequirement.FindBrilliantMove -> brilliantMovesFound
            else -> progress.currentValue
        }
    }
    
    /**
     * 성취도 해제
     */
    fun unlockAchievement(achievementId: String) {
        val achievement = achievements[achievementId] ?: return
        val progress = playerProgress[achievementId] ?: return
        
        if (!progress.isUnlocked) {
            val updatedProgress = progress.copy(
                isUnlocked = true,
                unlockedAt = LocalDateTime.now()
            )
            playerProgress[achievementId] = updatedProgress
            
            notifyAchievementUnlocked(achievement, updatedProgress)
            saveProgress()
        }
    }
    
    /**
     * 초기 진행도 생성
     */
    private fun createInitialProgress(achievement: Achievement): AchievementProgress {
        val targetValue = when (achievement.requirement) {
            is AchievementRequirement.PlayGames -> achievement.requirement.count
            is AchievementRequirement.WinGames -> achievement.requirement.count
            is AchievementRequirement.WinStreak -> achievement.requirement.count
            is AchievementRequirement.TotalPlayTime -> achievement.requirement.hours
            is AchievementRequirement.DefeatAIDifficulty -> achievement.requirement.count
            is AchievementRequirement.SolvePuzzles -> achievement.requirement.count
            is AchievementRequirement.WinTournaments -> achievement.requirement.count
            is AchievementRequirement.AnalyzeGames -> achievement.requirement.count
            is AchievementRequirement.TryAllThemes -> getDefaultThemeCount()
            else -> 1
        }
        
        val progress = AchievementProgress(
            achievementId = achievement.id,
            targetValue = targetValue
        )
        playerProgress[achievement.id] = progress
        return progress
    }
    
    /**
     * 성취도 추가
     */
    fun addAchievement(achievement: Achievement) {
        achievements[achievement.id] = achievement
        if (!playerProgress.containsKey(achievement.id)) {
            createInitialProgress(achievement)
        }
    }
    
    /**
     * 모든 성취도 가져오기
     */
    fun getAllAchievements(): List<Achievement> {
        return achievements.values.sortedWith(
            compareBy<Achievement> { it.category.ordinal }
                .thenBy { it.points }
                .thenBy { it.name }
        )
    }
    
    /**
     * 카테고리별 성취도 가져오기
     */
    fun getAchievementsByCategory(category: AchievementCategory): List<Achievement> {
        return achievements.values.filter { it.category == category }
            .sortedWith(compareBy<Achievement> { it.points }.thenBy { it.name })
    }
    
    /**
     * 해제된 성취도 가져오기
     */
    fun getUnlockedAchievements(): List<Pair<Achievement, AchievementProgress>> {
        return achievements.values.mapNotNull { achievement ->
            val progress = playerProgress[achievement.id]
            if (progress?.isUnlocked == true) {
                achievement to progress
            } else null
        }.sortedByDescending { it.second.unlockedAt }
    }
    
    /**
     * 진행 중인 성취도 가져오기
     */
    fun getInProgressAchievements(): List<Pair<Achievement, AchievementProgress>> {
        return achievements.values.mapNotNull { achievement ->
            val progress = playerProgress[achievement.id]
            if (progress != null && !progress.isUnlocked && progress.currentValue > 0) {
                achievement to progress
            } else null
        }.sortedByDescending { it.second.progressPercentage }
    }
    
    /**
     * 성취도 진행도 가져오기
     */
    fun getProgress(achievementId: String): AchievementProgress? {
        return playerProgress[achievementId]
    }
    
    /**
     * 전체 통계 가져오기
     */
    fun getPlayerStats(): PlayerStats {
        return PlayerStats(
            gamesPlayed = gamesPlayed,
            gamesWon = gamesWon,
            winRate = if (gamesPlayed > 0) (gamesWon.toFloat() / gamesPlayed * 100) else 0f,
            currentWinStreak = currentWinStreak,
            bestWinStreak = bestWinStreak,
            totalPlayTimeMinutes = totalPlayTimeMinutes,
            achievementsUnlocked = playerProgress.values.count { it.isUnlocked },
            totalAchievements = achievements.size,
            totalPoints = playerProgress.values.filter { it.isUnlocked }
                .sumOf { progress ->
                    achievements[progress.achievementId]?.points ?: 0
                },
            puzzlesSolved = puzzlesSolved,
            tournamentsWon = tournamentsWon,
            gamesAnalyzed = gamesAnalyzed,
            themesUsed = themesUsed.size,
            brilliantMovesFound = brilliantMovesFound
        )
    }
    
    /**
     * 리스너 등록
     */
    fun addListener(listener: AchievementListener) {
        listeners.add(listener)
    }
    
    /**
     * 리스너 제거
     */
    fun removeListener(listener: AchievementListener) {
        listeners.remove(listener)
    }
    
    /**
     * 성취도 해제 알림
     */
    private fun notifyAchievementUnlocked(achievement: Achievement, progress: AchievementProgress) {
        listeners.forEach { it.onAchievementUnlocked(achievement, progress) }
    }
    
    /**
     * 진행도 업데이트 알림
     */
    private fun notifyProgressUpdate(achievement: Achievement, progress: AchievementProgress) {
        listeners.forEach { it.onProgressUpdated(achievement, progress) }
    }
    
    /**
     * 성취도 데이터 로드
     */
    private fun loadAchievements() {
        // 기본 성취도들 로드
        getDefaultAchievements().forEach { achievement ->
            achievements[achievement.id] = achievement
        }
    }
    
    /**
     * 플레이어 진행도 로드
     */
    private fun loadPlayerProgress() {
        try {
            val savedProgress = repository.loadProgress()
            savedProgress.forEach { (id, progress) ->
                playerProgress[id] = progress
            }
            
            val savedStats = repository.loadStats()
            savedStats?.let { stats ->
                gamesPlayed = stats.gamesPlayed
                gamesWon = stats.gamesWon
                currentWinStreak = stats.currentWinStreak
                bestWinStreak = stats.bestWinStreak
                totalPlayTimeMinutes = stats.totalPlayTimeMinutes
                puzzlesSolved = stats.puzzlesSolved
                tournamentsWon = stats.tournamentsWon
                gamesAnalyzed = stats.gamesAnalyzed
                // themesUsed는 저장된 값에서 복원하되, 실제로는 Set으로 관리
                // 기존 저장 값은 무시하고 새로 시작
                hasCreatedTheme = stats.hasCreatedTheme
                aiDefeats = stats.aiDefeats.toMutableMap()
                brilliantMovesFound = stats.brilliantMovesFound
            }
        } catch (e: Exception) {
            // 로드 실패 시 기본값 사용
        }
    }
    
    /**
     * 진행도 저장
     */
    private fun saveProgress() {
        repository.saveProgress(playerProgress.toMap())
        repository.saveStats(PlayerStats(
            gamesPlayed = gamesPlayed,
            gamesWon = gamesWon,
            winRate = if (gamesPlayed > 0) (gamesWon.toFloat() / gamesPlayed * 100) else 0f,
            currentWinStreak = currentWinStreak,
            bestWinStreak = bestWinStreak,
            totalPlayTimeMinutes = totalPlayTimeMinutes,
            achievementsUnlocked = playerProgress.values.count { it.isUnlocked },
            totalAchievements = achievements.size,
            totalPoints = playerProgress.values.filter { it.isUnlocked }
                .sumOf { progress ->
                    achievements[progress.achievementId]?.points ?: 0
                },
            puzzlesSolved = puzzlesSolved,
            tournamentsWon = tournamentsWon,
            gamesAnalyzed = gamesAnalyzed,
            themesUsed = themesUsed.size,
            brilliantMovesFound = brilliantMovesFound,
            hasCreatedTheme = hasCreatedTheme,
            aiDefeats = aiDefeats.toMap()
        ))
    }
    
    private fun getDefaultThemeCount(): Int = 6 // 기본 테마 수
    
    /**
     * 기본 성취도 목록
     */
    private fun getDefaultAchievements(): List<Achievement> {
        return listOf(
            // 게임 플레이 관련
            Achievement(
                id = "first_game",
                name = "첫 게임",
                description = "첫 번째 게임을 플레이하세요",
                category = AchievementCategory.GAMEPLAY,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.PlayGames(1),
                points = 5
            ),
            Achievement(
                id = "play_10_games",
                name = "게임 애호가",
                description = "10게임을 플레이하세요",
                category = AchievementCategory.GAMEPLAY,
                type = AchievementType.Cumulative(10),
                requirement = AchievementRequirement.PlayGames(10),
                points = 15
            ),
            Achievement(
                id = "play_50_games",
                name = "베테랑 플레이어",
                description = "50게임을 플레이하세요",
                category = AchievementCategory.GAMEPLAY,
                type = AchievementType.Cumulative(50),
                requirement = AchievementRequirement.PlayGames(50),
                points = 30
            ),
            
            // 승리 관련
            Achievement(
                id = "first_win",
                name = "첫 승리",
                description = "첫 번째 승리를 달성하세요",
                category = AchievementCategory.WINS,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.WinGames(1),
                points = 10
            ),
            Achievement(
                id = "win_streak_3",
                name = "연승 행진",
                description = "3연승을 달성하세요",
                category = AchievementCategory.WINS,
                type = AchievementType.Streak(3),
                requirement = AchievementRequirement.WinStreak(3),
                points = 20
            ),
            Achievement(
                id = "win_streak_5",
                name = "연승왕",
                description = "5연승을 달성하세요",
                category = AchievementCategory.WINS,
                type = AchievementType.Streak(5),
                requirement = AchievementRequirement.WinStreak(5),
                points = 35
            ),
            
            // AI 관련
            Achievement(
                id = "defeat_easy_ai",
                name = "AI 격파 - 쉬움",
                description = "쉬움 AI를 격파하세요",
                category = AchievementCategory.STRATEGY,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.DefeatAIDifficulty("EASY", 1),
                points = 10
            ),
            Achievement(
                id = "defeat_hard_ai",
                name = "AI 마스터",
                description = "어려움 AI를 격파하세요",
                category = AchievementCategory.STRATEGY,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.DefeatAIDifficulty("HARD", 1),
                points = 50
            ),
            
            // 시간 관련
            Achievement(
                id = "fast_win_30s",
                name = "번개같은 승리",
                description = "30초 이내에 승리하세요",
                category = AchievementCategory.TIME,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.FastWin(30),
                points = 25
            ),
            Achievement(
                id = "total_1h_play",
                name = "시간 투자자",
                description = "총 1시간 플레이하세요",
                category = AchievementCategory.TIME,
                type = AchievementType.Cumulative(1),
                requirement = AchievementRequirement.TotalPlayTime(1),
                points = 15
            ),
            
            // 특별한 승리
            Achievement(
                id = "perfect_game",
                name = "완벽한 게임",
                description = "금수 없이 승리하세요",
                category = AchievementCategory.SPECIAL,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.WinWithoutForbidden,
                points = 30
            ),
            
            // 퍼즐 관련
            Achievement(
                id = "solve_first_puzzle",
                name = "퍼즐 해결사",
                description = "첫 번째 퍼즐을 해결하세요",
                category = AchievementCategory.EXPLORATION,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.SolvePuzzles(1),
                points = 15
            ),
            
            // 토너먼트 관련
            Achievement(
                id = "first_tournament_win",
                name = "토너먼트 챔피언",
                description = "첫 토너먼트 우승을 차지하세요",
                category = AchievementCategory.WINS,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.FirstTournamentWin,
                points = 40
            ),
            
            // 분석 관련
            Achievement(
                id = "find_brilliant_move",
                name = "명수 발견자",
                description = "명수를 발견하세요",
                category = AchievementCategory.STRATEGY,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.FindBrilliantMove,
                points = 25
            ),
            
            // 커스터마이징 관련
            Achievement(
                id = "create_custom_theme",
                name = "디자이너",
                description = "커스텀 테마를 만드세요",
                category = AchievementCategory.EXPLORATION,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.CreateCustomTheme,
                points = 20
            ),
            Achievement(
                id = "try_all_themes",
                name = "테마 수집가",
                description = "모든 기본 테마를 사용해보세요",
                category = AchievementCategory.EXPLORATION,
                type = AchievementType.OneTime,
                requirement = AchievementRequirement.TryAllThemes,
                points = 25
            )
        )
    }
}

/**
 * 성취도 리스너
 */
interface AchievementListener {
    fun onAchievementUnlocked(achievement: Achievement, progress: AchievementProgress)
    fun onProgressUpdated(achievement: Achievement, progress: AchievementProgress)
}

/**
 * 플레이어 통계
 */
data class PlayerStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val winRate: Float = 0f,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val totalPlayTimeMinutes: Long = 0,
    val achievementsUnlocked: Int = 0,
    val totalAchievements: Int = 0,
    val totalPoints: Int = 0,
    val puzzlesSolved: Int = 0,
    val tournamentsWon: Int = 0,
    val gamesAnalyzed: Int = 0,
    val themesUsed: Int = 0,
    val brilliantMovesFound: Int = 0,
    val hasCreatedTheme: Boolean = false,
    val aiDefeats: Map<String, Int> = emptyMap()
) : java.io.Serializable