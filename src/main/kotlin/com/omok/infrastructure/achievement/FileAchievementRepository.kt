package com.omok.infrastructure.achievement

import com.omok.domain.achievement.Achievement
import com.omok.domain.achievement.AchievementProgress
import com.omok.domain.achievement.AchievementRepository
import com.omok.domain.achievement.PlayerStats
import com.omok.infrastructure.logging.Logger
import java.io.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 파일 기반 성취도 저장소 구현
 */
class FileAchievementRepository : AchievementRepository {
    
    companion object {
        private const val PROGRESS_FILE = "achievements_progress.dat"
        private const val STATS_FILE = "player_stats.dat"
        private const val CUSTOM_ACHIEVEMENTS_DIR = "custom_achievements"
    }
    
    init {
        // 커스텀 성취도 디렉토리 생성
        val customDir = File(CUSTOM_ACHIEVEMENTS_DIR)
        if (!customDir.exists()) {
            customDir.mkdirs()
        }
    }
    
    override fun saveProgress(progress: Map<String, AchievementProgress>) {
        try {
            ObjectOutputStream(FileOutputStream(PROGRESS_FILE)).use { out ->
                out.writeObject(HashMap(progress))
            }
            Logger.info("FileAchievementRepository", "Achievement progress saved successfully")
        } catch (e: Exception) {
            Logger.error("FileAchievementRepository", "Failed to save achievement progress", e)
        }
    }
    
    override fun loadProgress(): Map<String, AchievementProgress> {
        val file = File(PROGRESS_FILE)
        if (!file.exists()) {
            return emptyMap()
        }
        
        return try {
            ObjectInputStream(FileInputStream(file)).use { input ->
                val loaded = input.readObject() as HashMap<String, AchievementProgress>
                ConcurrentHashMap(loaded)
            }
        } catch (e: Exception) {
            Logger.error("FileAchievementRepository", "Failed to load achievement progress", e)
            emptyMap()
        }
    }
    
    override fun saveStats(stats: PlayerStats) {
        try {
            ObjectOutputStream(FileOutputStream(STATS_FILE)).use { out ->
                out.writeObject(stats)
            }
            Logger.info("FileAchievementRepository", "Player stats saved successfully")
        } catch (e: Exception) {
            Logger.error("FileAchievementRepository", "Failed to save player stats", e)
        }
    }
    
    override fun loadStats(): PlayerStats? {
        val file = File(STATS_FILE)
        if (!file.exists()) {
            return null
        }
        
        return try {
            ObjectInputStream(FileInputStream(file)).use { input ->
                input.readObject() as PlayerStats
            }
        } catch (e: Exception) {
            Logger.error("FileAchievementRepository", "Failed to load player stats", e)
            null
        }
    }
    
    override fun saveCustomAchievement(achievement: Achievement) {
        val file = File(CUSTOM_ACHIEVEMENTS_DIR, "${achievement.id}.achievement")
        try {
            ObjectOutputStream(FileOutputStream(file)).use { out ->
                out.writeObject(achievement)
            }
            Logger.info("FileAchievementRepository", "Custom achievement saved: ${achievement.name}")
        } catch (e: Exception) {
            Logger.error("FileAchievementRepository", "Failed to save custom achievement", e)
        }
    }
    
    override fun loadCustomAchievements(): List<Achievement> {
        val dir = File(CUSTOM_ACHIEVEMENTS_DIR)
        if (!dir.exists()) {
            return emptyList()
        }
        
        val achievements = mutableListOf<Achievement>()
        
        dir.listFiles { file -> file.extension == "achievement" }?.forEach { file ->
            try {
                ObjectInputStream(FileInputStream(file)).use { input ->
                    val achievement = input.readObject() as Achievement
                    achievements.add(achievement)
                }
            } catch (e: Exception) {
                Logger.error("FileAchievementRepository", "Failed to load custom achievement from ${file.name}", e)
            }
        }
        
        return achievements
    }
    
    override fun reset() {
        try {
            // 진행도 파일 삭제
            val progressFile = File(PROGRESS_FILE)
            if (progressFile.exists()) {
                progressFile.delete()
                Logger.info("FileAchievementRepository", "Achievement progress file deleted")
            }
            
            // 통계 파일 삭제
            val statsFile = File(STATS_FILE)
            if (statsFile.exists()) {
                statsFile.delete()
                Logger.info("FileAchievementRepository", "Player stats file deleted")
            }
            
            // 커스텀 성취도 파일들 삭제
            val customDir = File(CUSTOM_ACHIEVEMENTS_DIR)
            if (customDir.exists()) {
                customDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
                Logger.info("FileAchievementRepository", "Custom achievements deleted")
            }
            
        } catch (e: Exception) {
            Logger.error("FileAchievementRepository", "Failed to reset achievement data", e)
        }
    }
}