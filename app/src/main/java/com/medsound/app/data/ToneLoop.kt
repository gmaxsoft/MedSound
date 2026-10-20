package com.medsound.app.data

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * Writes a seamless sine loop for a fixed frequency.
 * The buffer holds a whole number of cycles, so repeating it does not click.
 */
object ToneLoop {

    private const val SAMPLE_RATE = 48_000
    private const val MIN_SECONDS = 20
    private const val AMPLITUDE = 0.2

    fun file(context: Context, frequencyHz: Int): File {
        val dir = File(context.cacheDir, "tones").apply { mkdirs() }
        val file = File(dir, "$frequencyHz.wav")
        if (file.length() > 44) return file
        write(file, frequencyHz)
        return file
    }

    private fun write(file: File, frequencyHz: Int) {
        val period = SAMPLE_RATE / gcd(SAMPLE_RATE, frequencyHz)
        val minSamples = SAMPLE_RATE * MIN_SECONDS
        val periods = (minSamples + period - 1) / period
        val total = periods * period
        val dataBytes = total * 2

        BufferedOutputStream(FileOutputStream(file)).use { out ->
            out.write(wavHeader(dataBytes))
            val step = 2.0 * PI * frequencyHz / SAMPLE_RATE
            for (i in 0 until total) {
                val sample = (sin(step * i) * AMPLITUDE * Short.MAX_VALUE).toInt()
                out.write(sample and 0xFF)
                out.write((sample shr 8) and 0xFF)
            }
        }
    }

    private fun wavHeader(dataBytes: Int): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeInt(header, 4, 36 + dataBytes)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeInt(header, 16, 16)
        writeShort(header, 20, 1)
        writeShort(header, 22, 1)
        writeInt(header, 24, SAMPLE_RATE)
        writeInt(header, 28, SAMPLE_RATE * 2)
        writeShort(header, 32, 2)
        writeShort(header, 34, 16)
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeInt(header, 40, dataBytes)
        return header
    }

    private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShort(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val next = x % y
            x = y
            y = next
        }
        return x
    }
}
