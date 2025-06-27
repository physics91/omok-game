package com.omok.domain.tournament

import com.omok.domain.model.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 토너먼트 도메인 모델
 */
data class Tournament(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val format: TournamentFormat,
    val participants: List<TournamentParticipant>,
    val settings: TournamentSettings,
    val rounds: List<TournamentRound> = emptyList(),
    val currentRoundIndex: Int = 0,
    val status: TournamentStatus = TournamentStatus.NOT_STARTED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val startedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null
) {
    val isActive: Boolean
        get() = status == TournamentStatus.IN_PROGRESS
    
    val isCompleted: Boolean
        get() = status == TournamentStatus.COMPLETED
    
    val currentRound: TournamentRound?
        get() = rounds.getOrNull(currentRoundIndex)
    
    /**
     * 토너먼트 시작
     */
    fun start(): Tournament {
        require(status == TournamentStatus.NOT_STARTED) { "Tournament already started" }
        require(participants.size >= 2) { "Need at least 2 participants" }
        
        val firstRound = when (format) {
            TournamentFormat.SINGLE_ELIMINATION -> generateSingleEliminationRound(participants)
            TournamentFormat.DOUBLE_ELIMINATION -> generateDoubleEliminationRound(participants)
            TournamentFormat.ROUND_ROBIN -> generateRoundRobinRounds(participants).first()
            TournamentFormat.SWISS -> generateSwissRound(participants, 1)
        }
        
        return copy(
            rounds = listOf(firstRound),
            status = TournamentStatus.IN_PROGRESS,
            startedAt = LocalDateTime.now()
        )
    }
    
    /**
     * 매치 결과 기록
     */
    fun recordMatchResult(matchId: String, result: MatchResult): Tournament {
        val updatedRounds = rounds.map { round ->
            if (round.matches.any { it.id == matchId }) {
                round.copy(
                    matches = round.matches.map { match ->
                        if (match.id == matchId) match.complete(result) else match
                    }
                )
            } else {
                round
            }
        }
        
        // 현재 라운드가 완료되었는지 확인
        val currentRound = updatedRounds[currentRoundIndex]
        return if (currentRound.isCompleted) {
            proceedToNextRound(updatedRounds)
        } else {
            copy(rounds = updatedRounds)
        }
    }
    
    /**
     * 다음 라운드로 진행
     */
    private fun proceedToNextRound(updatedRounds: List<TournamentRound>): Tournament {
        val winners = updatedRounds[currentRoundIndex].getWinners()
        
        return when (format) {
            TournamentFormat.SINGLE_ELIMINATION -> {
                if (winners.size == 1) {
                    // 토너먼트 종료
                    copy(
                        rounds = updatedRounds,
                        status = TournamentStatus.COMPLETED,
                        completedAt = LocalDateTime.now()
                    )
                } else {
                    // 다음 라운드 생성
                    val nextRound = generateSingleEliminationRound(winners)
                    copy(
                        rounds = updatedRounds + nextRound,
                        currentRoundIndex = currentRoundIndex + 1
                    )
                }
            }
            TournamentFormat.ROUND_ROBIN -> {
                val allRounds = generateRoundRobinRounds(participants)
                if (currentRoundIndex + 1 < allRounds.size) {
                    copy(
                        rounds = updatedRounds + allRounds[currentRoundIndex + 1],
                        currentRoundIndex = currentRoundIndex + 1
                    )
                } else {
                    copy(
                        rounds = updatedRounds,
                        status = TournamentStatus.COMPLETED,
                        completedAt = LocalDateTime.now()
                    )
                }
            }
            else -> TODO("Implement other formats")
        }
    }
    
    /**
     * 현재 순위 계산
     */
    fun getStandings(): List<TournamentStanding> {
        val stats = mutableMapOf<String, ParticipantStats>()
        
        // 모든 참가자 초기화
        participants.forEach { participant ->
            stats[participant.id] = ParticipantStats(participant)
        }
        
        // 완료된 매치 결과 집계
        rounds.forEach { round ->
            round.matches.filter { it.isCompleted }.forEach { match ->
                val result = match.result!!
                
                // 승자 통계 업데이트
                stats[result.winnerId]?.let { winnerStats ->
                    stats[result.winnerId] = winnerStats.copy(
                        wins = winnerStats.wins + 1,
                        points = winnerStats.points + settings.pointsForWin
                    )
                }
                
                // 패자 통계 업데이트
                val loserId = if (result.winnerId == match.player1.id) match.player2.id else match.player1.id
                stats[loserId]?.let { loserStats ->
                    stats[loserId] = loserStats.copy(
                        losses = loserStats.losses + 1,
                        points = loserStats.points + settings.pointsForLoss
                    )
                }
                
                // 무승부 처리
                if (result.isDraw) {
                    listOf(match.player1.id, match.player2.id).forEach { playerId ->
                        stats[playerId]?.let { playerStats ->
                            stats[playerId] = playerStats.copy(
                                draws = playerStats.draws + 1,
                                points = playerStats.points + settings.pointsForDraw
                            )
                        }
                    }
                }
            }
        }
        
        // 순위 계산
        return stats.values
            .sortedWith(compareByDescending<ParticipantStats> { it.points }
                .thenByDescending { it.wins }
                .thenBy { it.losses })
            .mapIndexed { index, stats ->
                TournamentStanding(
                    rank = index + 1,
                    participant = stats.participant,
                    wins = stats.wins,
                    losses = stats.losses,
                    draws = stats.draws,
                    points = stats.points
                )
            }
    }
    
    private fun generateSingleEliminationRound(participants: List<TournamentParticipant>): TournamentRound {
        val matches = participants.chunked(2).mapIndexed { index, pair ->
            if (pair.size == 2) {
                TournamentMatch(
                    id = UUID.randomUUID().toString(),
                    roundNumber = currentRoundIndex + 1,
                    matchNumber = index + 1,
                    player1 = pair[0],
                    player2 = pair[1],
                    gameSettings = settings.gameSettings
                )
            } else {
                // 부전승 처리
                TournamentMatch(
                    id = UUID.randomUUID().toString(),
                    roundNumber = currentRoundIndex + 1,
                    matchNumber = index + 1,
                    player1 = pair[0],
                    player2 = pair[0], // 더미
                    gameSettings = settings.gameSettings,
                    status = MatchStatus.COMPLETED,
                    result = MatchResult(
                        winnerId = pair[0].id,
                        gameRecord = null,
                        isDraw = false,
                        isBye = true
                    )
                )
            }
        }
        
        return TournamentRound(
            roundNumber = currentRoundIndex + 1,
            matches = matches,
            name = getRoundName(currentRoundIndex + 1, participants.size)
        )
    }
    
    private fun generateRoundRobinRounds(participants: List<TournamentParticipant>): List<TournamentRound> {
        val rounds = mutableListOf<TournamentRound>()
        val n = participants.size
        
        // 라운드 로빈 알고리즘
        for (round in 0 until n - 1) {
            val matches = mutableListOf<TournamentMatch>()
            
            for (i in 0 until n / 2) {
                val player1Index = (round + i) % (n - 1)
                val player2Index = if (i == 0) n - 1 else (round + n - 1 - i) % (n - 1)
                
                matches.add(
                    TournamentMatch(
                        id = UUID.randomUUID().toString(),
                        roundNumber = round + 1,
                        matchNumber = i + 1,
                        player1 = participants[player1Index],
                        player2 = participants[player2Index],
                        gameSettings = settings.gameSettings
                    )
                )
            }
            
            rounds.add(
                TournamentRound(
                    roundNumber = round + 1,
                    matches = matches,
                    name = "Round ${round + 1}"
                )
            )
        }
        
        return rounds
    }
    
    private fun generateDoubleEliminationRound(participants: List<TournamentParticipant>): TournamentRound {
        TODO("Implement double elimination")
    }
    
    private fun generateSwissRound(participants: List<TournamentParticipant>, roundNumber: Int): TournamentRound {
        TODO("Implement Swiss system")
    }
    
    private fun getRoundName(roundNumber: Int, totalParticipants: Int): String {
        val remainingPlayers = totalParticipants / (1 shl (roundNumber - 1))
        return when (remainingPlayers) {
            2 -> "결승"
            4 -> "준결승"
            8 -> "8강"
            16 -> "16강"
            else -> "Round $roundNumber"
        }
    }
}

/**
 * 토너먼트 형식
 */
enum class TournamentFormat(val displayName: String, val description: String) {
    SINGLE_ELIMINATION("싱글 엘리미네이션", "패배하면 탈락하는 방식"),
    DOUBLE_ELIMINATION("더블 엘리미네이션", "두 번 패배해야 탈락하는 방식"),
    ROUND_ROBIN("라운드 로빈", "모든 참가자가 서로 한 번씩 대결"),
    SWISS("스위스", "실력이 비슷한 참가자끼리 매칭")
}

/**
 * 토너먼트 상태
 */
enum class TournamentStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED
}

/**
 * 토너먼트 참가자
 */
data class TournamentParticipant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rating: Int = 1500, // ELO 레이팅
    val seed: Int? = null   // 시드 번호
)

/**
 * 토너먼트 설정
 */
data class TournamentSettings(
    val gameSettings: GameSettings,
    val pointsForWin: Int = 3,
    val pointsForDraw: Int = 1,
    val pointsForLoss: Int = 0,
    val timeControlEnabled: Boolean = false,
    val allowSpectators: Boolean = true
)

/**
 * 토너먼트 라운드
 */
data class TournamentRound(
    val roundNumber: Int,
    val name: String,
    val matches: List<TournamentMatch>
) {
    val isCompleted: Boolean
        get() = matches.all { it.isCompleted }
    
    fun getWinners(): List<TournamentParticipant> {
        return matches.filter { it.isCompleted && !it.result!!.isDraw }
            .map { match ->
                if (match.result!!.winnerId == match.player1.id) match.player1 else match.player2
            }
    }
}

/**
 * 토너먼트 매치
 */
data class TournamentMatch(
    val id: String,
    val roundNumber: Int,
    val matchNumber: Int,
    val player1: TournamentParticipant,
    val player2: TournamentParticipant,
    val gameSettings: GameSettings,
    val status: MatchStatus = MatchStatus.NOT_STARTED,
    val result: MatchResult? = null,
    val startedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null
) {
    val isCompleted: Boolean
        get() = status == MatchStatus.COMPLETED
    
    fun start(): TournamentMatch = copy(
        status = MatchStatus.IN_PROGRESS,
        startedAt = LocalDateTime.now()
    )
    
    fun complete(result: MatchResult): TournamentMatch = copy(
        status = MatchStatus.COMPLETED,
        result = result,
        completedAt = LocalDateTime.now()
    )
}

/**
 * 매치 상태
 */
enum class MatchStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

/**
 * 매치 결과
 */
data class MatchResult(
    val winnerId: String,
    val gameRecord: Game?,
    val isDraw: Boolean = false,
    val isBye: Boolean = false
)

/**
 * 토너먼트 순위
 */
data class TournamentStanding(
    val rank: Int,
    val participant: TournamentParticipant,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val points: Int
)

/**
 * 참가자 통계 (내부 사용)
 */
private data class ParticipantStats(
    val participant: TournamentParticipant,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val points: Int = 0
)