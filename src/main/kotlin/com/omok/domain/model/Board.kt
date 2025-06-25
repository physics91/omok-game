package com.omok.domain.model

import com.omok.domain.model.Position.Companion.BOARD_SIZE

class Board private constructor(
    private val stones: Array<Array<Player?>>
) {
    constructor() : this(Array(BOARD_SIZE) { Array(BOARD_SIZE) { null } })
    
    fun getStone(position: Position): Player? {
        require(position.isValid()) { "Invalid position: $position" }
        return stones[position.row][position.col]
    }
    
    fun isEmpty(position: Position): Boolean = getStone(position) == null
    
    fun placeStone(move: Move): Board {
        require(move.isValid()) { "Invalid move: $move" }
        require(isEmpty(move.position)) { "Position already occupied: ${move.position}" }
        
        val newStones = Array(BOARD_SIZE) { row ->
            Array(BOARD_SIZE) { col ->
                stones[row][col]
            }
        }
        newStones[move.position.row][move.position.col] = move.player
        return Board(newStones)
    }
    
    fun getAllStones(): Array<Array<Player?>> {
        return Array(BOARD_SIZE) { row ->
            Array(BOARD_SIZE) { col ->
                stones[row][col]
            }
        }
    }
    
    fun getEmptyPositions(): List<Position> {
        val emptyPositions = mutableListOf<Position>()
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val position = Position(row, col)
                if (isEmpty(position)) {
                    emptyPositions.add(position)
                }
            }
        }
        return emptyPositions
    }
    
    fun getMoveCount(): Int {
        var count = 0
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (stones[row][col] != null) {
                    count++
                }
            }
        }
        return count
    }
    
    fun isFull(): Boolean = getEmptyPositions().isEmpty()
    
    fun hasAdjacentStone(position: Position, range: Int = 2): Boolean {
        for (dr in -range..range) {
            for (dc in -range..range) {
                if (dr == 0 && dc == 0) continue
                val adjacent = Position(position.row + dr, position.col + dc)
                if (adjacent.isValid() && !isEmpty(adjacent)) {
                    return true
                }
            }
        }
        return false
    }
    
    fun copy(): Board = Board(getAllStones())
}