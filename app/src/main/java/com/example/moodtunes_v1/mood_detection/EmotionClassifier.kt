package com.yourpackagename

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmotionClassifier(private val context: Context) {

    private val interpreter: Interpreter
    private val vocab: Map<String, Int>
    private val maxLen = 128  // You can adjust based on the model’s needs

    init {
        interpreter = Interpreter(loadModelFile("model.tflite"), Interpreter.Options())
        vocab = loadVocab("vocab.txt")
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun loadVocab(fileName: String): Map<String, Int> {
        val inputStream = context.assets.open(fileName)
        val lines = inputStream.bufferedReader().readLines()
        val map = mutableMapOf<String, Int>()
//        lines.forEachIndexed { index, word -> map[word] = index }
//        return map
        return lines.withIndex().associate { it.value to it.index }
    }

    fun loadLabels(fileName: String): List<String> {
        val inputStream = context.assets.open(fileName)
        return inputStream.bufferedReader().readLines()
    }

    private fun tokenize(text: String): Pair<IntArray, IntArray> {
        // Split words & punctuation separately, preserving standalone punctuation
        val tokens = listOf("[CLS]") + text.lowercase()
            .replace(Regex("([.,!?;])"), " $1") // Keep punctuation as separate tokens
            .split(" ") + listOf("[SEP]")

        val tokenIds = tokens.map { token -> vocab[token] ?: vocab["##$token"] ?: vocab["[UNK]"] ?: 0 }

        // Generate attention mask (1 for actual tokens, 0 for padding)
        val attentionMask = IntArray(tokenIds.size) { 1 }

        return Pair(
            IntArray(maxLen) { i -> if (i < tokenIds.size) tokenIds[i] else vocab["[PAD]"] ?: 0 },
            attentionMask
        )
    }

    fun classify(text: String): FloatArray {
        val (inputIds, attentionMask) = tokenize(text)


        // Allocate correct buffer size
        val inputBuffer = ByteBuffer.allocateDirect(4 * maxLen).order(ByteOrder.nativeOrder())
        inputIds.forEach { inputBuffer.putInt(it) }

        val attentionBuffer = ByteBuffer.allocateDirect(4 * maxLen).order(ByteOrder.nativeOrder())
        attentionMask.forEach { attentionBuffer.putInt(it) }


        val outputBuffer = ByteBuffer.allocateDirect(4 * 6).order(ByteOrder.nativeOrder())

        Log.d("DEBUG_TFLITE", "Kotlin Input IDs: ${inputIds.joinToString()}")
        Log.d("DEBUG_TFLITE", "Kotlin Attention Mask: ${attentionMask.joinToString()}")

        // Pass all three inputs to TensorFlow Lite
        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer, attentionBuffer),
            mapOf(0 to outputBuffer)
        )

        outputBuffer.rewind()
        val labels = loadLabels("labels.txt")
        val predictions = FloatArray(labels.size)
        outputBuffer.asFloatBuffer().get(predictions)

        Log.d("DEBUG_TFLITE", "Raw Logits: ${predictions.joinToString()}")
        Log.d("DEBUG_TFLITE", "TFLite Model Output dtype: ${outputBuffer.asFloatBuffer()}")

        return softmax(predictions) // Normalize to ensure percentages sum to 100%
    }

    private fun softmax(values: FloatArray): FloatArray {
        val maxVal = values.maxOrNull() ?: 0f
        val expValues = values.map { Math.exp((it - maxVal).toDouble()).toFloat() }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toFloatArray()
    }


}
