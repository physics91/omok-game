package com.omok.application.usecase

import com.omok.domain.event.GameEvent
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.Game
import com.omok.domain.service.GameEngine
import com.omok.domain.service.GameProcessResult

class UndoMoveUseCase(
    private val gameEngine: GameEngine
) {
    
    fun execute(game: Game): Game? {
        val result = gameEngine.undoMove(game)
        
        return when (result) {
            is GameProcessResult.Success -> {
                val updatedGame = result.game
                GameEventBus.publish(GameEvent.MoveUndone(updatedGame))
                updatedGame
            }
            is GameProcessResult.Failure -> null
        }
    }
}