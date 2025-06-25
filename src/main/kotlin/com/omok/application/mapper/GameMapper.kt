package com.omok.application.mapper

import com.omok.application.dto.*
import com.omok.domain.model.*

object GameMapper {
    
    fun toDto(position: Position): PositionDto {
        return PositionDto(position.row, position.col)
    }
    
    fun toDomain(positionDto: PositionDto): Position {
        return Position(positionDto.row, positionDto.col)
    }
    
    fun toDto(player: Player): PlayerDto {
        return when (player) {
            Player.BLACK -> PlayerDto.BLACK
            Player.WHITE -> PlayerDto.WHITE
        }
    }
    
    fun toDomain(playerDto: PlayerDto): Player {
        return when (playerDto) {
            PlayerDto.BLACK -> Player.BLACK
            PlayerDto.WHITE -> Player.WHITE
        }
    }
    
    fun toDto(gameMode: GameMode): GameModeDto {
        return when (gameMode) {
            GameMode.PLAYER_VS_PLAYER -> GameModeDto.PLAYER_VS_PLAYER
            GameMode.PLAYER_VS_AI -> GameModeDto.PLAYER_VS_AI
        }
    }
    
    fun toDomain(gameModeDto: GameModeDto): GameMode {
        return when (gameModeDto) {
            GameModeDto.PLAYER_VS_PLAYER -> GameMode.PLAYER_VS_PLAYER
            GameModeDto.PLAYER_VS_AI -> GameMode.PLAYER_VS_AI
        }
    }
    
    fun toDto(aiDifficulty: AIDifficulty): AIDifficultyDto {
        return when (aiDifficulty) {
            AIDifficulty.EASY -> AIDifficultyDto.EASY
            AIDifficulty.MEDIUM -> AIDifficultyDto.MEDIUM
            AIDifficulty.HARD -> AIDifficultyDto.HARD
        }
    }
    
    fun toDomain(aiDifficultyDto: AIDifficultyDto): AIDifficulty {
        return when (aiDifficultyDto) {
            AIDifficultyDto.EASY -> AIDifficulty.EASY
            AIDifficultyDto.MEDIUM -> AIDifficulty.MEDIUM
            AIDifficultyDto.HARD -> AIDifficulty.HARD
        }
    }
    
    fun toDto(gameRule: GameRule): GameRuleDto {
        return when (gameRule) {
            GameRule.STANDARD_RENJU -> GameRuleDto.STANDARD_RENJU
            GameRule.OPEN_RENJU -> GameRuleDto.OPEN_RENJU
            GameRule.YAMAGUCHI_RULE -> GameRuleDto.YAMAGUCHI_RULE
            GameRule.SWAP_RULE -> GameRuleDto.SWAP_RULE
            GameRule.SWAP2_RULE -> GameRuleDto.SWAP2_RULE
            GameRule.SOOSYRV_RULE -> GameRuleDto.SOOSYRV_RULE
            GameRule.TARAGUCHI_RULE -> GameRuleDto.TARAGUCHI_RULE
            GameRule.FREESTYLE -> GameRuleDto.FREESTYLE
            GameRule.CARO_RULE -> GameRuleDto.CARO_RULE
        }
    }
    
    fun toDomain(gameRuleDto: GameRuleDto): GameRule {
        return when (gameRuleDto) {
            GameRuleDto.STANDARD_RENJU -> GameRule.STANDARD_RENJU
            GameRuleDto.OPEN_RENJU -> GameRule.OPEN_RENJU
            GameRuleDto.YAMAGUCHI_RULE -> GameRule.YAMAGUCHI_RULE
            GameRuleDto.SWAP_RULE -> GameRule.SWAP_RULE
            GameRuleDto.SWAP2_RULE -> GameRule.SWAP2_RULE
            GameRuleDto.SOOSYRV_RULE -> GameRule.SOOSYRV_RULE
            GameRuleDto.TARAGUCHI_RULE -> GameRule.TARAGUCHI_RULE
            GameRuleDto.FREESTYLE -> GameRule.FREESTYLE
            GameRuleDto.CARO_RULE -> GameRule.CARO_RULE
        }
    }
    
    fun toDto(gameSettings: GameSettings): GameSettingsDto {
        return GameSettingsDto(
            mode = toDto(gameSettings.mode),
            aiDifficulty = gameSettings.aiDifficulty?.let { toDto(it) },
            gameRule = toDto(gameSettings.gameRule)
        )
    }
    
    fun toDomain(gameSettingsDto: GameSettingsDto): GameSettings {
        return GameSettings(
            mode = toDomain(gameSettingsDto.mode),
            aiDifficulty = gameSettingsDto.aiDifficulty?.let { toDomain(it) },
            gameRule = toDomain(gameSettingsDto.gameRule)
        )
    }
    
    fun toDto(board: Board): BoardDto {
        val stones = Array(15) { row ->
            Array(15) { col ->
                val position = Position(row, col)
                board.getStone(position)?.let { toDto(it) }
            }
        }
        return BoardDto(stones)
    }
    
    fun toDto(move: Move): MoveDto {
        return MoveDto(
            position = toDto(move.position),
            player = toDto(move.player),
            moveNumber = move.moveNumber
        )
    }
    
    fun toDto(gameState: GameState): GameStateDto {
        return when (gameState) {
            is GameState.Playing -> GameStateDto.Playing
            is GameState.Won -> GameStateDto.Won(
                winner = toDto(gameState.winner),
                winningLine = gameState.winningLine.map { toDto(it) }
            )
            is GameState.Draw -> GameStateDto.Draw
        }
    }
    
    fun toDto(game: Game): GameDto {
        return GameDto(
            board = toDto(game.getBoard()),
            currentPlayer = toDto(game.getCurrentPlayer()),
            state = toDto(game.getState()),
            moveHistory = game.getMoveHistory().map { toDto(it) },
            settings = toDto(game.getSettings()),
            isPlayerTurn = game.isPlayerTurn(),
            isAITurn = game.isAITurn()
        )
    }
}