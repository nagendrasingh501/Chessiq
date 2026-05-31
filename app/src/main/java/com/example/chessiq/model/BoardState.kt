package com.example.chessiq.model

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true
) {
    fun update(from: Position, to: Position, piece: Piece?): CastlingRights {
        var wks = whiteKingSide
        var wqs = whiteQueenSide
        var bks = blackKingSide
        var bqs = blackQueenSide

        // White King moves
        if (piece?.type == PieceType.KING && piece.color == Color.WHITE) {
            wks = false
            wqs = false
        }
        // Black King moves
        if (piece?.type == PieceType.KING && piece.color == Color.BLACK) {
            bks = false
            bqs = false
        }

        // White Rooks move or get captured
        if (from == Position(7, 7) || to == Position(7, 7)) wks = false
        if (from == Position(7, 0) || to == Position(7, 0)) wqs = false

        // Black Rooks move or get captured
        if (from == Position(0, 7) || to == Position(0, 7)) bks = false
        if (from == Position(0, 0) || to == Position(0, 0)) bqs = false

        return CastlingRights(wks, wqs, bks, bqs)
    }
}

data class MoveRecord(
    val move: Move,
    val pieceMoved: Piece,
    val pieceCaptured: Piece? = null,
    val prevCastlingRights: CastlingRights,
    val prevEnPassantTarget: Position?,
    val prevHalfmoveClock: Int
)

data class BoardState(
    val board: List<List<Piece?>>,
    val activeColor: Color = Color.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Position? = null,
    val halfmoveClock: Int = 0,
    val fullmoveNumber: Int = 1,
    val moveHistory: List<MoveRecord> = emptyList()
) {
    companion object {
        fun startPosition(): BoardState {
            val board = MutableList(8) { MutableList<Piece?>(8) { null } }

            // Rooks
            board[0][0] = Piece(PieceType.ROOK, Color.BLACK)
            board[0][7] = Piece(PieceType.ROOK, Color.BLACK)
            board[7][0] = Piece(PieceType.ROOK, Color.WHITE)
            board[7][7] = Piece(PieceType.ROOK, Color.WHITE)

            // Knights
            board[0][1] = Piece(PieceType.KNIGHT, Color.BLACK)
            board[0][6] = Piece(PieceType.KNIGHT, Color.BLACK)
            board[7][1] = Piece(PieceType.KNIGHT, Color.WHITE)
            board[7][6] = Piece(PieceType.KNIGHT, Color.WHITE)

            // Bishops
            board[0][2] = Piece(PieceType.BISHOP, Color.BLACK)
            board[0][5] = Piece(PieceType.BISHOP, Color.BLACK)
            board[7][2] = Piece(PieceType.BISHOP, Color.WHITE)
            board[7][5] = Piece(PieceType.BISHOP, Color.WHITE)

            // Queens
            board[0][3] = Piece(PieceType.QUEEN, Color.BLACK)
            board[7][3] = Piece(PieceType.QUEEN, Color.WHITE)

            // Kings
            board[0][4] = Piece(PieceType.KING, Color.BLACK)
            board[7][4] = Piece(PieceType.KING, Color.WHITE)

            // Pawns
            for (col in 0..7) {
                board[1][col] = Piece(PieceType.PAWN, Color.BLACK)
                board[6][col] = Piece(PieceType.PAWN, Color.WHITE)
            }

            return BoardState(
                board = board.map { it.toList() }
            )
        }
    }

    fun getPiece(pos: Position): Piece? {
        if (!pos.isValid()) return null
        return board[pos.row][pos.col]
    }
}
