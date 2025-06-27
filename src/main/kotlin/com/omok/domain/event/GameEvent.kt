package com.omok.domain.event

import com.omok.domain.model.*

sealed class GameEvent {
    data class GameStarted(val game: Game) : GameEvent()
    data class MoveMade(val game: Game, val move: Move) : GameEvent()
    data class GameEnded(val game: Game, val result: GameState) : GameEvent()
    data class MoveUndone(val game: Game) : GameEvent()
    data class InvalidMoveAttempted(val position: Position, val reason: String) : GameEvent()
    data class AIThinking(val game: Game) : GameEvent()
    data class AIThinkingProgress(val thinkingInfo: AIThinkingInfo) : GameEvent()
    
    // 오픈 렌주룰 이벤트
    data class SwapDecision(val game: Game, val swapped: Boolean) : GameEvent()
    data class FifthMovesProposed(val game: Game, val positions: List<Position>) : GameEvent()
    data class InvalidAction(val reason: String) : GameEvent()
}

interface GameEventHandler {
    fun handle(event: GameEvent)
}

object GameEventBus {
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