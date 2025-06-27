package com.omok.infrastructure.tournament

import com.omok.domain.tournament.*
import com.omok.domain.model.*
import com.omok.infrastructure.logging.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * 토너먼트 관리자
 * 토너먼트의 생성, 진행, 저장을 담당
 */
class TournamentManager {
    
    private val tournaments = ConcurrentHashMap<String, Tournament>()
    private var activeTournamentId: String? = null
    
    /**
     * 새 토너먼트 생성
     */
    fun createTournament(
        name: String,
        format: TournamentFormat,
        participants: List<TournamentParticipant>,
        settings: TournamentSettings
    ): Tournament {
        val tournament = Tournament(
            name = name,
            format = format,
            participants = participants,
            settings = settings
        )
        
        tournaments[tournament.id] = tournament
        Logger.info("TournamentManager", "Created tournament: $name with ${participants.size} participants")
        
        return tournament
    }
    
    /**
     * 토너먼트 시작
     */
    fun startTournament(tournamentId: String): Tournament? {
        val tournament = tournaments[tournamentId] ?: return null
        
        return try {
            val startedTournament = tournament.start()
            tournaments[tournamentId] = startedTournament
            activeTournamentId = tournamentId
            
            Logger.info("TournamentManager", "Started tournament: ${tournament.name}")
            startedTournament
        } catch (e: Exception) {
            Logger.error("TournamentManager", "Failed to start tournament", e)
            null
        }
    }
    
    /**
     * 매치 시작
     */
    fun startMatch(tournamentId: String, matchId: String): TournamentMatch? {
        val tournament = tournaments[tournamentId] ?: return null
        val match = tournament.rounds.flatMap { it.matches }.find { it.id == matchId } ?: return null
        
        if (match.status != MatchStatus.NOT_STARTED) {
            Logger.warn("TournamentManager", "Match already started: $matchId")
            return null
        }
        
        val updatedMatch = match.start()
        updateMatch(tournamentId, updatedMatch)
        
        return updatedMatch
    }
    
    /**
     * 매치 결과 기록
     */
    fun recordMatchResult(
        tournamentId: String,
        matchId: String,
        winnerId: String,
        game: Game? = null
    ): Tournament? {
        val tournament = tournaments[tournamentId] ?: return null
        
        val result = MatchResult(
            winnerId = winnerId,
            gameRecord = game,
            isDraw = false
        )
        
        val updatedTournament = tournament.recordMatchResult(matchId, result)
        tournaments[tournamentId] = updatedTournament
        
        Logger.info("TournamentManager", "Recorded match result: $matchId, winner: $winnerId")
        
        // 토너먼트가 완료되었는지 확인
        if (updatedTournament.isCompleted) {
            Logger.info("TournamentManager", "Tournament completed: ${tournament.name}")
            if (activeTournamentId == tournamentId) {
                activeTournamentId = null
            }
        }
        
        return updatedTournament
    }
    
    /**
     * 활성 토너먼트 가져오기
     */
    fun getActiveTournament(): Tournament? {
        return activeTournamentId?.let { tournaments[it] }
    }
    
    /**
     * 토너먼트 가져오기
     */
    fun getTournament(tournamentId: String): Tournament? {
        return tournaments[tournamentId]
    }
    
    /**
     * 모든 토너먼트 목록
     */
    fun getAllTournaments(): List<Tournament> {
        return tournaments.values.toList()
    }
    
    /**
     * 현재 매치 가져오기
     */
    fun getCurrentMatch(tournamentId: String): TournamentMatch? {
        val tournament = tournaments[tournamentId] ?: return null
        return tournament.currentRound?.matches?.find { it.status == MatchStatus.IN_PROGRESS }
    }
    
    /**
     * 다음 매치 가져오기
     */
    fun getNextMatch(tournamentId: String): TournamentMatch? {
        val tournament = tournaments[tournamentId] ?: return null
        return tournament.currentRound?.matches?.find { it.status == MatchStatus.NOT_STARTED }
    }
    
    /**
     * 토너먼트 순위 가져오기
     */
    fun getStandings(tournamentId: String): List<TournamentStanding> {
        val tournament = tournaments[tournamentId] ?: return emptyList()
        return tournament.getStandings()
    }
    
    /**
     * 토너먼트 취소
     */
    fun cancelTournament(tournamentId: String): Boolean {
        val tournament = tournaments[tournamentId] ?: return false
        
        if (tournament.status == TournamentStatus.COMPLETED) {
            return false
        }
        
        tournaments[tournamentId] = tournament.copy(status = TournamentStatus.CANCELLED)
        
        if (activeTournamentId == tournamentId) {
            activeTournamentId = null
        }
        
        Logger.info("TournamentManager", "Cancelled tournament: ${tournament.name}")
        return true
    }
    
    /**
     * 토너먼트 일시정지
     */
    fun pauseTournament(tournamentId: String): Boolean {
        val tournament = tournaments[tournamentId] ?: return false
        
        if (tournament.status != TournamentStatus.IN_PROGRESS) {
            return false
        }
        
        tournaments[tournamentId] = tournament.copy(status = TournamentStatus.PAUSED)
        Logger.info("TournamentManager", "Paused tournament: ${tournament.name}")
        return true
    }
    
    /**
     * 토너먼트 재개
     */
    fun resumeTournament(tournamentId: String): Boolean {
        val tournament = tournaments[tournamentId] ?: return false
        
        if (tournament.status != TournamentStatus.PAUSED) {
            return false
        }
        
        tournaments[tournamentId] = tournament.copy(status = TournamentStatus.IN_PROGRESS)
        activeTournamentId = tournamentId
        Logger.info("TournamentManager", "Resumed tournament: ${tournament.name}")
        return true
    }
    
    /**
     * 매치 업데이트 (내부 헬퍼)
     */
    private fun updateMatch(tournamentId: String, updatedMatch: TournamentMatch) {
        val tournament = tournaments[tournamentId] ?: return
        
        val updatedRounds = tournament.rounds.map { round ->
            round.copy(
                matches = round.matches.map { match ->
                    if (match.id == updatedMatch.id) updatedMatch else match
                }
            )
        }
        
        tournaments[tournamentId] = tournament.copy(rounds = updatedRounds)
    }
    
    companion object {
        private var instance: TournamentManager? = null
        
        fun getInstance(): TournamentManager {
            return instance ?: synchronized(this) {
                instance ?: TournamentManager().also { instance = it }
            }
        }
    }
}