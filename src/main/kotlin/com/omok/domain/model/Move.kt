package com.omok.domain.model

data class Move(
    val position: Position,
    val player: Player,
    val moveNumber: Int = 0
) {
    fun isValid(): Boolean = position.isValid()
}