package com.omok.domain.model

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE
    
    companion object {
        const val BOARD_SIZE = 15
        
        fun center(): Position = Position(BOARD_SIZE / 2, BOARD_SIZE / 2)
    }
}