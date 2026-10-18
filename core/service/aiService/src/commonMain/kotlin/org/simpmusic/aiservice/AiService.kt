package org.simpmusic.aiservice

import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class AiService(
    private val aiHost: AIHost = AIHost.GEMINI,
    private val apiKey: String,
    private val customModelId: String? = null,
    private val customBaseUrl: String? = null,
    private val customHeaders: Map<String, String>? = null,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    private val httpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000L
            connectTimeoutMillis = 15_000L
            socketTimeoutMillis = 60_000L
        }
    }

    suspend fun translateLyrics(
        inputLyrics: Lyrics,
        targetLanguage: String,
    ): Lyrics {
        val lines = inputLyrics.lines ?: throw IllegalStateException("No lyrics lines to translate")

        val uniqueTextToId = mutableMapOf<String, String>()
        var idCounter = 0

        lines.forEach { line ->
            val words = line.words.trim()
            if (words.isNotEmpty() && words != "♫" && !uniqueTextToId.containsKey(words)) {
                uniqueTextToId[words] = (idCounter++).toString()
            }
        }

        if (uniqueTextToId.isEmpty()) {
            throw IllegalStateException("No translatable lyrics lines found")
        }

        val idToUniqueText = uniqueTextToId.entries.associate { (k, v) -> v to k }
        val inputJson = json.encodeToString(MapSerializer(String.serializer(), String.serializer()), idToUniqueText)

        val prompt = """
            You are an expert song lyrics translator.
            Task: Translate/convert the input lines into conversational Hinglish (Hindi written ONLY using the English/Latin alphabet, NEVER use Devanagari script).

            Rules:
            1. If the line is English or any other language: Translate the poetic meaning into natural, conversational Hinglish (e.g., "I will always love you" -> "Main hamesha tumse pyaar karunga", "Hold my hand" -> "Mera haath pakad lo").
            2. If the line is already in Hindi/Urdu/Punjabi: Transliterate it directly into clean Roman/English letters without altering the original lyrics.
            3. STRICT RULE: DO NOT use Devanagari script. Use ONLY English alphabet characters (A-Z, a-z).
            4. Keep all numeric keys identical to the input.
            5. Return JSON ONLY with format: {"translations": {"0": "translated line"}}

            Input JSON:
            $inputJson
        """.trimIndent()

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        putJsonArray("parts") {
                            add(
                                buildJsonObject {
                                    put("text", prompt)
                                }
                            )
                        }
                    }
                )
            }
            putJsonObject("generationConfig") {
                put("response_mime_type", "application/json")
                put("temperature", 0.1)
            }
        }

        val cleanKey = apiKey.trim()
        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$cleanKey"

        val response = httpClient.post(endpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseBody = response.bodyAsText()

        if (response.status.value !in 200..299) {
            throw IllegalStateException("Gemini API Error [${response.status.value}]: $responseBody")
        }

        val parsedResponse = json.parseToJsonElement(responseBody).jsonObject
        val candidates = parsedResponse["candidates"]?.jsonArray
        val candidate = candidates?.firstOrNull()?.jsonObject
        val parts = candidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
        val rawText = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw IllegalStateException("API Response text empty: $responseBody")

        val cleanedJson = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val translationResponse = json.decodeFromString<TranslationResponse>(cleanedJson)
        val idToTranslated = translationResponse.translations

        val wordToTranslatedMap = mutableMapOf<String, String>()
        uniqueTextToId.forEach { (originalWord, id) ->
            idToTranslated[id]?.let { translated ->
                wordToTranslatedMap[originalWord] = translated
            }
        }

        val translatedLines = lines.map { originalLine ->
            val words = originalLine.words.trim()
            val translatedWords = wordToTranslatedMap[words]

            if (translatedWords != null) {
                Line(
                    startTimeMs = originalLine.startTimeMs,
                    endTimeMs = originalLine.endTimeMs,
                    words = translatedWords,
                    syllables = null,
                )
            } else {
                originalLine
            }
        }

        return Lyrics(
            error = false,
            lines = translatedLines,
            syncType = inputLyrics.syncType,
        )
    }
}

@Serializable
data class TranslationResponse(
    val translations: Map<String, String> = emptyMap(),
)

enum class AIHost {
    GEMINI,
    OPENAI,
    CUSTOM_OPENAI,
}