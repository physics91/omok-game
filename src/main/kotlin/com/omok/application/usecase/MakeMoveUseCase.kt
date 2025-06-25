package com.omok.application.usecase

import com.omok.domain.event.GameEvent
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.Game
import com.omok.domain.model.GameState
import com.omok.domain.model.Position
import com.omok.domain.service.GameEngine
import com.omok.domain.service.GameProcessResult

class MakeMoveUseCase(
    private val gameEngine: GameEngine,
    private val eventBus: GameEventBus
) {
    
    fun execute(game: Game, position: Position): Game? {
        val result = gameEngine.processMove(game, position)
        
        return when (result) {
            is GameProcessResult.Success -> {
                val updatedGame = result.game
                val move = updatedGame.getLastMove()!!
                
                eventBus.publish(GameEvent.MoveMade(updatedGame, move))
                
                if (updatedGame.getState() != GameState.Playing) {
                    eventBus.publish(GameEvent.GameEnded(updatedGame, updatedGame.getState()))
                }
                
                updatedGame
            }
            is GameProcessResult.Failure -> {
                eventBus.publish(GameEvent.InvalidMoveAttempted(position, result.reason))
                null
            }
        }
    }
}