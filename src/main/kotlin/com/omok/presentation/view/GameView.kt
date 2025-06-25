package com.omok.presentation.view

import com.omok.application.dto.*
import com.omok.presentation.ui.GameWindow

/**
 * Clean Architecture에서 View 인터페이스 역할
 * UI 업데이트를 위한 추상화 계층
 */
interface GameView {
    fun updateBoard(board: BoardDto)
    fun updateStatus(status: String)
    fun showMessage(message: String)
    fun setLastMove(position: PositionDto)
    fun clearLastMove()
    fun setUndoEnabled(enabled: Boolean)
    fun showWinAnimation(winningLine: List<PositionDto>)
    fun resetView()
}

/**
 * GameView의 Swing 구현체
 */
class SwingGameView(private val window: GameWindow) : GameView {
    
    override fun updateBoard(board: BoardDto) {
        window.updateBoard(board)
    }
    
    override fun updateStatus(status: String) {
        window.updateStatus(status)
    }
    
    override fun showMessage(message: String) {
        window.showMessage(message)
    }
    
    override fun setLastMove(position: PositionDto) {
        window.setLastMove(position)
    }
    
    override fun clearLastMove() {
        window.clearLastMove()
    }
    
    override fun setUndoEnabled(enabled: Boolean) {
        window.setUndoEnabled(enabled)
    }
    
    override fun showWinAnimation(winningLine: List<PositionDto>) {
        window.showWinAnimation(winningLine)
    }
    
    override fun resetView() {
        // 뷰 초기화
        clearLastMove()
        setUndoEnabled(false)
        updateStatus("새 게임을 시작하세요")
    }
}