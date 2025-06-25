package com.omok.domain.model

enum class Player {
    BLACK, WHITE;
    
    fun opponent(): Player = if (this == BLACK) WHITE else BLACK
    
    companion object {
        fun first(): Player = BLACK
    }
}