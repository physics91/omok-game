package com.omok.application.usecase

import com.omok.domain.event.GameEvent
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.Game
import com.omok.domain.model.GameSettings
import com.omok.domain.service.GameEngine

class StartGameUseCase(
    private val gameEngine: GameEngine
) {
    
    fun execute(settings: GameSettings): Game {
        val game = gameEngine.startNewGame(settings)
        GameEventBus.publish(GameEvent.GameStarted(game))
        return game
    }
}