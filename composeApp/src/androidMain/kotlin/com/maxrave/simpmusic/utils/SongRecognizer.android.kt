package com.maxrave.simpmusic.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

private const val RECORD_SAMPLE_RATE = 44_100
private const val RECORD_SECONDS = 12
private const val MAX_ATTEMPTS = 3

private val userAgents =
    listOf(
        "Dalvik/2.1.0 (Linux; U; Android 5.0.2; VS980 4G Build/LRX22G)",
        "Dalvik/1.6.0 (Linux; U; Android 4.4.2; SM-T210 Build/KOT49H)",
        "Dalvik/2.1.0 (Linux; U; Android 5.1.1; SM-P905V Build/LMY47X)",
        "Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-G920F Build/MMB29K)",
        "Dalvik/2.1.0 (Linux; U; Android 5.0; SM-G900F Build/LRX21T)",
    )

private val timezones =
    listOf("Europe/Paris", "Europe/London", "America/New_York", "America/Los_Angeles", "Asia/Tokyo", "Asia/Dubai")

@Composable
actual fun rememberSongRecognitionController(
    onSongRecognized: (query: String) -> Unit,
): SongRecognitionController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<SongRecognitionState>(SongRecognitionState.Idle) }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var listenJob by remember { mutableStateOf<Job?>(null) }

    fun beginRecording() {
        listenJob?.cancel()
        state = SongRecognitionState.Listening
        amplitude = 0f

        listenJob = scope.launch {
            try {
                val pcm = recordPcm { amp ->
                    amplitude = amp
                }
                if (pcm.isEmpty()) {
                    state = SongRecognitionState.Error("Microphone returned empty audio.")
                    return@launch
                }

                state = SongRecognitionState.Processing
                amplitude = 0f

                val signature = withContext(Dispatchers.Default) {
                    ShazamSignature.fromPcm(pcm, RECORD_SAMPLE_RATE)
                }
                val match = askShazam(signature, sampleMs = pcm.size * 1000L / RECORD_SAMPLE_RATE)
                if (match != null) {
                    val (title, artist) = match
                    state = SongRecognitionState.Success(title, artist)
                    val query = if (artist.isNotBlank()) "$title $artist" else title
                    onSongRecognized(query)
                } else {
                    state = SongRecognitionState.Error("No song matched. Move closer to the music and try again.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive) {
                    state = SongRecognitionState.Error(e.message ?: "Recognition failed")
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            beginRecording()
        } else {
            state = SongRecognitionState.Error("Microphone permission is needed to identify songs.")
        }
    }

    val controller = remember {
        object : SongRecognitionController {
            override val state: SongRecognitionState
                get() = state
            override val amplitude: Float
                get() = amplitude

            override fun startListening() {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    beginRecording()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            override fun cancel() {
                listenJob?.cancel()
                listenJob = null
                amplitude = 0f
                state = SongRecognitionState.Idle
            }

            override fun reset() {
                cancel()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            listenJob?.cancel()
        }
    }

    return controller
}

@SuppressLint("MissingPermission")
private suspend fun recordPcm(
    onAmplitude: (Float) -> Unit,
): ShortArray =
    withContext(Dispatchers.IO) {
        val format = AudioFormat.ENCODING_PCM_16BIT
        val channel = AudioFormat.CHANNEL_IN_MONO
        val bufferSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE, channel, format)
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, RECORD_SAMPLE_RATE, channel, format, bufferSize)
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            error("Microphone unavailable")
        }
        val pcm = ShortArray(RECORD_SAMPLE_RATE * RECORD_SECONDS)
        var filled = 0
        try {
            recorder.startRecording()
            while (filled < pcm.size && isActive) {
                val read = recorder.read(pcm, filled, minOf(bufferSize / 2, pcm.size - filled))
                if (read < 0) error("AudioRecord.read failed: $read")
                if (read > 0) {
                    var peak = 0
                    for (i in filled until filled + read) {
                        val a = abs(pcm[i].toInt())
                        if (a > peak) peak = a
                    }
                    val normalized = (peak / 16384f).coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        onAmplitude(normalized)
                    }
                    filled += read
                }
            }
        } finally {
            try {
                recorder.stop()
            } catch (_: Exception) {
            }
            recorder.release()
        }
        pcm.copyOf(filled)
    }

private suspend fun askShazam(
    signature: String,
    sampleMs: Long,
): Pair<String, String>? {
    val client = HttpClient(CIO) { install(HttpTimeout) { requestTimeoutMillis = 30_000 } }
    try {
        repeat(MAX_ATTEMPTS) { attempt ->
            val response =
                client.post(
                    "https://amp.shazam.com/discovery/v5/en/US/android/-/tag/" +
                        "${UUID.randomUUID().toString().uppercase()}/${UUID.randomUUID()}",
                ) {
                    parameter("sync", "true")
                    parameter("webv3", "true")
                    parameter("sampling", "true")
                    parameter("connected", "")
                    parameter("shazamapiversion", "v3")
                    parameter("sharehub", "true")
                    parameter("video", "v3")
                    header("User-Agent", userAgents.random())
                    header("Content-Language", "en_US")
                    contentType(ContentType.Application.Json)
                    setBody(shazamRequestBody(signature, sampleMs))
                }
            when {
                response.status.isSuccess() -> return parseShazamResponse(response.bodyAsText())
                response.status == HttpStatusCode.NotFound -> return null
                response.status == HttpStatusCode.TooManyRequests && attempt < MAX_ATTEMPTS - 1 -> {
                    delay(2_000L shl attempt)
                }
                else -> error("Shazam answered HTTP ${response.status.value}")
            }
        }
        return null
    } finally {
        client.close()
    }
}

private fun shazamRequestBody(
    signature: String,
    sampleMs: Long,
): String {
    val now = System.currentTimeMillis() / 1000
    return buildJsonObject {
        putJsonObject("geolocation") {
            put("altitude", Random.nextDouble() * 400 + 100)
            put("latitude", Random.nextDouble() * 180 - 90)
            put("longitude", Random.nextDouble() * 360 - 180)
        }
        putJsonObject("signature") {
            put("samplems", sampleMs)
            put("timestamp", now)
            put("uri", signature)
        }
        put("timestamp", now)
        put("timezone", timezones.random())
    }.toString()
}

private fun parseShazamResponse(body: String): Pair<String, String>? {
    val track = (Json.parseToJsonElement(body) as? JsonObject)?.get("track") as? JsonObject
    val title = track?.get("title")?.jsonPrimitive?.contentOrNull?.trim()
    if (track == null || title.isNullOrEmpty()) return null
    val artist = track["subtitle"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    return Pair(title, artist)
}