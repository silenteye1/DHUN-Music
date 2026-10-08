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

        // 1. Unique lines extract karo taaki payload 50-60% chhota ho jaye aur response super fast aaye
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

        // Key -> unique text
        val idToUniqueText = uniqueTextToId.entries.associate { (k, v) -> v to k }
        val inputJson = json.encodeToString(MapSerializer(String.serializer(), String.serializer()), idToUniqueText)

        // 2. High-speed, compact prompt
        val prompt = """
            Song lyric processing:
            - If lines are Urdu/Punjabi/Hindi/South-Asian: Transliterate ONLY into Romanized Hinglish (English letters). Do not change original words.
            - If lines are English: Translate into natural Hindi lyrics.
            - Keep all numeric IDs identical.
            - Return JSON ONLY: {"translations": {"0": "text"}}
            Input:
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
                put("temperature", 0.0) // 0.0 temperature fastest generation deta hai
            }
        }

        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey"

        val response = httpClient.post(endpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseBody = response.bodyAsText()

        val parsedResponse = json.parseToJsonElement(responseBody).jsonObject
        val candidates = parsedResponse["candidates"]?.jsonArray
        val candidate = candidates?.firstOrNull()?.jsonObject
        val parts = candidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
        val rawText = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw IllegalStateException("API Error: $responseBody")

        val cleanedJson = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val translationResponse = json.decodeFromString<TranslationResponse>(cleanedJson)
        val idToTranslated = translationResponse.translations

        // 3. Translated text ko original unique words ke saath map karo
        val wordToTranslatedMap = mutableMapOf<String, String>()
        uniqueTextToId.forEach { (originalWord, id) ->
            idToTranslated[id]?.let { translated ->
                wordToTranslatedMap[originalWord] = translated
            }
        }

        // 4. Sabhi lines me instant assign kar do
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