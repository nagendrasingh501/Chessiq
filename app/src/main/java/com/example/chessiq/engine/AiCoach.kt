package com.example.chessiq.engine

import com.example.chessiq.model.*

enum class MoveCategory(val displayName: String, val colorHex: String) {
    BRILLIANT("Brilliant", "#00B0FF"), // Electric Blue / Cyan
    BEST("Best Move", "#2E7D32"),      // Premium green
    GOOD("Good", "#4CAF50"),           // Active green
    INACCURACY("Inaccuracy", "#FBC02D"), // Warm amber
    MISTAKE("Mistake", "#F57C00"),       // Rich orange
    BLUNDER("Blunder", "#D32F2F")        // Vivid red
}

data class CoachFeedback(
    val category: MoveCategory,
    val feedbackText: String,
    val bestMove: Move,
    val playedMove: Move,
    val scoreDiff: Int,
    val color: Color
)

object AiCoach {

    fun analyzeMove(
        stateBefore: BoardState,
        playedMove: Move,
        stateAfter: BoardState
    ): CoachFeedback {
        val playerColor = stateBefore.activeColor
        val isWhite = playerColor == Color.WHITE

        // 1. Find the best move according to the engine (search at depth 3 for fast, smart coach)
        val bestMove = ChessEngine.getBestMove(stateBefore, 3) ?: playedMove

        // 2. Evaluate both states
        val evalBestState = MoveGenerator.makeMove(stateBefore, bestMove)
        val evalBest = ChessEngine.evaluateBoard(evalBestState)
        val evalPlayed = ChessEngine.evaluateBoard(stateAfter)

        // Loss from player's perspective:
        // White wants higher evaluation, so loss is (evalBest - evalPlayed)
        // Black wants lower evaluation, so loss is (evalPlayed - evalBest)
        val scoreDiff = if (isWhite) {
            evalBest - evalPlayed
        } else {
            evalPlayed - evalBest
        }

        // Sacrifice detection: Check if the moving piece (excluding pawns) moves to an attacked square
        // and is either undefended or has a higher value than standard pawn.
        val isSacrifice = run {
            val opponentColor = playerColor.opponent()
            val piece = stateBefore.getPiece(playedMove.from)
            if (piece != null && piece.type != PieceType.PAWN) {
                val isAttacked = MoveGenerator.isSquareAttacked(playedMove.to, opponentColor, stateAfter.board)
                val isDefended = MoveGenerator.isSquareAttacked(playedMove.to, playerColor, stateAfter.board)
                isAttacked && (!isDefended || piece.type.value > 100)
            } else {
                false
            }
        }

        // Categorize based on evaluation loss (in centipawns)
        val category = when {
            scoreDiff <= 15 -> {
                if (isSacrifice) MoveCategory.BRILLIANT else MoveCategory.BEST
            }
            scoreDiff <= 45 -> MoveCategory.GOOD
            scoreDiff <= 90 -> MoveCategory.INACCURACY
            scoreDiff <= 180 -> MoveCategory.MISTAKE
            else -> MoveCategory.BLUNDER
        }

        // Generate explanation
        val pieceMoved = stateBefore.getPiece(playedMove.from)
        val pieceCaptured = stateBefore.getPiece(playedMove.to)
        val feedbackText = generateExplanation(
            category = category,
            pieceMoved = pieceMoved,
            playedMove = playedMove,
            bestMove = bestMove,
            isCapture = pieceCaptured != null,
            isCheck = MoveGenerator.isCheck(stateAfter, playerColor.opponent()),
            stateBefore = stateBefore,
            stateAfter = stateAfter
        )

        return CoachFeedback(
            category = category,
            feedbackText = feedbackText,
            bestMove = bestMove,
            playedMove = playedMove,
            scoreDiff = scoreDiff,
            color = playerColor
        )
    }

    private fun generateExplanation(
        category: MoveCategory,
        pieceMoved: Piece?,
        playedMove: Move,
        bestMove: Move,
        isCapture: Boolean,
        isCheck: Boolean,
        stateBefore: BoardState,
        stateAfter: BoardState
    ): String {
        val pieceName = pieceMoved?.type?.name?.lowercase() ?: "piece"
        val opponentColor = stateBefore.activeColor.opponent()

        if (category == MoveCategory.BRILLIANT) {
            return "Brilliant! A stunning sacrifice that unlocks a powerful tactical sequence or secures a decisive positional advantage."
        }

        if (category == MoveCategory.BEST || category == MoveCategory.GOOD) {
            return when {
                playedMove.isCastling -> "Excellent decision. Castling secures your King in safety and activates the Rook into play."
                isCheck -> "Superb! Placing the opponent King in check breaks their tempo and forces defensive moves."
                isCapture -> "Nice capture! You seized material and improved your standing on the board."
                pieceMoved?.type == PieceType.KNIGHT && playedMove.to.col in 2..5 && playedMove.to.row in 2..5 -> 
                    "Great knight placement. Developing knights towards the center maximizes their attacking range."
                pieceMoved?.type == PieceType.PAWN && playedMove.to.row in 3..4 -> 
                    "Excellent pawn push. Securing a foothold in the central squares controls the flow of the game."
                else -> "A highly accurate move. It strengthens your position, maintaining pressure and tactical control."
            }
        }

        // Warning cases (Inaccuracies, Mistakes, Blunders)
        // 1. Did we hang our piece? (i.e. did we move to a square attacked by opponent, and is it undefended?)
        val squareAttacked = MoveGenerator.isSquareAttacked(playedMove.to, opponentColor, stateAfter.board)
        // Check if we have defenders on 'to' square
        val squareDefended = MoveGenerator.isSquareAttacked(playedMove.to, stateBefore.activeColor, stateAfter.board)

        if (squareAttacked && !squareDefended) {
            return "Watch out! Moving your $pieceName to ${playedMove.to.toAlgebraic()} hangs it, exposing it to direct capture."
        }

        // 2. Did we miss a free capture?
        val bestMovePiece = stateBefore.getPiece(bestMove.from)
        val bestMoveCaptured = stateBefore.getPiece(bestMove.to)
        if (bestMoveCaptured != null && bestMovePiece != null) {
            return "Oops! You missed an opportunity to capture the opponent's ${bestMoveCaptured.type.name.lowercase()} with your ${bestMovePiece.type.name.lowercase()} at ${bestMove.to.toAlgebraic()}."
        }

        // 3. Did we block our own development or miss castling?
        if (bestMove.isCastling) {
            return "This move delays castling. It is critical to secure your King early before launching attacks."
        }

        // 4. Default advice pointing to best move
        val bestPiece = stateBefore.getPiece(bestMove.from)
        val bestPieceName = bestPiece?.type?.name?.lowercase() ?: "piece"
        return "An off-target move. The AI coach recommends moving the $bestPieceName from ${bestMove.from.toAlgebraic()} to ${bestMove.to.toAlgebraic()} to maximize control."
    }
}
