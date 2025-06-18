package com.omok

data class Position(val row: Int, val col: Int)

enum class Stone {
    EMPTY, BLACK, WHITE
}

class RenjuRule {
    companion object {
        const val BOARD_SIZE = 15
        
        fun isValidMove(board: Array<Array<Stone>>, row: Int, col: Int, player: Stone): Boolean {
            if (board[row][col] != Stone.EMPTY) return false
            
            if (player == Stone.BLACK) {
                board[row][col] = Stone.BLACK
                
                val isDoubleThree = checkDoubleThree(board, row, col, Stone.BLACK)
                val isDoubleFour = checkDoubleFour(board, row, col, Stone.BLACK)
                val isOverline = checkOverline(board, row, col, Stone.BLACK)
                
                board[row][col] = Stone.EMPTY
                
                return !isDoubleThree && !isDoubleFour && !isOverline
            }
            
            return true
        }
        
        private fun checkDoubleThree(board: Array<Array<Stone>>, row: Int, col: Int, player: Stone): Boolean {
            var openThreeCount = 0
            
            val directions = arrayOf(
                intArrayOf(0, 1),   // 가로
                intArrayOf(1, 0),   // 세로
                intArrayOf(1, 1),   // 대각선 \
                intArrayOf(1, -1)   // 대각선 /
            )
            
            for (dir in directions) {
                if (isOpenThree(board, row, col, dir[0], dir[1], player)) {
                    openThreeCount++
                }
            }
            
            return openThreeCount >= 2
        }
        
        private fun isOpenThree(board: Array<Array<Stone>>, row: Int, col: Int, dr: Int, dc: Int, player: Stone): Boolean {
            val line = getLine(board, row, col, dr, dc, player)
            val pattern = line.joinToString("")
            
            val openThreePatterns = listOf(
                "0XXX0",
                "0X0XX0", "0XX0X0",
                "00XXX0", "0XXX00",
                "0X00XX0", "0XX00X0", "0X0X0X0"
            )
            
            for (p in openThreePatterns) {
                if (pattern.contains(p)) return true
            }
            
            return false
        }
        
        private fun checkDoubleFour(board: Array<Array<Stone>>, row: Int, col: Int, player: Stone): Boolean {
            var fourCount = 0
            
            val directions = arrayOf(
                intArrayOf(0, 1),
                intArrayOf(1, 0),
                intArrayOf(1, 1),
                intArrayOf(1, -1)
            )
            
            for (dir in directions) {
                if (isFour(board, row, col, dir[0], dir[1], player)) {
                    fourCount++
                }
            }
            
            return fourCount >= 2
        }
        
        private fun isFour(board: Array<Array<Stone>>, row: Int, col: Int, dr: Int, dc: Int, player: Stone): Boolean {
            val count = countConsecutive(board, row, col, dr, dc, player) + 
                       countConsecutive(board, row, col, -dr, -dc, player) + 1
            
            if (count == 4) {
                val r1 = row + dr * countConsecutive(board, row, col, dr, dc, player) + dr
                val c1 = col + dc * countConsecutive(board, row, col, dr, dc, player) + dc
                val r2 = row - dr * countConsecutive(board, row, col, -dr, -dc, player) - dr
                val c2 = col - dc * countConsecutive(board, row, col, -dr, -dc, player) - dc
                
                val openEnds = (isValidPos(r1, c1) && board[r1][c1] == Stone.EMPTY).toInt() +
                              (isValidPos(r2, c2) && board[r2][c2] == Stone.EMPTY).toInt()
                
                return openEnds > 0
            }
            
            return false
        }
        
        private fun checkOverline(board: Array<Array<Stone>>, row: Int, col: Int, player: Stone): Boolean {
            val directions = arrayOf(
                intArrayOf(0, 1),
                intArrayOf(1, 0),
                intArrayOf(1, 1),
                intArrayOf(1, -1)
            )
            
            for (dir in directions) {
                val count = countConsecutive(board, row, col, dir[0], dir[1], player) + 
                           countConsecutive(board, row, col, -dir[0], -dir[1], player) + 1
                if (count > 5) return true
            }
            
            return false
        }
        
        private fun countConsecutive(board: Array<Array<Stone>>, row: Int, col: Int, dr: Int, dc: Int, player: Stone): Int {
            var count = 0
            var r = row + dr
            var c = col + dc
            
            while (isValidPos(r, c) && board[r][c] == player) {
                count++
                r += dr
                c += dc
            }
            
            return count
        }
        
        private fun getLine(board: Array<Array<Stone>>, row: Int, col: Int, dr: Int, dc: Int, player: Stone): List<Char> {
            val line = mutableListOf<Char>()
            
            for (i in -5..5) {
                val r = row + dr * i
                val c = col + dc * i
                
                when {
                    !isValidPos(r, c) -> line.add('B')
                    board[r][c] == Stone.EMPTY -> line.add('0')
                    board[r][c] == player -> line.add('X')
                    else -> line.add('B')
                }
            }
            
            return line
        }
        
        private fun isValidPos(row: Int, col: Int): Boolean {
            return row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE
        }
        
        private fun Boolean.toInt() = if (this) 1 else 0
        
        fun checkWin(board: Array<Array<Stone>>, row: Int, col: Int, player: Stone): Boolean {
            val directions = arrayOf(
                intArrayOf(0, 1),
                intArrayOf(1, 0),
                intArrayOf(1, 1),
                intArrayOf(1, -1)
            )
            
            for (dir in directions) {
                val count = countConsecutive(board, row, col, dir[0], dir[1], player) + 
                           countConsecutive(board, row, col, -dir[0], -dir[1], player) + 1
                
                if (player == Stone.BLACK && count == 5) return true
                if (player == Stone.WHITE && count >= 5) return true
            }
            
            return false
        }
    }
}