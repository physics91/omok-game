package com.omok.domain.model

sealed class GameState {
    object Playing : GameState()
    data class Won(val winner: Player, val winningLine: List<Position>) : GameState()
    object Draw : GameState()
}

enum class GameMode {
    PLAYER_VS_PLAYER,
    PLAYER_VS_AI
}

data class GameSettings(
    val mode: GameMode,
    val aiDifficulty: AIDifficulty? = AIDifficulty.MEDIUM,
    val gameRule: GameRule = GameRule.STANDARD_RENJU
)

enum class AIDifficulty(val depth: Int, val randomness: Double) {
    EASY(2, 0.3),
    MEDIUM(3, 0.1),
    HARD(4, 0.0)
}