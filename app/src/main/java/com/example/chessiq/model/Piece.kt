package com.example.chessiq.model

enum class Color {
    WHITE, BLACK;

    fun opponent(): Color = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: String, val value: Int) {
    PAWN("P", 100),
    KNIGHT("N", 320),
    BISHOP("B", 330),
    ROOK("R", 500),
    QUEEN("Q", 900),
    KING("K", 20000)
}

data class Piece(val type: PieceType, val color: Color)
