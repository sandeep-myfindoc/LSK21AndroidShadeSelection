package com.app.lsk21androidshadeselection.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class YuvToRgbConverter {
    private fun convert(yBuffer: ByteBuffer, uBuffer: ByteBuffer, vBuffer: ByteBuffer, width: Int, height: Int, bitmap: Bitmap) {
        val ySize = width * height
        val uSize = (width / 2) * (height / 2)
        val yData = ByteArray(ySize)
        val uData = ByteArray(uSize)
        val vData = ByteArray(uSize)

        // Copy data from ByteBuffer to ByteArray
        yBuffer.get(yData)
        uBuffer.get(uData)
        vBuffer.get(vData)

        // Convert YUV to RGB
        val rgb = IntArray(width * height)
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val y = (yData[j * width + i].toInt() and 0xFF) - 16
                val u = (uData[(j / 2) * (width / 2) + (i / 2)].toInt() and 0xFF) - 128
                val v = (vData[(j / 2) * (width / 2) + (i / 2)].toInt() and 0xFF) - 128

                var r = y + 1.402 * v
                var g = y - 0.344136 * u - 0.714136 * v
                var b = y + 1.772 * u

                r = r.coerceIn(0.0, 255.0)
                g = g.coerceIn(0.0, 255.0)
                b = b.coerceIn(0.0, 255.0)

                rgb[index++] = (0xFF shl 24) or ((r.toInt() shl 16) and 0xFF0000) or ((g.toInt() shl 8) and 0xFF00) or (b.toInt() and 0xFF)
            }
        }

        bitmap.setPixels(rgb, 0, width, 0, 0, width, height)
    }
    fun imageToBitmap(image: Image): Bitmap? {
        // Get image planes
        val planes = image.planes

        if (planes.isEmpty()) return null

        // Extract the buffer from the first plane (usually this is sufficient)
        val buffer: ByteBuffer = planes[0].buffer


        // Convert the ByteBuffer to a byte array
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Create a Bitmap from the byte array
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val width = image.width
        val height = image.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Convert the image data to the Bitmap (Assuming NV21 format for this example)
        // The following is a simplistic conversion. For a production scenario,
        // more sophisticated color space conversion may be required.

        convert(yBuffer, uBuffer, vBuffer, width, height, bitmap)
        return bitmap
    }
    private fun imageToBitmap1(image: Image): Bitmap {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Fill NV21 byte array
        yBuffer.get(nv21, 0, ySize)
        uBuffer.get(nv21, ySize, uSize)
        vBuffer.get(nv21, ySize + uSize, vSize)

        return YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null).let { yuvImage ->
            ByteArrayOutputStream().use { stream ->
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, stream)
                BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
            }
        }
    }

}