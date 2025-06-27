package com.omok.domain.service

import com.omok.domain.model.*

interface AIStrategy {
    fun getBestMove(board: Board, player: Player): Position?
    
    /**
     * AI 사고 과정 진행 상황을 알리기 위한 콜백 설정
     */
    fun setThinkingProgressCallback(callback: (AIThinkingInfo) -> Unit) {}
    
    /**
     * AI가 스왑(흑백 교체)를 할지 결정
     * @param board 현재 보드 상태
     * @param thirdMove 방금 놓인 3수 위치
     * @return true면 스왑, false면 스왑하지 않음
     */
    fun shouldSwap(board: Board, thirdMove: Position): Boolean = false
    
    /**
     * AI가 5수로 제시할 두 위치를 선택
     * @param board 현재 보드 상태
     * @return 5수로 제시할 두 위치
     */
    fun proposeFifthMoves(board: Board): List<Position> = emptyList()
    
    /**
     * AI가 제시된 5수 중 하나를 선택
     * @param board 현재 보드 상태
     * @param proposedMoves 제시된 5수 위치들
     * @return 선택한 5수 위치
     */
    fun selectFifthMove(board: Board, proposedMoves: List<Position>): Position? = proposedMoves.firstOrNull()
}