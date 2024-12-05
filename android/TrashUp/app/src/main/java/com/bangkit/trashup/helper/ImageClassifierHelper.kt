package com.bangkit.trashup.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.bangkit.trashup.ml.Garbageclassmodel1
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

@Suppress("DEPRECATION")
class ImageClassifierHelper(
    private val context: Context,
    private val classifierListener: ClassifierListener?
) {
    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(detectedLabel: String, probability: Float) // Mengembalikan hasil deteksi dan probabilitas
    }

    // Pemetaan class ID ke jenis sampah
    private val classLabels = mapOf(
        0 to "Besi",
        1 to "Kaca",
        2 to "Kardus",
        3 to "Kertas",
        4 to "Plastik",
        5 to "Sampah Perintilan"
    )

    fun classifyStaticImage(imageUri: Uri) {
        try {
            // Muat model
            val model = Garbageclassmodel1.newInstance(context)

            // Preprocessing gambar
            val tensorImage = preprocessImage(imageUri)

            // Buat input buffer
            val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 100, 100, 3), DataType.FLOAT32)
            inputBuffer.loadBuffer(tensorImage.buffer)

            // Jalankan model dan dapatkan output
            val outputs = model.process(inputBuffer)
            val outputBuffer = outputs.outputFeature0AsTensorBuffer
            val probabilities = outputBuffer.floatArray // Ambil array probabilitas

            // Cari indeks dengan probabilitas tertinggi
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] }
            val detectedLabel = classLabels[maxIndex] ?: "Jenis sampah tidak dikenal"
            val probability = probabilities[maxIndex ?: 0]

            // Kirim hasil ke listener
            classifierListener?.onResults(detectedLabel, probability)

            // Log hasil deteksi
            Log.d(TAG, "Sampah terdeteksi: $detectedLabel dengan probabilitas ${probability * 100}%")

            // Tutup model
            model.close()
        } catch (e: Exception) {
            val errorMessage = "Error processing image: ${e.message}"
            classifierListener?.onError(errorMessage)
            Log.e(TAG, errorMessage, e)
        }
    }

    private fun preprocessImage(imageUri: Uri): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(100, 100, ResizeOp.ResizeMethod.BILINEAR)) // Resize ke 100x100
            .add(CastOp(DataType.FLOAT32)) // Konversi ke FLOAT32
            .add(NormalizeOp(0.0f, 255.0f)) // Normalisasi piksel ke [0, 1]
            .build()

        val bitmap = loadBitmapFromUri(imageUri)
            ?: throw IllegalArgumentException("Failed to load bitmap from URI")

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    private fun loadBitmapFromUri(imageUri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                    .copy(Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap: ${e.message}", e)
            null
        }
    }

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}