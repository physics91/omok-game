package com.omok.domain.model

sealed class GameState {
    object Playing : GameState()
    data class Won(val winner: Player, val winningLine: List<Position>) : GameState()
    object Draw : GameState()
    
    // 오픈 렌주룰 특수 상태
    object WaitingForSwap : GameState() // 3수 후 백이 스왑 결정 대기
    data class WaitingForFifthMove(val proposedMoves: List<Position> = emptyList()) : GameState() // 흑이 5수 2개 제시
    object WaitingForFifthMoveSelection : GameState() // 백이 5수 중 하나 선택
}

enum class GameMode {
    PLAYER_VS_PLAYER,
    PLAYER_VS_AI
}

data class GameSettings(
    val mode: GameMode,
    val aiDifficulty: AIDifficulty? = AIDifficulty.MEDIUM,
    val gameRule: GameRule = GameRule.STANDARD_RENJU,
    val timeLimit: TimeLimit = TimeLimit.NONE
)

enum class AIDifficulty(
    val depth: Int, 
    val randomness: Double,
    val thinkingTimeMs: Long = 5000L
) {
    EASY(2, 0.3, 2000L),
    MEDIUM(3, 0.1, 5000L),
    HARD(4, 0.0, 10000L)
}