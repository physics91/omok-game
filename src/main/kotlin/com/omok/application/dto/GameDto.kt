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

enum class GameRuleDto(val displayName: String, val description: String) {
    STANDARD_RENJU("표준 렌주룰", "흑돌 금수: 3-3, 4-4, 장목(6목 이상)"),
    OPEN_RENJU("오픈 렌주룰", "1수: 천원, 2수: 8곳, 3수: 26점, 스왑 가능, 5수: 흑이 2개 제시 백이 선택"),
    YAMAGUCHI_RULE("야마구치룰", "흑이 첫 3수를 놓고, 백이 흑백 선택권 행사"),
    SWAP_RULE("스왑룰", "첫 수 이후 백이 흑백 교체 선택 가능"),
    SWAP2_RULE("스왑2룰", "흑이 3수를 놓고 백이 다양한 옵션 중 선택"),
    SOOSYRV_RULE("수시르브룰", "8가지 표준 오프닝 중 하나로 시작"),
    TARAGUCHI_RULE("타라구치룰", "10가지 표준 오프닝 + 백이 5번째 수 제안"),
    FREESTYLE("자유룰", "제한 없음 - 5개 이상 연속이면 승리"),
    CARO_RULE("카로룰", "베트남식 - 3-3, 4-4 허용, 6목 이상도 승리")
}

data class TimeLimitDto(
    val totalTimePerPlayer: Long,
    val incrementPerMove: Long = 0L,
    val mode: TimeLimitModeDto = TimeLimitModeDto.TOTAL_TIME
) {
    companion object {
        val NONE = TimeLimitDto(0L, 0L, TimeLimitModeDto.NONE)
    }
}

enum class TimeLimitModeDto(val displayName: String) {
    NONE("제한 없음"),
    TOTAL_TIME("총 시간"),
    FISCHER("피셔 모드"),
    BYOYOMI("초읽기")
}

data class PlayerTimeStateDto(
    val remainingTime: Long,
    val byoyomiTime: Long = 0L,
    val byoyomiPeriods: Int = 0,
    val isInByoyomi: Boolean = false
)

data class GameTimeStateDto(
    val timeLimit: TimeLimitDto,
    val blackTimeState: PlayerTimeStateDto,
    val whiteTimeState: PlayerTimeStateDto,
    val isTimerRunning: Boolean = false
)

data class GameSettingsDto(
    val mode: GameModeDto,
    val aiDifficulty: AIDifficultyDto? = null,
    val gameRule: GameRuleDto = GameRuleDto.STANDARD_RENJU,
    val timeLimit: TimeLimitDto = TimeLimitDto.NONE
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
    val isAITurn: Boolean,
    val timeState: GameTimeStateDto? = null,
    val mode: GameModeDto // Add this line
)