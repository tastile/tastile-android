package app.tastile.android.data.api

import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

object V1Idempotency {
    private val random = SecureRandom()

    fun generate(): String {
        val ms = Instant.now().toEpochMilli()
        val tsHigh = (ms ushr 32).toInt()
        val tsLow = (ms and 0xFFFFFFFFL).toInt()

        val bytes = ByteArray(16)
        // bits 0-47: timestamp ms (big-endian)
        bytes[0] = ((tsHigh ushr 24) and 0xFF).toByte()
        bytes[1] = ((tsHigh ushr 16) and 0xFF).toByte()
        bytes[2] = ((tsHigh ushr 8) and 0xFF).toByte()
        bytes[3] = (tsHigh and 0xFF).toByte()
        bytes[4] = ((tsLow ushr 24) and 0xFF).toByte()
        bytes[5] = ((tsLow ushr 16) and 0xFF).toByte()
        // version 7 in high 4 bits of byte 6
        bytes[6] = (0x70 or ((tsLow ushr 8) and 0x0F)).toByte()
        bytes[7] = (tsLow and 0xFF).toByte()
        // variant 10xx in high 2 bits of byte 8
        bytes[8] = (0x80 or (random.nextInt() and 0x3F)).toByte()
        // bits 72-127: random
        random.nextBytes(bytes.copyOfRange(9, 16))

        val msb = ((bytes[0].toLong() and 0xFF) shl 56) or
            ((bytes[1].toLong() and 0xFF) shl 48) or
            ((bytes[2].toLong() and 0xFF) shl 40) or
            ((bytes[3].toLong() and 0xFF) shl 32) or
            ((bytes[4].toLong() and 0xFF) shl 24) or
            ((bytes[5].toLong() and 0xFF) shl 16) or
            ((bytes[6].toLong() and 0xFF) shl 8) or
            (bytes[7].toLong() and 0xFF)
        val lsb = ((bytes[8].toLong() and 0xFF) shl 56) or
            ((bytes[9].toLong() and 0xFF) shl 48) or
            ((bytes[10].toLong() and 0xFF) shl 40) or
            ((bytes[11].toLong() and 0xFF) shl 32) or
            ((bytes[12].toLong() and 0xFF) shl 24) or
            ((bytes[13].toLong() and 0xFF) shl 16) or
            ((bytes[14].toLong() and 0xFF) shl 8) or
            (bytes[15].toLong() and 0xFF)
        return UUID(msb, lsb).toString()
    }
}