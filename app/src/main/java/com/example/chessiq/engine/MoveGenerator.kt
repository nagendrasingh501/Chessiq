package com.example.chessiq.engine

import com.example.chessiq.model.*

object MoveGenerator {

    fun generateLegalMoves(state: BoardState): List<Move> {
        val pseudoMoves = generatePseudoLegalMoves(state)
        return pseudoMoves.filter { move ->
            val nextState = makeMove(state, move)
            // The active player made the move, so in nextState, activeColor is opponent.
            // We check if the player who just moved (state.activeColor) has their king in check.
            !isCheck(nextState, state.activeColor)
        }
    }

    fun makeMove(state: BoardState, move: Move): BoardState {
        val nextBoard = state.board.map { it.toMutableList() }.toMutableList()
        val piece = state.getPiece(move.from) ?: return state

        var capturedPiece = state.getPiece(move.to)

        // Clear source square
        nextBoard[move.from.row][move.from.col] = null

        // Set destination square
        if (move.promotionType != null) {
            nextBoard[move.to.row][move.to.col] = Piece(move.promotionType, piece.color)
        } else {
            nextBoard[move.to.row][move.to.col] = piece
        }

        // Special Move: Castling
        if (move.isCastling) {
            val r = move.to.row
            if (move.to.col == 6) { // King Side
                val rook = nextBoard[r][7]
                nextBoard[r][7] = null
                nextBoard[r][5] = rook
            } else if (move.to.col == 2) { // Queen Side
                val rook = nextBoard[r][0]
                nextBoard[r][0] = null
                nextBoard[r][3] = rook
            }
        }

        // Special Move: En Passant
        if (move.isEnPassant) {
            capturedPiece = Piece(PieceType.PAWN, piece.color.opponent())
            nextBoard[move.from.row][move.to.col] = null
        }

        // Update Castling Rights
        val nextCastlingRights = state.castlingRights.update(move.from, move.to, piece)

        // Update En Passant Target
        val nextEnPassantTarget = if (move.isDoublePawnPush) {
            Position((move.from.row + move.to.row) / 2, move.from.col)
        } else {
            null
        }

        // Update Halfmove Clock (reset on pawn move or capture)
        val isCapture = capturedPiece != null || move.isEnPassant
        val nextHalfmoveClock = if (piece.type == PieceType.PAWN || isCapture) {
            0
        } else {
            state.halfmoveClock + 1
        }

        // Update Fullmove Number
        val nextFullmoveNumber = if (state.activeColor == Color.BLACK) {
            state.fullmoveNumber + 1
        } else {
            state.fullmoveNumber
        }

        // Update History
        val record = MoveRecord(
            move = move,
            pieceMoved = piece,
            pieceCaptured = capturedPiece,
            prevCastlingRights = state.castlingRights,
            prevEnPassantTarget = state.enPassantTarget,
            prevHalfmoveClock = state.halfmoveClock
        )

        return BoardState(
            board = nextBoard.map { it.toList() },
            activeColor = state.activeColor.opponent(),
            castlingRights = nextCastlingRights,
            enPassantTarget = nextEnPassantTarget,
            halfmoveClock = nextHalfmoveClock,
            fullmoveNumber = nextFullmoveNumber,
            moveHistory = state.moveHistory + record
        )
    }

    fun undoMove(state: BoardState): BoardState {
        if (state.moveHistory.isEmpty()) return state

        val lastRecord = state.moveHistory.last()
        val prevHistory = state.moveHistory.dropLast(1)
        val move = lastRecord.move

        val nextBoard = state.board.map { it.toMutableList() }.toMutableList()

        // Place the moved piece back to 'from'
        nextBoard[move.from.row][move.from.col] = lastRecord.pieceMoved
        // Clear or restore the 'to' square
        nextBoard[move.to.row][move.to.col] = if (move.isEnPassant) null else lastRecord.pieceCaptured

        // Special Move: Castling
        if (move.isCastling) {
            val r = move.to.row
            if (move.to.col == 6) { // King Side
                val rook = nextBoard[r][5]
                nextBoard[r][5] = null
                nextBoard[r][7] = rook
            } else if (move.to.col == 2) { // Queen Side
                val rook = nextBoard[r][3]
                nextBoard[r][3] = null
                nextBoard[r][0] = rook
            }
        }

        // Special Move: En Passant
        if (move.isEnPassant) {
            nextBoard[move.from.row][move.to.col] = lastRecord.pieceCaptured
        }

        // Fullmove number calculation
        val nextFullmoveNumber = if (state.activeColor == Color.WHITE) {
            state.fullmoveNumber - 1
        } else {
            state.fullmoveNumber
        }

        return BoardState(
            board = nextBoard.map { it.toList() },
            activeColor = state.activeColor.opponent(), // switch back
            castlingRights = lastRecord.prevCastlingRights,
            enPassantTarget = lastRecord.prevEnPassantTarget,
            halfmoveClock = lastRecord.prevHalfmoveClock,
            fullmoveNumber = nextFullmoveNumber,
            moveHistory = prevHistory
        )
    }

    fun isCheck(state: BoardState, color: Color): Boolean {
        val kingPos = findKing(state, color) ?: return false
        return isSquareAttacked(kingPos, color.opponent(), state.board)
    }

    fun isCheckmate(state: BoardState): Boolean {
        if (!isCheck(state, state.activeColor)) return false
        return generateLegalMoves(state).isEmpty()
    }

    fun isStalemate(state: BoardState): Boolean {
        if (isCheck(state, state.activeColor)) return false
        return generateLegalMoves(state).isEmpty()
    }

    fun isDraw(state: BoardState): Boolean {
        // 50-move rule
        if (state.halfmoveClock >= 100) return true

        // Threefold repetition
        if (isThreefoldRepetition(state)) return true

        // Insufficient material
        if (isInsufficientMaterial(state)) return true

        return false
    }

    private fun isThreefoldRepetition(state: BoardState): Boolean {
        // A simple simplification: compare board representation matches.
        // We will match the current board layout against previous layouts in the history.
        val currentBoard = state.board
        var occurrences = 1
        // Search backwards in history
        for (i in state.moveHistory.size - 2 downTo 0 step 2) {
            // To have repetition, it must be the same active player's turn, so we step by 2
            // Let's rebuild the board state at that point or compare history states
            // Actually, we can check how many times this board layout appears in game history
            // For a fast check, we compare board layout.
            // To be fully accurate we would compare activeColor, castling rights, and enPassant.
            // But checking board layout is a robust heuristic for simple chess.
            val historyBoard = getHistoryBoardAt(state, i)
            if (historyBoard == currentBoard) {
                occurrences++
                if (occurrences >= 3) return true
            }
        }
        return false
    }

    private fun getHistoryBoardAt(state: BoardState, moveIndex: Int): List<List<Piece?>> {
        var tempState = state
        val movesToUndo = state.moveHistory.size - 1 - moveIndex
        for (i in 0 until movesToUndo) {
            tempState = undoMove(tempState)
        }
        return tempState.board
    }

    private fun isInsufficientMaterial(state: BoardState): Boolean {
        val pieces = mutableListOf<Piece>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c]
                if (p != null) pieces.add(p)
            }
        }

        // Only kings left
        if (pieces.size == 2) return true

        // King and Bishop vs King, or King and Knight vs King
        if (pieces.size == 3) {
            val hasBishopOrKnight = pieces.any { it.type == PieceType.BISHOP || it.type == PieceType.KNIGHT }
            if (hasBishopOrKnight) return true
        }

        // King and Bishop vs King and Bishop (same color squares)
        // (A slight simplification is fine for this offline app phase)
        return false
    }

    fun isSquareAttacked(pos: Position, byColor: Color, board: List<List<Piece?>>): Boolean {
        // 1. Pawn attacks
        val pawnRowOffset = if (byColor == Color.WHITE) 1 else -1
        val pawnLeft = Position(pos.row + pawnRowOffset, pos.col - 1)
        val pawnRight = Position(pos.row + pawnRowOffset, pos.col + 1)
        if (pawnLeft.isValid()) {
            val p = board[pawnLeft.row][pawnLeft.col]
            if (p != null && p.type == PieceType.PAWN && p.color == byColor) return true
        }
        if (pawnRight.isValid()) {
            val p = board[pawnRight.row][pawnRight.col]
            if (p != null && p.type == PieceType.PAWN && p.color == byColor) return true
        }

        // 2. Knight attacks
        val knightOffsets = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for (offset in knightOffsets) {
            val target = Position(pos.row + offset.first, pos.col + offset.second)
            if (target.isValid()) {
                val p = board[target.row][target.col]
                if (p != null && p.type == PieceType.KNIGHT && p.color == byColor) return true
            }
        }

        // 3. King attacks
        val kingOffsets = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1,           0 to 1,
            1 to -1,  1 to 0,  1 to 1
        )
        for (offset in kingOffsets) {
            val target = Position(pos.row + offset.first, pos.col + offset.second)
            if (target.isValid()) {
                val p = board[target.row][target.col]
                if (p != null && p.type == PieceType.KING && p.color == byColor) return true
            }
        }

        // 4. Sliding Rook/Queen attacks
        val rookDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for (dir in rookDirs) {
            var step = 1
            while (true) {
                val target = Position(pos.row + dir.first * step, pos.col + dir.second * step)
                if (!target.isValid()) break
                val p = board[target.row][target.col]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break // Blocked
                }
                step++
            }
        }

        // 5. Sliding Bishop/Queen attacks
        val bishopDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for (dir in bishopDirs) {
            var step = 1
            while (true) {
                val target = Position(pos.row + dir.first * step, pos.col + dir.second * step)
                if (!target.isValid()) break
                val p = board[target.row][target.col]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break // Blocked
                }
                step++
            }
        }

        return false
    }

    private fun findKing(state: BoardState, color: Color): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    private fun generatePseudoLegalMoves(state: BoardState): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                val piece = state.getPiece(pos)
                if (piece != null && piece.color == state.activeColor) {
                    generatePseudoMovesForPiece(state, pos, piece, moves)
                }
            }
        }
        return moves
    }

    private fun generatePseudoMovesForPiece(state: BoardState, pos: Position, piece: Piece, moves: MutableList<Move>) {
        when (piece.type) {
            PieceType.PAWN -> generatePawnMoves(state, pos, piece, moves)
            PieceType.KNIGHT -> generateKnightMoves(state, pos, piece, moves)
            PieceType.BISHOP -> generateBishopMoves(state, pos, piece, moves)
            PieceType.ROOK -> generateRookMoves(state, pos, piece, moves)
            PieceType.QUEEN -> generateQueenMoves(state, pos, piece, moves)
            PieceType.KING -> generateKingMoves(state, pos, piece, moves)
        }
    }

    private fun generatePawnMoves(state: BoardState, pos: Position, piece: Piece, moves: MutableList<Move>) {
        val isWhite = piece.color == Color.WHITE
        val startRow = if (isWhite) 6 else 1
        val promoteRow = if (isWhite) 0 else 7
        val dir = if (isWhite) -1 else 1

        // 1. One step forward
        val nextRow = pos.row + dir
        val oneStep = Position(nextRow, pos.col)
        if (oneStep.isValid() && state.getPiece(oneStep) == null) {
            if (nextRow == promoteRow) {
                addPromotions(pos, oneStep, moves)
            } else {
                moves.add(Move(pos, oneStep))
            }

            // 2. Two steps forward (only if first is empty and we are on starting row)
            val twoStep = Position(pos.row + 2 * dir, pos.col)
            if (pos.row == startRow && twoStep.isValid() && state.getPiece(twoStep) == null) {
                moves.add(Move(pos, twoStep, isDoublePawnPush = true))
            }
        }

        // 3. Diagonal captures (including en passant)
        val cols = listOf(pos.col - 1, pos.col + 1)
        for (c in cols) {
            val target = Position(nextRow, c)
            if (target.isValid()) {
                val targetPiece = state.getPiece(target)
                if (targetPiece != null && targetPiece.color != piece.color) {
                    if (nextRow == promoteRow) {
                        addPromotions(pos, target, moves)
                    } else {
                        moves.add(Move(pos, target))
                    }
                } else if (target == state.enPassantTarget) {
                    moves.add(Move(pos, target, isEnPassant = true))
                }
            }
        }
    }

    private fun addPromotions(from: Position, to: Position, moves: MutableList<Move>) {
        moves.add(Move(from, to, promotionType = PieceType.QUEEN))
        moves.add(Move(from, to, promotionType = PieceType.ROOK))
        moves.add(Move(from, to, promotionType = PieceType.BISHOP))
        moves.add(Move(from, to, promotionType = PieceType.KNIGHT))
    }

    private fun generateKnightMoves(state: BoardState, pos: Position, piece: Piece, moves: MutableList<Move>) {
        val offsets = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for (offset in offsets) {
            val target = Position(pos.row + offset.first, pos.col + offset.second)
            if (target.isValid()) {
                val targetPiece = state.getPiece(target)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    moves.add(Move(pos, target))
                }
            }
        }
    }

    private fun generateBishopMoves(state: BoardState, pos: Position, piece: Piece, moves: MutableList<Move>) {
        val dirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for (dir in dirs) {
            var step = 1
            while (true) {
                val target = Position(pos.row + dir.first * step, pos.col + dir.second * step)
                if (!target.isValid()) break
                val targetPiece = state.getPiece(target)
                if (targetPiece == null) {
                    moves.add(Move(pos, target))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(pos, target))
                    }
                    break
                }
                step++
            }
        }
    }

    private fun generateRookMoves(state: BoardState, pos: Position, piece: Piece, moves: MutableList<Move>) {
        val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for (dir in dirs) {
            var step = 1
            while (true) {
                val target = Position(pos.row + dir.first * step, pos.col + dir.second * step)
                if (!target.isValid()) break
                val targetPiece = state.getPiece(target)
                if (targetPiece == null) {
                    moves.add(Move(pos, target))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(pos, target))
                    }
                    break
                }
                step++
            }
        }
    }

    private fun generateQueenMoves(state: BoardState, pos: Position, piece: Piece, moves: MutableList<Move>) {
        generateBishopMoves(state, pos, piece, moves)
        generateRookMoves(state, pos, piece, moves)
    }

    private fun generateKingMoves(state: BoardState, pos: Position, piece: Piece, moves: MutableList<Move>) {
        val dirs = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1,           0 to 1,
            1 to -1,  1 to 0,  1 to 1
        )
        for (dir in dirs) {
            val target = Position(pos.row + dir.first, pos.col + dir.second)
            if (target.isValid()) {
                val targetPiece = state.getPiece(target)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    moves.add(Move(pos, target))
                }
            }
        }

        // Castling
        val isWhite = piece.color == Color.WHITE
        val r = if (isWhite) 7 else 0

        if (pos == Position(r, 4)) {
            val checkColor = piece.color.opponent()
            val inCheck = isSquareAttacked(pos, checkColor, state.board)
            if (!inCheck) {
                // King side
                val kRights = if (isWhite) state.castlingRights.whiteKingSide else state.castlingRights.blackKingSide
                if (kRights) {
                    val rookPos = Position(r, 7)
                    val rook = state.getPiece(rookPos)
                    if (rook != null && rook.type == PieceType.ROOK && rook.color == piece.color) {
                        val sq1 = Position(r, 5)
                        val sq2 = Position(r, 6)
                        if (state.getPiece(sq1) == null && state.getPiece(sq2) == null) {
                            if (!isSquareAttacked(sq1, checkColor, state.board) && !isSquareAttacked(sq2, checkColor, state.board)) {
                                moves.add(Move(pos, sq2, isCastling = true))
                            }
                        }
                    }
                }

                // Queen side
                val qRights = if (isWhite) state.castlingRights.whiteQueenSide else state.castlingRights.blackQueenSide
                if (qRights) {
                    val rookPos = Position(r, 0)
                    val rook = state.getPiece(rookPos)
                    if (rook != null && rook.type == PieceType.ROOK && rook.color == piece.color) {
                        val sq1 = Position(r, 1)
                        val sq2 = Position(r, 2)
                        val sq3 = Position(r, 3)
                        if (state.getPiece(sq1) == null && state.getPiece(sq2) == null && state.getPiece(sq3) == null) {
                            if (!isSquareAttacked(sq3, checkColor, state.board) && !isSquareAttacked(sq2, checkColor, state.board)) {
                                moves.add(Move(pos, sq2, isCastling = true))
                            }
                        }
                    }
                }
            }
        }
    }
}
