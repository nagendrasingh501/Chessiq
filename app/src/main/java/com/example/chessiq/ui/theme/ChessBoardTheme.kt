package com.example.chessiq.ui.theme

import androidx.compose.ui.graphics.Color

enum class ChessBoardTheme(
    val themeName: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val selectedSquare: Color,
    val legalMoveDot: Color,
    val legalMoveCapture: Color,
    val lastMoveHighlight: Color,
    val checkHighlight: Color
) {
    GREEN_CLASSIC(
        themeName = "Green Classic",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF769656),
        selectedSquare = Color(0x90F7F785),
        legalMoveDot = Color(0x401B5E20),
        legalMoveCapture = Color(0x60D32F2F),
        lastMoveHighlight = Color(0x60BACA44),
        checkHighlight = Color(0x80D32F2F)
    ),
    NEON(
        themeName = "Neon Cyberpunk",
        lightSquare = Color(0xFF1E1E2F),
        darkSquare = Color(0xFF0D0D15),
        selectedSquare = Color(0x8000FFFF),
        legalMoveDot = Color(0xFF00FFCC),
        legalMoveCapture = Color(0xFFFF0055),
        lastMoveHighlight = Color(0x60FF00FF),
        checkHighlight = Color(0x80FF0055)
    ),
    DARK_MODE(
        themeName = "Dark Slate",
        lightSquare = Color(0xFF334155),
        darkSquare = Color(0xFF1E293B),
        selectedSquare = Color(0x8094A3B8),
        legalMoveDot = Color(0x6038BDF8),
        legalMoveCapture = Color(0x80EF4444),
        lastMoveHighlight = Color(0x60475569),
        checkHighlight = Color(0x80F43F5E)
    ),
    WOOD(
        themeName = "Warm Wood",
        lightSquare = Color(0xFFF0D9B5),
        darkSquare = Color(0xFFB58863),
        selectedSquare = Color(0x80D7CCC8),
        legalMoveDot = Color(0x403E2723),
        legalMoveCapture = Color(0x60D84315),
        lastMoveHighlight = Color(0x608D6E63),
        checkHighlight = Color(0x80C62828)
    ),
    GALAXY(
        themeName = "Galaxy Nebula",
        lightSquare = Color(0xFF1E1B4B),
        darkSquare = Color(0xFF0F0B26),
        selectedSquare = Color(0x80A855F7),
        legalMoveDot = Color(0xFFC084FC),
        legalMoveCapture = Color(0xFFF43F5E),
        lastMoveHighlight = Color(0x606366F1),
        checkHighlight = Color(0x80EC4899)
    )

}
