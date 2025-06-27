package com.omok.domain.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 게임 직렬화 유틸리티 - 간단한 텍스트 형식 사용
 */
object GameSerializer {
    private const val VERSION = "1.0"
    private const val SEPARATOR = "|"
    
    /**
     * 게임을 문자열로 직렬화
     */
    fun serialize(game: Game): String {
        val lines = mutableListOf<String>()
        
        // 헤더
        lines.add("OMOK_SAVE_V$VERSION")
        lines.add("timestamp${SEPARATOR}${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
        
        // 게임 설정
        val settings = game.getSettings()
        lines.add("mode${SEPARATOR}${settings.mode.name}")
        lines.add("ai_difficulty${SEPARATOR}${settings.aiDifficulty?.name ?: "NONE"}")
        lines.add("rule${SEPARATOR}${settings.gameRule.name}")
        lines.add("current_player${SEPARATOR}${game.getCurrentPlayer().name}")
        
        // 무브 히스토리
        lines.add("moves${SEPARATOR}${game.getMoveHistory().size}")
        game.getMoveHistory().forEach { move ->
            lines.add("${move.position.row},${move.position.col},${move.player.name},${move.moveNumber}")
        }
        
        return lines.joinToString("\n")
    }
    
    /**
     * 문자열에서 게임 역직렬화
     */
    fun deserialize(content: String): Game? {
        return try {
            val lines = content.lines()
            if (lines.isEmpty() || !lines[0].startsWith("OMOK_SAVE_V")) {
                return null
            }
            
            val data = mutableMapOf<String, String>()
            val moves = mutableListOf<String>()
            var moveCount = 0
            var readingMoves = false
            
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue
                
                if (readingMoves) {
                    moves.add(line)
                } else if (line.contains(SEPARATOR)) {
                    val parts = line.split(SEPARATOR, limit = 2)
                    if (parts.size == 2) {
                        data[parts[0]] = parts[1]
                        if (parts[0] == "moves") {
                            moveCount = parts[1].toIntOrNull() ?: 0
                            readingMoves = true
                        }
                    }
                }
            }
            
            // 설정 복원
            val gameMode = GameMode.valueOf(data["mode"] ?: return null)
            val aiDifficulty = data["ai_difficulty"]?.let { 
                if (it != "NONE") AIDifficulty.valueOf(it) else null 
            }
            val gameRule = GameRule.valueOf(data["rule"] ?: "STANDARD_RENJU")
            val settings = GameSettings(gameMode, aiDifficulty, gameRule)
            
            // 게임 생성 및 수 복원
            var game = Game(settings)
            
            moves.take(moveCount).forEach { moveStr ->
                val parts = moveStr.split(",")
                if (parts.size >= 2) {
                    val row = parts[0].toIntOrNull() ?: return null
                    val col = parts[1].toIntOrNull() ?: return null
                    game = game.makeMove(Position(row, col))
                }
            }
            
            game
        } catch (e: Exception) {
            null
        }
    }
}