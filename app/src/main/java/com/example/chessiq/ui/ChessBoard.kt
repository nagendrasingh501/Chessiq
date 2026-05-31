package com.example.chessiq.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessiq.model.*
import com.example.chessiq.model.Color as ChessColor
import com.example.chessiq.ui.theme.ChessBoardTheme

@Composable
fun ChessBoard(
    boardState: BoardState,
    theme: ChessBoardTheme,
    selectedSquare: Position?,
    onSquareClick: (Position) -> Unit,
    legalMoves: List<Move>,
    is3dMode: Boolean,
    isRotated: Boolean,
    isPassAndPlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Collect last move positions for highlighting
    val lastMove = boardState.moveHistory.lastOrNull()?.move
    val lastMoveFrom = lastMove?.from
    val lastMoveTo = lastMove?.to

    val animOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    
    LaunchedEffect(lastMove) {
        if (lastMove != null) {
            val from = lastMove.from
            val to = lastMove.to
            
            // Calculate starting offset in square units
            val colDiff = if (isRotated) (to.col - from.col) else (from.col - to.col)
            val rowDiff = if (isRotated) (to.row - from.row) else (from.row - to.row)
            
            // Set initial offset
            animOffset.snapTo(Offset(colDiff.toFloat(), rowDiff.toFloat()))
            // Animate to zero
            animOffset.animateTo(
                targetValue = Offset.Zero,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        } else {
            animOffset.snapTo(Offset.Zero)
        }
    }

    // Check if white or black king is in check
    val isWhiteInCheck = boardState.activeColor == ChessColor.WHITE && com.example.chessiq.engine.MoveGenerator.isCheck(boardState, ChessColor.WHITE)
    val isBlackInCheck = boardState.activeColor == ChessColor.BLACK && com.example.chessiq.engine.MoveGenerator.isCheck(boardState, ChessColor.BLACK)

    val legalTargets = legalMoves.map { it.to }

    // Infinite transitions for pulsing glows & nebula movement
    val infiniteTransition = rememberInfiniteTransition(label = "boardAnimations")

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val checkPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "checkPulseScale"
    )

    val nebulaOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebulaOffset1"
    )
    val nebulaOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebulaOffset2"
    )

    // Twinkling stars for Galaxy theme
    val stars = remember {
        List(20) {
            Triple(
                (0.05f + Math.random().toFloat() * 0.9f),
                (0.05f + Math.random().toFloat() * 0.9f),
                (600 + Math.random().toFloat() * 1400).toInt()
            )
        }
    }

    val starAlphas = stars.mapIndexed { idx, star ->
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(star.third, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "starAlpha_$idx"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 3D Bevel/Base block underneath the board for visual depth in 3D Mode
        if (is3dMode) {
            val baseBrush = if (theme == ChessBoardTheme.GALAXY) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF150A2F),
                        Color(0xFF070414)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        theme.darkSquare.copy(alpha = 0.9f),
                        Color(0xFF0F172A)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 8.dp)
                    .background(baseBrush, shape = RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0xFF0F172A), shape = RoundedCornerShape(12.dp))
            )
        }

        val boardBg = if (theme == ChessBoardTheme.GALAXY) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF070414),
                    Color(0xFF150A2F),
                    Color(0xFF0F0721),
                    Color(0xFF070414)
                ),
                start = Offset(nebulaOffset1, 0f),
                end = Offset(1000f + nebulaOffset2, 1000f)
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(theme.darkSquare, theme.darkSquare)
            )
        }

        val boardBorder = if (theme == ChessBoardTheme.NEON) {
            BorderStroke(2.dp, Color(0xFF00FFFF).copy(alpha = pulseGlow))
        } else if (theme == ChessBoardTheme.GALAXY) {
            BorderStroke(2.dp, Color(0xFFA855F7).copy(alpha = pulseGlow))
        } else {
            BorderStroke(2.dp, theme.darkSquare.copy(alpha = 0.5f))
        }

        // The main chess board grid
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (is3dMode) 4.dp else 8.dp, shape = RoundedCornerShape(8.dp))
                .background(boardBg, shape = RoundedCornerShape(8.dp))
                .border(boardBorder, shape = RoundedCornerShape(8.dp))
        ) {
            if (theme == ChessBoardTheme.GALAXY) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    stars.forEachIndexed { idx, star ->
                        val (xRatio, yRatio, _) = star
                        drawCircle(
                            color = Color.White.copy(alpha = starAlphas[idx].value),
                            radius = (1.5f + starAlphas[idx].value * 2f).dp.toPx(),
                            center = Offset(xRatio * width, yRatio * height)
                        )
                    }
                }
            }

            val squareSize = maxWidth / 8

            // Determine row/col ranges based on rotation (Black vs White perspective)
            val rowRange = if (isRotated) (7 downTo 0) else (0..7)
            val colRange = if (isRotated) (7 downTo 0) else (0..7)

            Column {
                for (row in rowRange) {
                    Row {
                        for (col in colRange) {
                            val pos = Position(row, col)
                            val isLight = (row + col) % 2 == 0
                            val piece = boardState.getPiece(pos)

                            val isSelected = selectedSquare == pos
                            val isLegalTarget = legalTargets.contains(pos)
                            val isLastMove = pos == lastMoveFrom || pos == lastMoveTo

                            val isKingInCheck = (piece?.type == PieceType.KING && piece.color == ChessColor.WHITE && isWhiteInCheck) ||
                                    (piece?.type == PieceType.KING && piece.color == ChessColor.BLACK && isBlackInCheck)

                            // Choose base square color
                            val baseColor = if (isLight) theme.lightSquare else theme.darkSquare

                            Box(
                                modifier = Modifier
                                    .size(squareSize)
                                    .background(baseColor)
                                    .clickable { onSquareClick(pos) },
                                contentAlignment = Alignment.Center
                            ) {
                                // Highlight overlays
                                if (isLastMove) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(theme.lastMoveHighlight)
                                    )
                                }

                                 if (isSelected) {
                                    if (theme == ChessBoardTheme.NEON) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(2.dp, Color(0xFF00FFFF).copy(alpha = pulseGlow))
                                                .background(Color(0xFF00FFFF).copy(alpha = 0.15f * pulseGlow))
                                        )
                                    } else if (theme == ChessBoardTheme.GALAXY) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(2.dp, Color(0xFFA855F7).copy(alpha = pulseGlow))
                                                .background(Color(0xFFA855F7).copy(alpha = 0.15f * pulseGlow))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(theme.selectedSquare)
                                        )
                                    }
                                }

                                if (isKingInCheck) {
                                    if (theme == ChessBoardTheme.NEON) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .scale(checkPulseScale)
                                                .border(2.5.dp, Color(0xFFFF0055))
                                                .background(Color(0xFFFF0055).copy(alpha = 0.25f))
                                        )
                                    } else if (theme == ChessBoardTheme.GALAXY) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .scale(checkPulseScale)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(Color(0xFFEC4899), Color.Transparent),
                                                        radius = squareSize.value * 2f
                                                    )
                                                )
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(theme.checkHighlight.copy(alpha = pulseGlow))
                                        )
                                    }
                                }

                                // Render drop shadow in 3D mode
                                if (piece != null && is3dMode) {
                                    Box(
                                        modifier = Modifier
                                            .size(squareSize * 0.45f, squareSize * 0.15f)
                                            .offset(y = (squareSize.value * 0.18f).dp)
                                            .background(Color(0x40000000), shape = CircleShape)
                                    )
                                }

                                // Render Piece (tilted slightly up in 3D mode for floating effect)
                                if (piece != null) {
                                    val movementOffsetModifier = if (pos == lastMoveTo) {
                                        Modifier.offset(
                                            x = (animOffset.value.x * squareSize.value).dp,
                                            y = (animOffset.value.y * squareSize.value).dp
                                        )
                                    } else {
                                        Modifier
                                    }

                                    val pieceModifier = if (is3dMode) {
                                        movementOffsetModifier.offset(y = (-4).dp)
                                    } else {
                                        movementOffsetModifier
                                    }

                                    val rotatePiece = isPassAndPlay && (if (isRotated) piece.color == ChessColor.WHITE else piece.color == ChessColor.BLACK)
                                    val finalPieceModifier = if (rotatePiece) {
                                        pieceModifier.rotate(180f)
                                    } else {
                                        pieceModifier
                                    }

                                    Box(modifier = finalPieceModifier) {
                                        PieceRepresentation(
                                            piece = piece,
                                            sizeSp = (squareSize.value * 0.7f).sp,
                                            theme = theme
                                        )
                                    }
                                }

                                // Render Legal Move Indicators
                                if (isLegalTarget) {
                                    if (piece == null) {
                                        // Empty square dot
                                        Box(
                                            modifier = Modifier
                                                .size(squareSize * 0.3f)
                                                .background(theme.legalMoveDot, shape = CircleShape)
                                        )
                                    } else {
                                        // Capture ring
                                        Box(
                                            modifier = Modifier
                                                .size(squareSize * 0.85f)
                                                .border(width = 3.dp, color = theme.legalMoveCapture, shape = CircleShape)
                                        )
                                    }
                                }

                                // Draw rank coordinates on leftmost column
                                if (col == (if (isRotated) 7 else 0)) {
                                    val labelColor = if (isLight) theme.darkSquare else theme.lightSquare
                                    Text(
                                        text = (8 - row).toString(),
                                        color = labelColor.copy(alpha = 0.7f),
                                        fontSize = (squareSize.value * 0.18f).sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 3.dp, top = 1.dp)
                                    )
                                }

                                // Draw file coordinates on bottom row
                                if (row == (if (isRotated) 0 else 7)) {
                                    val labelColor = if (isLight) theme.darkSquare else theme.lightSquare
                                    Text(
                                        text = ('a' + col).toString(),
                                        color = labelColor.copy(alpha = 0.7f),
                                        fontSize = (squareSize.value * 0.18f).sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 3.dp, bottom = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceRepresentation(
    piece: Piece,
    sizeSp: androidx.compose.ui.unit.TextUnit,
    theme: ChessBoardTheme
) {
    val symbol = when (piece.type) {
        PieceType.KING -> "♚"
        PieceType.QUEEN -> "♛"
        PieceType.ROOK -> "♜"
        PieceType.BISHOP -> "♝"
        PieceType.KNIGHT -> "♞"
        PieceType.PAWN -> "♟"
    }

    val isWhite = piece.color == ChessColor.WHITE

    val textCol = when (theme) {
        ChessBoardTheme.NEON -> if (isWhite) Color(0xFF00FFFF) else Color(0xFFFF00FF)
        ChessBoardTheme.GALAXY -> if (isWhite) Color(0xFFFFDF00) else Color(0xFFC084FC)
        else -> if (isWhite) Color.White else Color(0xFF1E1E1E)
    }

    val shadowCol = when (theme) {
        ChessBoardTheme.NEON -> if (isWhite) Color(0x8000FFFF) else Color(0x80FF00FF)
        ChessBoardTheme.GALAXY -> if (isWhite) Color(0x80FFDF00) else Color(0x80C084FC)
        else -> if (isWhite) Color(0x90000000) else Color(0x30FFFFFF)
    }

    val shadowOffset = if (isWhite) Offset(2f, 2f) else Offset(-1f, -1f)
    val blurRadius = if (theme == ChessBoardTheme.NEON || theme == ChessBoardTheme.GALAXY) 8f else 3f

    Text(
        text = symbol,
        color = textCol,
        fontSize = sizeSp,
        textAlign = TextAlign.Center,
        style = androidx.compose.ui.text.TextStyle(
            shadow = Shadow(
                color = shadowCol,
                offset = shadowOffset,
                blurRadius = blurRadius
            )
        ),
        modifier = Modifier.wrapContentSize()
    )
}

@Composable
fun PromotionDialog(
    color: ChessColor,
    onSelect: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Pair(PieceType.QUEEN, "♛ Queen"),
        Pair(PieceType.ROOK, "♜ Rook"),
        Pair(PieceType.BISHOP, "♝ Bishop"),
        Pair(PieceType.KNIGHT, "♞ Knight")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pawn Promotion",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        containerColor = Color(0xFF1F2937),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (type, label) ->
                    TextButton(
                        onClick = { onSelect(type) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF374151), shape = CircleShape)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (color == ChessColor.WHITE) Color.White else Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
