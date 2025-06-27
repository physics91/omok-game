package com.omok.domain.service

import com.omok.domain.model.*
import com.omok.domain.logging.DomainLogger
import com.omok.domain.logging.NoOpLogger

class GameEngine(
    private val ruleValidator: RuleValidator = RuleValidator(),
    private val aiStrategy: AIStrategy? = null,
    private val logger: DomainLogger = NoOpLogger
) {
    
    fun processMove(game: Game, position: Position): GameProcessResult {
        logger.debug("GameEngine: Processing move: Player=${game.getCurrentPlayer()}, Position=(${position.row}, ${position.col})")
        
        val currentState = game.getState()
        
        // 오픈 렌주룰 특수 상태 처리
        when (currentState) {
            is GameState.WaitingForFifthMove -> {
                // 5수 제시 중인 경우
                if (game.getCurrentPlayer() != Player.BLACK) {
                    return GameProcessResult.Failure("흑이 5수를 제시해야 합니다")
                }
                // 5수 제시 로직은 별도 메서드로 처리
                return GameProcessResult.Failure("5수 제시는 proposeFifthMove 메서드를 사용하세요")
            }
            is GameState.WaitingForFifthMoveSelection -> {
                // 5수 선택 중인 경우
                if (game.getCurrentPlayer() != Player.WHITE) {
                    return GameProcessResult.Failure("백이 5수를 선택해야 합니다")
                }
                // 5수 선택 로직은 별도 메서드로 처리
                return GameProcessResult.Failure("5수 선택은 selectFifthMove 메서드를 사용하세요")
            }
            is GameState.WaitingForSwap -> {
                // 스왑 대기 중인 경우
                return GameProcessResult.Failure("스왑 결정은 processSwap 메서드를 사용하세요")
            }
            is GameState.Playing -> {
                // 정상 게임 진행
            }
            else -> {
                logger.warn("GameEngine: Move rejected: Game is not in playing state (current: $currentState)")
                return GameProcessResult.Failure("Game is not in playing state")
            }
        }
        
        val currentPlayer = game.getCurrentPlayer()
        
        if (!ruleValidator.isValidMove(game.getBoard(), position, currentPlayer)) {
            val reason = getInvalidMoveReason(game.getBoard(), position, currentPlayer, ruleValidator)
            logger.warn("GameEngine: Move rejected: $reason at position (${position.row}, ${position.col}) for player $currentPlayer")
            return GameProcessResult.Failure(reason)
        }
        
        val gameAfterMove = game.makeMove(position)
        
        // 오픈 렌주룰의 경우 checkGameState 사용
        val newState = if (ruleValidator is com.omok.domain.service.rule.OpenRenjuValidator) {
            ruleValidator.checkGameState(gameAfterMove.getBoard(), Move(position, currentPlayer))
        } else {
            ruleValidator.checkWin(gameAfterMove.getBoard(), position, currentPlayer)
        }
        
        val finalGame = gameAfterMove.updateState(newState)
        
        logger.info("GameEngine: Move processed successfully: Player=$currentPlayer, Position=(${position.row}, ${position.col}), NewState=$newState")
        return GameProcessResult.Success(finalGame)
    }
    
    fun processAIMove(game: Game): GameProcessResult {
        logger.info("GameEngine: Processing AI move for player: ${game.getCurrentPlayer()}, state: ${game.getState()}")
        
        if (!game.isAITurn() || aiStrategy == null) {
            logger.warn("GameEngine: AI move rejected: Not AI turn (${game.isAITurn()}) or AI strategy not available (${aiStrategy != null})")
            return GameProcessResult.Failure("Not AI turn or AI strategy not available")
        }
        
        // 특수 상태에 따른 AI 처리
        return when (val state = game.getState()) {
            is GameState.WaitingForSwap -> {
                // AI가 스왑 결정
                val lastMove = game.getLastMove()
                if (lastMove == null) {
                    logger.error("GameEngine: No last move found for swap decision")
                    return GameProcessResult.Failure("No last move found for swap decision")
                }
                
                val shouldSwap = aiStrategy.shouldSwap(game.getBoard(), lastMove.position)
                logger.info("GameEngine: AI swap decision: $shouldSwap")
                processSwap(game, shouldSwap)
            }
            
            is GameState.WaitingForFifthMove -> {
                // AI가 5수 제시
                val proposedMoves = aiStrategy.proposeFifthMoves(game.getBoard())
                if (proposedMoves.size != 2) {
                    logger.error("GameEngine: AI failed to propose exactly 2 fifth moves")
                    return GameProcessResult.Failure("AI must propose exactly 2 fifth moves")
                }
                logger.info("GameEngine: AI proposed fifth moves: ${proposedMoves.map { "(${it.row}, ${it.col})" }}")
                proposeFifthMoves(game, proposedMoves)
            }
            
            is GameState.WaitingForFifthMoveSelection -> {
                // AI가 5수 선택
                if (ruleValidator !is com.omok.domain.service.rule.OpenRenjuValidator) {
                    return GameProcessResult.Failure("오픈 렌주룰이 아닙니다")
                }
                
                val proposedMoves = ruleValidator.getProposedFifthMoves()
                val selectedMove = aiStrategy.selectFifthMove(game.getBoard(), proposedMoves)
                
                if (selectedMove == null || !proposedMoves.contains(selectedMove)) {
                    logger.error("GameEngine: AI failed to select a valid fifth move")
                    return GameProcessResult.Failure("AI must select from proposed moves")
                }
                
                logger.info("GameEngine: AI selected fifth move: (${selectedMove.row}, ${selectedMove.col})")
                selectFifthMove(game, selectedMove)
            }
            
            is GameState.Playing -> {
                // 일반적인 수 처리
                
                // AI 사고 과정 콜백 설정
                aiStrategy.setThinkingProgressCallback { thinkingInfo ->
                    com.omok.domain.event.GameEventBus.publish(
                        com.omok.domain.event.GameEvent.AIThinkingProgress(thinkingInfo)
                    )
                }
                
                val startTime = System.currentTimeMillis()
                val aiMove = aiStrategy.getBestMove(game.getBoard(), game.getCurrentPlayer())
                val endTime = System.currentTimeMillis()
                
                if (aiMove == null) {
                    logger.error("GameEngine: AI could not find a valid move")
                    return GameProcessResult.Failure("AI could not find a valid move")
                }
                
                logger.info("GameEngine: AI found move in ${endTime - startTime}ms: Position=(${aiMove.row}, ${aiMove.col})")
                processMove(game, aiMove)
            }
            
            else -> {
                logger.warn("GameEngine: AI cannot act in state: $state")
                GameProcessResult.Failure("AI cannot act in current game state")
            }
        }
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
    
    /**
     * 오픈 렌주룰 - 스왑 처리
     */
    fun processSwap(game: Game, swap: Boolean): GameProcessResult {
        val currentState = game.getState()
        if (currentState !is GameState.WaitingForSwap) {
            return GameProcessResult.Failure("스왑 대기 상태가 아닙니다")
        }
        
        if (ruleValidator !is com.omok.domain.service.rule.OpenRenjuValidator) {
            return GameProcessResult.Failure("오픈 렌주룰이 아닙니다")
        }
        
        logger.info("GameEngine: Processing swap decision: swap=$swap")
        
        if (swap) {
            // 스왑 적용 - 플레이어 교체
            ruleValidator.setSwapped(true)
            // 게임 상태를 Playing으로 변경하고 플레이어는 그대로 (백이 흑이 됨)
            val swappedGame = game.updateState(GameState.Playing)
            logger.info("GameEngine: Swap applied - players switched")
            return GameProcessResult.Success(swappedGame)
        } else {
            // 스왑 거부 - 게임 계속
            val continueGame = game.updateState(GameState.Playing)
            logger.info("GameEngine: Swap rejected - continuing normal play")
            return GameProcessResult.Success(continueGame)
        }
    }
    
    /**
     * 오픈 렌주룰 - 5수 제시
     */
    fun proposeFifthMoves(game: Game, positions: List<Position>): GameProcessResult {
        val currentState = game.getState()
        if (currentState !is GameState.WaitingForFifthMove) {
            return GameProcessResult.Failure("5수 제시 대기 상태가 아닙니다")
        }
        
        if (game.getCurrentPlayer() != Player.BLACK) {
            return GameProcessResult.Failure("흑이 5수를 제시해야 합니다")
        }
        
        if (positions.size != 2) {
            return GameProcessResult.Failure("5수는 정확히 2개를 제시해야 합니다")
        }
        
        // 제시된 위치가 유효한지 확인
        val board = game.getBoard()
        for (pos in positions) {
            if (!pos.isValid() || !board.isEmpty(pos)) {
                return GameProcessResult.Failure("유효하지 않은 위치입니다: (${pos.row}, ${pos.col})")
            }
        }
        
        if (ruleValidator !is com.omok.domain.service.rule.OpenRenjuValidator) {
            return GameProcessResult.Failure("오픈 렌주룰이 아닙니다")
        }
        
        // 제시된 5수 저장
        ruleValidator.setProposedFifthMoves(positions)
        
        // 상태를 5수 선택 대기로 변경
        val updatedGame = game.updateState(GameState.WaitingForFifthMoveSelection)
        
        logger.info("GameEngine: Fifth moves proposed: ${positions.map { "(${it.row}, ${it.col})" }}")
        return GameProcessResult.Success(updatedGame)
    }
    
    /**
     * 오픈 렌주룰 - 5수 선택
     */
    fun selectFifthMove(game: Game, position: Position): GameProcessResult {
        val currentState = game.getState()
        if (currentState !is GameState.WaitingForFifthMoveSelection) {
            return GameProcessResult.Failure("5수 선택 대기 상태가 아닙니다")
        }
        
        if (game.getCurrentPlayer() != Player.WHITE) {
            return GameProcessResult.Failure("백이 5수를 선택해야 합니다")
        }
        
        if (ruleValidator !is com.omok.domain.service.rule.OpenRenjuValidator) {
            return GameProcessResult.Failure("오픈 렌주룰이 아닙니다")
        }
        
        val proposedMoves = ruleValidator.getProposedFifthMoves()
        if (!proposedMoves.contains(position)) {
            return GameProcessResult.Failure("제시된 5수 중에서 선택해야 합니다")
        }
        
        // 선택된 5수를 흑이 두도록 처리
        // 현재 플레이어는 백이지만, 5수는 흑이 두는 것
        val blackMove = Move(position, Player.BLACK)
        val gameAfterMove = game.makeMove(position) // 이것은 백의 차례를 소비
        val finalGame = gameAfterMove.updateState(GameState.Playing)
        
        logger.info("GameEngine: Fifth move selected: (${position.row}, ${position.col})")
        return GameProcessResult.Success(finalGame)
    }
    
    /**
     * 잘못된 수에 대한 구체적인 이유 제공
     */
    private fun getInvalidMoveReason(board: Board, position: Position, player: Player, ruleValidator: RuleValidator): String {
        // 기본 검증
        if (!position.isValid()) return "보드 범위를 벗어났습니다"
        if (!board.isEmpty(position)) return "이미 돌이 놓여있습니다"
        
        // 오픈 렌주룰 특별 검증
        if (ruleValidator is com.omok.domain.service.rule.OpenRenjuValidator) {
            val moveCount = board.getMoveCount()
            val center = Position.center()
            
            when (moveCount) {
                0 -> {
                    if (position != center) {
                        return "오픈 렌주룰: 첫 수는 반드시 중앙(천원)에 두어야 합니다"
                    }
                }
                1 -> {
                    val rowDistance = kotlin.math.abs(position.row - center.row)
                    val colDistance = kotlin.math.abs(position.col - center.col)
                    val isIn8Area = rowDistance <= 1 && colDistance <= 1 && position != center
                    
                    if (!isIn8Area) {
                        return "오픈 렌주룰: 둘째 수(백)는 천원 주위 8곳(3×3 범위)에만 둘 수 있습니다"
                    }
                }
                2 -> {
                    val rowDistance = kotlin.math.abs(position.row - center.row)
                    val colDistance = kotlin.math.abs(position.col - center.col)
                    val isIn26Area = rowDistance <= 2 && colDistance <= 2 && position != center
                    
                    if (!isIn26Area) {
                        return "오픈 렌주룰: 셋째 수(흑)는 천원에서 2칸 이내(5×5 범위)에만 둘 수 있습니다"
                    }
                }
            }
        }
        
        // 렌주룰 금수 검증
        if (player == Player.BLACK) {
            val testBoard = board.placeStone(Move(position, player))
            
            if (ruleValidator.isDoubleThree(testBoard, position, player)) {
                return "렌주룰 금수: 삼삼 (열린 3을 동시에 2개 만드는 수)"
            }
            if (ruleValidator.isDoubleFour(testBoard, position, player)) {
                return "렌주룰 금수: 사사 (4를 동시에 2개 만드는 수)"
            }
            if (ruleValidator.isOverline(testBoard, position, player)) {
                return "렌주룰 금수: 장목 (6개 이상 연속)"
            }
        }
        
        return "잘못된 수입니다"
    }
}

sealed class GameProcessResult {
    data class Success(val game: Game) : GameProcessResult()
    data class Failure(val reason: String) : GameProcessResult()
}