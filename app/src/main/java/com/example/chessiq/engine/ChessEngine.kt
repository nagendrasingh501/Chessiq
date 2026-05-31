package com.example.chessiq.engine

import com.example.chessiq.model.*
import kotlin.random.Random

object ChessEngine {

    // Piece-Square Tables (PST) for White.
    // For Black, the row index is mirrored (7 - r).
    private val pawnPST = arrayOf(
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0),
        intArrayOf( 50,  50,  50,  50,  50,  50,  50,  50),
        intArrayOf( 10,  10,  20,  30,  30,  20,  10,  10),
        intArrayOf(  5,   5,  10,  25,  25,  10,   5,   5),
        intArrayOf(  0,   0,   0,  20,  20,   0,   0,   0),
        intArrayOf(  5,  -5, -10,   0,   0, -10,  -5,   5),
        intArrayOf(  5,  10,  10, -20, -20,  10,  10,   5),
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0)
    )

    private val knightPST = arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20,   0,   0,   0,   0, -20, -40),
        intArrayOf(-30,   0,  10,  15,  15,  10,   0, -30),
        intArrayOf(-30,   5,  15,  20,  20,  15,   5, -30),
        intArrayOf(-30,   0,  15,  20,  20,  15,   0, -30),
        intArrayOf(-30,   5,  10,  15,  15,  10,   5, -30),
        intArrayOf(-40, -20,   0,   5,   5,   0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    private val bishopPST = arrayOf(
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
        intArrayOf(-10,   0,   0,   0,   0,   0,   0, -10),
        intArrayOf(-10,   0,   5,  10,  10,   5,   0, -10),
        intArrayOf(-10,   5,   5,  10,  10,   5,   5, -10),
        intArrayOf(-10,   0,  10,  10,  10,  10,   0, -10),
        intArrayOf(-10,  10,  10,  10,  10,  10,  10, -10),
        intArrayOf(-10,   5,   0,   0,   0,   0,   5, -10),
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20)
    )

    private val rookPST = arrayOf(
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0),
        intArrayOf(  5,  10,  10,  10,  10,  10,  10,   5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf(  0,   0,   0,   5,   5,   5,   0,   0)
    )

    private val queenPST = arrayOf(
        intArrayOf(-20, -10, -10,  -5,  -5, -10, -10, -20),
        intArrayOf(-10,   0,   0,   0,   0,   0,   0, -10),
        intArrayOf(-10,   0,   5,   5,   5,   5,   0, -10),
        intArrayOf( -5,   0,   5,   5,   5,   5,   0,  -5),
        intArrayOf(  0,   0,   5,   5,   5,   5,   0,  -5),
        intArrayOf(-10,   5,   5,   5,   5,   5,   5, -10),
        intArrayOf(-10,   0,   5,   0,   0,   5,   0, -10),
        intArrayOf(-20, -10, -10,  -5,  -5, -10, -10, -20)
    )

    private val kingPST = arrayOf(
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-20, -30, -30, -40, -40, -30, -30, -20),
        intArrayOf(-10, -20, -20, -20, -20, -20, -20, -10),
        intArrayOf( 20,  20,   0,   0,   0,   0,  20,  20),
        intArrayOf( 20,  30,  10,   0,   0,  10,  30,  20)
    )

    fun evaluateBoard(state: BoardState): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c] ?: continue
                val isWhite = piece.color == Color.WHITE
                val baseVal = piece.type.value

                // PST lookup
                val pstTable = when (piece.type) {
                    PieceType.PAWN -> pawnPST
                    PieceType.KNIGHT -> knightPST
                    PieceType.BISHOP -> bishopPST
                    PieceType.ROOK -> rookPST
                    PieceType.QUEEN -> queenPST
                    PieceType.KING -> kingPST
                }

                val rowIdx = if (isWhite) r else 7 - r
                val pstVal = pstTable[rowIdx][c]

                val totalVal = baseVal + pstVal
                if (isWhite) {
                    score += totalVal
                } else {
                    score -= totalVal
                }
            }
        }
        return score
    }

    // Sort moves for Alpha-Beta efficiency (MVV-LVA)
    private fun orderMoves(state: BoardState, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            val mover = state.getPiece(move.from) ?: return@sortedByDescending 0
            val target = state.getPiece(move.to)

            // Capture bonus
            if (target != null) {
                // MVV-LVA: capturing valuable pieces with cheap pieces is best
                score += 10 * target.type.value - mover.type.value
            }

            // Promotion bonus
            if (move.promotionType != null) {
                score += move.promotionType.value
            }

            // Pawn moving to control center
            if (mover.type == PieceType.PAWN && move.to.col in 3..4 && move.to.row in 3..4) {
                score += 50
            }

            // Avoid moving king unless castling
            if (mover.type == PieceType.KING) {
                if (move.isCastling) score += 100 else score -= 50
            }

            score
        }
    }

    // Alpha-Beta Minimax search
    private fun search(
        state: BoardState,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizing: Boolean
    ): Pair<Int, Move?> {
        // Base cases
        val isWhiteTurn = state.activeColor == Color.WHITE

        if (MoveGenerator.isCheckmate(state)) {
            // If it's checkmate, the side to move lost.
            // White turn: Black won (huge negative)
            // Black turn: White won (huge positive)
            val mateScore = if (isWhiteTurn) -30000 - depth else 30000 + depth
            return Pair(mateScore, null)
        }

        if (MoveGenerator.isStalemate(state) || MoveGenerator.isDraw(state)) {
            return Pair(0, null)
        }

        if (depth == 0) {
            return Pair(evaluateBoard(state), null)
        }

        val moves = MoveGenerator.generateLegalMoves(state)
        if (moves.isEmpty()) {
            return Pair(evaluateBoard(state), null)
        }

        val orderedMoves = orderMoves(state, moves)
        var bestMove: Move? = null

        if (maximizing) {
            var maxEval = Int.MIN_VALUE
            var currentAlpha = alpha
            for (move in orderedMoves) {
                val nextState = MoveGenerator.makeMove(state, move)
                val eval = search(nextState, depth - 1, currentAlpha, beta, false).first
                if (eval > maxEval) {
                    maxEval = eval
                    bestMove = move
                }
                currentAlpha = maxOf(currentAlpha, eval)
                if (beta <= currentAlpha) {
                    break // Beta cutoff
                }
            }
            return Pair(maxEval, bestMove)
        } else {
            var minEval = java.lang.Integer.MAX_VALUE
            var currentBeta = beta
            for (move in orderedMoves) {
                val nextState = MoveGenerator.makeMove(state, move)
                val eval = search(nextState, depth - 1, alpha, currentBeta, true).first
                if (eval < minEval) {
                    minEval = eval
                    bestMove = move
                }
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= alpha) {
                    break // Alpha cutoff
                }
            }
            return Pair(minEval, bestMove)
        }
    }

    // Find the best move for the active color at the given level
    fun getBestMove(state: BoardState, level: Int): Move? {
        val moves = MoveGenerator.generateLegalMoves(state)
        if (moves.isEmpty()) return null

        val isWhite = state.activeColor == Color.WHITE

        return when (level) {
            1 -> { // Beginner: ELO ~400. 60% random moves, 40% depth 1.
                if (Random.nextDouble() < 0.6) {
                    moves.random()
                } else {
                    search(state, 1, Int.MIN_VALUE, Int.MAX_VALUE, isWhite).second ?: moves.random()
                }
            }
            2 -> { // Easy: ELO ~800. 30% random, 70% depth 2 with evaluation noise.
                if (Random.nextDouble() < 0.3) {
                    moves.random()
                } else {
                    // Search at depth 2
                    val orderedMoves = orderMoves(state, moves)
                    val evals = orderedMoves.map { move ->
                        val next = MoveGenerator.makeMove(state, move)
                        val evalVal = search(next, 1, Int.MIN_VALUE, Int.MAX_VALUE, !isWhite).first
                        val noise = Random.nextInt(-40, 40)
                        Pair(evalVal + noise, move)
                    }
                    if (isWhite) {
                        evals.maxByOrNull { it.first }?.second
                    } else {
                        evals.minByOrNull { it.first }?.second
                    } ?: moves.random()
                }
            }
            3 -> { // Medium: ELO ~1200. Depth 3 minimax search.
                search(state, 3, Int.MIN_VALUE, Int.MAX_VALUE, isWhite).second ?: moves.random()
            }
            4 -> { // Hard: ELO ~1800. Depth 4 minimax search.
                search(state, 4, Int.MIN_VALUE, Int.MAX_VALUE, isWhite).second ?: moves.random()
            }
            5 -> { // Master: ELO ~2500. Depth 5 minimax search.
                search(state, 5, Int.MIN_VALUE, Int.MAX_VALUE, isWhite).second ?: moves.random()
            }
            else -> search(state, 3, Int.MIN_VALUE, Int.MAX_VALUE, isWhite).second ?: moves.random()
        }
    }
}
