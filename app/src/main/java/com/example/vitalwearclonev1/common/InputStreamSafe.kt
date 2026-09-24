package com.example.vitalwearclonev1.common

import java.io.InputStream
import java.io.IOException

object InputStreamSafe {
    /**
     * A safe backport of readNBytes(int) for older Android versions.
     */
    fun readNBytes(inputStream: InputStream, n: Int): ByteArray {
        val b = ByteArray(n)
        var read = 0
        while (read < n) {
            val count = inputStream.read(b, read, n - read)
            if (count < 0) break
            read += count
        }
        return if (read == n) b else b.copyOf(read)
    }

    /**
     * A safe backport of readNBytes(byte[], int, int).
     */
    fun readNBytes(inputStream: InputStream, b: ByteArray, off: Int, len: Int): Int {
        var n = 0
        while (n < len) {
            val count = inputStream.read(b, off + n, len - n)
            if (count < 0) break
            n += count
        }
        return n
    }
}
