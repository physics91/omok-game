package com.omok.domain.model

/**
 * 시간 제한 설정
 */
data class TimeLimit(
    val totalTimePerPlayer: Long, // 플레이어당 총 시간 (초)
    val incrementPerMove: Long = 0L, // 수당 추가 시간 (초)
    val mode: TimeLimitMode = TimeLimitMode.TOTAL_TIME
) {
    companion object {
        val NONE = TimeLimit(0L, 0L, TimeLimitMode.NONE)
        val BLITZ_3_MINUTES = TimeLimit(180L, 0L, TimeLimitMode.TOTAL_TIME)
        val RAPID_10_MINUTES = TimeLimit(600L, 0L, TimeLimitMode.TOTAL_TIME)
        val CLASSICAL_30_MINUTES = TimeLimit(1800L, 0L, TimeLimitMode.TOTAL_TIME)
        val FISCHER_5_PLUS_3 = TimeLimit(300L, 3L, TimeLimitMode.FISCHER)
        val BYOYOMI_10_PLUS_30 = TimeLimit(600L, 30L, TimeLimitMode.BYOYOMI)
    }
}

/**
 * 시간 제한 모드
 */
enum class TimeLimitMode(val displayName: String, val description: String) {
    NONE("제한 없음", "시간 제한이 없습니다"),
    TOTAL_TIME("총 시간", "플레이어당 정해진 총 시간 내에 게임을 완료해야 합니다"),
    FISCHER("피셔 모드", "매 수마다 추가 시간이 주어집니다"),
    BYOYOMI("초읽기", "시간이 부족할 때 수당 일정 시간이 주어집니다")
}

/**
 * 플레이어별 시간 상태
 */
data class PlayerTimeState(
    val remainingTime: Long, // 남은 시간 (초)
    val byoyomiTime: Long = 0L, // 초읽기 시간 (초)
    val byoyomiPeriods: Int = 0, // 남은 초읽기 횟수
    val isInByoyomi: Boolean = false // 초읽기 상태 여부
) {
    companion object {
        fun create(timeLimit: TimeLimit): PlayerTimeState {
            return PlayerTimeState(
                remainingTime = timeLimit.totalTimePerPlayer,
                byoyomiTime = if (timeLimit.mode == TimeLimitMode.BYOYOMI) timeLimit.incrementPerMove else 0L,
                byoyomiPeriods = if (timeLimit.mode == TimeLimitMode.BYOYOMI) 3 else 0,
                isInByoyomi = false
            )
        }
    }
    
    fun isTimeUp(): Boolean {
        return remainingTime <= 0 && (!isInByoyomi || byoyomiPeriods <= 0)
    }
    
    fun canContinue(): Boolean {
        return !isTimeUp()
    }
}

/**
 * 게임 시간 상태
 */
data class GameTimeState(
    val timeLimit: TimeLimit,
    val blackTimeState: PlayerTimeState,
    val whiteTimeState: PlayerTimeState,
    val lastMoveTime: Long = System.currentTimeMillis(), // 마지막 수의 시간
    val isTimerRunning: Boolean = false,
    val currentPlayerStartTime: Long = 0L // 현재 플레이어의 턴 시작 시간
) {
    companion object {
        fun create(timeLimit: TimeLimit): GameTimeState {
            return GameTimeState(
                timeLimit = timeLimit,
                blackTimeState = PlayerTimeState.create(timeLimit),
                whiteTimeState = PlayerTimeState.create(timeLimit),
                lastMoveTime = System.currentTimeMillis(),
                isTimerRunning = timeLimit.mode != TimeLimitMode.NONE,
                currentPlayerStartTime = System.currentTimeMillis()
            )
        }
    }
    
    fun getTimeState(player: Player): PlayerTimeState {
        return when (player) {
            Player.BLACK -> blackTimeState
            Player.WHITE -> whiteTimeState
        }
    }
    
    fun updateTimeState(player: Player, newState: PlayerTimeState): GameTimeState {
        return when (player) {
            Player.BLACK -> copy(blackTimeState = newState)
            Player.WHITE -> copy(whiteTimeState = newState)
        }
    }
    
    fun startTimer(currentPlayer: Player): GameTimeState {
        return copy(
            isTimerRunning = true,
            currentPlayerStartTime = System.currentTimeMillis()
        )
    }
    
    fun stopTimer(): GameTimeState {
        return copy(isTimerRunning = false)
    }
    
    fun isCurrentPlayerTimeUp(currentPlayer: Player): Boolean {
        return getTimeState(currentPlayer).isTimeUp()
    }
}