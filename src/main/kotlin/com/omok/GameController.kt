package com.omok

import javax.swing.SwingUtilities

class GameController(private val gameView: GameView) {
    private val board = Array(RenjuRule.BOARD_SIZE) { Array(RenjuRule.BOARD_SIZE) { Stone.EMPTY } }
    private var currentPlayer = Stone.BLACK
    private var gameMode = GameMode.PVP
    private var aiDifficulty = AI.Difficulty.MEDIUM
    private var ai: AI? = null
    private var gameState = GameState.PLAYING
    private var moveHistory = mutableListOf<Position>()
    
    enum class GameMode {
        PVP, PVE
    }
    
    enum class GameState {
        PLAYING, BLACK_WIN, WHITE_WIN, DRAW
    }
    
    fun startNewGame(mode: GameMode, difficulty: AI.Difficulty? = null) {
        resetBoard()
        gameMode = mode
        gameState = GameState.PLAYING
        currentPlayer = Stone.BLACK
        moveHistory.clear()
        
        if (mode == GameMode.PVE && difficulty != null) {
            aiDifficulty = difficulty
            ai = AI(aiDifficulty)
        } else {
            ai = null
        }
        
        gameView.updateBoard(board)
        gameView.updateStatus("흑돌 차례입니다.")
        gameView.setUndoEnabled(false)
    }
    
    fun makeMove(row: Int, col: Int): Boolean {
        if (gameState != GameState.PLAYING) return false
        if (board[row][col] != Stone.EMPTY) return false
        
        if (!RenjuRule.isValidMove(board, row, col, currentPlayer)) {
            if (currentPlayer == Stone.BLACK) {
                gameView.showMessage("금수입니다! 다른 위치를 선택하세요.")
            }
            return false
        }
        
        board[row][col] = currentPlayer
        moveHistory.add(Position(row, col))
        gameView.updateBoard(board)
        gameView.setLastMove(row, col)
        gameView.setUndoEnabled(moveHistory.size > 0)
        
        if (RenjuRule.checkWin(board, row, col, currentPlayer)) {
            gameState = if (currentPlayer == Stone.BLACK) GameState.BLACK_WIN else GameState.WHITE_WIN
            val winner = if (currentPlayer == Stone.BLACK) "흑돌" else "백돌"
            gameView.updateStatus("$winner 승리!")
            gameView.showWinAnimation(row, col)
            return true
        }
        
        if (isBoardFull()) {
            gameState = GameState.DRAW
            gameView.updateStatus("무승부!")
            return true
        }
        
        currentPlayer = if (currentPlayer == Stone.BLACK) Stone.WHITE else Stone.BLACK
        val nextPlayer = if (currentPlayer == Stone.BLACK) "흑돌" else "백돌"
        gameView.updateStatus("$nextPlayer 차례입니다.")
        
        if (gameMode == GameMode.PVE && currentPlayer == Stone.WHITE) {
            gameView.updateStatus("AI가 생각 중...")
            Thread {
                Thread.sleep(500)
                makeAIMove()
            }.start()
        }
        
        return true
    }
    
    private fun makeAIMove() {
        if (ai == null || gameState != GameState.PLAYING) return
        
        val aiMove = ai!!.getBestMove(board, Stone.WHITE)
        if (aiMove != null) {
            SwingUtilities.invokeLater {
                makeMove(aiMove.row, aiMove.col)
            }
        }
    }
    
    fun undoMove() {
        if (moveHistory.isEmpty()) return
        
        val lastMove = moveHistory.removeAt(moveHistory.size - 1)
        board[lastMove.row][lastMove.col] = Stone.EMPTY
        
        if (gameMode == GameMode.PVE && moveHistory.isNotEmpty()) {
            val aiMove = moveHistory.removeAt(moveHistory.size - 1)
            board[aiMove.row][aiMove.col] = Stone.EMPTY
        }
        
        currentPlayer = Stone.BLACK
        gameState = GameState.PLAYING
        
        if (moveHistory.isNotEmpty()) {
            val prevMove = moveHistory.last()
            gameView.setLastMove(prevMove.row, prevMove.col)
        } else {
            gameView.setLastMove(-1, -1)
        }
        
        gameView.updateBoard(board)
        gameView.updateStatus("흑돌 차례입니다.")
        gameView.setUndoEnabled(moveHistory.size > 0)
    }
    
    fun getCurrentPlayer(): Stone = currentPlayer
    
    fun isPlayerTurn(): Boolean {
        return gameState == GameState.PLAYING && 
               (gameMode == GameMode.PVP || currentPlayer == Stone.BLACK)
    }
    
    private fun resetBoard() {
        for (i in 0 until RenjuRule.BOARD_SIZE) {
            for (j in 0 until RenjuRule.BOARD_SIZE) {
                board[i][j] = Stone.EMPTY
            }
        }
    }
    
    private fun isBoardFull(): Boolean {
        for (i in 0 until RenjuRule.BOARD_SIZE) {
            for (j in 0 until RenjuRule.BOARD_SIZE) {
                if (board[i][j] == Stone.EMPTY) return false
            }
        }
        return true
    }
}