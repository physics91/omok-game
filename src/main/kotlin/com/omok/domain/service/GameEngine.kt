package com.omok.domain.service

import com.omok.domain.model.*

class GameEngine(
    private val ruleValidator: RuleValidator = RuleValidator(),
    private val aiStrategy: AIStrategy? = null
) {
    
    fun processMove(game: Game, position: Position): GameProcessResult {
        val currentState = game.getState()
        if (currentState !is GameState.Playing) {
            return GameProcessResult.Failure("Game is not in playing state")
        }
        
        val currentPlayer = game.getCurrentPlayer()
        
        if (!ruleValidator.isValidMove(game.getBoard(), position, currentPlayer)) {
            val reason = if (currentPlayer == Player.BLACK) "렌주룰 금수입니다" else "잘못된 수입니다"
            return GameProcessResult.Failure(reason)
        }
        
        val gameAfterMove = game.makeMove(position)
        val newState = ruleValidator.checkWin(gameAfterMove.getBoard(), position, currentPlayer)
        val finalGame = gameAfterMove.updateState(newState)
        
        return GameProcessResult.Success(finalGame)
    }
    
    fun processAIMove(game: Game): GameProcessResult {
        if (!game.isAITurn() || aiStrategy == null) {
            return GameProcessResult.Failure("Not AI turn or AI strategy not available")
        }
        
        val aiMove = aiStrategy.getBestMove(game.getBoard(), game.getCurrentPlayer())
            ?: return GameProcessResult.Failure("AI could not find a valid move")
            
        return processMove(game, aiMove)
    }
    
    fun undoMove(game: Game): GameProcessResult {
        val settings = game.getSettings()
        
        val gameAfterUndo = if (settings.mode == GameMode.PLAYER_VS_AI) {
            // PvE에서는 플레이어와 AI 수를 모두 되돌림
            game.undoMove()?.undoMove()
        } else {
            // PvP에서는 한 수만 되돌림
            game.undoMove()
        }
        
        return if (gameAfterUndo != null) {
            GameProcessResult.Success(gameAfterUndo)
        } else {
            GameProcessResult.Failure("Cannot undo move")
        }
    }
    
    fun startNewGame(settings: GameSettings): Game {
        return Game(settings)
    }
    
    fun getForbiddenMoves(game: Game): Set<Position> {
        if (game.getCurrentPlayer() != Player.BLACK) {
            return emptySet()
        }
        
        val forbiddenMoves = mutableSetOf<Position>()
        val board = game.getBoard()
        
        for (row in 0 until Position.BOARD_SIZE) {
            for (col in 0 until Position.BOARD_SIZE) {
                val position = Position(row, col)
                if (board.isEmpty(position) && 
                    !ruleValidator.isValidMove(board, position, Player.BLACK)) {
                    forbiddenMoves.add(position)
                }
            }
        }
        
        return forbiddenMoves
    }
}

sealed class GameProcessResult {
    data class Success(val game: Game) : GameProcessResult()
    data class Failure(val reason: String) : GameProcessResult()
}