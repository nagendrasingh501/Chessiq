package com.example.chessiq.model

data class Move(
    val from: Position,
    val to: Position,
    val promotionType: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    val isDoublePawnPush: Boolean = false
) {
    fun toAlgebraic(): String {
        val promStr = if (promotionType != null) "=" + promotionType.symbol else ""
        return "${from.toAlgebraic()}${to.toAlgebraic()}$promStr"
    }
}
