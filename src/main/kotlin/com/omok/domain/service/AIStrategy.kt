package com.omok.domain.service

import com.omok.domain.model.*

interface AIStrategy {
    fun getBestMove(board: Board, player: Player): Position?
}