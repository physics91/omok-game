package com.omok.application.usecase

import com.omok.domain.event.GameEvent
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.Game
import com.omok.domain.model.GameState
import com.omok.domain.service.GameEngine
import com.omok.domain.service.GameProcessResult
import kotlinx.coroutines.*

class ProcessAIMoveUseCase(
    private val gameEngine: GameEngine,
    private val eventBus: GameEventBus
) {
    
    suspend fun execute(game: Game): Game? = withContext(Dispatchers.Default) {
        if (!game.isAITurn()) return@withContext null
        
        eventBus.publish(GameEvent.AIThinking(game))
        
        // AI 사고 시간 시뮬레이션
        delay(500)
        
        val result = gameEngine.processAIMove(game)
        
        return@withContext when (result) {
            is GameProcessResult.Success -> {
                val updatedGame = result.game
                val move = updatedGame.getLastMove()!!
                
                withContext(Dispatchers.Main) {
                    eventBus.publish(GameEvent.MoveMade(updatedGame, move))
                    
                    if (updatedGame.getState() != GameState.Playing) {
                        eventBus.publish(GameEvent.GameEnded(updatedGame, updatedGame.getState()))
                    }
                }
                
                updatedGame
            }
            is GameProcessResult.Failure -> null
        }
    }
}