package com.example.chessiq

import com.example.chessiq.engine.MoveGenerator
import com.example.chessiq.model.BoardState
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun testStartMoves() {
        val state = BoardState.startPosition()
        val moves = MoveGenerator.generateLegalMoves(state)
        System.out.println("Start position moves count: ${moves.size}")
        for (m in moves) {
            System.out.println("Move: ${m.toAlgebraic()}")
        }
        assertTrue(moves.isNotEmpty())
    }
}