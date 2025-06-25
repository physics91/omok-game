package com.omok.domain.service.rule

import com.omok.domain.model.*
import com.omok.domain.service.RuleValidator

/**
 * 게임 규칙에 따른 RuleValidator 생성 팩토리
 */
object RuleValidatorFactory {
    
    fun create(rule: GameRule): RuleValidator {
        return when (rule) {
            GameRule.STANDARD_RENJU -> StandardRenjuValidator()
            GameRule.OPEN_RENJU -> OpenRenjuValidator()
            GameRule.YAMAGUCHI_RULE -> YamaguchiRuleValidator()
            GameRule.SWAP_RULE -> SwapRuleValidator()
            GameRule.SWAP2_RULE -> Swap2RuleValidator()
            GameRule.SOOSYRV_RULE -> SoosyrvRuleValidator()
            GameRule.TARAGUCHI_RULE -> TaraguchiRuleValidator()
            GameRule.FREESTYLE -> FreestyleValidator()
            GameRule.CARO_RULE -> CaroRuleValidator()
        }
    }
}

/**
 * 표준 렌주룰 검증기
 */
class StandardRenjuValidator : RuleValidator() {
    // 기존 RuleValidator와 동일
}

/**
 * 오픈 렌주룰 검증기
 */
class OpenRenjuValidator : RuleValidator() {
    
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        val moveCount = board.getMoveCount()
        
        // 첫 수는 반드시 중앙(천원)
        if (moveCount == 0) {
            return position == Position.center()
        }
        
        // 둘째 수(백)는 천원 주변 3×3 범위
        if (moveCount == 1) {
            val center = Position.center()
            val distance = maxOf(
                kotlin.math.abs(position.row - center.row),
                kotlin.math.abs(position.col - center.col)
            )
            return distance <= 1
        }
        
        // 셋째 수(흑)는 중앙 5×5 범위
        if (moveCount == 2) {
            val center = Position.center()
            val distance = maxOf(
                kotlin.math.abs(position.row - center.row),
                kotlin.math.abs(position.col - center.col)
            )
            return distance <= 2
        }
        
        // 이후는 표준 렌주룰
        return super.isValidMove(board, position, player)
    }
}

/**
 * 야마구치룰 검증기
 */
class YamaguchiRuleValidator : RuleValidator() {
    
    private var isSwapped = false
    
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        val moveCount = board.getMoveCount()
        
        // 첫 3수는 제한 없음
        if (moveCount < 3) {
            return board.isEmpty(position)
        }
        
        // 3수 이후 백이 스왑 결정
        if (moveCount == 3 && !isSwapped) {
            // 스왑 로직은 게임 컨트롤러에서 처리
            return true
        }
        
        // 이후는 표준 렌주룰
        return super.isValidMove(board, position, player)
    }
    
    fun setSwapped(swapped: Boolean) {
        isSwapped = swapped
    }
}

/**
 * 스왑룰 검증기
 */
class SwapRuleValidator : RuleValidator() {
    
    private var isSwapped = false
    
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        val moveCount = board.getMoveCount()
        
        // 첫 수는 제한 없음
        if (moveCount == 0) {
            return board.isEmpty(position)
        }
        
        // 첫 수 이후 백이 스왑 가능
        if (moveCount == 1 && !isSwapped) {
            // 스왑 로직은 게임 컨트롤러에서 처리
            return true
        }
        
        // 이후는 표준 렌주룰
        return super.isValidMove(board, position, player)
    }
    
    fun setSwapped(swapped: Boolean) {
        isSwapped = swapped
    }
}

/**
 * 스왑2룰 검증기
 */
class Swap2RuleValidator : RuleValidator() {
    
    enum class Phase {
        OPENING,      // 흑이 첫 3수를 둠
        SELECTION,    // 백이 선택 (스왑 or 백 두기)
        NORMAL        // 일반 게임
    }
    
    private var phase = Phase.OPENING
    
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        val moveCount = board.getMoveCount()
        
        when (phase) {
            Phase.OPENING -> {
                // 첫 3수는 제한 없음
                if (moveCount < 3) {
                    return board.isEmpty(position)
                }
                if (moveCount == 3) {
                    phase = Phase.SELECTION
                }
            }
            Phase.SELECTION -> {
                // 백의 선택 단계
                phase = Phase.NORMAL
                return true
            }
            Phase.NORMAL -> {
                // 표준 렌주룰 적용
                return super.isValidMove(board, position, player)
            }
        }
        
        return true
    }
}

/**
 * 수시르브룰 검증기
 */
class SoosyrvRuleValidator : RuleValidator() {
    
    // 8가지 표준 오프닝 패턴
    private val openings = listOf(
        listOf(Position(7,7), Position(7,8), Position(8,8)),    // D1
        listOf(Position(7,7), Position(7,8), Position(6,8)),    // D2
        listOf(Position(7,7), Position(7,8), Position(8,7)),    // D3
        listOf(Position(7,7), Position(7,8), Position(6,7)),    // D4
        listOf(Position(7,7), Position(8,8), Position(6,6)),    // I1
        listOf(Position(7,7), Position(8,8), Position(6,8)),    // I2
        listOf(Position(7,7), Position(8,8), Position(8,6)),    // I3
        listOf(Position(7,7), Position(8,8), Position(6,9))     // I4
    )
    
    private var selectedOpening: List<Position>? = null
    
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        val moveCount = board.getMoveCount()
        
        // 첫 3수는 정해진 오프닝 중 하나
        if (moveCount < 3) {
            if (selectedOpening == null) {
                // 첫 수로 오프닝 결정
                for (opening in openings) {
                    if (opening[0] == position) {
                        selectedOpening = opening
                        return true
                    }
                }
                return false
            } else {
                // 선택된 오프닝에 따라 검증
                return selectedOpening!![moveCount] == position
            }
        }
        
        // 이후는 표준 렌주룰
        return super.isValidMove(board, position, player)
    }
}

/**
 * 타라구치룰 검증기
 */
class TaraguchiRuleValidator : RuleValidator() {
    
    // 10가지 표준 오프닝 패턴
    private val openings = listOf(
        // 직접 오프닝 (Direct)
        listOf(Position(7,7), Position(8,7), Position(7,8)),
        listOf(Position(7,7), Position(8,7), Position(7,6)),
        listOf(Position(7,7), Position(6,7), Position(7,8)),
        listOf(Position(7,7), Position(6,7), Position(7,6)),
        // 간접 오프닝 (Indirect)
        listOf(Position(7,7), Position(8,8), Position(6,8)),
        listOf(Position(7,7), Position(8,8), Position(8,6)),
        listOf(Position(7,7), Position(6,8), Position(8,8)),
        listOf(Position(7,7), Position(6,8), Position(8,6)),
        listOf(Position(7,7), Position(8,6), Position(6,8)),
        listOf(Position(7,7), Position(8,6), Position(8,8))
    )
    
    private var selectedOpening: List<Position>? = null
    
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        val moveCount = board.getMoveCount()
        
        // 첫 3수는 정해진 오프닝
        if (moveCount < 3) {
            if (selectedOpening == null) {
                for (opening in openings) {
                    if (opening[0] == position) {
                        selectedOpening = opening
                        return true
                    }
                }
                return false
            } else {
                return selectedOpening!![moveCount] == position
            }
        }
        
        // 4수(백)는 자유
        if (moveCount == 3) {
            return board.isEmpty(position)
        }
        
        // 5수(흑)는 백이 제안한 위치들 중 선택
        if (moveCount == 4) {
            // 이 부분은 게임 컨트롤러에서 처리
            return true
        }
        
        // 이후는 표준 렌주룰
        return super.isValidMove(board, position, player)
    }
}

/**
 * 자유룰 검증기 (제한 없음)
 */
class FreestyleValidator : RuleValidator() {
    
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        // 빈 칸이면 모두 가능
        return board.isEmpty(position)
    }
    
    override fun checkWin(board: Board, position: Position, player: Player): GameState {
        // 5개 이상 연속이면 승리
        val directions = arrayOf(
            intArrayOf(0, 1),   // 가로
            intArrayOf(1, 0),   // 세로
            intArrayOf(1, 1),   // 대각선 \
            intArrayOf(1, -1)   // 대각선 /
        )
        
        for (dir in directions) {
            val count = countConsecutive(board, position, dir[0], dir[1], player) +
                       countConsecutive(board, position, -dir[0], -dir[1], player) + 1
            if (count >= 5) {
                val winningLine = getWinningLine(board, position, dir[0], dir[1], player, count)
                return GameState.Won(player, winningLine)
            }
        }
        
        return if (board.isFull()) {
            GameState.Draw
        } else {
            GameState.Playing
        }
    }
}

/**
 * 카로룰 검증기
 */
class CaroRuleValidator : RuleValidator() {
    
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        // 빈 칸이면 모두 가능 (3-3, 4-4 허용)
        return board.isEmpty(position)
    }
    
    override fun checkWin(board: Board, position: Position, player: Player): GameState {
        // 5개 이상 연속이면 승리 (6목 이상도 승리)
        return FreestyleValidator().checkWin(board, position, player)
    }
    
    override fun isDoubleThree(board: Board, position: Position, player: Player): Boolean {
        // 카로룰에서는 3-3 허용
        return false
    }
    
    override fun isDoubleFour(board: Board, position: Position, player: Player): Boolean {
        // 카로룰에서는 4-4 허용
        return false
    }
}