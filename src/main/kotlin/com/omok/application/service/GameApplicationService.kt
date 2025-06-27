package com.omok.application.service

import com.omok.application.dto.*
import com.omok.application.mapper.GameMapper
import com.omok.application.usecase.*
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.*
import com.omok.domain.service.GameEngine
import com.omok.infrastructure.logging.ExceptionHandler
import com.omok.infrastructure.logging.safeCall
import com.omok.infrastructure.persistence.AutoSaveService
import kotlinx.coroutines.*

class GameApplicationService(
    private val startGameUseCase: StartGameUseCase,
    private val makeMoveUseCase: MakeMoveUseCase,
    private val undoMoveUseCase: UndoMoveUseCase,
    private val processAIMoveUseCase: ProcessAIMoveUseCase,
    private val gameEngine: GameEngine
) {
    
    private var currentGame: Game? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + ExceptionHandler.coroutineExceptionHandler)
    private val autoSaveService = AutoSaveService()
    
    fun startNewGame(settings: GameSettings): Game? {
        return safeCall {
            currentGame = startGameUseCase.execute(settings)
            currentGame?.let {
                autoSaveService.startAutoSave(it)
            }
            currentGame!!
        }
    }
    
    fun startNewGameDto(settingsDto: GameSettingsDto): GameDto? {
        return safeCall {
            val settings = GameMapper.toDomain(settingsDto)
            val game = startNewGame(settings) ?: return@safeCall null
            GameMapper.toDto(game)
        }
    }
    
    fun makeMove(position: Position): Game? {
        return safeCall {
            val game = currentGame ?: return@safeCall null
            val updatedGame = makeMoveUseCase.execute(game, position)
            
            if (updatedGame != null) {
                currentGame = updatedGame
                autoSaveService.updateGame(updatedGame)
                
                // AI 턴이면 자동으로 AI 수를 처리
                if (updatedGame.isAITurn()) {
                    processAIMove()
                }
            }
            
            updatedGame
        }
    }
    
    fun makeMoveDto(positionDto: PositionDto): GameDto? {
        return safeCall {
            val position = GameMapper.toDomain(positionDto)
            val game = makeMove(position)
            game?.let { GameMapper.toDto(it) }
        }
    }
    
    fun undoMove(): Game? {
        return safeCall {
            val game = currentGame ?: return@safeCall null
            val updatedGame = undoMoveUseCase.execute(game)
            
            if (updatedGame != null) {
                currentGame = updatedGame
                autoSaveService.updateGame(updatedGame)
            }
            
            updatedGame
        }
    }
    
    fun undoMoveDto(): GameDto? {
        return safeCall {
            val game = undoMove()
            game?.let { GameMapper.toDto(it) }
        }
    }
    
    private fun processAIMove() {
        coroutineScope.launch {
            val game = currentGame ?: return@launch
            val updatedGame = processAIMoveUseCase.execute(game)
            
            if (updatedGame != null) {
                currentGame = updatedGame
                autoSaveService.updateGame(updatedGame)
            }
        }
    }
    
    fun getCurrentGame(): Game? = currentGame
    
    fun getCurrentGameDto(): GameDto? {
        val game = currentGame
        return game?.let { GameMapper.toDto(it) }
    }
    
    fun getForbiddenMoves(): Set<Position> {
        return safeCall {
            val game = currentGame ?: return@safeCall emptySet()
            gameEngine.getForbiddenMoves(game)
        } ?: emptySet()
    }
    
    fun getForbiddenMovesDto(): Set<PositionDto> {
        return safeCall {
            val positions = getForbiddenMoves()
            positions.map { GameMapper.toDto(it) }.toSet()
        } ?: emptySet()
    }
    
    /**
     * 오픈 렌주룰 - 스왑 처리
     */
    fun processSwap(swap: Boolean): Game? {
        return safeCall {
            val game = currentGame ?: return@safeCall null
            
            val result = gameEngine.processSwap(game, swap)
            when (result) {
                is com.omok.domain.service.GameProcessResult.Success -> {
                    currentGame = result.game
                    autoSaveService.updateGame(result.game)
                    GameEventBus.publish(com.omok.domain.event.GameEvent.SwapDecision(result.game, swap))
                    
                    // 스왑 후 AI 턴이면 AI 수 처리
                    if (result.game.isAITurn()) {
                        processAIMove()
                    }
                    
                    result.game
                }
                is com.omok.domain.service.GameProcessResult.Failure -> {
                    GameEventBus.publish(com.omok.domain.event.GameEvent.InvalidAction(result.reason))
                    null
                }
            }
        }
    }
    
    /**
     * 오픈 렌주룰 - 5수 제시
     */
    fun proposeFifthMoves(positions: List<Position>): Game? {
        return safeCall {
            val game = currentGame ?: return@safeCall null
            
            val result = gameEngine.proposeFifthMoves(game, positions)
            when (result) {
                is com.omok.domain.service.GameProcessResult.Success -> {
                    currentGame = result.game
                    autoSaveService.updateGame(result.game)
                    GameEventBus.publish(com.omok.domain.event.GameEvent.FifthMovesProposed(result.game, positions))
                    
                    // 5수 제시 후 AI 턴이면 AI가 선택
                    if (result.game.isAITurn()) {
                        processAIMove()
                    }
                    
                    result.game
                }
                is com.omok.domain.service.GameProcessResult.Failure -> {
                    GameEventBus.publish(com.omok.domain.event.GameEvent.InvalidAction(result.reason))
                    null
                }
            }
        }
    }
    
    /**
     * 오픈 렌주룰 - 5수 선택
     */
    fun selectFifthMove(position: Position): Game? {
        return safeCall {
            val game = currentGame ?: return@safeCall null
            
            val result = gameEngine.selectFifthMove(game, position)
            when (result) {
                is com.omok.domain.service.GameProcessResult.Success -> {
                    currentGame = result.game
                    autoSaveService.updateGame(result.game)
                    GameEventBus.publish(com.omok.domain.event.GameEvent.MoveMade(result.game, Move(position, Player.BLACK)))
                    
                    // 5수 선택 후 AI 턴이면 AI 수 처리
                    if (result.game.isAITurn()) {
                        processAIMove()
                    }
                    
                    result.game
                }
                is com.omok.domain.service.GameProcessResult.Failure -> {
                    GameEventBus.publish(com.omok.domain.event.GameEvent.InvalidAction(result.reason))
                    null
                }
            }
        }
    }
    
    fun cleanup() {
        autoSaveService.stopAutoSave()
        coroutineScope.cancel()
    }
    
    fun saveGame(fileName: String): Boolean {
        return safeCall {
            val game = currentGame ?: return@safeCall false
            autoSaveService.saveGame(game, fileName)
        } ?: false
    }
    
    fun loadGame(fileName: String): Game? {
        return safeCall {
            val game = autoSaveService.loadGame(fileName)
            if (game != null) {
                currentGame = game
                autoSaveService.startAutoSave(game)
                GameEventBus.publish(com.omok.domain.event.GameEvent.GameStarted(game))
            }
            game
        }
    }
    
    fun getSavedGames(): List<com.omok.infrastructure.persistence.SavedGameInfo> {
        return safeCall {
            autoSaveService.getSavedGames()
        } ?: emptyList()
    }
    
    fun loadLatestAutoSave(): Game? {
        return safeCall {
            val latestSave = autoSaveService.getLatestAutoSave()
            if (latestSave != null) {
                loadGame(latestSave.fileName)
            } else {
                null
            }
        }
    }
    
    /**
     * AI 사고시간 업데이트
     */
    fun updateAIThinkingTime(timeMs: Long) {
        processAIMoveUseCase.updateAIThinkingTime(timeMs)
    }
}