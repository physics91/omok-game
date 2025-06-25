package com.omok.domain.event

import com.omok.domain.model.*

sealed class GameEvent {
    data class GameStarted(val game: Game) : GameEvent()
    data class MoveMade(val game: Game, val move: Move) : GameEvent()
    data class GameEnded(val game: Game, val result: GameState) : GameEvent()
    data class MoveUndone(val game: Game) : GameEvent()
    data class InvalidMoveAttempted(val position: Position, val reason: String) : GameEvent()
    data class AIThinking(val game: Game) : GameEvent()
}

interface GameEventHandler {
    fun handle(event: GameEvent)
}

class GameEventBus {
    private val handlers = mutableListOf<GameEventHandler>()
    
    fun subscribe(handler: GameEventHandler) {
        handlers.add(handler)
    }
    
    fun unsubscribe(handler: GameEventHandler) {
        handlers.remove(handler)
    }
    
    fun publish(event: GameEvent) {
        handlers.forEach { it.handle(event) }
    }
}