package com.omok.presentation.controller

import com.omok.application.dto.*
import com.omok.application.mapper.GameMapper
import com.omok.application.service.GameApplicationService
import com.omok.domain.event.GameEvent
import com.omok.domain.event.GameEventHandler
import com.omok.presentation.ui.GameWindow
import javax.swing.SwingUtilities

class GameController(
    private val gameApplicationService: GameApplicationService,
    private val gameWindow: GameWindow
) : GameEventHandler {
    
    init {
        // 이벤트 핸들러 등록
        gameApplicationService.getCurrentGame()?.let { game ->
            // EventBus에 핸들러 등록은 DependencyContainer에서 처리
        }
    }
    
    override fun handle(event: GameEvent) {
        SwingUtilities.invokeLater {
            when (event) {
                is GameEvent.GameStarted -> handleGameStarted(event.game)
                is GameEvent.MoveMade -> handleMoveMade(event.game, event.move)
                is GameEvent.GameEnded -> handleGameEnded(event.game, event.result)
                is GameEvent.MoveUndone -> handleMoveUndone(event.game)
                is GameEvent.InvalidMoveAttempted -> handleInvalidMove(event.position, event.reason)
                is GameEvent.AIThinking -> handleAIThinking(event.game)
            }
        }
    }
    
    fun startNewGame(mode: GameModeDto, aiDifficulty: AIDifficultyDto? = null, rule: GameRuleDto = GameRuleDto.STANDARD_RENJU) {
        val settings = GameSettingsDto(
            mode = mode,
            aiDifficulty = aiDifficulty,
            gameRule = rule
        )
        gameApplicationService.startNewGameDto(settings)
    }
    
    fun makeMove(position: PositionDto): Boolean {
        val result = gameApplicationService.makeMoveDto(position)
        return result != null
    }
    
    fun undoMove(): Boolean {
        val result = gameApplicationService.undoMoveDto()
        return result != null
    }
    
    fun getCurrentGame(): GameDto? {
        return gameApplicationService.getCurrentGameDto()
    }
    
    fun getForbiddenMoves(): Set<PositionDto> {
        return gameApplicationService.getForbiddenMovesDto()
    }
    
    private fun handleGameStarted(game: com.omok.domain.model.Game) {
        val gameDto = GameMapper.toDto(game)
        gameWindow.updateBoard(gameDto.board)
        gameWindow.updateStatus("게임이 시작되었습니다. 흑돌 차례입니다.")
        gameWindow.setUndoEnabled(false)
        gameWindow.clearLastMove()
    }
    
    private fun handleMoveMade(game: com.omok.domain.model.Game, move: com.omok.domain.model.Move) {
        val gameDto = GameMapper.toDto(game)
        val moveDto = GameMapper.toDto(move)
        gameWindow.updateBoard(gameDto.board)
        gameWindow.setLastMove(moveDto.position)
        gameWindow.setUndoEnabled(gameDto.moveHistory.isNotEmpty())
        
        val nextPlayer = when (gameDto.currentPlayer) {
            PlayerDto.BLACK -> "흑돌"
            PlayerDto.WHITE -> "백돌"
        }
        gameWindow.updateStatus("$nextPlayer 차례입니다.")
    }
    
    private fun handleGameEnded(game: com.omok.domain.model.Game, result: com.omok.domain.model.GameState) {
        val gameDto = GameMapper.toDto(game)
        val resultDto = GameMapper.toDto(result)
        gameWindow.updateBoard(gameDto.board)
        
        when (resultDto) {
            is GameStateDto.Won -> {
                val winner = when (resultDto.winner) {
                    PlayerDto.BLACK -> "흑돌"
                    PlayerDto.WHITE -> "백돌"
                }
                gameWindow.updateStatus("$winner 승리!")
                gameWindow.showWinAnimation(resultDto.winningLine)
            }
            is GameStateDto.Draw -> {
                gameWindow.updateStatus("무승부!")
            }
            is GameStateDto.Playing -> {
                // 이 경우는 발생하지 않아야 함
            }
        }
    }
    
    private fun handleMoveUndone(game: com.omok.domain.model.Game) {
        val gameDto = GameMapper.toDto(game)
        gameWindow.updateBoard(gameDto.board)
        gameWindow.setUndoEnabled(gameDto.moveHistory.isNotEmpty())
        
        val lastMove = gameDto.moveHistory.lastOrNull()
        if (lastMove != null) {
            gameWindow.setLastMove(lastMove.position)
        } else {
            gameWindow.clearLastMove()
        }
        
        gameWindow.updateStatus("흑돌 차례입니다.")
    }
    
    private fun handleInvalidMove(position: com.omok.domain.model.Position, reason: String) {
        gameWindow.showMessage(reason)
    }
    
    private fun handleAIThinking(game: com.omok.domain.model.Game) {
        gameWindow.updateStatus("AI가 생각 중...")
    }
    
    fun cleanup() {
        gameApplicationService.cleanup()
    }
}