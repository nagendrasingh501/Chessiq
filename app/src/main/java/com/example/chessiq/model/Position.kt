package com.example.chessiq.model

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7

    fun toAlgebraic(): String {
        val file = ('a' + col).toString()
        val rank = (8 - row).toString()
        return "$file$rank"
    }

    companion object {
        fun fromAlgebraic(s: String): Position? {
            if (s.length != 2) return null
            val fileChar = s[0]
            val rankChar = s[1]
            if (fileChar !in 'a'..'h' || rankChar !in '1'..'8') return null
            val col = fileChar - 'a'
            val row = '8' - rankChar
            return Position(row, col)
        }
    }
}
