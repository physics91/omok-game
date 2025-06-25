package com.omok.application.service

import com.omok.application.dto.*
import com.omok.application.mapper.GameMapper
import com.omok.application.usecase.*
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.*
import com.omok.domain.service.GameEngine
import kotlinx.coroutines.*

class GameApplicationService(
    private val startGameUseCase: StartGameUseCase,
    private val makeMoveUseCase: MakeMoveUseCase,
    private val undoMoveUseCase: UndoMoveUseCase,
    private val processAIMoveUseCase: ProcessAIMoveUseCase,
    private val gameEngine: GameEngine
) {
    
    private var currentGame: Game? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    fun startNewGame(settings: GameSettings): Game {
        currentGame = startGameUseCase.execute(settings)
        return currentGame!!
    }
    
    fun startNewGameDto(settingsDto: GameSettingsDto): GameDto? {
        val settings = GameMapper.toDomain(settingsDto)
        val game = startNewGame(settings)
        return GameMapper.toDto(game)
    }
    
    fun makeMove(position: Position): Game? {
        val game = currentGame ?: return null
        val updatedGame = makeMoveUseCase.execute(game, position)
        
        if (updatedGame != null) {
            currentGame = updatedGame
            
            // AI 턴이면 자동으로 AI 수를 처리
            if (updatedGame.isAITurn()) {
                processAIMove()
            }
        }
        
        return updatedGame
    }
    
    fun makeMoveDto(positionDto: PositionDto): GameDto? {
        val position = GameMapper.toDomain(positionDto)
        val game = makeMove(position)
        return game?.let { GameMapper.toDto(it) }
    }
    
    fun undoMove(): Game? {
        val game = currentGame ?: return null
        val updatedGame = undoMoveUseCase.execute(game)
        
        if (updatedGame != null) {
            currentGame = updatedGame
        }
        
        return updatedGame
    }
    
    fun undoMoveDto(): GameDto? {
        val game = undoMove()
        return game?.let { GameMapper.toDto(it) }
    }
    
    private fun processAIMove() {
        coroutineScope.launch {
            val game = currentGame ?: return@launch
            val updatedGame = processAIMoveUseCase.execute(game)
            
            if (updatedGame != null) {
                currentGame = updatedGame
            }
        }
    }
    
    fun getCurrentGame(): Game? = currentGame
    
    fun getCurrentGameDto(): GameDto? {
        val game = currentGame
        return game?.let { GameMapper.toDto(it) }
    }
    
    fun getForbiddenMoves(): Set<Position> {
        val game = currentGame ?: return emptySet()
        return gameEngine.getForbiddenMoves(game)
    }
    
    fun getForbiddenMovesDto(): Set<PositionDto> {
        val positions = getForbiddenMoves()
        return positions.map { GameMapper.toDto(it) }.toSet()
    }
    
    fun cleanup() {
        coroutineScope.cancel()
    }
}