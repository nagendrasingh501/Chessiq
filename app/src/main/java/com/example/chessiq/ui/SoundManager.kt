package com.example.chessiq.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread

object SoundManager {
    fun playTriumphFanfare() {
        // play C5, E5, G5, C6
        playMelody(
            listOf(
                Pair(523.25, 150), // C5
                Pair(659.25, 150), // E5
                Pair(783.99, 150), // G5
                Pair(1046.50, 450) // C6
            )
        )
    }

    fun playDefeatMelody() {
        // play G4, E4, C4, B3
        playMelody(
            listOf(
                Pair(392.00, 200), // G4
                Pair(329.63, 200), // E4
                Pair(261.63, 200), // C4
                Pair(246.94, 500)  // B3
            )
        )
    }

    fun playDrawChime() {
        // play G4, D4, G4
        playMelody(
            listOf(
                Pair(392.00, 250), // G4
                Pair(293.66, 250), // D4
                Pair(392.00, 400)  // G4
            )
        )
    }

    fun playMoveSound() {
        val sampleRate = 44100
        val durationMs = 60
        val totalSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(totalSamples)
        
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 900.0
            val decay = Math.exp(-45.0 * t)
            val wave = Math.sin(2.0 * Math.PI * freq * t) * decay
            
            val mixed = wave * 16384
            buffer[i] = mixed.coerceIn(-32768.0, 32767.0).toInt().toShort()
        }
        playRawBuffer(buffer)
    }

    fun playCaptureSound() {
        val sampleRate = 44100
        val durationMs = 120
        val totalSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(totalSamples)
        
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            
            // Wood click: high frequency sine wave with rapid decay
            val clickFreq = 1100.0
            val clickDecay = Math.exp(-35.0 * t)
            val clickWave = Math.sin(2.0 * Math.PI * clickFreq * t) * clickDecay
            
            // Impact thump: low frequency sine wave with medium decay
            val thumpFreq = 180.0
            val thumpDecay = Math.exp(-15.0 * t)
            val thumpWave = Math.sin(2.0 * Math.PI * thumpFreq * t) * thumpDecay
            
            // Add a tiny bit of noise for impact friction
            val noise = (Math.random() * 2.0 - 1.0) * Math.exp(-40.0 * t) * 0.2
            
            val mixed = (clickWave * 0.6 + thumpWave * 0.4 + noise) * 16384
            buffer[i] = mixed.coerceIn(-32768.0, 32767.0).toInt().toShort()
        }
        playRawBuffer(buffer)
    }

    private fun playRawBuffer(buffer: ShortArray) {
        val sampleRate = 44100
        try {
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer.size * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            
            // Release the AudioTrack after it has finished playing.
            Thread {
                try {
                    val durationMs = (buffer.size * 1000L) / sampleRate
                    Thread.sleep(durationMs + 100)
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playMelody(notes: List<Pair<Double, Int>>) {
        val sampleRate = 44100
        val totalDurationMs = notes.sumOf { it.second }
        val totalSamples = (totalDurationMs * sampleRate) / 1000
        val buffer = ShortArray(totalSamples)

        var currentSample = 0
        for ((freq, durationMs) in notes) {
            val noteSamples = (durationMs * sampleRate) / 1000
            for (i in 0 until noteSamples) {
                if (currentSample >= totalSamples) break
                // Sine wave synthesis
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * freq * t
                
                // Add fade out at the end of each note to prevent clicking sound
                // Use a much quicker decay envelope for move sounds (short duration)
                val decayFactor = if (durationMs <= 50) 0.35 else 0.85
                val fadeOutThreshold = (noteSamples * decayFactor).toInt()
                val volume = if (i > fadeOutThreshold) {
                    val fadeProgress = (i - fadeOutThreshold).toDouble() / (noteSamples - fadeOutThreshold)
                    1.0 - fadeProgress
                } else {
                    1.0
                }

                buffer[currentSample] = (Math.sin(angle) * 16384 * volume).toInt().toShort()
                currentSample++
            }
        }

        playRawBuffer(buffer)
    }

    fun playCheckSound() {
        // play E5, B5 in rapid succession
        playMelody(
            listOf(
                Pair(659.25, 80),   // E5
                Pair(987.77, 250)   // B5
            )
        )
    }

    private var musicThread: Thread? = null
    @Volatile
    private var isMusicRunning = false

    fun startBackgroundMusic() {
        synchronized(this) {
            if (isMusicRunning) return
            isMusicRunning = true
            musicThread = thread(start = true, name = "ChessiqMusicThread") {
                runProceduralMusic()
            }
        }
    }

    fun stopBackgroundMusic() {
        synchronized(this) {
            isMusicRunning = false
            musicThread?.interrupt()
            musicThread = null
        }
    }

    private fun runProceduralMusic() {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = try {
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize.coerceAtLeast(4096),
                AudioTrack.MODE_STREAM
            )
        } catch (e: Exception) {
            return
        }

        try {
            audioTrack.play()
        } catch (e: Exception) {
            audioTrack.release()
            return
        }

        // Frequencies for chords: Am7, Fmaj7, Cmaj7, G6
        val chords = listOf(
            listOf(220.0, 261.63, 329.63, 392.0),   // Am7 (A3, C4, E4, G4)
            listOf(174.61, 220.0, 261.63, 329.63),  // Fmaj7 (F3, A3, C4, E4)
            listOf(130.81, 164.81, 196.0, 246.94),  // Cmaj7 (C3, E3, G3, B3)
            listOf(196.0, 246.94, 293.66, 329.63)   // G6 (G3, B3, D4, E4)
        )

        // Pentatonic notes for ambient chimes (C5, D5, E5, G5, A5, C6)
        val pentatonic = listOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.50)

        val writeBuffer = ShortArray(2048)
        var sampleIndex = 0L

        val chordDurationSec = 6.0
        val transitionDurationSec = 2.0
        val samplesPerChord = (chordDurationSec * sampleRate).toLong()

        // Random chime state
        var chimeFreq = 0.0
        var chimeStartSample = -1L
        val chimeDurationSamples = (4.0 * sampleRate).toLong() // 4 seconds chime duration

        while (isMusicRunning && !Thread.currentThread().isInterrupted) {
            for (i in writeBuffer.indices) {
                val t = sampleIndex.toDouble() / sampleRate
                
                // Determine chord crossfade
                val chordCycleProgress = (sampleIndex % samplesPerChord).toDouble() / sampleRate
                val chordIndex = ((sampleIndex / samplesPerChord) % chords.size).toInt()
                val nextChordIndex = (chordIndex + 1) % chords.size
                
                // Read current and next chord frequencies
                val currentFreqs = chords[chordIndex]
                val nextFreqs = chords[nextChordIndex]
                
                // Calculate current chord pad mix
                var currentChordMix = 0.0
                for (f in currentFreqs) {
                    // Detuned chorus + main frequency
                    val mainWave = Math.sin(2.0 * Math.PI * f * t)
                    val detunedWave = Math.sin(2.0 * Math.PI * f * 1.003 * t)
                    val subWave = Math.sin(2.0 * Math.PI * (f * 0.5) * t) * 0.4
                    currentChordMix += (mainWave * 0.6 + detunedWave * 0.4 + subWave)
                }
                currentChordMix /= currentFreqs.size
                
                // Calculate next chord pad mix
                var nextChordMix = 0.0
                for (f in nextFreqs) {
                    val mainWave = Math.sin(2.0 * Math.PI * f * t)
                    val detunedWave = Math.sin(2.0 * Math.PI * f * 1.003 * t)
                    val subWave = Math.sin(2.0 * Math.PI * (f * 0.5) * t) * 0.4
                    nextChordMix += (mainWave * 0.6 + detunedWave * 0.4 + subWave)
                }
                nextChordMix /= nextFreqs.size

                // Crossfade calculation (linear crossfade at the end of the chord cycle)
                val padWave: Double
                if (chordCycleProgress > (chordDurationSec - transitionDurationSec)) {
                    val fadeTime = chordCycleProgress - (chordDurationSec - transitionDurationSec)
                    val nextRatio = fadeTime / transitionDurationSec
                    val currentRatio = 1.0 - nextRatio
                    padWave = currentChordMix * currentRatio + nextChordMix * nextRatio
                } else {
                    padWave = currentChordMix
                }

                // Smooth swell/LFO (slow modulation of the overall pad volume)
                val padVolumeMod = 0.6 + 0.4 * Math.sin(2.0 * Math.PI * t / 12.0)
                var finalWave = padWave * 0.3 * padVolumeMod

                // Random chimes
                if (chimeStartSample == -1L) {
                    if (Math.random() < 0.000015) { // Roughly once every 1.5 seconds average
                        chimeFreq = pentatonic.random()
                        chimeStartSample = sampleIndex
                    }
                } else {
                    val elapsedChimeSamples = sampleIndex - chimeStartSample
                    if (elapsedChimeSamples >= chimeDurationSamples) {
                        chimeStartSample = -1L // Finished chime
                    } else {
                        val chimeT = elapsedChimeSamples.toDouble() / sampleRate
                        // Rapid attack (0.02s) and slow exponential decay
                        val envelope = if (chimeT < 0.02) {
                            chimeT / 0.02
                        } else {
                            Math.exp(-2.2 * (chimeT - 0.02))
                        }
                        
                        val chimeWave = Math.sin(2.0 * Math.PI * chimeFreq * chimeT)
                        val echoWave = Math.sin(2.0 * Math.PI * chimeFreq * (chimeT - 0.15)) * 0.4
                        finalWave += (chimeWave + echoWave) * 0.18 * envelope
                    }
                }

                // Write to buffer
                val pcmValue = (finalWave * 16384).coerceIn(-32768.0, 32767.0).toInt().toShort()
                writeBuffer[i] = pcmValue
                sampleIndex++
            }

            audioTrack.write(writeBuffer, 0, writeBuffer.size)
        }

        try {
            audioTrack.stop()
        } catch (e: Exception) {}
        try {
            audioTrack.release()
        } catch (e: Exception) {}
    }
}

