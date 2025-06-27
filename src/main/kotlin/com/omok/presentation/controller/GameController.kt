package com.omok.presentation.controller

import com.omok.application.dto.*
import com.omok.application.mapper.GameMapper
import com.omok.application.service.GameApplicationService
import com.omok.domain.event.GameEvent
import com.omok.domain.event.GameEventHandler
import com.omok.presentation.ui.GameWindow
import com.omok.infrastructure.logging.Logger
import com.omok.infrastructure.logging.safeCall
import com.omok.presentation.ui.effects.SoundEffects
import com.omok.infrastructure.statistics.GameStatisticsService
import com.omok.application.usecase.ProcessAchievementEventUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.SwingUtilities

class GameController(
    private var gameApplicationService: GameApplicationService,
    private val gameWindow: GameWindow,
    private val dependencyContainer: com.omok.infrastructure.DependencyContainer
) : GameEventHandler {
    
    private val statisticsService = GameStatisticsService.getInstance()
    private val achievementEventUseCase = dependencyContainer.createProcessAchievementEventUseCase()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var gameStartTime: Long = 0L
    
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
                is GameEvent.AIThinkingProgress -> handleAIThinkingProgress(event.thinkingInfo)
                is GameEvent.SwapDecision -> handleSwapDecision(event.game, event.swapped)
                is GameEvent.FifthMovesProposed -> handleFifthMovesProposed(event.game, event.positions)
                is GameEvent.InvalidAction -> handleInvalidAction(event.reason)
            }
        }
    }
    
    fun startNewGame(
        mode: GameModeDto, 
        aiDifficulty: AIDifficultyDto? = null, 
        rule: GameRuleDto = GameRuleDto.STANDARD_RENJU,
        timeLimit: TimeLimitDto = TimeLimitDto.NONE
    ) {
        safeCall {
            Logger.info("GameController", "Starting new game - Mode: $mode, Difficulty: $aiDifficulty, Rule: ${rule.displayName}")
            
            // 선택된 룰을 DependencyContainer에 설정하고 새로운 GameApplicationService 생성
            val domainRule = GameMapper.toDomain(rule)
            dependencyContainer.setGameRule(domainRule)
            Logger.info("GameController", "Game rule set to: ${domainRule.displayName}")
            
            // AI 난이도에 따라 새로운 ApplicationService 생성
            val difficulty = aiDifficulty?.let { GameMapper.toDomain(it) } ?: com.omok.domain.model.AIDifficulty.MEDIUM
            val newGameApplicationService = dependencyContainer.createGameApplicationService(difficulty)
            Logger.info("GameController", "Created new GameApplicationService with difficulty: $difficulty")
            
            val settings = GameSettingsDto(
                mode = mode,
                aiDifficulty = aiDifficulty,
                gameRule = rule,
                timeLimit = timeLimit
            )
            newGameApplicationService.startNewGameDto(settings)
            
            // 현재 GameApplicationService를 새로운 것으로 교체
            updateGameApplicationService(newGameApplicationService)
            
            // 게임 시작 시간 기록
            gameStartTime = System.currentTimeMillis()
            
            Logger.info("GameController", "Game started successfully")
        } ?: run {
            gameWindow.showMessage("게임 시작 중 오류가 발생했습니다.")
        }
    }
    
    private fun updateGameApplicationService(newService: GameApplicationService) {
        gameApplicationService = newService
        // 이벤트 핸들러 재등록
        dependencyContainer.eventBus.subscribe(this)
    }
    
    fun makeMove(position: PositionDto): Boolean {
        return safeCall {
            Logger.info("GameController", "Making move at position: (${position.row}, ${position.col})")
            
            // 현재 게임 상태 확인
            val currentGame = gameApplicationService.getCurrentGame()
            if (currentGame != null) {
                val state = currentGame.getState()
                
                // 5수 선택 대기 상태인 경우
                if (state is com.omok.domain.model.GameState.WaitingForFifthMoveSelection) {
                    val domainPosition = com.omok.domain.model.Position(position.row, position.col)
                    // GameApplicationService에서 selectFifthMove를 호출하면 내부적으로 검증됨
                    val result = gameApplicationService.selectFifthMove(domainPosition)
                    if (result != null) {
                        Logger.info("GameController", "Fifth move selected successfully")
                        return@safeCall true
                    } else {
                        Logger.warn("GameController", "Fifth move selection failed")
                        gameWindow.showMessage("제시된 5수 위치 중 하나를 선택해주세요.")
                        return@safeCall false
                    }
                }
            }
            
            // 일반적인 수 놓기
            val result = gameApplicationService.makeMoveDto(position)
            if (result != null) {
                Logger.info("GameController", "Move successful")
                true
            } else {
                Logger.warn("GameController", "Move failed - invalid move at position (${position.row}, ${position.col})")
                false
            }
        } ?: run {
            gameWindow.showMessage("수를 둘 수 없습니다.")
            false
        }
    }
    
    fun undoMove(): Boolean {
        return safeCall {
            Logger.info("GameController", "Undoing move")
            val result = gameApplicationService.undoMoveDto()
            if (result != null) {
                Logger.info("GameController", "Undo successful")
                SoundEffects.playUndo()
                true
            } else {
                Logger.warn("GameController", "Undo failed - no moves to undo")
                SoundEffects.playError()
                false
            }
        } ?: run {
            gameWindow.showMessage("무르기 중 오류가 발생했습니다.")
            false
        }
    }
    
    fun getCurrentGame(): GameDto? {
        return safeCall {
            gameApplicationService.getCurrentGameDto()
        }
    }
    
    fun getForbiddenMoves(): Set<PositionDto> {
        return safeCall {
            gameApplicationService.getForbiddenMovesDto()
        } ?: emptySet()
    }
    
    fun updateMouseCoordinate(position: PositionDto?) {
        gameWindow.updateMouseCoordinate(position)
    }
    
    private fun handleGameStarted(game: com.omok.domain.model.Game) {
        val gameDto = GameMapper.toDto(game)
        gameWindow.updateBoard(gameDto)
        gameWindow.updateStatus("게임이 시작되었습니다. 흑돌 차례입니다.")
        gameWindow.setUndoEnabled(false)
        gameWindow.clearLastMove()
        gameWindow.clearWinAnimation()
        gameWindow.updateTimer(gameDto)
        
        // 게임 시작 시간 기록
        gameStartTime = System.currentTimeMillis()
        
        // 성취도 이벤트 처리
        coroutineScope.launch {
            achievementEventUseCase.onGameStarted()
        }
        
        // 게임 시작 사운드
        SoundEffects.playGameStart()
    }
    
    private fun handleMoveMade(game: com.omok.domain.model.Game, move: com.omok.domain.model.Move) {
        val gameDto = GameMapper.toDto(game)
        val moveDto = GameMapper.toDto(move)
        gameWindow.updateBoard(gameDto)
        gameWindow.setLastMove(moveDto.position)
        gameWindow.setUndoEnabled(gameDto.moveHistory.isNotEmpty())
        gameWindow.updateTimer(gameDto)

        // 특수 상태 확인
        when (game.getState()) {
            is com.omok.domain.model.GameState.WaitingForSwap -> {
                gameWindow.updateStatus("3수가 놓였습니다. 백이 흑백 교체를 선택할 수 있습니다.")
                if (!game.isAITurn()) {
                    showSwapDialog()
                }
            }
            is com.omok.domain.model.GameState.WaitingForFifthMove -> {
                gameWindow.updateStatus("4수가 놓였습니다. 흑이 5수를 2개 제시해야 합니다.")
                if (!game.isAITurn()) {
                    showFifthMoveProposalDialog()
                }
            }
            is com.omok.domain.model.GameState.Playing -> {
                val nextPlayer = when (gameDto.currentPlayer) {
                    PlayerDto.BLACK -> "흑돌"
                    PlayerDto.WHITE -> "백돌"
                }
                gameWindow.updateStatus("$nextPlayer 차례입니다.")
            }
            else -> {
                // Won, Draw 등은 handleGameEnded에서 처리
            }
        }
    }
    
    private fun handleGameEnded(game: com.omok.domain.model.Game, result: com.omok.domain.model.GameState) {
        val gameDto = GameMapper.toDto(game)
        val resultDto = GameMapper.toDto(result)
        gameWindow.updateBoard(gameDto)
        
        // 게임 통계 기록
        val gameDuration = System.currentTimeMillis() - gameStartTime
        statisticsService.recordGameFromDomain(game, result, gameDuration)
        
        // 성취도 이벤트 처리
        coroutineScope.launch {
            val won = result is com.omok.domain.model.GameState.Won
            val aiDifficulty = game.getSettings().aiDifficulty?.name
            val hadForbiddenMoves = false // TODO: 금수 체크 로직 구현 필요
            
            achievementEventUseCase.onGameEnded(
                won = won,
                duration = gameDuration,
                moveCount = game.getMoveHistory().size,
                aiDifficulty = aiDifficulty,
                hadForbiddenMoves = hadForbiddenMoves
            )
        }
        
        when (resultDto) {
            is GameStateDto.Won -> {
                val winner = when (resultDto.winner) {
                    PlayerDto.BLACK -> "흑돌"
                    PlayerDto.WHITE -> "백돌"
                }
                gameWindow.updateStatus("$winner 승리!")
                gameWindow.showWinAnimation(resultDto.winningLine)
                
                // 승리 사운드
                SoundEffects.playWin()
            }
            is GameStateDto.Draw -> {
                gameWindow.updateStatus("무승부!")
                // 무승부 사운드 (이미 있다면)
                // SoundEffects.playDraw()
            }
            is GameStateDto.Playing -> {
                // 이 경우는 발생하지 않아야 함
            }
        }
    }
    
    private fun handleMoveUndone(game: com.omok.domain.model.Game) {
        val gameDto = GameMapper.toDto(game)
        gameWindow.updateBoard(gameDto)
        gameWindow.setUndoEnabled(gameDto.moveHistory.isNotEmpty())
        gameWindow.updateTimer(gameDto)
        
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
    
    private fun handleAIThinkingProgress(thinkingInfo: com.omok.domain.model.AIThinkingInfo) {
        // AI 사고 과정을 보드에 표시
        val thinkingDto = GameMapper.toDto(thinkingInfo)
        gameWindow.showAIThinking(thinkingDto)
    }
    
    private fun handleSwapDecision(game: com.omok.domain.model.Game, swapped: Boolean) {
        if (swapped) {
            gameWindow.updateStatus("백이 흑백을 교체했습니다. 백(이전 흑) 차례입니다.")
        } else {
            gameWindow.updateStatus("백이 교체를 거부했습니다. 백 차례입니다.")
        }
    }
    
    private fun handleFifthMovesProposed(game: com.omok.domain.model.Game, positions: List<com.omok.domain.model.Position>) {
        val positionsStr = positions.map { "(${it.row}, ${it.col})" }.joinToString(", ")
        gameWindow.updateStatus("흑이 5수를 제시했습니다: $positionsStr. 백이 하나를 선택해야 합니다.")
        
        // 5수 위치를 보드에 표시
        val positionDtos = positions.map { GameMapper.toDto(it) }
        gameWindow.highlightFifthMoveOptions(positionDtos)
        
        // AI가 백이고 5수 선택 차례인 경우 AI가 처리하도록 함
        if (game.getSettings().mode == com.omok.domain.model.GameMode.PLAYER_VS_AI && game.isAITurn()) {
            Logger.info("GameController", "AI should select fifth move")
            // AI가 ProcessAIMoveUseCase를 통해 자동으로 선택할 것임
        } else {
            // 플레이어가 보드에서 직접 선택하도록 함 (makeMove 메서드에서 처리)
            gameWindow.showMessage("제시된 5수 위치 중 하나를 클릭하여 선택하세요.")
        }
    }
    
    private fun handleInvalidAction(reason: String) {
        gameWindow.showMessage(reason)
    }
    
    private fun showSwapDialog() {
        val options = arrayOf("교체", "거부")
        val choice = javax.swing.JOptionPane.showOptionDialog(
            gameWindow,
            "흑백을 교체하시겠습니까?\n교체하면 백이 흑이 되고, 흑이 백이 됩니다.",
            "오픈 렌주룰 - 스왑",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1]
        )
        
        safeCall {
            gameApplicationService.processSwap(choice == 0)
        }
    }
    
    private fun showFifthMoveProposalDialog() {
        gameWindow.showMessage("보드에서 5수로 둘 위치를 2개 클릭하세요.")
        gameWindow.enableFifthMoveProposalMode { positions ->
            safeCall {
                val domainPositions = positions.map { GameMapper.toDomain(it) }
                gameApplicationService.proposeFifthMoves(domainPositions)
            }
        }
    }
    
    private fun showFifthMoveSelectionDialog(positions: List<com.omok.domain.model.Position>) {
        gameWindow.highlightFifthMoveOptions(positions.map { GameMapper.toDto(it) })
        gameWindow.showMessage("제시된 5수 중 하나를 선택하세요.")
        gameWindow.enableFifthMoveSelectionMode(positions.map { GameMapper.toDto(it) }) { selected ->
            safeCall {
                val domainPosition = GameMapper.toDomain(selected)
                gameApplicationService.selectFifthMove(domainPosition)
            }
        }
    }
    
    fun saveGame(fileName: String): Boolean {
        return safeCall {
            gameApplicationService.saveGame(fileName)
        } ?: false
    }
    
    fun loadGame(fileName: String): GameDto? {
        return safeCall {
            val game = gameApplicationService.loadGame(fileName)
            game?.let { GameMapper.toDto(it) }
        }
    }
    
    fun loadLatestAutoSave(): GameDto? {
        return safeCall {
            val game = gameApplicationService.loadLatestAutoSave()
            game?.let { GameMapper.toDto(it) }
        }
    }
    
    fun handleTimeUp(player: PlayerDto) {
        safeCall {
            val playerDomain = GameMapper.toDomain(player)
            val winner = playerDomain.opponent()
            val playerName = when (player) {
                PlayerDto.BLACK -> "흑돌"
                PlayerDto.WHITE -> "백돌"
            }
            val winnerName = when (winner) {
                com.omok.domain.model.Player.BLACK -> "흑돌"
                com.omok.domain.model.Player.WHITE -> "백돌"
            }
            
            gameWindow.showMessage("$playerName 시간 초과! $winnerName 승리!")
            
            // 게임 종료 이벤트 발생 (시간 초과로 인한 승리)
            val currentGame = gameApplicationService.getCurrentGame()
            if (currentGame != null) {
                val timeUpResult = com.omok.domain.model.GameState.Won(winner, emptyList())
                com.omok.domain.event.GameEventBus.publish(
                    com.omok.domain.event.GameEvent.GameEnded(currentGame, timeUpResult)
                )
            }
        }
    }
    
    fun cleanup() {
        gameApplicationService.cleanup()
    }
    
    /**
     * AI 사고시간 업데이트
     */
    fun updateAIThinkingTime(timeMs: Long) {
        gameApplicationService.updateAIThinkingTime(timeMs)
    }
}