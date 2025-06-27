package com.omok.infrastructure.statistics

import com.omok.domain.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 게임 통계 관리
 */
data class GameStatistics(
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val winsByRule: Map<GameRule, Int> = emptyMap(),
    val winsByMode: Map<GameMode, Int> = emptyMap(),
    val averageGameLength: Double = 0.0,
    val longestWinStreak: Int = 0,
    val currentWinStreak: Int = 0,
    val lastPlayed: LocalDateTime? = null
) {
    val winRate: Double
        get() = if (totalGames > 0) (wins.toDouble() / totalGames.toDouble()) * 100 else 0.0
    
    val lossRate: Double
        get() = if (totalGames > 0) (losses.toDouble() / totalGames.toDouble()) * 100 else 0.0
    
    val drawRate: Double
        get() = if (totalGames > 0) (draws.toDouble() / totalGames.toDouble()) * 100 else 0.0
}

/**
 * 게임 기록
 */
data class GameRecord(
    val result: GameResult,
    val mode: GameMode,
    val rule: GameRule,
    val aiDifficulty: AIDifficulty? = null,
    val gameLength: Int, // 수의 개수
    val duration: Long, // 게임 시간 (밀리초)
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class GameResult {
    WIN, LOSS, DRAW
}

/**
 * 게임 통계 서비스
 */
class GameStatisticsService {
    private var statistics = GameStatistics()
    private val gameRecords = mutableListOf<GameRecord>()
    private var currentWinStreak = 0
    private var longestWinStreak = 0
    
    /**
     * 게임 결과 기록
     */
    fun recordGame(
        result: GameResult,
        mode: GameMode,
        rule: GameRule,
        aiDifficulty: AIDifficulty? = null,
        gameLength: Int,
        duration: Long
    ) {
        val record = GameRecord(result, mode, rule, aiDifficulty, gameLength, duration)
        gameRecords.add(record)
        
        // 연승 기록 업데이트
        when (result) {
            GameResult.WIN -> {
                currentWinStreak++
                if (currentWinStreak > longestWinStreak) {
                    longestWinStreak = currentWinStreak
                }
            }
            GameResult.LOSS, GameResult.DRAW -> {
                currentWinStreak = 0
            }
        }
        
        updateStatistics()
    }
    
    /**
     * 게임 결과를 도메인 모델에서 변환
     */
    fun recordGameFromDomain(
        game: Game,
        gameState: GameState,
        duration: Long
    ) {
        val result = when (gameState) {
            is GameState.Won -> {
                when (game.getSettings().mode) {
                    GameMode.PLAYER_VS_PLAYER -> {
                        // PvP 모드에서는 흑돌이 플레이어라고 가정
                        if (gameState.winner == Player.BLACK) GameResult.WIN else GameResult.LOSS
                    }
                    GameMode.PLAYER_VS_AI -> {
                        // 플레이어는 항상 흑돌
                        if (gameState.winner == Player.BLACK) GameResult.WIN else GameResult.LOSS
                    }
                }
            }
            is GameState.Draw -> GameResult.DRAW
            else -> return // 게임이 끝나지 않은 경우
        }
        
        recordGame(
            result = result,
            mode = game.getSettings().mode,
            rule = game.getSettings().gameRule,
            aiDifficulty = game.getSettings().aiDifficulty,
            gameLength = game.getMoveHistory().size,
            duration = duration
        )
    }
    
    /**
     * 통계 업데이트
     */
    private fun updateStatistics() {
        val wins = gameRecords.count { it.result == GameResult.WIN }
        val losses = gameRecords.count { it.result == GameResult.LOSS }
        val draws = gameRecords.count { it.result == GameResult.DRAW }
        
        val winsByRule = gameRecords
            .filter { it.result == GameResult.WIN }
            .groupBy { it.rule }
            .mapValues { it.value.size }
        
        val winsByMode = gameRecords
            .filter { it.result == GameResult.WIN }
            .groupBy { it.mode }
            .mapValues { it.value.size }
        
        val averageGameLength = if (gameRecords.isNotEmpty()) {
            gameRecords.map { it.gameLength }.average()
        } else 0.0
        
        statistics = GameStatistics(
            totalGames = gameRecords.size,
            wins = wins,
            losses = losses,
            draws = draws,
            winsByRule = winsByRule,
            winsByMode = winsByMode,
            averageGameLength = averageGameLength,
            longestWinStreak = longestWinStreak,
            currentWinStreak = currentWinStreak,
            lastPlayed = gameRecords.lastOrNull()?.timestamp
        )
    }
    
    /**
     * 현재 통계 반환
     */
    fun getStatistics(): GameStatistics = statistics
    
    /**
     * 게임 기록 반환
     */
    fun getGameRecords(): List<GameRecord> = gameRecords.toList()
    
    /**
     * 최근 N개 게임의 통계
     */
    fun getRecentStatistics(count: Int): GameStatistics {
        val recentRecords = gameRecords.takeLast(count)
        if (recentRecords.isEmpty()) return GameStatistics()
        
        val wins = recentRecords.count { it.result == GameResult.WIN }
        val losses = recentRecords.count { it.result == GameResult.LOSS }
        val draws = recentRecords.count { it.result == GameResult.DRAW }
        
        return GameStatistics(
            totalGames = recentRecords.size,
            wins = wins,
            losses = losses,
            draws = draws,
            averageGameLength = recentRecords.map { it.gameLength }.average(),
            lastPlayed = recentRecords.lastOrNull()?.timestamp
        )
    }
    
    /**
     * 특정 모드/룰에 대한 통계
     */
    fun getStatisticsFor(mode: GameMode? = null, rule: GameRule? = null): GameStatistics {
        val filteredRecords = gameRecords.filter { record ->
            (mode == null || record.mode == mode) &&
            (rule == null || record.rule == rule)
        }
        
        if (filteredRecords.isEmpty()) return GameStatistics()
        
        val wins = filteredRecords.count { it.result == GameResult.WIN }
        val losses = filteredRecords.count { it.result == GameResult.LOSS }
        val draws = filteredRecords.count { it.result == GameResult.DRAW }
        
        return GameStatistics(
            totalGames = filteredRecords.size,
            wins = wins,
            losses = losses,
            draws = draws,
            averageGameLength = filteredRecords.map { it.gameLength }.average(),
            lastPlayed = filteredRecords.lastOrNull()?.timestamp
        )
    }
    
    /**
     * 통계 초기화
     */
    fun resetStatistics() {
        gameRecords.clear()
        currentWinStreak = 0
        longestWinStreak = 0
        updateStatistics()
    }
    
    /**
     * 통계를 텍스트로 포맷
     */
    fun formatStatistics(): String {
        val stats = getStatistics()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        
        return buildString {
            appendLine("=== 게임 통계 ===")
            appendLine("총 게임 수: ${stats.totalGames}")
            if (stats.totalGames > 0) {
                appendLine("승률: ${"%.1f".format(stats.winRate)}% (${stats.wins}승)")
                appendLine("패율: ${"%.1f".format(stats.lossRate)}% (${stats.losses}패)")
                appendLine("무승부: ${"%.1f".format(stats.drawRate)}% (${stats.draws}무)")
                appendLine("평균 게임 길이: ${"%.1f".format(stats.averageGameLength)}수")
                appendLine("최고 연승: ${stats.longestWinStreak}연승")
                appendLine("현재 연승: ${stats.currentWinStreak}연승")
                
                if (stats.lastPlayed != null) {
                    appendLine("마지막 게임: ${stats.lastPlayed.format(formatter)}")
                }
                
                if (stats.winsByMode.isNotEmpty()) {
                    appendLine()
                    appendLine("모드별 승수:")
                    stats.winsByMode.forEach { (mode, wins) ->
                        val modeName = when (mode) {
                            GameMode.PLAYER_VS_PLAYER -> "사람 vs 사람"
                            GameMode.PLAYER_VS_AI -> "사람 vs AI"
                        }
                        appendLine("  $modeName: ${wins}승")
                    }
                }
                
                if (stats.winsByRule.isNotEmpty()) {
                    appendLine()
                    appendLine("룰별 승수:")
                    stats.winsByRule.forEach { (rule, wins) ->
                        appendLine("  ${rule.displayName}: ${wins}승")
                    }
                }
            } else {
                appendLine("아직 게임 기록이 없습니다.")
            }
        }
    }
    
    companion object {
        private var instance: GameStatisticsService? = null
        
        fun getInstance(): GameStatisticsService {
            if (instance == null) {
                instance = GameStatisticsService()
            }
            return instance!!
        }
    }
}