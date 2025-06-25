package com.omok.domain.model

class Game private constructor(
    private val board: Board,
    private val currentPlayer: Player,
    private val state: GameState,
    private val moveHistory: List<Move>,
    private val settings: GameSettings
) {
    constructor(settings: GameSettings) : this(
        board = Board(),
        currentPlayer = Player.first(),
        state = GameState.Playing,
        moveHistory = emptyList(),
        settings = settings
    )
    
    fun getBoard(): Board = board
    fun getCurrentPlayer(): Player = currentPlayer
    fun getState(): GameState = state
    fun getMoveHistory(): List<Move> = moveHistory.toList()
    fun getSettings(): GameSettings = settings
    fun getLastMove(): Move? = moveHistory.lastOrNull()
    
    fun makeMove(position: Position): Game {
        require(state is GameState.Playing) { "Game is not in playing state" }
        require(board.isEmpty(position)) { "Position is not empty: $position" }
        
        val move = Move(position, currentPlayer, moveHistory.size + 1)
        val newBoard = board.placeStone(move)
        val newMoveHistory = moveHistory + move
        
        return Game(
            board = newBoard,
            currentPlayer = currentPlayer.opponent(),
            state = GameState.Playing, // Will be updated by game engine
            moveHistory = newMoveHistory,
            settings = settings
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
        return state is GameState.Playing && 
               settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == Player.WHITE
    }
}