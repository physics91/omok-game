package com.omok.application.mapper

import com.omok.application.dto.*
import com.omok.domain.model.*

object GameMapper {
    
    fun toDto(position: Position): PositionDto {
        return PositionDto(position.row, position.col)
    }
    
    fun toDomain(positionDto: PositionDto): Position {
        return Position(positionDto.row, positionDto.col)
    }
    
    fun toDto(player: Player): PlayerDto {
        return when (player) {
            Player.BLACK -> PlayerDto.BLACK
            Player.WHITE -> PlayerDto.WHITE
        }
    }
    
    fun toDomain(playerDto: PlayerDto): Player {
        return when (playerDto) {
            PlayerDto.BLACK -> Player.BLACK
            PlayerDto.WHITE -> Player.WHITE
        }
    }
    
    fun toDto(gameMode: GameMode): GameModeDto {
        return when (gameMode) {
            GameMode.PLAYER_VS_PLAYER -> GameModeDto.PLAYER_VS_PLAYER
            GameMode.PLAYER_VS_AI -> GameModeDto.PLAYER_VS_AI
        }
    }
    
    fun toDomain(gameModeDto: GameModeDto): GameMode {
        return when (gameModeDto) {
            GameModeDto.PLAYER_VS_PLAYER -> GameMode.PLAYER_VS_PLAYER
            GameModeDto.PLAYER_VS_AI -> GameMode.PLAYER_VS_AI
        }
    }
    
    fun toDto(aiDifficulty: AIDifficulty): AIDifficultyDto {
        return when (aiDifficulty) {
            AIDifficulty.EASY -> AIDifficultyDto.EASY
            AIDifficulty.MEDIUM -> AIDifficultyDto.MEDIUM
            AIDifficulty.HARD -> AIDifficultyDto.HARD
        }
    }
    
    fun toDomain(aiDifficultyDto: AIDifficultyDto): AIDifficulty {
        return when (aiDifficultyDto) {
            AIDifficultyDto.EASY -> AIDifficulty.EASY
            AIDifficultyDto.MEDIUM -> AIDifficulty.MEDIUM
            AIDifficultyDto.HARD -> AIDifficulty.HARD
        }
    }
    
    fun toDto(gameRule: GameRule): GameRuleDto {
        return when (gameRule) {
            GameRule.STANDARD_RENJU -> GameRuleDto.STANDARD_RENJU
            GameRule.OPEN_RENJU -> GameRuleDto.OPEN_RENJU
            GameRule.YAMAGUCHI_RULE -> GameRuleDto.YAMAGUCHI_RULE
            GameRule.SWAP_RULE -> GameRuleDto.SWAP_RULE
            GameRule.SWAP2_RULE -> GameRuleDto.SWAP2_RULE
            GameRule.SOOSYRV_RULE -> GameRuleDto.SOOSYRV_RULE
            GameRule.TARAGUCHI_RULE -> GameRuleDto.TARAGUCHI_RULE
            GameRule.FREESTYLE -> GameRuleDto.FREESTYLE
            GameRule.CARO_RULE -> GameRuleDto.CARO_RULE
        }
    }
    
    fun toDomain(gameRuleDto: GameRuleDto): GameRule {
        return when (gameRuleDto) {
            GameRuleDto.STANDARD_RENJU -> GameRule.STANDARD_RENJU
            GameRuleDto.OPEN_RENJU -> GameRule.OPEN_RENJU
            GameRuleDto.YAMAGUCHI_RULE -> GameRule.YAMAGUCHI_RULE
            GameRuleDto.SWAP_RULE -> GameRule.SWAP_RULE
            GameRuleDto.SWAP2_RULE -> GameRule.SWAP2_RULE
            GameRuleDto.SOOSYRV_RULE -> GameRule.SOOSYRV_RULE
            GameRuleDto.TARAGUCHI_RULE -> GameRule.TARAGUCHI_RULE
            GameRuleDto.FREESTYLE -> GameRule.FREESTYLE
            GameRuleDto.CARO_RULE -> GameRule.CARO_RULE
        }
    }
    
    fun toDto(gameSettings: GameSettings): GameSettingsDto {
        return GameSettingsDto(
            mode = toDto(gameSettings.mode),
            aiDifficulty = gameSettings.aiDifficulty?.let { toDto(it) },
            gameRule = toDto(gameSettings.gameRule),
            timeLimit = toDto(gameSettings.timeLimit)
        )
    }

    fun toDomain(gameSettingsDto: GameSettingsDto): GameSettings {
        return GameSettings(
            mode = toDomain(gameSettingsDto.mode),
            aiDifficulty = gameSettingsDto.aiDifficulty?.let { toDomain(it) },
            gameRule = toDomain(gameSettingsDto.gameRule),
            timeLimit = toDomain(gameSettingsDto.timeLimit)
        )
    }
    
    fun toDto(board: Board): BoardDto {
        val stones = Array(15) { row ->
            Array(15) { col ->
                val position = Position(row, col)
                board.getStone(position)?.let { toDto(it) }
            }
        }
        return BoardDto(stones)
    }
    
    fun toDto(move: Move): MoveDto {
        return MoveDto(
            position = toDto(move.position),
            player = toDto(move.player),
            moveNumber = move.moveNumber
        )
    }
    
    fun toDto(gameState: GameState): GameStateDto {
        return when (gameState) {
            is GameState.Playing -> GameStateDto.Playing
            is GameState.Won -> GameStateDto.Won(
                winner = toDto(gameState.winner),
                winningLine = gameState.winningLine.map { toDto(it) }
            )
            is GameState.Draw -> GameStateDto.Draw
            // 오픈 렌주룰 특수 상태는 모두 Playing으로 매핑 (UI에서 별도 처리)
            is GameState.WaitingForSwap -> GameStateDto.Playing
            is GameState.WaitingForFifthMove -> GameStateDto.Playing
            is GameState.WaitingForFifthMoveSelection -> GameStateDto.Playing
        }
    }
    
    fun toDto(timeLimit: TimeLimit): TimeLimitDto {
        return TimeLimitDto(
            totalTimePerPlayer = timeLimit.totalTimePerPlayer,
            incrementPerMove = timeLimit.incrementPerMove,
            mode = toDto(timeLimit.mode)
        )
    }
    
    fun toDomain(timeLimitDto: TimeLimitDto): TimeLimit {
        return TimeLimit(
            totalTimePerPlayer = timeLimitDto.totalTimePerPlayer,
            incrementPerMove = timeLimitDto.incrementPerMove,
            mode = toDomain(timeLimitDto.mode)
        )
    }
    
    fun toDto(mode: TimeLimitMode): TimeLimitModeDto {
        return when (mode) {
            TimeLimitMode.NONE -> TimeLimitModeDto.NONE
            TimeLimitMode.TOTAL_TIME -> TimeLimitModeDto.TOTAL_TIME
            TimeLimitMode.FISCHER -> TimeLimitModeDto.FISCHER
            TimeLimitMode.BYOYOMI -> TimeLimitModeDto.BYOYOMI
        }
    }
    
    fun toDomain(modeDto: TimeLimitModeDto): TimeLimitMode {
        return when (modeDto) {
            TimeLimitModeDto.NONE -> TimeLimitMode.NONE
            TimeLimitModeDto.TOTAL_TIME -> TimeLimitMode.TOTAL_TIME
            TimeLimitModeDto.FISCHER -> TimeLimitMode.FISCHER
            TimeLimitModeDto.BYOYOMI -> TimeLimitMode.BYOYOMI
        }
    }
    
    fun toDto(playerTimeState: PlayerTimeState): PlayerTimeStateDto {
        return PlayerTimeStateDto(
            remainingTime = playerTimeState.remainingTime,
            byoyomiTime = playerTimeState.byoyomiTime,
            byoyomiPeriods = playerTimeState.byoyomiPeriods,
            isInByoyomi = playerTimeState.isInByoyomi
        )
    }
    
    fun toDto(gameTimeState: GameTimeState): GameTimeStateDto {
        return GameTimeStateDto(
            timeLimit = toDto(gameTimeState.timeLimit),
            blackTimeState = toDto(gameTimeState.blackTimeState),
            whiteTimeState = toDto(gameTimeState.whiteTimeState),
            isTimerRunning = gameTimeState.isTimerRunning
        )
    }
    
    fun toDto(game: Game): GameDto {
        return GameDto(
            board = toDto(game.getBoard()),
            currentPlayer = toDto(game.getCurrentPlayer()),
            state = toDto(game.getState()),
            moveHistory = game.getMoveHistory().map { toDto(it) },
            settings = toDto(game.getSettings()),
            isPlayerTurn = game.isPlayerTurn(),
            isAITurn = game.isAITurn(),
            timeState = game.getTimeState()?.let { toDto(it) },
            mode = toDto(game.getSettings().mode) // Add this line
        )
    }
    
    fun toDto(evaluation: AIEvaluation): AIEvaluationDto {
        return AIEvaluationDto(
            position = toDto(evaluation.position),
            score = evaluation.score,
            depth = evaluation.depth,
            isCandidate = evaluation.isCandidate,
            reason = evaluation.reason
        )
    }
    
    fun toDto(thinkingInfo: AIThinkingInfo): AIThinkingInfoDto {
        return AIThinkingInfoDto(
            evaluations = thinkingInfo.evaluations.map { toDto(it) },
            currentBestMove = thinkingInfo.currentBestMove?.let { toDto(it) },
            thinkingProgress = thinkingInfo.thinkingProgress,
            nodesEvaluated = thinkingInfo.nodesEvaluated,
            currentDepth = thinkingInfo.currentDepth
        )
    }
    
    fun toDomain(moveDto: MoveDto): Move {
        return Move(
            position = toDomain(moveDto.position),
            player = toDomain(moveDto.player),
            moveNumber = moveDto.moveNumber
        )
    }
    
    /**
     * GameDto를 Game 도메인 모델로 변환
     * SGF 내보내기를 위해 사용
     */
    fun toDomain(gameDto: GameDto): Game {
        // 게임 설정으로 새 게임 생성
        val settings = toDomain(gameDto.settings)
        var game = Game(settings)
        
        // 수순 재생
        for (moveDto in gameDto.moveHistory) {
            val move = toDomain(moveDto)
            game = game.makeMove(move.position)
        }
        
        // 게임 상태 업데이트
        val domainState = when (gameDto.state) {
            is GameStateDto.Won -> GameState.Won(
                winner = toDomain(gameDto.state.winner),
                winningLine = gameDto.state.winningLine.map { toDomain(it) }
            )
            is GameStateDto.Draw -> GameState.Draw
            is GameStateDto.Playing -> GameState.Playing
        }
        
        game = game.updateState(domainState)
        
        return game
    }
}