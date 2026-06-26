package com.elementfriends.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

object SoundSynth {
    private var bgmTrack: AudioTrack? = null
    var isAudioEnabled = true
        private set
    private var appContext: android.content.Context? = null

    fun initialize(context: android.content.Context) {
        val firstInit = (appContext == null)
        if (firstInit) {
            appContext = context.applicationContext
            // Prepare BGM track with correct context
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    bgmTrack?.let { track ->
                        track.stop()
                        track.release()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                bgmTrack = null
                prepareBgmTrack()
            }
        }
    }

    /**
     * Toggles BGM and all sound effects.
     */
    fun toggleAudio() {
        isAudioEnabled = !isAudioEnabled
        if (!isAudioEnabled) {
            stopBgm()
        } else {
            startBgm()
        }
    }

    /**
     * Starts playing the gentle ambient background music.
     */
    fun startBgm() {
        if (!isAudioEnabled) return
        try {
            if (bgmTrack == null) {
                prepareBgmTrack()
            }
            bgmTrack?.let { track ->
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Pauses the background music.
     */
    fun stopBgm() {
        try {
            bgmTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Synthesizes and pre-loads a gorgeous 9.6-second ambient chill/pentatonic loop
     * into a static looping AudioTrack. Minimal memory, zero CPU overhead during looping.
     */
    private fun prepareBgmTrack() {
        try {
            val sampleRate = 22050
            val durationSec = 9.6
            val numSamples = (sampleRate * durationSec).toInt()
            val sample = DoubleArray(numSamples)
            val buffer = ShortArray(numSamples)

            // Function to add a gentle bell note with warm harmonic synthesis & exponential decay
            fun addNote(startTime: Double, freq: Double, duration: Double, volume: Double) {
                val startIdx = (startTime * sampleRate).toInt()
                val numNoteSamples = (duration * sampleRate).toInt()
                for (step in 0 until numNoteSamples) {
                    val idx = startIdx + step
                    if (idx >= numSamples) break
                    val t = step.toDouble() / sampleRate
                    
                    // Cute chime envelope (attack and fast decay)
                    val attack = 0.04
                    val envelope = when {
                        t < attack -> (t / attack)
                        else -> Math.max(0.0, Math.exp(-(t - attack) * 3.5))
                    }
                    
                    // Dynamic additive bell sound (fundamental + 2nd harmonic + 3nd harmonic)
                    val signal = sin(2 * Math.PI * freq * t) + 
                                 0.3 * sin(2 * Math.PI * (freq * 2) * t) * Math.exp(-t * 9.0) +
                                 0.15 * sin(2 * Math.PI * (freq * 3) * t) * Math.exp(-t * 14.0)
                    sample[idx] += signal * envelope * volume
                }
            }

            // --- Gentle Lullaby Melody ---
            // Bar 1
            addNote(0.0, 523.25, 1.8, 0.08)  // C5
            addNote(0.6, 659.25, 1.8, 0.06)  // E5
            addNote(1.2, 783.99, 1.8, 0.06)  // G5
            addNote(1.8, 880.00, 1.8, 0.06)  // A5
            // Bar 2
            addNote(2.4, 1046.50, 2.0, 0.08) // C6
            addNote(3.0, 880.00, 1.8, 0.06)  // A5
            addNote(3.6, 783.99, 1.8, 0.06)  // G5
            addNote(4.2, 659.25, 1.8, 0.06)  // E5
            // Bar 3
            addNote(4.8, 698.46, 1.8, 0.08)  // F5
            addNote(5.4, 880.00, 1.8, 0.06)  // A5
            addNote(6.0, 1046.50, 1.8, 0.06) // C6
            addNote(6.6, 1174.66, 1.8, 0.06) // D6
            // Bar 4
            addNote(7.2, 783.99, 2.0, 0.08)  // G5
            addNote(7.8, 659.25, 1.8, 0.06)  // E5
            addNote(8.4, 587.33, 1.8, 0.06)  // D5
            addNote(9.0, 523.25, 1.8, 0.06)  // C5

            // --- Ambient Bass Pad Accompaniment ---
            addNote(0.0, 130.81, 2.4, 0.05)  // C3
            addNote(2.4, 174.61, 2.4, 0.05)  // F3
            addNote(4.8, 196.00, 2.4, 0.05)  // G3
            addNote(7.2, 130.81, 2.4, 0.05)  // C3

            // Convert and clamp float samples to signed 16-bit PCM buffer
            for (i in 0 until numSamples) {
                val value = sample[i]
                val clamped = Math.max(-1.0, Math.min(1.0, value))
                buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
            }

            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                appContext?.let { builder.setContext(it) }
            }
            bgmTrack = builder.build()

            bgmTrack?.let { track ->
                track.write(buffer, 0, buffer.size)
                track.setLoopPoints(0, buffer.size, -1) // Loop forever
                if (isAudioEnabled) {
                    track.play()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Synthesizes and plays a delightful high-pitch bubble tactile pop effect.
     */
    suspend fun playPopSound() = withContext(Dispatchers.Default) {
        if (!isAudioEnabled) return@withContext
        try {
            val sampleRate = 22050
            val durationMs = 120
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = DoubleArray(numSamples)
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                // Sweet cartoon bubble sweep (180Hz -> 650Hz)
                val freq = 180.0 + progress * 470.0
                // Smooth tactile balloon-pop envelope
                val volumeFade = if (progress < 0.15) progress / 0.15 else Math.exp(-(progress - 0.15) * 10.0)
                sample[i] = sin(2 * Math.PI * i * freq / sampleRate) * volumeFade * 0.35
            }

            for (i in 0 until numSamples) {
                buffer[i] = (sample[i] * Short.MAX_VALUE).toInt().toShort()
            }

            val audioTrack = createSonificationTrack(sampleRate, buffer.size * 2)
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            kotlinx.coroutines.delay(durationMs.toLong() + 20)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Plays the combination sound: Sparkly chords for success, low warm cartoon spring for failure.
     */
    suspend fun playCombineSound(success: Boolean) = withContext(Dispatchers.Default) {
        if (!isAudioEnabled) return@withContext
        try {
            val sampleRate = 22050
            val durationMs = if (success) 400 else 250
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = DoubleArray(numSamples)
            val buffer = ShortArray(numSamples)

            if (success) {
                // Success: Play a beautiful major triad sweep (C5 -> E5 -> G5) overlapping sparkling design
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var signal = 0.0
                    
                    // C5 (523.25 Hz) Sparkle
                    val envC = Math.max(0.0, Math.exp(-t * 8.0))
                    signal += sin(2 * Math.PI * 523.25 * t) * envC * 0.25
                    
                    // E5 (659.25 Hz) starts 80ms later
                    if (t >= 0.08) {
                        val envE = Math.max(0.0, Math.exp(-(t - 0.08) * 8.0))
                        signal += sin(2 * Math.PI * 659.25 * (t - 0.08)) * envE * 0.25
                    }
                    
                    // G5 (783.99 Hz) starts 160ms later
                    if (t >= 0.16) {
                        val envG = Math.max(0.0, Math.exp(-(t - 0.16) * 6.0))
                        signal += sin(2 * Math.PI * 783.99 * (t - 0.16)) * envG * 0.25
                    }

                    // Sparkle harmonic overlay (high C6 chime)
                    if (t >= 0.24) {
                        val envHigh = Math.max(0.0, Math.exp(-(t - 0.24) * 12.0))
                        signal += sin(2 * Math.PI * 1046.50 * (t - 0.24)) * envHigh * 0.15
                    }

                    sample[i] = signal
                }
            } else {
                // Gentle cartoon elastic bounce (no scary noises, cute springy bounce)
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = 200.0 - sin(progress * Math.PI) * 40.0 // Pitch-bends down then up slightly
                    val volumeFade = if (progress > 0.8) (1.0 - progress) / 0.2 else 1.0
                    sample[i] = sin(2 * Math.PI * i * freq / sampleRate) * volumeFade * 0.3
                }
            }

            for (i in 0 until numSamples) {
                buffer[i] = (sample[i] * Short.MAX_VALUE).toInt().toShort()
            }

            val audioTrack = createSonificationTrack(sampleRate, buffer.size * 2)
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            kotlinx.coroutines.delay(durationMs.toLong() + 30)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Synthesizes and plays a glorious rewarding 4-note bell chime arpeggio (C5 -> E5 -> G5 -> C6).
     */
    suspend fun playLevelUpSound() = withContext(Dispatchers.Default) {
        if (!isAudioEnabled) return@withContext
        try {
            val sampleRate = 22050
            val durationMs = 900
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = DoubleArray(numSamples)
            val buffer = ShortArray(numSamples)

            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            for (step in 0 until numSamples) {
                val t = step.toDouble() / sampleRate
                var totalSound = 0.0
                for (n in notes.indices) {
                    val noteStartTime = n * 0.14
                    if (t >= noteStartTime) {
                        val noteT = t - noteStartTime
                        val env = Math.max(0.0, Math.exp(-noteT * 4.5))
                        val freq = notes[n]
                        val bellSound = sin(2 * Math.PI * freq * noteT) + 
                                        0.35 * sin(2 * Math.PI * (freq * 2) * noteT) * Math.exp(-noteT * 9.0)
                        totalSound += bellSound * env * 0.22
                    }
                }
                sample[step] = totalSound
            }

            for (i in 0 until numSamples) {
                buffer[i] = (clampedVal(sample[i]) * Short.MAX_VALUE).toInt().toShort()
            }

            val audioTrack = createSonificationTrack(sampleRate, buffer.size * 2)
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            kotlinx.coroutines.delay(durationMs.toLong() + 40)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clampedVal(value: Double): Double {
        return Math.max(-1.0, Math.min(1.0, value))
    }

    private fun createSonificationTrack(sampleRate: Int, bufferSizeInBytes: Int): AudioTrack {
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeInBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            appContext?.let { builder.setContext(it) }
        }
        return builder.build()
    }
}
