package com.example.moodtunes_v1

data class MoodResult(val mood: String, val confidence: Float)

object MoodClassifier {

    private val moodGroups = mapOf(
        "Happy" to listOf("admiration", "amusement", "approval", "caring", "excitement", "gratitude", "joy", "love", "optimism", "pride", "relief", "happiness", "fun", "sense of belonging", "creativity"),
        "Sad" to listOf("disappointment", "disapproval", "embarrassment", "fear", "grief", "remorse", "sadness", "worry", "empty", "boredom"),
        "Angry" to listOf("anger", "annoyance", "disgust", "hate"),
        "Calm" to listOf("neutral", "relief", "empty")
    )

    fun classifyMood(emotions: List<HfResponseItem>): MoodResult {
        val moodScores = mutableMapOf(
            "Happy" to 0f, "Sad" to 0f, "Angry" to 0f, "Calm" to 0f
        )

        emotions.forEach { emotion ->
            moodGroups.forEach { (mood, emotionList) ->
                if (emotion.label.lowercase() in emotionList) {
                    moodScores[mood] = moodScores[mood]!! + emotion.score
                }
            }
        }

        // Define a minimum confidence threshold (e.g., 30% confidence)
        val confidenceThreshold = 0.3f

        val topMoodEntry = moodScores.maxByOrNull { it.value }

        return if (topMoodEntry != null && topMoodEntry.value >= confidenceThreshold) {
            MoodResult(topMoodEntry.key, topMoodEntry.value)
        } else {
            MoodResult("Calm", moodScores["Calm"] ?: 0f)
        }


//        val topMood = moodScores.maxByOrNull { it.value } ?: moodScores.entries.first()
//        return MoodResult(topMood.key, topMood.value)
    }
}
