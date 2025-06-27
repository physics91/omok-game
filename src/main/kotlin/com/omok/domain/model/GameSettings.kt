package com.omok.domain.model

data class GameSettings(
    val mode: GameMode,
    val aiDifficulty: AIDifficulty? = null,
    val playerColor: Player? = Player.BLACK,
    val gameRule: GameRule = GameRule.STANDARD_RENJU,
    val timeLimit: TimeLimit = TimeLimit.NONE
)