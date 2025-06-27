package com.omok.domain.service.rule

import com.omok.domain.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class OpenRenjuValidatorTest {
    
    @Test
    fun `첫 수는 반드시 천원에 놓아야 함`() {
        val validator = OpenRenjuValidator()
        val board = Board()
        
        // 천원이 아닌 곳은 거부
        assertFalse(validator.isValidMove(board, Position(0, 0), Player.BLACK))
        assertFalse(validator.isValidMove(board, Position(7, 6), Player.BLACK))
        
        // 천원은 허용
        assertTrue(validator.isValidMove(board, Position.center(), Player.BLACK))
    }
    
    @Test
    fun `둘째 수는 천원 주위 8곳만 가능`() {
        val validator = OpenRenjuValidator()
        var board = Board()
        board = board.placeStone(Move(Position.center(), Player.BLACK))
        
        // 천원 주위 8곳은 허용
        assertTrue(validator.isValidMove(board, Position(6, 6), Player.WHITE))
        assertTrue(validator.isValidMove(board, Position(6, 7), Player.WHITE))
        assertTrue(validator.isValidMove(board, Position(6, 8), Player.WHITE))
        assertTrue(validator.isValidMove(board, Position(7, 6), Player.WHITE))
        assertTrue(validator.isValidMove(board, Position(7, 8), Player.WHITE))
        assertTrue(validator.isValidMove(board, Position(8, 6), Player.WHITE))
        assertTrue(validator.isValidMove(board, Position(8, 7), Player.WHITE))
        assertTrue(validator.isValidMove(board, Position(8, 8), Player.WHITE))
        
        // 그 외는 거부
        assertFalse(validator.isValidMove(board, Position(5, 7), Player.WHITE))
        assertFalse(validator.isValidMove(board, Position(7, 7), Player.WHITE)) // 중앙
        assertFalse(validator.isValidMove(board, Position(10, 10), Player.WHITE))
    }
    
    @Test
    fun `셋째 수는 천원에서 2칸 이내 26점만 가능`() {
        val validator = OpenRenjuValidator()
        var board = Board()
        board = board.placeStone(Move(Position.center(), Player.BLACK))
        board = board.placeStone(Move(Position(6, 7), Player.WHITE))
        
        // 5x5 범위 내는 허용 (중앙 제외)
        assertTrue(validator.isValidMove(board, Position(5, 5), Player.BLACK))
        assertTrue(validator.isValidMove(board, Position(5, 7), Player.BLACK))
        assertTrue(validator.isValidMove(board, Position(5, 9), Player.BLACK))
        assertTrue(validator.isValidMove(board, Position(7, 5), Player.BLACK))
        assertTrue(validator.isValidMove(board, Position(7, 9), Player.BLACK))
        assertTrue(validator.isValidMove(board, Position(9, 5), Player.BLACK))
        assertTrue(validator.isValidMove(board, Position(9, 7), Player.BLACK))
        assertTrue(validator.isValidMove(board, Position(9, 9), Player.BLACK))
        
        // 중앙은 거부
        assertFalse(validator.isValidMove(board, Position(7, 7), Player.BLACK))
        
        // 범위 밖은 거부
        assertFalse(validator.isValidMove(board, Position(4, 7), Player.BLACK))
        assertFalse(validator.isValidMove(board, Position(10, 7), Player.BLACK))
    }
    
    @Test
    fun `3수 후 스왑 대기 상태가 됨`() {
        val validator = OpenRenjuValidator()
        var board = Board()
        board = board.placeStone(Move(Position.center(), Player.BLACK))
        board = board.placeStone(Move(Position(6, 7), Player.WHITE))
        board = board.placeStone(Move(Position(5, 7), Player.BLACK))
        
        val state = validator.checkGameState(board, Move(Position(5, 7), Player.BLACK))
        assertTrue(state is GameState.WaitingForSwap)
    }
    
    @Test
    fun `4수 후 5수 제시 대기 상태가 됨`() {
        val validator = OpenRenjuValidator()
        validator.setSwapped(false) // 스왑하지 않음
        
        var board = Board()
        board = board.placeStone(Move(Position.center(), Player.BLACK))
        board = board.placeStone(Move(Position(6, 7), Player.WHITE))
        board = board.placeStone(Move(Position(5, 7), Player.BLACK))
        board = board.placeStone(Move(Position(8, 8), Player.WHITE))
        
        val state = validator.checkGameState(board, Move(Position(8, 8), Player.WHITE))
        assertTrue(state is GameState.WaitingForFifthMove)
    }
    
    @Test
    fun `스왑 후에는 플레이어 역할이 바뀜`() {
        val validator = OpenRenjuValidator()
        validator.setSwapped(true)
        
        var board = Board()
        board = board.placeStone(Move(Position.center(), Player.BLACK))
        board = board.placeStone(Move(Position(6, 7), Player.WHITE))
        board = board.placeStone(Move(Position(5, 7), Player.BLACK))
        
        // 스왑 후 4수는 아무 곳이나 가능
        assertTrue(validator.isValidMove(board, Position(10, 10), Player.WHITE))
        assertTrue(validator.isValidMove(board, Position(0, 0), Player.WHITE))
    }
    
    @Test
    fun `5수 제시와 선택 관리`() {
        val validator = OpenRenjuValidator()
        
        // 5수 제시
        val proposedMoves = listOf(Position(10, 10), Position(4, 4))
        validator.setProposedFifthMoves(proposedMoves)
        
        // 제시된 5수 확인
        assertEquals(proposedMoves, validator.getProposedFifthMoves())
    }
}