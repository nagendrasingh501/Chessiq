package com.example.chessiq.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessiq.engine.*
import com.example.chessiq.model.*
import com.example.chessiq.model.Color as ChessColor
import com.example.chessiq.ui.theme.ChessBoardTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.saveable.rememberSaveable
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

enum class PlayMode {
    VS_AI, PASS_AND_PLAY
}

@Composable
fun ChessGameScreen() {
    var playMode by remember { mutableStateOf(PlayMode.VS_AI) }
    var boardState by remember { mutableStateOf(BoardState.startPosition()) }
    var selectedSquare by remember { mutableStateOf<Position?>(null) }
    var boardTheme by remember { mutableStateOf(ChessBoardTheme.GREEN_CLASSIC) }
    var aiLevel by remember { mutableStateOf(3) } // Default Medium
    var showIqDashboard by remember { mutableStateOf(false) }
    var is3dMode by remember { mutableStateOf(true) } // Default to premium 3D mode!
    var showSplash by remember { mutableStateOf(true) }
    var isMusicOn by rememberSaveable { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isMusicOn) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isMusicOn) {
                    SoundManager.startBackgroundMusic()
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                SoundManager.stopBackgroundMusic()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        if (isMusicOn) {
            SoundManager.startBackgroundMusic()
        } else {
            SoundManager.stopBackgroundMusic()
        }
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            SoundManager.stopBackgroundMusic()
        }
    }

    // Coach Feedback states
    var lastFeedback by remember { mutableStateOf<CoachFeedback?>(null) }
    var coachTipText by remember { mutableStateOf<String?>(null) }
    var showPromotionDialog by remember { mutableStateOf<Move?>(null) }

    // IQ calculation states
    var tacticsScore by remember { mutableStateOf(100) }
    var strategyScore by remember { mutableStateOf(100) }
    var openingScore by remember { mutableStateOf(100) }
    var endgameScore by remember { mutableStateOf(100) }

    val coroutineScope = rememberCoroutineScope()

    // Game Over states
    var showGameOverDialog by remember { mutableStateOf(false) }
    var gameOverMessage by remember { mutableStateOf("") }
    var gameOverTitle by remember { mutableStateOf("") }

    // Time Chance System (Chess Clock) states
    var selectedTimerMinutes by remember { mutableStateOf<Int?>(10) } // Default 10 min
    var selectedIncrementSeconds by remember { mutableStateOf(0) } // Default 0 sec
    var whiteTimeLeft by remember { mutableStateOf(10 * 60 * 1000L) }
    var blackTimeLeft by remember { mutableStateOf(10 * 60 * 1000L) }
    var hasTimerStarted by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showTimeOutDialog by remember { mutableStateOf(false) }
    var winnerByTimeOut by remember { mutableStateOf<ChessColor?>(null) }
    var showTimeConfigDialog by remember { mutableStateOf(false) }
    var manualRotateBoard by remember { mutableStateOf(false) }
    val matchAnalysis = remember { mutableStateListOf<CoachFeedback>() }
    var showMatchReview by remember { mutableStateOf(false) }
    var reviewMoveIndex by remember { mutableStateOf(-1) }

    val isGameActiveTimer by remember(hasTimerStarted, isPaused, selectedTimerMinutes, showGameOverDialog, showTimeOutDialog, showMatchReview) {
        derivedStateOf {
            hasTimerStarted && !isPaused && selectedTimerMinutes != null && !showGameOverDialog && !showTimeOutDialog && !showMatchReview
        }
    }

    LaunchedEffect(isGameActiveTimer, boardState.activeColor) {
        if (isGameActiveTimer) {
            while (true) {
                kotlinx.coroutines.delay(100)
                if (boardState.activeColor == ChessColor.WHITE) {
                    whiteTimeLeft = (whiteTimeLeft - 100).coerceAtLeast(0L)
                    if (whiteTimeLeft == 0L) {
                        winnerByTimeOut = ChessColor.BLACK
                        showTimeOutDialog = true
                        SoundManager.playDefeatMelody()
                        break
                    }
                } else {
                    blackTimeLeft = (blackTimeLeft - 100).coerceAtLeast(0L)
                    if (blackTimeLeft == 0L) {
                        winnerByTimeOut = ChessColor.WHITE
                        showTimeOutDialog = true
                        SoundManager.playTriumphFanfare()
                        break
                    }
                }
            }
        }
    }

    fun applyMoveClockChanges(activeColorBefore: ChessColor) {
        if (!hasTimerStarted) {
            hasTimerStarted = true
        }
        if (selectedTimerMinutes != null) {
            if (activeColorBefore == ChessColor.WHITE) {
                whiteTimeLeft += selectedIncrementSeconds * 1000L
            } else {
                blackTimeLeft += selectedIncrementSeconds * 1000L
            }
        }
    }

    val isCheckmate = remember(boardState) { MoveGenerator.isCheckmate(boardState) }
    val isStalemate = remember(boardState) { MoveGenerator.isStalemate(boardState) }
    val isDraw = remember(boardState) { MoveGenerator.isDraw(boardState) }

    LaunchedEffect(isCheckmate, isStalemate, isDraw) {
        if (isCheckmate) {
            val winnerColor = boardState.activeColor.opponent()
            if (playMode == PlayMode.VS_AI) {
                if (winnerColor == ChessColor.WHITE) {
                    gameOverTitle = "🏆 Victory!"
                    gameOverMessage = "Congratulations! You checkmated the AI and won the game!"
                    SoundManager.playTriumphFanfare()
                } else {
                    gameOverTitle = "💀 Defeat"
                    gameOverMessage = "The AI has checkmated your King. Better luck next time!"
                    SoundManager.playDefeatMelody()
                }
            } else {
                val winnerName = if (winnerColor == ChessColor.WHITE) "White Player" else "Black Player"
                gameOverTitle = "🎉 Game Over"
                gameOverMessage = "$winnerName has checkmated the opponent and won!"
                SoundManager.playTriumphFanfare()
            }
            showGameOverDialog = true
        } else if (isStalemate) {
            gameOverTitle = "🤝 Stalemate"
            gameOverMessage = "The game has ended in a Stalemate (Draw)."
            SoundManager.playDrawChime()
            showGameOverDialog = true
        } else if (isDraw) {
            gameOverTitle = "🤝 Draw"
            gameOverMessage = "The game has ended in a Draw (50-move rule or repetition)."
            SoundManager.playDrawChime()
            showGameOverDialog = true
        }
    }

    val legalMoves by remember(boardState, selectedSquare) {
        derivedStateOf {
            if (selectedSquare != null) {
                MoveGenerator.generateLegalMoves(boardState).filter { it.from == selectedSquare }
            } else {
                emptyList()
            }
        }
    }

    // Auto-rotate board in Pass & Play if it's Black's turn
    val isRotated = manualRotateBoard

    // AI move trigger
    val isAiTurn by remember(playMode, boardState, isPaused) {
        derivedStateOf {
            playMode == PlayMode.VS_AI &&
                    boardState.activeColor == ChessColor.BLACK &&
                    !isPaused &&
                    !MoveGenerator.isCheckmate(boardState) &&
                    !MoveGenerator.isStalemate(boardState) &&
                    !MoveGenerator.isDraw(boardState)
        }
    }

    LaunchedEffect(isAiTurn) {
        Log.e("Chessiq", "LaunchedEffect(isAiTurn) triggered, isAiTurn=$isAiTurn")
        if (isAiTurn) {
            val aiMove = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                Log.e("Chessiq", "AI starting move generation for level=$aiLevel")
                ChessEngine.getBestMove(boardState, aiLevel)
            }
            Log.e("Chessiq", "AI generated move: $aiMove")
            if (aiMove != null) {
                val stateBefore = boardState
                val nextState = MoveGenerator.makeMove(boardState, aiMove)
                
                // Play sound for AI move (if game is not checkmate/draw)
                val isCapture = stateBefore.getPiece(aiMove.to) != null || aiMove.isEnPassant
                val isCheck = MoveGenerator.isCheck(nextState, nextState.activeColor)
                val isGameOver = MoveGenerator.isCheckmate(nextState) || MoveGenerator.isStalemate(nextState) || MoveGenerator.isDraw(nextState)
                if (!isGameOver) {
                    if (isCheck) {
                        SoundManager.playCheckSound()
                    } else if (isCapture) {
                        SoundManager.playCaptureSound()
                    } else {
                        SoundManager.playMoveSound()
                    }
                }
                
                boardState = nextState
                coachTipText = null
                applyMoveClockChanges(stateBefore.activeColor)
                
                // Analyze AI move in background
                coroutineScope.launch {
                    val feedback = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        AiCoach.analyzeMove(stateBefore, aiMove, nextState)
                    }
                    matchAnalysis.add(feedback)
                }
            }
        }
    }

    // Reset game
    fun resetGame(mode: PlayMode = playMode) {
        playMode = mode
        boardState = BoardState.startPosition()
        selectedSquare = null
        lastFeedback = null
        coachTipText = null
        tacticsScore = 100
        strategyScore = 100
        openingScore = 100
        endgameScore = 100
        showGameOverDialog = false

        // Clock Reset
        val initTime = if (selectedTimerMinutes != null) selectedTimerMinutes!! * 60 * 1000L else 0L
        whiteTimeLeft = initTime
        blackTimeLeft = initTime
        hasTimerStarted = false
        isPaused = false
        showTimeOutDialog = false
        winnerByTimeOut = null
        manualRotateBoard = false
        
        matchAnalysis.clear()
        showMatchReview = false
        reviewMoveIndex = -1
    }

    // Undo move
    fun undoMove() {
        var tempState = boardState
        if (playMode == PlayMode.VS_AI) {
            // Undo 2 moves (AI move and player move)
            if (tempState.moveHistory.isNotEmpty()) {
                tempState = MoveGenerator.undoMove(tempState)
                if (matchAnalysis.isNotEmpty()) matchAnalysis.removeLast()
            }
            if (tempState.moveHistory.isNotEmpty() && tempState.activeColor == ChessColor.BLACK) {
                tempState = MoveGenerator.undoMove(tempState)
                if (matchAnalysis.isNotEmpty()) matchAnalysis.removeLast()
            }
        } else {
            // Pass & Play: just undo the single previous move
            if (tempState.moveHistory.isNotEmpty()) {
                tempState = MoveGenerator.undoMove(tempState)
                if (matchAnalysis.isNotEmpty()) matchAnalysis.removeLast()
            }
        }
        boardState = tempState
        selectedSquare = null
        lastFeedback = null
        coachTipText = null
        showGameOverDialog = false

        if (boardState.moveHistory.isEmpty()) {
            hasTimerStarted = false
            val initTime = if (selectedTimerMinutes != null) selectedTimerMinutes!! * 60 * 1000L else 0L
            whiteTimeLeft = initTime
            blackTimeLeft = initTime
        }
    }

    // Update IQ score based on a user's move feedback
    fun updateIqScore(feedback: CoachFeedback, moveNumber: Int) {
        val change = when (feedback.category) {
            MoveCategory.BRILLIANT -> 10
            MoveCategory.BEST -> 5
            MoveCategory.GOOD -> 2
            MoveCategory.INACCURACY -> -2
            MoveCategory.MISTAKE -> -5
            MoveCategory.BLUNDER -> -10
        }

        if (moveNumber <= 8) {
            openingScore = (openingScore + change).coerceIn(50, 200)
        } else {
            var pieceCount = 0
            for (r in 0..7) {
                for (c in 0..7) {
                    if (boardState.board[r][c] != null) pieceCount++
                }
            }

            if (pieceCount < 12) {
                endgameScore = (endgameScore + change).coerceIn(50, 200)
            } else {
                if (feedback.scoreDiff > 100 || feedback.feedbackText.contains("capture", ignoreCase = true)) {
                    tacticsScore = (tacticsScore + change).coerceIn(50, 200)
                } else {
                    strategyScore = (strategyScore + change).coerceIn(50, 200)
                }
            }
        }
    }

    // Handle user move execution
    fun executeUserMove(move: Move) {
        Log.e("Chessiq", "executeUserMove: $move")
        val stateBefore = boardState
        val nextState = MoveGenerator.makeMove(boardState, move)
        
        // Play sound for User move (if game is not checkmate/draw)
        val isCapture = stateBefore.getPiece(move.to) != null || move.isEnPassant
        val isCheck = MoveGenerator.isCheck(nextState, nextState.activeColor)
        val isGameOver = MoveGenerator.isCheckmate(nextState) || MoveGenerator.isStalemate(nextState) || MoveGenerator.isDraw(nextState)
        if (!isGameOver) {
            if (isCheck) {
                SoundManager.playCheckSound()
            } else if (isCapture) {
                SoundManager.playCaptureSound()
            } else {
                SoundManager.playMoveSound()
            }
        }
        
        boardState = nextState
        selectedSquare = null
        coachTipText = null
        applyMoveClockChanges(stateBefore.activeColor)

        // Trigger AI Coach analysis in background
        coroutineScope.launch {
            Log.e("Chessiq", "AI Coach analysis starting in background")
            val feedback = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                AiCoach.analyzeMove(stateBefore, move, nextState)
            }
            Log.e("Chessiq", "AI Coach analysis finished: ${feedback.category}")
            lastFeedback = feedback
            updateIqScore(feedback, nextState.fullmoveNumber)
            matchAnalysis.add(feedback)
        }
    }

    // Handle board click
    fun handleSquareClick(pos: Position) {
        Log.e("Chessiq", "handleSquareClick: pos=$pos, isAiTurn=$isAiTurn, isPaused=$isPaused, selectedSquare=$selectedSquare, activeColor=${boardState.activeColor}")
        if (isAiTurn || isPaused) {
            Log.e("Chessiq", "handleSquareClick returned because isAiTurn=true or isPaused=true")
            return
        }

        val piece = boardState.getPiece(pos)
        val selected = selectedSquare
        Log.e("Chessiq", "Piece at pos: type=${piece?.type}, color=${piece?.color}")
        Log.e("Chessiq", "Current legalMoves: ${legalMoves.map { it.toAlgebraic() }}")

        if (selected == null) {
            if (piece != null && piece.color == boardState.activeColor) {
                Log.e("Chessiq", "Selecting square: $pos")
                selectedSquare = pos
            } else {
                Log.e("Chessiq", "Ignoring click (selected is null, click piece is null or opponent's)")
            }
        } else {
            if (piece != null && piece.color == boardState.activeColor) {
                Log.e("Chessiq", "Reselecting new square: $pos")
                selectedSquare = pos
            } else {
                val matchedMove = legalMoves.find { it.to == pos }
                Log.e("Chessiq", "Move matching destination $pos: $matchedMove")
                if (matchedMove != null) {
                    val mover = boardState.getPiece(selected)
                    if (mover?.type == PieceType.PAWN && (pos.row == 0 || pos.row == 7)) {
                        Log.e("Chessiq", "Triggering promotion dialog for move: $matchedMove")
                        showPromotionDialog = matchedMove
                    } else {
                        Log.e("Chessiq", "Executing user move: $matchedMove")
                        executeUserMove(matchedMove)
                    }
                } else {
                    Log.e("Chessiq", "No matched move, clearing selection")
                    selectedSquare = null
                }
            }
        }
    }

    // Show AI Coach suggestions in background
    fun getCoachSuggestion() {
        if (isAiTurn) return
        coroutineScope.launch {
            val bestMove = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                ChessEngine.getBestMove(boardState, 3)
            }
            if (bestMove != null) {
                val mover = boardState.getPiece(bestMove.from)
                val moverName = mover?.type?.name?.lowercase() ?: "piece"
                val detail = if (boardState.getPiece(bestMove.to) != null) "capturing at" else "to"
                coachTipText = "Coach Tip: Move $moverName from ${bestMove.from.toAlgebraic()} $detail ${bestMove.to.toAlgebraic()}.\nThis holds the best strategic control in this position."
            }
        }
    }

    // Calculate captured pieces
    val capturedPieces = remember(boardState) {
        boardState.moveHistory.mapNotNull { it.pieceCaptured }
    }
    val piecesCapturedByWhite = capturedPieces.filter { it.color == ChessColor.BLACK }
    val piecesCapturedByBlack = capturedPieces.filter { it.color == ChessColor.WHITE }

    val materialBalance = getMaterialBalance(boardState.board)

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF0F172A)
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B)
                        )
                    )
                )
        ) {
            val isVictory = remember(showGameOverDialog, gameOverTitle, showTimeOutDialog, winnerByTimeOut) {
                (showGameOverDialog && (gameOverTitle.contains("Victory", ignoreCase = true) || gameOverTitle.contains("White Player", ignoreCase = true))) ||
                (showTimeOutDialog && winnerByTimeOut == ChessColor.WHITE)
            }

            ConfettiEffect(isActive = isVictory)

            if (showIqDashboard) {
                IqDashboard(
                    tactics = tacticsScore,
                    strategy = strategyScore,
                    opening = openingScore,
                    endgame = endgameScore,
                    onBack = { showIqDashboard = false }
                )
            } else if (showMatchReview) {
                MatchReviewScreen(
                    boardState = boardState,
                    theme = boardTheme,
                    is3dMode = is3dMode,
                    isRotated = manualRotateBoard,
                    onToggleRotate = { manualRotateBoard = !manualRotateBoard },
                    matchAnalysis = matchAnalysis,
                    reviewMoveIndex = reviewMoveIndex,
                    onReviewMoveIndexChange = { reviewMoveIndex = it },
                    onClose = {
                        showMatchReview = false
                        resetGame()
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!hasTimerStarted || isPaused) {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Chessiq",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (playMode == PlayMode.VS_AI) "Vs AI Opponent" else "Pass & Play (Local 2P)",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Music Toggle Button
                                IconButton(
                                    onClick = { isMusicOn = !isMusicOn },
                                    modifier = Modifier.background(
                                        if (isMusicOn) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                        shape = CircleShape
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isMusicOn) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                        contentDescription = "Toggle Music",
                                        tint = if (isMusicOn) Color(0xFF10B981) else Color.White
                                    )
                                }

                                // 3D Toggle Button
                                IconButton(
                                    onClick = { is3dMode = !is3dMode },
                                    modifier = Modifier.background(
                                        if (is3dMode) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                        shape = CircleShape
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Layers,
                                        contentDescription = "Toggle 3D Mode",
                                        tint = if (is3dMode) Color(0xFF38BDF8) else Color.White
                                    )
                                }

                                // Theme Selector Button
                                IconButton(
                                    onClick = {
                                        val themes = ChessBoardTheme.values()
                                        val nextIndex = (boardTheme.ordinal + 1) % themes.size
                                        boardTheme = themes[nextIndex]
                                    },
                                    modifier = Modifier.background(Color(0xFF1E293B), shape = CircleShape)
                                ) {
                                    Icon(Icons.Default.Palette, contentDescription = "Switch Theme", tint = Color(0xFFF59E0B))
                                }

                                // IQ Dashboard Button
                                IconButton(
                                    onClick = { showIqDashboard = true },
                                    modifier = Modifier.background(Color(0xFF1E293B), shape = CircleShape)
                                ) {
                                    Icon(Icons.Default.Analytics, contentDescription = "IQ Dashboard", tint = Color(0xFF10B981))
                                }
                            }
                        }

                        // Game Mode Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(0xFF1E293B), shape = RoundedCornerShape(20.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            PlayModeButton(
                                title = "vs AI Opponent",
                                isActive = playMode == PlayMode.VS_AI,
                                onClick = { resetGame(PlayMode.VS_AI) },
                                modifier = Modifier.weight(1f)
                            )
                            PlayModeButton(
                                title = "Pass & Play",
                                isActive = playMode == PlayMode.PASS_AND_PLAY,
                                onClick = { resetGame(PlayMode.PASS_AND_PLAY) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Clock Option Chip (Visible before first move)
                    if (!hasTimerStarted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimeConfigDialog = true }
                                .background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = "Select Time",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Time Control",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (selectedTimerMinutes == null) "Unlimited (∞)" else "${selectedTimerMinutes}m" + if (selectedIncrementSeconds > 0) " | +${selectedIncrementSeconds}s" else "",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = "Select Time Control",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Active Timer Status & Pause/Resume (Visible during game)
                    if (hasTimerStarted && selectedTimerMinutes != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(0xFF1E293B), shape = RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Timer Active",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Timer: ${selectedTimerMinutes}m" + if (selectedIncrementSeconds > 0) " | +${selectedIncrementSeconds}s" else "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isPaused = !isPaused }
                                    .background(
                                        if (isPaused) Color(0xFF10B981) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    tint = if (isPaused) Color.Black else Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPaused) "Resume" else "Pause",
                                    color = if (isPaused) Color.Black else Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Top Player HUD (AI or Player 2)
                    if (playMode == PlayMode.VS_AI) {
                        OpponentHud(
                            aiLevel = aiLevel,
                            onLevelChange = { aiLevel = it },
                            capturedPieces = piecesCapturedByBlack,
                            materialBalance = if (materialBalance < 0) -materialBalance else 0,
                            clockContent = {
                                ChessClockDisplay(
                                    timeLeftMs = blackTimeLeft,
                                    isActive = boardState.activeColor == ChessColor.BLACK && hasTimerStarted && !isPaused,
                                    isUnlimited = selectedTimerMinutes == null
                                )
                            }
                        )
                    } else {
                        PassPlayPlayerHud(
                            name = "Black Player",
                            isActive = boardState.activeColor == ChessColor.BLACK,
                            capturedPieces = piecesCapturedByBlack,
                            materialBalance = if (materialBalance < 0) -materialBalance else 0,
                            clockContent = {
                                ChessClockDisplay(
                                    timeLeftMs = blackTimeLeft,
                                    isActive = boardState.activeColor == ChessColor.BLACK && hasTimerStarted && !isPaused,
                                    isUnlimited = selectedTimerMinutes == null
                                )
                            }
                        )
                    }

                    // Chess Board Container
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ChessBoard(
                            boardState = boardState,
                            theme = boardTheme,
                            selectedSquare = selectedSquare,
                            onSquareClick = { pos -> handleSquareClick(pos) },
                            legalMoves = legalMoves,
                            is3dMode = is3dMode,
                            isRotated = isRotated,
                            isPassAndPlay = (playMode == PlayMode.PASS_AND_PLAY),
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isPaused) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.85f))
                                    .clickable(enabled = true, onClick = {}),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PauseCircle,
                                        contentDescription = "Paused",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "GAME PAUSED",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap Resume to play",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    // Settings inside Pause Screen (Focus Mode Accessibility)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 3D Mode Toggle
                                            Button(
                                                onClick = { is3dMode = !is3dMode },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (is3dMode) Color(0xFF38BDF8) else Color(0xFF334155)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Layers,
                                                    contentDescription = "Toggle 3D Mode",
                                                    tint = if (is3dMode) Color(0xFF38BDF8) else Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (is3dMode) "3D On" else "3D Off",
                                                    color = if (is3dMode) Color.Black else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            // Theme Switcher
                                            Button(
                                                onClick = {
                                                    val themes = ChessBoardTheme.values()
                                                    val nextIndex = (boardTheme.ordinal + 1) % themes.size
                                                    boardTheme = themes[nextIndex]
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Palette,
                                                    contentDescription = "Switch Theme",
                                                    tint = Color(0xFFF59E0B)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = boardTheme.themeName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Music Toggle Button
                                        Button(
                                            onClick = { isMusicOn = !isMusicOn },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isMusicOn) Color(0xFF10B981) else Color(0xFF334155)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.width(180.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMusicOn) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                                contentDescription = "Toggle Music",
                                                tint = if (isMusicOn) Color.Black else Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isMusicOn) "Music: On" else "Music: Off",
                                                color = if (isMusicOn) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Move History Log Ticker
                    if (boardState.moveHistory.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "History:",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            
                            val listState = rememberLazyListState()
                            LaunchedEffect(boardState.moveHistory.size) {
                                if (boardState.moveHistory.isNotEmpty()) {
                                    listState.animateScrollToItem(boardState.moveHistory.size - 1)
                                }
                            }
                            
                            LazyRow(
                                state = listState,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
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
                                    
                                    val whiteMove = if (whiteIndex < history.size) formatMoveRecord(history[whiteIndex]) else ""
                                    val blackMove = if (blackIndex < history.size) formatMoveRecord(history[blackIndex]) else ""
                                    
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
                                        Text(
                                            text = whiteMove,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (blackMove.isNotEmpty()) {
                                            Text(
                                                text = blackMove,
                                                color = Color(0xFF38BDF8),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Player HUD (User or Player 1)
                    if (playMode == PlayMode.VS_AI) {
                        PlayerHud(
                            capturedPieces = piecesCapturedByWhite,
                            materialBalance = if (materialBalance > 0) materialBalance else 0,
                            clockContent = {
                                ChessClockDisplay(
                                    timeLeftMs = whiteTimeLeft,
                                    isActive = boardState.activeColor == ChessColor.WHITE && hasTimerStarted && !isPaused,
                                    isUnlimited = selectedTimerMinutes == null
                                )
                            }
                        )
                    } else {
                        PassPlayPlayerHud(
                            name = "White Player",
                            isActive = boardState.activeColor == ChessColor.WHITE,
                            capturedPieces = piecesCapturedByWhite,
                            materialBalance = if (materialBalance > 0) materialBalance else 0,
                            clockContent = {
                                ChessClockDisplay(
                                    timeLeftMs = whiteTimeLeft,
                                    isActive = boardState.activeColor == ChessColor.WHITE && hasTimerStarted && !isPaused,
                                    isUnlimited = selectedTimerMinutes == null
                                )
                            }
                        )
                    }

                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { undoMove() },
                            enabled = !isPaused,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { manualRotateBoard = !manualRotateBoard },
                            enabled = !isPaused,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Rotate Board", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { getCoachSuggestion() },
                            enabled = !isPaused,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1.2f).padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = "AI Coach Tip")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Coach Tip", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    // Collapsible AI Coach panel
                    if (coachTipText != null || lastFeedback != null) {
                        // Collapsible AI Coach panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .padding(bottom = 12.dp)
                                .background(Color(0xFF1E293B).copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(16.dp))
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                item {
                                    Text(
                                        text = "AI COACH FEEDBACK",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }

                                if (coachTipText != null) {
                                    item {
                                        Text(
                                            text = coachTipText ?: "",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else if (lastFeedback != null) {
                                    val fb = lastFeedback!!
                                    item {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Text(
                                                text = fb.category.displayName,
                                                color = Color.Black,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                modifier = Modifier
                                                    .background(
                                                        Color(android.graphics.Color.parseColor(fb.category.colorHex)),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = fb.feedbackText,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Show promotion dialog if needed
        showPromotionDialog?.let { move ->
            PromotionDialog(
                color = boardState.activeColor,
                onSelect = { type ->
                    val finalMove = move.copy(promotionType = type)
                    executeUserMove(finalMove)
                    showPromotionDialog = null
                },
                onDismiss = {
                    showPromotionDialog = null
                    selectedSquare = null
                }
            )
        }

        // Show game over dialog if needed
        if (showGameOverDialog) {
            AlertDialog(
                onDismissRequest = { showGameOverDialog = false },
                title = {
                    Text(
                        text = gameOverTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                containerColor = Color(0xFF1E293B),
                text = {
                    Text(
                        text = gameOverMessage,
                        fontSize = 16.sp,
                        color = Color(0xFFE2E8F0)
                    )
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                showGameOverDialog = false
                                showMatchReview = true
                                reviewMoveIndex = boardState.moveHistory.size - 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                        ) {
                            Text("Analyze Match", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showGameOverDialog = false
                                resetGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Text("Play Again", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGameOverDialog = false }) {
                        Text("Close", color = Color.Gray)
                    }
                }
            )
        }

        // Show time out dialog if needed
        if (showTimeOutDialog) {
            val winnerName = when {
                winnerByTimeOut == ChessColor.WHITE -> if (playMode == PlayMode.VS_AI) "You" else "White Player"
                winnerByTimeOut == ChessColor.BLACK -> if (playMode == PlayMode.VS_AI) "AI Opponent" else "Black Player"
                else -> "Unknown"
            }
            val loserName = if (winnerByTimeOut == ChessColor.WHITE) {
                if (playMode == PlayMode.VS_AI) "AI Opponent" else "Black Player"
            } else {
                if (playMode == PlayMode.VS_AI) "You" else "White Player"
            }
            AlertDialog(
                onDismissRequest = { showTimeOutDialog = false },
                title = {
                    Text(
                        text = "⏳ Time Out",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                containerColor = Color(0xFF1E293B),
                text = {
                    Text(
                        text = "$loserName ran out of time! $winnerName wins the game.",
                        fontSize = 16.sp,
                        color = Color(0xFFE2E8F0)
                    )
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                showTimeOutDialog = false
                                showMatchReview = true
                                reviewMoveIndex = boardState.moveHistory.size - 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                        ) {
                            Text("Analyze Match", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showTimeOutDialog = false
                                resetGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Replay", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimeOutDialog = false }) {
                        Text("Close", color = Color.Gray)
                    }
                }
            )
        }

        // Show time config dialog if needed
        if (showTimeConfigDialog) {
            AlertDialog(
                onDismissRequest = { showTimeConfigDialog = false },
                title = {
                    Text(
                        text = "🕒 Select Time Control",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Duration",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val timeOptions = listOf(
                            Pair("1m", 1),
                            Pair("3m", 3),
                            Pair("5m", 5),
                            Pair("10m", 10),
                            Pair("15m", 15),
                            Pair("∞", null)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (chunk in timeOptions.chunked(3)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    chunk.forEach { (label, mins) ->
                                        val selected = selectedTimerMinutes == mins
                                        val bg = if (selected) Color(0xFF10B981) else Color(0xFF334155)
                                        val textCol = if (selected) Color.Black else Color.White
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .background(bg, shape = RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedTimerMinutes = mins
                                                    val initTime = if (mins != null) mins * 60 * 1000L else 0L
                                                    whiteTimeLeft = initTime
                                                    blackTimeLeft = initTime
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = textCol,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (selectedTimerMinutes != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Increment per move",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val incrementOptions = listOf(0, 2, 5)
                                incrementOptions.forEach { secs ->
                                    val selected = selectedIncrementSeconds == secs
                                    val label = if (secs == 0) "None" else "+${secs}s"
                                    val bg = if (selected) Color(0xFF10B981) else Color(0xFF334155)
                                    val textCol = if (selected) Color.Black else Color.White
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .background(bg, shape = RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedIncrementSeconds = secs
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = textCol,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showTimeConfigDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
      }
    }
}

@Composable
fun PlayModeButton(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isActive) Color(0xFF38BDF8) else Color.Transparent
    val textCol = if (isActive) Color.Black else Color(0xFF94A3B8)
    val weight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bg, shape = RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textCol,
            fontSize = 13.sp,
            fontWeight = weight
        )
    }
}

@Composable
fun PassPlayPlayerHud(
    name: String,
    isActive: Boolean,
    capturedPieces: List<Piece>,
    materialBalance: Int,
    clockContent: @Composable () -> Unit = {}
) {
    val outlineCol = if (isActive) Color(0xFF38BDF8) else Color(0xFF334155)
    val borderStroke = if (isActive) 1.5.dp else 1.dp
    val bgAlpha = if (isActive) 0.8f else 0.4f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B).copy(alpha = bgAlpha), shape = RoundedCornerShape(12.dp))
            .border(borderStroke, outlineCol, shape = RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Turn",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .background(Color(0xFF38BDF8), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    capturedPieces.forEach { p ->
                        Text(
                            text = getPieceSymbol(p.type),
                            color = if (p.color == ChessColor.WHITE) Color.White else Color(0xFF1E1E1E),
                            fontSize = 16.sp
                        )
                    }
                    if (materialBalance > 0) {
                        Text(
                            text = "+$materialBalance",
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            clockContent()
        }
    }
}

@Composable
fun OpponentHud(
    aiLevel: Int,
    onLevelChange: (Int) -> Unit,
    capturedPieces: List<Piece>,
    materialBalance: Int,
    clockContent: @Composable () -> Unit = {}
) {
    val elos = listOf(400, 800, 1200, 1800, 2500)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B).copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Stockfish AI (Level $aiLevel)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ELO ${elos[aiLevel - 1]}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(Color(0xFF334155), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (aiLevel > 1) onLevelChange(aiLevel - 1) },
                        modifier = Modifier.size(28.dp).background(Color(0xFF334155), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Level", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = aiLevel.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { if (aiLevel < 5) onLevelChange(aiLevel + 1) },
                        modifier = Modifier.size(28.dp).background(Color(0xFF334155), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Level", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    capturedPieces.forEach { p ->
                        Text(
                            text = getPieceSymbol(p.type),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    if (materialBalance > 0) {
                        Text(
                            text = "+$materialBalance",
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                clockContent()
            }
        }
    }
}

@Composable
fun PlayerHud(
    capturedPieces: List<Piece>,
    materialBalance: Int,
    clockContent: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B).copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "You",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Player",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF0369A1), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    capturedPieces.forEach { p ->
                        Text(
                            text = getPieceSymbol(p.type),
                            color = Color(0xFF1E1E1E),
                            fontSize = 16.sp
                        )
                    }
                    if (materialBalance > 0) {
                        Text(
                            text = "+$materialBalance",
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            clockContent()
        }
    }
}

@Composable
fun ChessClockDisplay(
    timeLeftMs: Long,
    isActive: Boolean,
    isUnlimited: Boolean,
    modifier: Modifier = Modifier
) {
    if (isUnlimited) {
        Box(
            modifier = modifier
                .background(Color(0xFF334155).copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "∞",
                color = Color(0xFF94A3B8),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val isWarning = timeLeftMs < 10000 && isActive
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by if (isWarning) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val warningAlpha by if (isWarning) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "warningAlpha"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val bg = when {
        isWarning -> Color(0xFFEF4444).copy(alpha = warningAlpha) // Red flashing/pulsing
        isActive -> Color(0xFF10B981) // Emerald Green for active
        else -> Color(0xFF334155).copy(alpha = 0.5f) // Dark slate for inactive
    }

    val textCol = when {
        isActive -> Color.Black // Dark text on bright active clock for high contrast
        else -> Color.White
    }

    val formattedTime = remember(timeLeftMs) {
        val totalSeconds = timeLeftMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val tenths = (timeLeftMs % 1000) / 100
        if (timeLeftMs < 10000) {
            String.format("%d.%d", seconds, tenths)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .background(bg, shape = RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (isActive && !isWarning) Color(0xFF10B981) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formattedTime,
            color = textCol,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

fun getPieceSymbol(type: PieceType): String {
    return when (type) {
        PieceType.KING -> "♚"
        PieceType.QUEEN -> "♛"
        PieceType.ROOK -> "♜"
        PieceType.BISHOP -> "♝"
        PieceType.KNIGHT -> "♞"
        PieceType.PAWN -> "♟"
    }
}

fun getMaterialBalance(board: List<List<Piece?>>): Int {
    var whiteVal = 0
    var blackVal = 0
    for (r in 0..7) {
        for (c in 0..7) {
            val p = board[r][c] ?: continue
            val v = when (p.type) {
                PieceType.PAWN -> 1
                PieceType.KNIGHT -> 3
                PieceType.BISHOP -> 3
                PieceType.ROOK -> 5
                PieceType.QUEEN -> 9
                PieceType.KING -> 0
            }
            if (p.color == ChessColor.WHITE) whiteVal += v else blackVal += v
        }
    }
    return whiteVal - blackVal
}

@Composable
fun IqDashboard(
    tactics: Int,
    strategy: Int,
    opening: Int,
    endgame: Int,
    onBack: () -> Unit
) {
    val overallIq = ((tactics + strategy + opening + endgame) / 4.0).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color(0xFF1E293B), shape = CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Chessiq Score System",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(24.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OVERALL CHESSIQ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.sweepGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF38BDF8))
                            ),
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .background(Color(0xFF0F172A), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = overallIq.toString(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = getIqDescription(overallIq),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SKILL SUB-SCORES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        val subScores = listOf(
            Triple("Tactics", tactics, Color(0xFFF43F5E)),
            Triple("Strategy", strategy, Color(0xFF10B981)),
            Triple("Opening Theory", opening, Color(0xFFF59E0B)),
            Triple("Endgame", endgame, Color(0xFF38BDF8))
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(subScores) { (title, score, color) ->
                IqBar(title = title, score = score, barColor = color)
            }
        }
    }
}

@Composable
fun IqBar(title: String, score: Int, barColor: Color) {
    val progress = (score / 200.5f).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = score.toString(), color = barColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFF334155), shape = RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(barColor, shape = RoundedCornerShape(5.dp))
            )
        }
    }
}

fun getIqDescription(iq: Int): String {
    return when {
        iq >= 160 -> "Grandmaster Level. Brilliant tactical vision and near-flawless strategy."
        iq >= 130 -> "Master Level. Strong positional understanding and sharp calculations."
        iq >= 110 -> "Candidate Master. Developed tactical awareness and solid opening play."
        iq >= 90 -> "Intermediate. Steady progress with standard board rules."
        else -> "Beginner. Start solving more tactical puzzles and secure your King early."
    }
}

fun android.content.Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return currentContext as? Activity
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    
    DisposableEffect(activity) {
        activity?.window?.let { window ->
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            // Hide status bars and navigation bars
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Configure behavior to swipe to reveal temporarily
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        onDispose {
            activity?.window?.let { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                // Restore status bars and navigation bars
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200) // Show logo for 2.2 seconds
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF062A11)), // Dark green matching the logo background
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = com.example.chessiq.R.drawable.chessiq_splash),
            contentDescription = "Chessiq Splash Logo",
            contentScale = ContentScale.Crop, // Crop to cover the screen
            modifier = Modifier.fillMaxSize()
        )
        
        CircularProgressIndicator(
            color = Color(0xFF38BDF8),
            strokeWidth = 3.dp,
            modifier = Modifier
                .padding(bottom = 64.dp)
                .size(36.dp)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ConfettiEffect(isActive: Boolean) {
    if (!isActive) return
    
    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    val particles = remember {
        List(80) {
            val x = Math.random().toFloat()
            val yStart = -Math.random().toFloat() * 500f
            val speed = (100f + Math.random().toFloat() * 150f)
            val color = when ((0..4).random()) {
                0 -> Color(0xFFF59E0B) // Gold
                1 -> Color(0xFF10B981) // Emerald Green
                2 -> Color(0xFF38BDF8) // Sky Blue
                3 -> Color(0xFFEC4899) // Pink
                else -> Color(0xFF8B5CF6) // Purple
            }
            val size = (8..18).random().dp
            val rotationSpeed = (90f + Math.random().toFloat() * 270f)
            Triple(x, speed, Pair(color, Pair(size, rotationSpeed)))
        }
    }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth.value
        val height = maxHeight.value
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { (xRatio, speed, data) ->
                val (color, sizeData) = data
                val (size, rotationSpeed) = sizeData
                
                val startY = -size.toPx()
                val currentY = startY + (progress * 4000f * (speed / 150f))
                
                val finalY = currentY % (height + 100f)
                val finalX = xRatio * width
                
                val rotation = progress * rotationSpeed
                
                rotate(rotation, pivot = Offset(finalX, finalY)) {
                    drawRect(
                        color = color,
                        topLeft = Offset(finalX - size.toPx() / 2, finalY - size.toPx() / 2),
                        size = androidx.compose.ui.geometry.Size(size.toPx(), size.toPx() * 0.6f)
                    )
                }
            }
        }
    }
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



