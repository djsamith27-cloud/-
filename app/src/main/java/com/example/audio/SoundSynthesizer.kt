package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class SfxType {
    STEP, SWING, CHOP, MINE, BERRY, DRINK, CRAFT, WOLF, HURT
}

class SoundSynthesizer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050
    private val activeTracks = AtomicInteger(0)

    @Volatile
    var isMuted: Boolean = false

    fun play(type: SfxType) {
        if (isMuted) return
        if (activeTracks.get() >= 6) return
        scope.launch {
            if (activeTracks.incrementAndGet() > 6) {
                activeTracks.decrementAndGet()
                return@launch
            }
            var track: AudioTrack? = null
            try {
                val samples = synthesize(type)
                track = playPcm(samples)
                val durationMs = (samples.size * 1000L / sampleRate) + 80L
                delay(durationMs)
            } catch (_: Throwable) {
                // Ignore audio hardware failures on headless or muted emulators
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
                activeTracks.decrementAndGet()
            }
        }
    }

    private fun synthesize(type: SfxType): ShortArray {
        return when (type) {
            SfxType.STEP -> {
                val f0 = 95.0 + Random.nextDouble() * 25.0
                generateTone(0.08, f0, 35.0, 0.14, Wave.TRIANGLE)
            }
            SfxType.SWING -> generateTone(0.14, 340.0, 95.0, 0.22, Wave.SINE)
            SfxType.CHOP -> generateTone(0.14, 145.0, 42.0, 0.30, Wave.SQUARE)
            SfxType.MINE -> generateTone(0.16, 980.0, 420.0, 0.26, Wave.SINE)
            SfxType.BERRY -> generateTone(0.12, 480.0, 790.0, 0.22, Wave.SINE)
            SfxType.DRINK -> generateTone(0.20, 260.0, 460.0, 0.24, Wave.TRIANGLE, linearRamp = true)
            SfxType.CRAFT -> generateCraftChime()
            SfxType.WOLF -> generateWolfHowl()
            SfxType.HURT -> generateTone(0.26, 115.0, 48.0, 0.34, Wave.SAWTOOTH, linearRamp = true)
        }
    }

    private enum class Wave { SINE, TRIANGLE, SQUARE, SAWTOOTH }

    private fun sampleWave(phase: Double, wave: Wave): Double {
        val p = phase - kotlin.math.floor(phase)
        return when (wave) {
            Wave.SINE -> sin(p * 2.0 * PI)
            Wave.TRIANGLE -> 1.0 - 4.0 * kotlin.math.abs(p - 0.5)
            Wave.SQUARE -> if (p < 0.5) 1.0 else -1.0
            Wave.SAWTOOTH -> 2.0 * p - 1.0
        }
    }

    private fun generateTone(
        durationSec: Double,
        startFreq: Double,
        endFreq: Double,
        peakGain: Double,
        wave: Wave,
        linearRamp: Boolean = false
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = if (linearRamp) {
                startFreq + (endFreq - startFreq) * t
            } else {
                startFreq * (endFreq / startFreq).pow(t)
            }
            phase += freq / sampleRate
            val env = peakGain * exp(-3.4 * t) * (1.0 - t)
            val s = (sampleWave(phase, wave) * env).coerceIn(-1.0, 1.0)
            out[i] = (s * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun generateCraftChime(): ShortArray {
        val durationSec = 0.29
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val sec = i.toDouble() / sampleRate
            val t = i.toDouble() / count
            val freq = when {
                sec < 0.08 -> 440.0
                sec < 0.16 -> 554.0
                else -> 659.0
            }
            phase += freq / sampleRate
            val env = 0.26 * exp(-2.8 * t) * (1.0 - t)
            val s = (sampleWave(phase, Wave.TRIANGLE) * env).coerceIn(-1.0, 1.0)
            out[i] = (s * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun generateWolfHowl(): ShortArray {
        val durationSec = 0.46
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val sec = i.toDouble() / sampleRate
            val t = i.toDouble() / count
            val freq = if (sec < 0.14) {
                175.0 + (220.0 - 175.0) * (sec / 0.14)
            } else {
                val u = (sec - 0.14) / (durationSec - 0.14)
                220.0 * (80.0 / 220.0).pow(u)
            }
            phase += freq / sampleRate
            val env = 0.20 * exp(-2.5 * t) * (1.0 - t)
            val s = (sampleWave(phase, Wave.SAWTOOTH) * env).coerceIn(-1.0, 1.0)
            out[i] = (s * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun playPcm(samples: ShortArray): AudioTrack {
        val bufferBytes = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
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
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bufferBytes)
            .build()

        track.write(samples, 0, samples.size)
        track.play()
        return track
    }
}
