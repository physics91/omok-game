package com.omok.infrastructure.persistence

import com.omok.domain.model.Game
import com.omok.domain.model.GameSerializer
import com.omok.infrastructure.logging.Logger
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timer

/**
 * 자동 저장 서비스
 */
class AutoSaveService {
    companion object {
        private const val SAVE_DIR = "saves"
        private const val AUTO_SAVE_INTERVAL = 30_000L // 30초
        private const val MAX_AUTO_SAVES = 10
        private const val AUTO_SAVE_PREFIX = "autosave_"
    }
    
    private var autoSaveTimer: Timer? = null
    private var currentGame: Game? = null
    private val saveDirectory = File(SAVE_DIR)
    
    init {
        // 저장 디렉토리 생성
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs()
        }
    }
    
    /**
     * 자동 저장 시작
     */
    fun startAutoSave(game: Game) {
        currentGame = game
        stopAutoSave() // 기존 타이머 정지
        
        autoSaveTimer = timer(period = AUTO_SAVE_INTERVAL, initialDelay = AUTO_SAVE_INTERVAL) {
            currentGame?.let { 
                autoSave(it)
            }
        }
        
        Logger.info("AutoSaveService", "Auto-save started with ${AUTO_SAVE_INTERVAL/1000}s interval")
    }
    
    /**
     * 자동 저장 정지
     */
    fun stopAutoSave() {
        autoSaveTimer?.cancel()
        autoSaveTimer = null
        Logger.info("AutoSaveService", "Auto-save stopped")
    }
    
    /**
     * 게임 업데이트
     */
    fun updateGame(game: Game) {
        currentGame = game
    }
    
    /**
     * 자동 저장 수행
     */
    private fun autoSave(game: Game) {
        try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "${AUTO_SAVE_PREFIX}${timestamp}.omok"
            val file = File(saveDirectory, fileName)
            
            val serialized = GameSerializer.serialize(game)
            file.writeText(serialized)
            
            Logger.info("AutoSaveService", "Game auto-saved to: $fileName")
            
            // 오래된 자동 저장 파일 삭제
            cleanupOldAutoSaves()
        } catch (e: Exception) {
            Logger.error("AutoSaveService", "Failed to auto-save game", e)
        }
    }
    
    /**
     * 수동 저장
     */
    fun saveGame(game: Game, fileName: String): Boolean {
        return try {
            val file = File(saveDirectory, "$fileName.omok")
            val serialized = GameSerializer.serialize(game)
            file.writeText(serialized)
            
            Logger.info("AutoSaveService", "Game saved to: $fileName.omok")
            true
        } catch (e: Exception) {
            Logger.error("AutoSaveService", "Failed to save game", e)
            false
        }
    }
    
    /**
     * 게임 불러오기
     */
    fun loadGame(fileName: String): Game? {
        return try {
            val file = File(saveDirectory, fileName)
            if (!file.exists()) {
                Logger.warn("AutoSaveService", "Save file not found: $fileName")
                return null
            }
            
            val content = file.readText()
            val game = GameSerializer.deserialize(content)
            
            if (game != null) {
                Logger.info("AutoSaveService", "Game loaded from: $fileName")
            } else {
                Logger.error("AutoSaveService", "Failed to deserialize game from: $fileName")
            }
            
            game
        } catch (e: Exception) {
            Logger.error("AutoSaveService", "Failed to load game", e)
            null
        }
    }
    
    /**
     * 저장된 게임 목록
     */
    fun getSavedGames(): List<SavedGameInfo> {
        return try {
            saveDirectory.listFiles { file ->
                file.isFile && file.extension == "omok"
            }?.map { file ->
                SavedGameInfo(
                    fileName = file.name,
                    displayName = file.nameWithoutExtension,
                    lastModified = LocalDateTime.ofEpochSecond(
                        file.lastModified() / 1000, 0, 
                        java.time.ZoneOffset.systemDefault().rules.getOffset(java.time.Instant.now())
                    ),
                    isAutoSave = file.name.startsWith(AUTO_SAVE_PREFIX)
                )
            }?.sortedByDescending { it.lastModified } ?: emptyList()
        } catch (e: Exception) {
            Logger.error("AutoSaveService", "Failed to list saved games", e)
            emptyList()
        }
    }
    
    /**
     * 가장 최근 자동 저장 파일
     */
    fun getLatestAutoSave(): SavedGameInfo? {
        return getSavedGames()
            .filter { it.isAutoSave }
            .maxByOrNull { it.lastModified }
    }
    
    /**
     * 오래된 자동 저장 파일 정리
     */
    private fun cleanupOldAutoSaves() {
        try {
            val autoSaves = saveDirectory.listFiles { file ->
                file.isFile && file.name.startsWith(AUTO_SAVE_PREFIX)
            }?.sortedByDescending { it.lastModified() } ?: return
            
            // 최근 N개만 남기고 삭제
            autoSaves.drop(MAX_AUTO_SAVES).forEach { file ->
                if (file.delete()) {
                    Logger.debug("AutoSaveService", "Deleted old auto-save: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Logger.error("AutoSaveService", "Failed to cleanup old auto-saves", e)
        }
    }
}

/**
 * 저장된 게임 정보
 */
data class SavedGameInfo(
    val fileName: String,
    val displayName: String,
    val lastModified: LocalDateTime,
    val isAutoSave: Boolean
)