package com.omok.domain.model

class Game private constructor(
    private val board: Board,
    private val currentPlayer: Player,
    private val state: GameState,
    private val moveHistory: List<Move>,
    private val settings: GameSettings,
    private val timeState: GameTimeState? = null
) {
    constructor(settings: GameSettings) : this(
        board = Board(),
        currentPlayer = Player.first(),
        state = GameState.Playing,
        moveHistory = emptyList(),
        settings = settings,
        timeState = if (settings.timeLimit.mode != TimeLimitMode.NONE) 
            GameTimeState.create(settings.timeLimit) else null
    )
    
    fun getBoard(): Board = board
    fun getCurrentPlayer(): Player = currentPlayer
    fun getState(): GameState = state
    fun getMoveHistory(): List<Move> = moveHistory.toList()
    fun getSettings(): GameSettings = settings
    fun getLastMove(): Move? = moveHistory.lastOrNull()
    fun getTimeState(): GameTimeState? = timeState
    
    fun hasTimeLimit(): Boolean = timeState != null
    
    fun getRemainingTime(player: Player): Long? {
        return timeState?.getTimeState(player)?.remainingTime
    }
    
    fun isPlayerTimeUp(player: Player): Boolean {
        return timeState?.isCurrentPlayerTimeUp(player) ?: false
    }
    
    fun makeMove(position: Position): Game {
        require(state is GameState.Playing) { "Game is not in playing state" }
        require(board.isEmpty(position)) { "Position is not empty: $position" }
        
        val move = Move(position, currentPlayer, moveHistory.size + 1)
        val newBoard = board.placeStone(move)
        val newMoveHistory = moveHistory + move
        
        // 시간 업데이트
        val newTimeState = timeState?.let { updateTimeAfterMove(it, currentPlayer) }
        
        return Game(
            board = newBoard,
            currentPlayer = currentPlayer.opponent(),
            state = GameState.Playing, // Will be updated by game engine
            moveHistory = newMoveHistory,
            settings = settings,
            timeState = newTimeState
        )
    }
    
    fun undoMove(): Game? {
        if (moveHistory.isEmpty()) return null
        
        val newMoveHistory = moveHistory.dropLast(1)
        val newBoard = Board()
        
        // Rebuild board from history
        var rebuiltBoard = newBoard
        for (move in newMoveHistory) {
            rebuiltBoard = rebuiltBoard.placeStone(move)
        }
        
        val newCurrentPlayer = if (newMoveHistory.isEmpty()) {
            Player.first()
        } else {
            newMoveHistory.last().player.opponent()
        }
        
        return Game(
            board = rebuiltBoard,
            currentPlayer = newCurrentPlayer,
            state = GameState.Playing,
            moveHistory = newMoveHistory,
            settings = settings
        )
    }
    
    fun updateState(newState: GameState): Game {
        return Game(
            board = board,
            currentPlayer = currentPlayer,
            state = newState,
            moveHistory = moveHistory,
            settings = settings
        )
    }
    
    fun isPlayerTurn(): Boolean {
        return state is GameState.Playing && 
               (settings.mode == GameMode.PLAYER_VS_PLAYER || currentPlayer == Player.BLACK)
    }
    
    fun isAITurn(): Boolean {
        return settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == Player.WHITE
    }
    
    private fun updateTimeAfterMove(timeState: GameTimeState, playerWhoMoved: Player): GameTimeState {
        val currentTime = System.currentTimeMillis()
        val timeUsed = (currentTime - timeState.currentPlayerStartTime) / 1000L // 초 단위
        
        val playerTimeState = timeState.getTimeState(playerWhoMoved)
        val newPlayerTimeState = when (timeState.timeLimit.mode) {
            TimeLimitMode.NONE -> playerTimeState
            TimeLimitMode.TOTAL_TIME -> {
                playerTimeState.copy(
                    remainingTime = (playerTimeState.remainingTime - timeUsed).coerceAtLeast(0L)
                )
            }
            TimeLimitMode.FISCHER -> {
                val newTime = playerTimeState.remainingTime - timeUsed + timeState.timeLimit.incrementPerMove
                playerTimeState.copy(remainingTime = newTime.coerceAtLeast(0L))
            }
            TimeLimitMode.BYOYOMI -> {
                if (playerTimeState.isInByoyomi) {
                    // 초읽기 상태에서 수를 두었으므로 초읽기 시간 리셋
                    playerTimeState.copy(
                        byoyomiTime = timeState.timeLimit.incrementPerMove,
                        byoyomiPeriods = if (timeUsed > timeState.timeLimit.incrementPerMove) 
                            (playerTimeState.byoyomiPeriods - 1).coerceAtLeast(0) 
                        else playerTimeState.byoyomiPeriods
                    )
                } else {
                    val newTime = playerTimeState.remainingTime - timeUsed
                    if (newTime <= 0) {
                        // 초읽기 상태로 전환
                        playerTimeState.copy(
                            remainingTime = 0L,
                            isInByoyomi = true,
                            byoyomiTime = timeState.timeLimit.incrementPerMove
                        )
                    } else {
                        playerTimeState.copy(remainingTime = newTime)
                    }
                }
            }
        }
        
        return timeState
            .updateTimeState(playerWhoMoved, newPlayerTimeState)
            .startTimer(playerWhoMoved.opponent())
    }
    
    fun updateTimeState(newTimeState: GameTimeState): Game {
        return Game(
            board = board,
            currentPlayer = currentPlayer,
            state = state,
            moveHistory = moveHistory,
            settings = settings,
            timeState = newTimeState
        )
    }
    
    fun startTimer(): Game {
        val newTimeState = timeState?.startTimer(currentPlayer)
        return Game(
            board = board,
            currentPlayer = currentPlayer,
            state = state,
            moveHistory = moveHistory,
            settings = settings,
            timeState = newTimeState
        )
    }
    
    fun stopTimer(): Game {
        val newTimeState = timeState?.stopTimer()
        return Game(
            board = board,
            currentPlayer = currentPlayer,
            state = state,
            moveHistory = moveHistory,
            settings = settings,
            timeState = newTimeState
        )
    }
}