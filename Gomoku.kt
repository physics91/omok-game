import java.util.Scanner

class Gomoku {
    companion object {
        const val BOARD_SIZE = 15
        const val EMPTY = ' '
        const val BLACK = '●'
        const val WHITE = '○'
    }

    private val board = Array(BOARD_SIZE) { CharArray(BOARD_SIZE) { EMPTY } }
    private var currentPlayer = BLACK
    private val scanner = Scanner(System.`in`)

    fun play() {
        println("오목 게임을 시작합니다!")
        println("좌표는 1~$BOARD_SIZE 사이의 숫자로 입력하세요. (예: 8 8)")
        println("흑돌: $BLACK, 백돌: $WHITE")
        println()

        while (true) {
            displayBoard()
            val currentPlayerName = if (currentPlayer == BLACK) "흑돌" else "백돌"
            println("\n$currentPlayerName($currentPlayer) 차례입니다.")
            
            val (row, col) = getPlayerMove()
            
            if (board[row][col] != EMPTY) {
                println("이미 돌이 놓인 위치입니다. 다른 위치를 선택하세요.")
                continue
            }
            
            board[row][col] = currentPlayer
            
            if (checkWin(row, col)) {
                displayBoard()
                println("\n$currentPlayerName($currentPlayer)이 승리했습니다!")
                break
            }
            
            if (isBoardFull()) {
                displayBoard()
                println("\n무승부입니다!")
                break
            }
            
            currentPlayer = if (currentPlayer == BLACK) WHITE else BLACK
        }
    }

    private fun displayBoard() {
        print("   ")
        for (i in 1..BOARD_SIZE) {
            print(String.format("%2d ", i))
        }
        println()
        
        for (i in 0 until BOARD_SIZE) {
            print(String.format("%2d ", i + 1))
            for (j in 0 until BOARD_SIZE) {
                print(" ${board[i][j]} ")
            }
            println()
        }
    }

    private fun getPlayerMove(): Pair<Int, Int> {
        while (true) {
            print("좌표 입력 (행 열): ")
            try {
                val input = scanner.nextLine().trim().split(" ")
                if (input.size != 2) {
                    println("잘못된 입력입니다. 두 개의 숫자를 공백으로 구분하여 입력하세요.")
                    continue
                }
                
                val row = input[0].toInt() - 1
                val col = input[1].toInt() - 1
                
                if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
                    return Pair(row, col)
                } else {
                    println("좌표는 1~$BOARD_SIZE 사이여야 합니다.")
                }
            } catch (e: Exception) {
                println("잘못된 입력입니다. 숫자를 입력하세요.")
            }
        }
    }

    private fun checkWin(row: Int, col: Int): Boolean {
        val player = board[row][col]
        
        // 가로 체크
        if (countConsecutive(row, col, 0, 1, player) + countConsecutive(row, col, 0, -1, player) + 1 >= 5) {
            return true
        }
        
        // 세로 체크
        if (countConsecutive(row, col, 1, 0, player) + countConsecutive(row, col, -1, 0, player) + 1 >= 5) {
            return true
        }
        
        // 대각선 (↘) 체크
        if (countConsecutive(row, col, 1, 1, player) + countConsecutive(row, col, -1, -1, player) + 1 >= 5) {
            return true
        }
        
        // 대각선 (↙) 체크
        if (countConsecutive(row, col, 1, -1, player) + countConsecutive(row, col, -1, 1, player) + 1 >= 5) {
            return true
        }
        
        return false
    }

    private fun countConsecutive(row: Int, col: Int, dr: Int, dc: Int, player: Char): Int {
        var count = 0
        var r = row + dr
        var c = col + dc
        
        while (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE && board[r][c] == player) {
            count++
            r += dr
            c += dc
        }
        
        return count
    }

    private fun isBoardFull(): Boolean {
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                if (board[i][j] == EMPTY) {
                    return false
                }
            }
        }
        return true
    }
}

fun main() {
    val game = Gomoku()
    game.play()
}