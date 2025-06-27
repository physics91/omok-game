package com.omok.domain.ai.pattern

import com.omok.domain.model.Player

/**
 * 오목 패턴 정의
 */
data class Pattern(
    val name: String,
    val pattern: String,
    val score: Int,
    val category: PatternCategory
) {
    companion object {
        // 패턴 기호:
        // X = 자기 돌
        // O = 상대 돌
        // . = 빈 공간
        // # = 벽 (보드 경계)
        // * = 평가 위치 (빈 공간이어야 함)
        
        val PATTERNS = listOf(
            // 5목 패턴
            Pattern("Five", "XXXXX", 1000000, PatternCategory.WINNING),
            
            // 열린 4 패턴
            Pattern("OpenFour", ".XXXX.", 50000, PatternCategory.CRITICAL),
            Pattern("OpenFour2", "*XXXX.", 50000, PatternCategory.CRITICAL),
            Pattern("OpenFour3", ".XXXX*", 50000, PatternCategory.CRITICAL),
            
            // 4목 패턴
            Pattern("Four1", "XXXX.", 10000, PatternCategory.FORCING),
            Pattern("Four2", ".XXXX", 10000, PatternCategory.FORCING),
            Pattern("Four3", "XXX.X", 10000, PatternCategory.FORCING),
            Pattern("Four4", "XX.XX", 10000, PatternCategory.FORCING),
            Pattern("Four5", "X.XXX", 10000, PatternCategory.FORCING),
            
            // 열린 3 패턴
            Pattern("OpenThree1", "..XXX..", 5000, PatternCategory.THREATENING),
            Pattern("OpenThree2", ".*XXX..", 5000, PatternCategory.THREATENING),
            Pattern("OpenThree3", "..XXX*.", 5000, PatternCategory.THREATENING),
            Pattern("OpenThree4", ".XX.X.", 5000, PatternCategory.THREATENING),
            Pattern("OpenThree5", ".X.XX.", 5000, PatternCategory.THREATENING),
            
            // 3목 패턴
            Pattern("Three1", "XXX..", 1000, PatternCategory.BUILDING),
            Pattern("Three2", "..XXX", 1000, PatternCategory.BUILDING),
            Pattern("Three3", ".XXX.", 1000, PatternCategory.BUILDING),
            Pattern("Three4", "XX.X.", 1000, PatternCategory.BUILDING),
            Pattern("Three5", ".XX.X", 1000, PatternCategory.BUILDING),
            Pattern("Three6", "X.XX.", 1000, PatternCategory.BUILDING),
            Pattern("Three7", ".X.XX", 1000, PatternCategory.BUILDING),
            Pattern("Three8", "X..XX", 800, PatternCategory.BUILDING),
            Pattern("Three9", "XX..X", 800, PatternCategory.BUILDING),
            
            // 열린 2 패턴
            Pattern("OpenTwo1", "...XX...", 500, PatternCategory.DEVELOPING),
            Pattern("OpenTwo2", "...*X...", 500, PatternCategory.DEVELOPING),
            Pattern("OpenTwo3", "...X*...", 500, PatternCategory.DEVELOPING),
            Pattern("OpenTwo4", "..X.X..", 500, PatternCategory.DEVELOPING),
            Pattern("OpenTwo5", "..*X.X..", 400, PatternCategory.DEVELOPING),
            Pattern("OpenTwo6", "..X.*X..", 400, PatternCategory.DEVELOPING),
            Pattern("OpenTwo7", "..X.X*..", 400, PatternCategory.DEVELOPING),
            
            // 2목 패턴
            Pattern("Two1", "XX...", 100, PatternCategory.DEVELOPING),
            Pattern("Two2", "...XX", 100, PatternCategory.DEVELOPING),
            Pattern("Two3", ".XX..", 100, PatternCategory.DEVELOPING),
            Pattern("Two4", "..XX.", 100, PatternCategory.DEVELOPING),
            Pattern("Two5", "X.X..", 80, PatternCategory.DEVELOPING),
            Pattern("Two6", "..X.X", 80, PatternCategory.DEVELOPING),
            Pattern("Two7", ".X.X.", 80, PatternCategory.DEVELOPING),
            Pattern("Two8", "X...X", 50, PatternCategory.DEVELOPING),
            
            // 방어 패턴 (상대방 패턴)
            Pattern("BlockFive", "OOOO*", 900000, PatternCategory.DEFENSIVE),
            Pattern("BlockFive2", "*OOOO", 900000, PatternCategory.DEFENSIVE),
            Pattern("BlockFive3", "OOO*O", 900000, PatternCategory.DEFENSIVE),
            Pattern("BlockFive4", "O*OOO", 900000, PatternCategory.DEFENSIVE),
            Pattern("BlockFive5", "OO*OO", 900000, PatternCategory.DEFENSIVE),
            
            Pattern("BlockOpenFour", ".OOO*.", 45000, PatternCategory.DEFENSIVE),
            Pattern("BlockOpenFour2", ".*OOO.", 45000, PatternCategory.DEFENSIVE),
            
            Pattern("BlockFour", "OOO*.", 9000, PatternCategory.DEFENSIVE),
            Pattern("BlockFour2", ".OOO*", 9000, PatternCategory.DEFENSIVE),
            Pattern("BlockFour3", "*OOO.", 9000, PatternCategory.DEFENSIVE),
            Pattern("BlockFour4", ".*OOO", 9000, PatternCategory.DEFENSIVE),
            
            // 금수 패턴 (렌주룰)
            Pattern("DoubleFour", "XXXX.+.XXXX", -100000, PatternCategory.FORBIDDEN),
            Pattern("DoubleThree", ".XXX.+.XXX.", -100000, PatternCategory.FORBIDDEN),
            Pattern("Overline", "XXXXXX", -100000, PatternCategory.FORBIDDEN)
        )
        
        // 패턴을 점수 기준으로 정렬 (높은 점수 우선)
        val SORTED_PATTERNS = PATTERNS.sortedByDescending { it.score }
    }
}

/**
 * 패턴 카테고리
 */
enum class PatternCategory {
    WINNING,      // 승리 패턴
    CRITICAL,     // 치명적 위협
    FORCING,      // 강제 수
    THREATENING,  // 위협
    BUILDING,     // 구축
    DEVELOPING,   // 전개
    DEFENSIVE,    // 방어
    FORBIDDEN     // 금수
}

/**
 * 패턴 매칭 결과
 */
data class PatternMatch(
    val pattern: Pattern,
    val position: com.omok.domain.model.Position,
    val direction: Direction,
    val score: Int
)

/**
 * 방향 정의
 */
enum class Direction(val dx: Int, val dy: Int) {
    HORIZONTAL(0, 1),      // 가로 →
    VERTICAL(1, 0),        // 세로 ↓
    DIAGONAL_DOWN(1, 1),   // 대각선 ↘
    DIAGONAL_UP(1, -1)     // 대각선 ↗
}

/**
 * 패턴 인식기
 */
class PatternRecognizer {
    
    /**
     * 보드에서 특정 위치에 돌을 놓았을 때의 패턴 점수 계산
     */
    fun evaluatePosition(
        board: com.omok.domain.model.Board,
        position: com.omok.domain.model.Position,
        player: Player
    ): Int {
        var totalScore = 0
        
        // 모든 방향에서 패턴 검사
        Direction.values().forEach { direction ->
            val patternString = extractPatternString(board, position, player, direction)
            
            // 모든 패턴과 매칭
            Pattern.SORTED_PATTERNS.forEach { pattern ->
                if (matchesPattern(patternString, pattern.pattern)) {
                    totalScore += pattern.score
                    
                    // 중요 패턴은 추가 가중치
                    if (pattern.category == PatternCategory.CRITICAL || 
                        pattern.category == PatternCategory.WINNING) {
                        totalScore += pattern.score / 10
                    }
                }
            }
        }
        
        // 중앙 보너스
        val centerDistance = kotlin.math.abs(position.row - 7) + kotlin.math.abs(position.col - 7)
        totalScore += (14 - centerDistance) * 10
        
        return totalScore
    }
    
    /**
     * 보드 전체의 패턴 인식
     */
    fun findAllPatterns(
        board: com.omok.domain.model.Board,
        player: Player
    ): List<PatternMatch> {
        val matches = mutableListOf<PatternMatch>()
        
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val position = com.omok.domain.model.Position(row, col)
                
                if (board.isEmpty(position)) {
                    Direction.values().forEach { direction ->
                        val patternString = extractPatternString(board, position, player, direction)
                        
                        Pattern.SORTED_PATTERNS.forEach { pattern ->
                            if (matchesPattern(patternString, pattern.pattern)) {
                                matches.add(
                                    PatternMatch(
                                        pattern = pattern,
                                        position = position,
                                        direction = direction,
                                        score = pattern.score
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        return matches.sortedByDescending { it.score }
    }
    
    /**
     * 특정 방향으로 패턴 문자열 추출
     */
    private fun extractPatternString(
        board: com.omok.domain.model.Board,
        position: com.omok.domain.model.Position,
        player: Player,
        direction: Direction
    ): String {
        val pattern = StringBuilder()
        
        // 중심 위치에서 양방향으로 5칸씩 확인
        for (i in -5..5) {
            val checkRow = position.row + direction.dx * i
            val checkCol = position.col + direction.dy * i
            val checkPos = com.omok.domain.model.Position(checkRow, checkCol)
            
            when {
                i == 0 -> pattern.append('*') // 평가 위치
                !checkPos.isValid() -> pattern.append('#') // 벽
                board.isEmpty(checkPos) -> pattern.append('.') // 빈 공간
                board.getStone(checkPos) == player -> pattern.append('X') // 자기 돌
                else -> pattern.append('O') // 상대 돌
            }
        }
        
        return pattern.toString()
    }
    
    /**
     * 패턴 매칭
     */
    private fun matchesPattern(boardPattern: String, targetPattern: String): Boolean {
        // targetPattern이 boardPattern의 부분 문자열인지 확인
        if (targetPattern.contains('+')) {
            // 복합 패턴 처리 (예: 쌍삼, 쌍사)
            val parts = targetPattern.split('+')
            return parts.all { part -> boardPattern.contains(part) }
        }
        
        // 패턴에 *가 포함된 경우 특별 처리
        if (targetPattern.contains('*')) {
            val index = boardPattern.indexOf('*')
            if (index == -1) return false
            
            // *를 중심으로 패턴 매칭
            val patternIndex = targetPattern.indexOf('*')
            if (patternIndex == -1) return false
            
            val startBoard = index - patternIndex
            if (startBoard < 0 || startBoard + targetPattern.length > boardPattern.length) {
                return false
            }
            
            for (i in targetPattern.indices) {
                val boardChar = boardPattern[startBoard + i]
                val patternChar = targetPattern[i]
                
                if (patternChar != '*' && boardChar != patternChar) {
                    return false
                }
            }
            return true
        }
        
        return boardPattern.contains(targetPattern)
    }
    
    /**
     * 위협 수준 평가
     */
    fun evaluateThreatLevel(
        board: com.omok.domain.model.Board,
        player: Player
    ): ThreatLevel {
        val patterns = findAllPatterns(board, player)
        
        val winningPatterns = patterns.count { it.pattern.category == PatternCategory.WINNING }
        val criticalPatterns = patterns.count { it.pattern.category == PatternCategory.CRITICAL }
        val forcingPatterns = patterns.count { it.pattern.category == PatternCategory.FORCING }
        
        return when {
            winningPatterns > 0 -> ThreatLevel.IMMEDIATE_WIN
            criticalPatterns >= 2 -> ThreatLevel.DOUBLE_THREAT
            criticalPatterns > 0 -> ThreatLevel.SINGLE_THREAT
            forcingPatterns >= 2 -> ThreatLevel.FORCING_SEQUENCE
            forcingPatterns > 0 -> ThreatLevel.MINOR_THREAT
            else -> ThreatLevel.NO_THREAT
        }
    }
}

/**
 * 위협 수준
 */
enum class ThreatLevel {
    IMMEDIATE_WIN,     // 즉시 승리
    DOUBLE_THREAT,     // 이중 위협
    SINGLE_THREAT,     // 단일 위협
    FORCING_SEQUENCE,  // 강제 수순
    MINOR_THREAT,      // 작은 위협
    NO_THREAT         // 위협 없음
}