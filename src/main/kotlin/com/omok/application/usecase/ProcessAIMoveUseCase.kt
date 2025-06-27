package com.omok.application.usecase

import com.omok.domain.event.GameEvent
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.Game
import com.omok.domain.model.GameState
import com.omok.domain.service.GameEngine
import com.omok.domain.service.GameProcessResult
import com.omok.infrastructure.logging.Logger
import kotlinx.coroutines.*

class ProcessAIMoveUseCase(
    private val gameEngine: GameEngine,
    private var aiThinkingTimeMs: Long = 1500L
) {
    
    suspend fun execute(game: Game): Game? = withContext(Dispatchers.Default) {
        if (!game.isAITurn()) return@withContext null
        
        val currentState = game.getState()
        Logger.info("ProcessAIMoveUseCase", "Processing AI move in state: $currentState")
        
        GameEventBus.publish(GameEvent.AIThinking(game))
        
        // AI 사고 시간
        delay(aiThinkingTimeMs)
        
        val result = gameEngine.processAIMove(game)
        
        return@withContext when (result) {
            is GameProcessResult.Success -> {
                val updatedGame = result.game
                
                withContext(Dispatchers.Main) {
                    // 상태에 따른 적절한 이벤트 발행
                    when (currentState) {
                        is GameState.WaitingForSwap -> {
                            // 스왑 결정이 이미 GameEngine에서 처리되고 이벤트가 발행됨
                            Logger.info("ProcessAIMoveUseCase", "AI processed swap decision")
                        }
                        is GameState.WaitingForFifthMove -> {
                            // 5수 제시가 이미 처리됨
                            Logger.info("ProcessAIMoveUseCase", "AI proposed fifth moves")
                        }
                        is GameState.WaitingForFifthMoveSelection -> {
                            // 5수 선택이 이미 처리됨
                            Logger.info("ProcessAIMoveUseCase", "AI selected fifth move")
                        }
                        is GameState.Playing -> {
                            // 일반적인 수인 경우에만 MoveMade 이벤트 발행
                            val move = updatedGame.getLastMove()
                            if (move != null) {
                                GameEventBus.publish(GameEvent.MoveMade(updatedGame, move))
                            }
                        }
                        else -> {
                            Logger.warn("ProcessAIMoveUseCase", "Unexpected state after AI move: ${updatedGame.getState()}")
                        }
                    }
                    
                    // 게임 종료 상태 확인
                    val newState = updatedGame.getState()
                    if (newState is GameState.Won || newState is GameState.Draw) {
                        GameEventBus.publish(GameEvent.GameEnded(updatedGame, newState))
                    }
                }
                
                updatedGame
            }
            is GameProcessResult.Failure -> {
                Logger.error("ProcessAIMoveUseCase", "AI move failed: ${result.reason}")
                null
            }
        }
    }
    
    /**
     * AI 사고시간 설정 업데이트
     */
    fun updateAIThinkingTime(timeMs: Long) {
        aiThinkingTimeMs = timeMs
        Logger.info("ProcessAIMoveUseCase", "AI thinking time updated to ${timeMs}ms")
    }
}