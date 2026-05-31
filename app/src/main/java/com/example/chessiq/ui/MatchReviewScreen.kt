package com.example.chessiq.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessiq.engine.*
import com.example.chessiq.model.*
import com.example.chessiq.model.Color as ChessColor
import com.example.chessiq.ui.theme.ChessBoardTheme

@Composable
fun MatchReviewScreen(
    boardState: BoardState,
    theme: ChessBoardTheme,
    is3dMode: Boolean,
    isRotated: Boolean,
    onToggleRotate: () -> Unit,
    matchAnalysis: List<CoachFeedback>,
    reviewMoveIndex: Int,
    onReviewMoveIndexChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    // 1. Calculate accuracy scores
    val whiteAccuracy = remember(matchAnalysis) { calculateAccuracy(matchAnalysis, ChessColor.WHITE) }
    val blackAccuracy = remember(matchAnalysis) { calculateAccuracy(matchAnalysis, ChessColor.BLACK) }

    // 2. Count move categories
    val moveCategories = listOf(
        MoveCategory.BRILLIANT,
        MoveCategory.BEST,
        MoveCategory.GOOD,
        MoveCategory.INACCURACY,
        MoveCategory.MISTAKE,
        MoveCategory.BLUNDER
    )

    // Reconstruct boardState dynamically up to reviewMoveIndex
    val reviewBoardState = remember(reviewMoveIndex, boardState.moveHistory) {
        var temp = BoardState.startPosition()
        val history = boardState.moveHistory
        val limit = reviewMoveIndex + 1
        for (i in 0 until limit.coerceAtMost(history.size)) {
            temp = MoveGenerator.makeMove(temp, history[i].move)
        }
        temp
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Match Review",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Analyze game accuracy & mistakes",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color(0xFF991B1B).copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Review", tint = Color(0xFFEF4444))
                }
            }
        }

        // Summary Card (Accuracy Scores)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("White Accuracy", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$whiteAccuracy%",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(50.dp)
                                .background(Color(0xFF334155))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Black Accuracy", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$blackAccuracy%",
                                color = Color(0xFF38BDF8),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFF334155), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Move Stats Table/Grid
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Rating", modifier = Modifier.weight(1.5f), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("White", modifier = Modifier.weight(1f), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("Black", modifier = Modifier.weight(1f), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        moveCategories.forEach { cat ->
                            val whiteCount = matchAnalysis.count { it.color == ChessColor.WHITE && it.category == cat }
                            val blackCount = matchAnalysis.count { it.color == ChessColor.BLACK && it.category == cat }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.5f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(android.graphics.Color.parseColor(cat.colorHex)), shape = RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cat.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("$whiteCount", modifier = Modifier.weight(1f), color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                Text("$blackCount", modifier = Modifier.weight(1f), color = Color(0xFF38BDF8), fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // The Reconstructed Chessboard
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                ChessBoard(
                    boardState = reviewBoardState,
                    theme = theme,
                    selectedSquare = null,
                    onSquareClick = {},
                    legalMoves = emptyList(),
                    is3dMode = is3dMode,
                    isRotated = isRotated,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Navigation Toolbar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onReviewMoveIndexChange(-1) },
                    enabled = reviewMoveIndex >= 0,
                    modifier = Modifier.background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "First Position", tint = Color.White)
                }

                IconButton(
                    onClick = { onReviewMoveIndexChange(reviewMoveIndex - 1) },
                    enabled = reviewMoveIndex >= 0,
                    modifier = Modifier.background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Move", tint = Color.White)
                }

                // Rotated Board Toggle Button
                IconButton(
                    onClick = onToggleRotate,
                    modifier = Modifier.background(Color(0xFF334155), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Rotate Board", tint = Color(0xFF38BDF8))
                }

                IconButton(
                    onClick = { onReviewMoveIndexChange(reviewMoveIndex + 1) },
                    enabled = reviewMoveIndex < boardState.moveHistory.size - 1,
                    modifier = Modifier.background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Move", tint = Color.White)
                }

                IconButton(
                    onClick = { onReviewMoveIndexChange(boardState.moveHistory.size - 1) },
                    enabled = reviewMoveIndex < boardState.moveHistory.size - 1,
                    modifier = Modifier.background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Final Position", tint = Color.White)
                }
            }
        }

        // Horizontal Move Ticker List
        item {
            val listState = rememberLazyListState()
            LaunchedEffect(reviewMoveIndex) {
                if (reviewMoveIndex >= 0) {
                    val pairIndex = reviewMoveIndex / 2
                    listState.animateScrollToItem(pairIndex)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Moves:",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp)
                ) {
                    val history = boardState.moveHistory
                    val pairsCount = (history.size + 1) / 2
                    items(pairsCount) { index ->
                        val moveNum = index + 1
                        val whiteIndex = index * 2
                        val blackIndex = index * 2 + 1

                        val isWhiteSelected = reviewMoveIndex == whiteIndex
                        val isBlackSelected = reviewMoveIndex == blackIndex

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$moveNum.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (whiteIndex < history.size) {
                                val text = formatMoveRecord(history[whiteIndex])
                                Text(
                                    text = text,
                                    color = if (isWhiteSelected) Color(0xFF00B0FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isWhiteSelected) FontWeight.Black else FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .background(
                                            if (isWhiteSelected) Color(0xFF00B0FF).copy(alpha = 0.15f) else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onReviewMoveIndexChange(whiteIndex) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            if (blackIndex < history.size) {
                                val text = formatMoveRecord(history[blackIndex])
                                Text(
                                    text = text,
                                    color = if (isBlackSelected) Color(0xFF00B0FF) else Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isBlackSelected) FontWeight.Black else FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .background(
                                            if (isBlackSelected) Color(0xFF00B0FF).copy(alpha = 0.15f) else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onReviewMoveIndexChange(blackIndex) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Selected Move Feedback Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(bottom = 12.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.9f), shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                if (reviewMoveIndex >= 0 && reviewMoveIndex < matchAnalysis.size) {
                    val fb = matchAnalysis[reviewMoveIndex]
                    val playerLabel = if (fb.color == ChessColor.WHITE) "White" else "Black"
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$playerLabel Played ${formatMoveRecord(boardState.moveHistory[reviewMoveIndex])}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = fb.category.displayName,
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(
                                        Color(android.graphics.Color.parseColor(fb.category.colorHex)),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = fb.feedbackText,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Starting Position",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use the navigation controls to step through the match.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Internal helpers
private fun calculateAccuracy(feedbacks: List<CoachFeedback>, color: ChessColor): Int {
    val playerFeedbacks = feedbacks.filter { it.color == color }
    if (playerFeedbacks.isEmpty()) return 100
    val total = playerFeedbacks.map { fb ->
        when (fb.category) {
            MoveCategory.BRILLIANT -> 100
            MoveCategory.BEST -> 100
            MoveCategory.GOOD -> 85
            MoveCategory.INACCURACY -> 60
            MoveCategory.MISTAKE -> 30
            MoveCategory.BLUNDER -> 0
        }
    }.sum()
    return (total.toDouble() / playerFeedbacks.size).toInt()
}

private fun formatMoveRecord(record: MoveRecord): String {
    if (record.move.isCastling) {
        return if (record.move.to.col == 6) "O-O" else "O-O-O"
    }
    val piecePrefix = when (record.pieceMoved.type) {
        PieceType.PAWN -> if (record.pieceCaptured != null || record.move.isEnPassant) record.move.from.toAlgebraic().substring(0, 1) else ""
        PieceType.KING -> "K"
        PieceType.QUEEN -> "Q"
        PieceType.ROOK -> "R"
        PieceType.BISHOP -> "B"
        PieceType.KNIGHT -> "N"
    }
    val capture = if (record.pieceCaptured != null || record.move.isEnPassant) "x" else ""
    val promStr = if (record.move.promotionType != null) "=" + record.move.promotionType.symbol else ""
    return "$piecePrefix$capture${record.move.to.toAlgebraic()}$promStr"
}
