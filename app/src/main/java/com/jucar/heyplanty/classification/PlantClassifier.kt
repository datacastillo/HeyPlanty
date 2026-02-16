package com.jucar.heyplanty.classification

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class PlantClassifier(
    private val context: Context,
    private val classificationListener: ClassificationListener? = null
) {

    private var imageClassifier: ImageClassifier? = null

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(2)
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMaxResults(1)
            .build()

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                MODEL_NAME,
                options
            )
        } catch (e: IllegalStateException) {
            classificationListener?.onError("Error al inicializar el clasificador.")
        }
    }

    fun classify(image: Bitmap, rotation: Int) {
        if (imageClassifier == null) {
            setupClassifier()
        }

        val imageProcessor = ImageProcessor.Builder().add(Rot90Op(-rotation / 90)).build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = imageClassifier?.classify(tensorImage)

        classificationListener?.onClassificationResult(results)
    }

    interface ClassificationListener {
        fun onError(error: String)
        fun onClassificationResult(results: List<org.tensorflow.lite.task.vision.classifier.Classifications>?)
    }

    companion object {
        private const val MODEL_NAME = "model.tflite"
    }
}
