package com.omok.infrastructure.puzzle

import com.omok.domain.model.*
import com.omok.domain.puzzle.*
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 메모리 기반 퍼즐 리포지토리 구현
 */
class InMemoryPuzzleRepository : PuzzleRepository {
    
    private val puzzles = mutableListOf<OmokPuzzle>()
    private val completionRecords = ConcurrentHashMap<String, MutableList<PuzzleCompletionRecord>>()
    
    init {
        // 초기 퍼즐 데이터 로드
        loadDefaultPuzzles()
    }
    
    override fun getAllPuzzles(): List<OmokPuzzle> = puzzles.toList()
    
    override fun getPuzzleById(id: String): OmokPuzzle? = puzzles.find { it.id == id }
    
    override fun getPuzzlesByCategory(category: PuzzleCategory): List<OmokPuzzle> =
        puzzles.filter { it.category == category }
    
    override fun getPuzzlesByDifficulty(difficulty: PuzzleDifficulty): List<OmokPuzzle> =
        puzzles.filter { it.difficulty == difficulty }
    
    override fun recordCompletion(puzzleId: String, progress: PuzzleProgress) {
        val record = PuzzleCompletionRecord(
            puzzleId = puzzleId,
            completedAt = LocalDateTime.now(),
            success = progress.success,
            attempts = progress.attempts,
            hintsUsed = progress.hintsUsed,
            solvingTime = progress.getSolvedTime()
        )
        
        completionRecords.computeIfAbsent(puzzleId) { mutableListOf() }.add(record)
    }
    
    override fun getCompletionRecords(): List<PuzzleCompletionRecord> =
        completionRecords.values.flatten()
    
    override fun getRecommendedPuzzles(limit: Int): List<OmokPuzzle> {
        // 완료하지 않은 퍼즐 우선 추천
        val completedPuzzleIds = completionRecords.keys
        val uncompletedPuzzles = puzzles.filter { it.id !in completedPuzzleIds }
        
        return if (uncompletedPuzzles.size >= limit) {
            uncompletedPuzzles.take(limit)
        } else {
            // 부족하면 실패한 퍼즐 추가
            val failedPuzzleIds = completionRecords.entries
                .filter { entry -> entry.value.none { it.success } }
                .map { it.key }
            
            val failedPuzzles = puzzles.filter { it.id in failedPuzzleIds }
            (uncompletedPuzzles + failedPuzzles).distinct().take(limit)
        }
    }
    
    private fun loadDefaultPuzzles() {
        // 초급 퍼즐들
        puzzles.addAll(createBeginnerPuzzles())
        
        // 중급 퍼즐들
        puzzles.addAll(createIntermediatePuzzles())
        
        // 상급 퍼즐들
        puzzles.addAll(createAdvancedPuzzles())
    }
    
    private fun createBeginnerPuzzles(): List<OmokPuzzle> {
        val puzzles = mutableListOf<OmokPuzzle>()
        
        // 퍼즐 1: 1수 승리 - 가로
        puzzles.add(
            OmokPuzzle(
                id = "beginner-1",
                title = "1수 승리 - 가로",
                description = "흑이 한 수만에 승리하는 위치를 찾으세요.",
                difficulty = PuzzleDifficulty.BEGINNER,
                category = PuzzleCategory.ENDGAME,
                objective = PuzzleObjective.WIN_IN_1,
                initialBoard = createBoard(listOf(
                    "B:7,4", "B:7,5", "B:7,6", "B:7,7",  // 흑 4개 연속
                    "W:6,5", "W:6,6", "W:8,5"             // 백 방해돌
                )),
                currentPlayer = Player.BLACK,
                solutions = listOf(
                    PuzzleSolution(
                        mainLine = listOf(Position(7, 8)),  // 정답: (7,8)에 두면 5목 완성
                        variations = emptyList(),
                        explanation = "가로로 4개가 연속되어 있으므로 오른쪽 끝에 두면 5목이 완성됩니다."
                    )
                ),
                hints = listOf(
                    "가로로 연속된 돌들을 확인하세요.",
                    "4개가 연속된 곳의 양 끝을 확인하세요.",
                    "(7,8) 위치를 확인해보세요."
                )
            )
        )
        
        // 퍼즐 2: 1수 승리 - 세로
        puzzles.add(
            OmokPuzzle(
                id = "beginner-2",
                title = "1수 승리 - 세로",
                description = "백이 한 수만에 승리하는 위치를 찾으세요.",
                difficulty = PuzzleDifficulty.BEGINNER,
                category = PuzzleCategory.ENDGAME,
                objective = PuzzleObjective.WIN_IN_1,
                initialBoard = createBoard(listOf(
                    "W:3,7", "W:4,7", "W:5,7", "W:6,7",  // 백 4개 연속
                    "B:3,6", "B:4,6", "B:5,8"             // 흑 방해돌
                )),
                currentPlayer = Player.WHITE,
                solutions = listOf(
                    PuzzleSolution(
                        mainLine = listOf(Position(7, 7)),  // 정답: (7,7)에 두면 5목 완성
                        variations = emptyList(),
                        explanation = "세로로 4개가 연속되어 있으므로 아래쪽에 두면 5목이 완성됩니다."
                    )
                ),
                hints = listOf(
                    "세로로 연속된 돌들을 확인하세요.",
                    "4개가 연속된 곳의 위아래를 확인하세요.",
                    "(7,7) 위치를 확인해보세요."
                )
            )
        )
        
        // 퍼즐 3: 열린 4 만들기
        puzzles.add(
            OmokPuzzle(
                id = "beginner-3",
                title = "열린 4 만들기",
                description = "흑이 열린 4를 만들어 다음 수에 승리를 확정하세요.",
                difficulty = PuzzleDifficulty.BEGINNER,
                category = PuzzleCategory.TACTICS,
                objective = PuzzleObjective.FIND_BEST_MOVE,
                initialBoard = createBoard(listOf(
                    "B:7,5", "B:7,6", "B:7,7",           // 흑 3개 연속
                    "W:6,6", "W:8,6", "W:7,9"            // 백 방해돌
                )),
                currentPlayer = Player.BLACK,
                solutions = listOf(
                    PuzzleSolution(
                        mainLine = listOf(Position(7, 4)),  // 정답: (7,4)에 두면 열린 4
                        variations = emptyList(),
                        explanation = "왼쪽에 두면 양쪽이 열린 4가 되어 다음 수에 승리가 확정됩니다."
                    )
                ),
                hints = listOf(
                    "3개가 연속된 돌의 양쪽을 확인하세요.",
                    "어느 쪽에 두면 양쪽이 모두 열릴까요?",
                    "왼쪽 (7,4) 위치를 확인해보세요."
                )
            )
        )
        
        return puzzles
    }
    
    private fun createIntermediatePuzzles(): List<OmokPuzzle> {
        val puzzles = mutableListOf<OmokPuzzle>()
        
        // 퍼즐 1: 3-3 만들기
        puzzles.add(
            OmokPuzzle(
                id = "intermediate-1",
                title = "쌍삼 만들기",
                description = "흑이 쌍삼(3-3)을 만들어 승리를 확정하세요.",
                difficulty = PuzzleDifficulty.INTERMEDIATE,
                category = PuzzleCategory.TACTICS,
                objective = PuzzleObjective.WIN_IN_3,
                initialBoard = createBoard(listOf(
                    "B:7,6", "B:7,7",                    // 가로 2개
                    "B:5,7", "B:6,7",                    // 세로 2개
                    "W:7,5", "W:8,7", "W:6,8", "W:4,7"   // 백 방해돌
                )),
                currentPlayer = Player.BLACK,
                solutions = listOf(
                    PuzzleSolution(
                        mainLine = listOf(Position(7, 8)),  // 정답: (7,8)에 두면 가로 3 완성
                        variations = emptyList(),
                        explanation = "이 위치에 두면 가로와 세로로 동시에 열린 3이 만들어져 쌍삼이 됩니다."
                    )
                ),
                hints = listOf(
                    "두 방향으로 3을 만들 수 있는 위치를 찾으세요.",
                    "가로와 세로가 만나는 지점을 확인하세요.",
                    "(7,8) 위치가 핵심입니다."
                )
            )
        )
        
        // 퍼즐 2: 4-3 만들기
        puzzles.add(
            OmokPuzzle(
                id = "intermediate-2",
                title = "4-3 조합",
                description = "백이 4와 열린 3을 동시에 만들어 승리하세요.",
                difficulty = PuzzleDifficulty.INTERMEDIATE,
                category = PuzzleCategory.TACTICS,
                objective = PuzzleObjective.WIN_IN_3,
                initialBoard = createBoard(listOf(
                    "W:7,4", "W:7,5", "W:7,6",           // 백 가로 3개
                    "W:5,6", "W:6,6",                    // 백 세로 2개
                    "B:7,3", "B:8,6", "B:4,6", "B:6,7"   // 흑 방해돌
                )),
                currentPlayer = Player.WHITE,
                solutions = listOf(
                    PuzzleSolution(
                        mainLine = listOf(Position(7, 7)),  // 정답
                        variations = emptyList(),
                        explanation = "이 위치에 두면 가로로 4가 되고 세로로 열린 3이 되어 다음 수에 승리합니다."
                    )
                ),
                hints = listOf(
                    "4를 만들면서 동시에 다른 위협을 만들 수 있는 위치를 찾으세요.",
                    "가로와 세로를 동시에 활용하세요.",
                    "교차점이 핵심입니다."
                )
            )
        )
        
        return puzzles
    }
    
    private fun createAdvancedPuzzles(): List<OmokPuzzle> {
        val puzzles = mutableListOf<OmokPuzzle>()
        
        // 퍼즐 1: 복잡한 연계
        puzzles.add(
            OmokPuzzle(
                id = "advanced-1",
                title = "5수 내 승리",
                description = "흑이 정확한 수순으로 5수 내에 승리하세요.",
                difficulty = PuzzleDifficulty.ADVANCED,
                category = PuzzleCategory.COMBINATION,
                objective = PuzzleObjective.WIN_IN_5,
                initialBoard = createBoard(listOf(
                    "B:6,6", "B:7,7", "B:8,8",           // 흑 대각선
                    "B:7,5", "B:7,6",                    // 흑 가로
                    "W:7,4", "W:7,8", "W:9,9", "W:5,5",  // 백 방어
                    "W:6,7", "W:8,7"                     // 백 추가 방어
                )),
                currentPlayer = Player.BLACK,
                solutions = listOf(
                    PuzzleSolution(
                        mainLine = listOf(
                            Position(7, 9),  // 첫 수: 가로 위협
                            Position(9, 7),  // 둘째 수: 새로운 위협
                            Position(8, 6)   // 셋째 수: 승리
                        ),
                        variations = emptyList(),
                        explanation = "여러 방향의 위협을 연계하여 상대방이 모두 막을 수 없게 만드는 수순입니다."
                    )
                ),
                hints = listOf(
                    "한 방향만으로는 승리할 수 없습니다.",
                    "여러 위협을 동시에 만들어야 합니다.",
                    "첫 수는 즉각적인 위협을 만드는 것이 중요합니다."
                )
            )
        )
        
        return puzzles
    }
    
    private fun createBoard(positions: List<String>): Board {
        var board = Board()
        
        for (pos in positions) {
            val parts = pos.split(":")
            val player = if (parts[0] == "B") Player.BLACK else Player.WHITE
            val coords = parts[1].split(",")
            val row = coords[0].toInt()
            val col = coords[1].toInt()
            
            board = board.placeStone(Move(Position(row, col), player))
        }
        
        return board
    }
}