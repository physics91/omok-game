package com.omok.application.dto

data class PositionDto(val row: Int, val col: Int)

data class MoveDto(val position: PositionDto, val player: PlayerDto, val moveNumber: Int)

enum class PlayerDto {
    BLACK, WHITE
}

enum class GameModeDto {
    PLAYER_VS_PLAYER,
    PLAYER_VS_AI
}

enum class AIDifficultyDto {
    EASY, MEDIUM, HARD
}

enum class GameRuleDto(val displayName: String) {
    STANDARD_RENJU("표준 렌주룰"),
    OPEN_RENJU("오픈 렌주룰"),
    YAMAGUCHI_RULE("야마구치룰"),
    SWAP_RULE("스왑룰"),
    SWAP2_RULE("스영2룰"),
    SOOSYRV_RULE("수시르브룰"),
    TARAGUCHI_RULE("타라구치룰"),
    FREESTYLE("자유룰"),
    CARO_RULE("카로룰")
}

data class GameSettingsDto(
    val mode: GameModeDto,
    val aiDifficulty: AIDifficultyDto? = null,
    val gameRule: GameRuleDto = GameRuleDto.STANDARD_RENJU
)

sealed class GameStateDto {
    object Playing : GameStateDto()
    data class Won(val winner: PlayerDto, val winningLine: List<PositionDto>) : GameStateDto()
    object Draw : GameStateDto()
}

data class BoardDto(val stones: Array<Array<PlayerDto?>>) {
    fun getStone(position: PositionDto): PlayerDto? {
        return stones[position.row][position.col]
    }
    
    fun isEmpty(position: PositionDto): Boolean = getStone(position) == null
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as BoardDto
        
        if (!stones.contentDeepEquals(other.stones)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        return stones.contentDeepHashCode()
    }
}

data class GameDto(
    val board: BoardDto,
    val currentPlayer: PlayerDto,
    val state: GameStateDto,
    val moveHistory: List<MoveDto>,
    val settings: GameSettingsDto,
    val isPlayerTurn: Boolean,
    val isAITurn: Boolean
)