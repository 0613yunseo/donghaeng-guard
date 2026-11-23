package com.donghaeng.guard

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

//백엔드 포트 관리
val API_BASE_URL: String = BuildConfig.API_BASE_URL

// ══════════════════════════════════════════════════════════════════════
// 색상 시스템 (theme.css → AppColors)
// ══════════════════════════════════════════════════════════════════════
/** 로고 글씨(동행가드 DongHaeng Guard). 원본은 res/drawable-nodpi/logo_text.png (배경 투명) */
@Composable
fun LogoText(height: Dp = 60.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_text),
        contentDescription = "동행가드 DongHaeng Guard",
        modifier = modifier.height(height)
    )
}

/** 앱 로고(방패 + 휠체어 + 경로). 원본은 res/drawable-nodpi/logo_mark.png */
@Composable
fun LogoMark(size: Dp = 96.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_mark),
        contentDescription = "동행가드 로고",
        modifier = modifier.size(size)
    )
}

/**
 * 접근성·알림 설정. SharedPreferences에 저장하고, Compose 상태라 바뀌면 화면이 바로 갱신된다.
 * DongHaengGuardApp 시작 시 init(context)를 한 번 호출해야 한다.
 */
object UiPrefs {
    private var sp: SharedPreferences? = null

    var notifEnabled by mutableStateOf(true);       private set
    var vibrEnabled  by mutableStateOf(true);       private set
    var soundEnabled by mutableStateOf(true);       private set
    var fontSize     by mutableStateOf("medium");   private set   // small | medium | large
    var contrast     by mutableStateOf("normal");   private set   // normal | high
    var colorMode    by mutableStateOf("default");  private set   // default | redgreen

    fun init(context: Context) {
        if (sp != null) return
        val p = context.applicationContext.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)
        sp = p
        notifEnabled = p.getBoolean("notif", true)
        vibrEnabled  = p.getBoolean("vibr", true)
        soundEnabled = p.getBoolean("sound", true)
        fontSize     = p.getString("fontSize", "medium") ?: "medium"
        contrast     = p.getString("contrast", "normal") ?: "normal"
        // 예전 적색약/녹색약 설정은 적록색약 하나로 합쳤다
        colorMode    = (p.getString("colorMode", "default") ?: "default").let { if (it == "protanopia" || it == "deuteranopia") "redgreen" else it }
    }

    fun saveNotif(v: Boolean)  { notifEnabled = v;  sp?.edit()?.putBoolean("notif", v)?.apply() }
    fun saveVibr(v: Boolean)   { vibrEnabled = v;   sp?.edit()?.putBoolean("vibr", v)?.apply() }
    fun saveSound(v: Boolean)  { soundEnabled = v;  sp?.edit()?.putBoolean("sound", v)?.apply() }
    fun saveFontSize(v: String)  { fontSize = v;    sp?.edit()?.putString("fontSize", v)?.apply() }
    fun saveContrast(v: String)  { contrast = v;    sp?.edit()?.putString("contrast", v)?.apply() }
    fun saveColorMode(v: String) { colorMode = v;   sp?.edit()?.putString("colorMode", v)?.apply() }

    // 서버에 닿지 않아 보내지 못한 주행 종료. 서버가 다시 켜지면 앱이 자동으로 다시 보낸다. 형식: "tripId|endedAt"
    fun savePendingEnd(tripId: Long, endedAt: String) { sp?.edit()?.putString("pendingEnd", "$tripId|$endedAt")?.apply() }
    fun clearPendingEnd() { sp?.edit()?.remove("pendingEnd")?.apply() }
    fun pendingEnd(): Pair<Long, String>? =
        sp?.getString("pendingEnd", null)?.split("|")?.takeIf { it.size == 2 }
            ?.let { (id, at) -> id.toLongOrNull()?.let { it to at } }

    /** 시스템 글씨 크기에 곱하는 배율 */
    val fontScale: Float get() = when (fontSize) { "small" -> 0.9f; "large" -> 1.2f; else -> 1f }
    val highContrast: Boolean get() = contrast == "high"
    val colorBlind: Boolean get() = colorMode != "default"
}

object AppColors {
    val Background  = Color(0xFFF8FAFC)
    val Card        = Color(0xFFFFFFFF)
    val Primary     = Color(0xFF2563EB)
    val PrimaryBg   = Color(0xFFDBEAFE)
    val OnPrimary   = Color(0xFFFFFFFF)
    val Surface     = Color(0xFFF1F5F9)
    // 높은 대비: 글씨·테두리를 더 진하게
    val Text        get() = if (UiPrefs.highContrast) Color(0xFF000000) else Color(0xFF111827)
    val TextSub     get() = if (UiPrefs.highContrast) Color(0xFF374151) else Color(0xFF6B7280)
    val Border      get() = if (UiPrefs.highContrast) Color(0xFF6B7280) else Color(0xFFE5E7EB)
    // 색약 보정: 빨강/초록 구분 대신 Okabe-Ito 팔레트(파랑·주황·진주황)로 바꿈
    val Destructive get() = Danger
    val Safe        get() = if (UiPrefs.colorBlind) Color(0xFF0072B2) else Color(0xFF22C55E)
    val SafeBg      get() = if (UiPrefs.colorBlind) Color(0xFFD6EAF8) else Color(0xFFDCFCE7)
    val Warning     get() = if (UiPrefs.colorBlind) Color(0xFFE6A100) else Color(0xFFF59E0B)
    val WarningBg   = Color(0xFFFEF3C7)
    val Danger      get() = if (UiPrefs.colorBlind) Color(0xFFD55E00) else Color(0xFFEF4444)
    val DangerBg    get() = if (UiPrefs.colorBlind) Color(0xFFFBE3D0) else Color(0xFFFEE2E2)
    val SwitchOff   = Color(0xFFD1D5DB)
}

/** 위험 단계가 바뀔 때 진동·소리로 알린다. 설정(알림/진동/소리)을 따른다. WARNING은 짧게, DANGER는 길고 강하게. */
@Suppress("DEPRECATION")
fun alertRisk(context: Context, level: RiskLevel) {
    if (level == RiskLevel.SAFE || !UiPrefs.notifEnabled) return
    val danger = level == RiskLevel.DANGER
    if (UiPrefs.vibrEnabled) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        else context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        val timings = if (danger) longArrayOf(0, 500, 150, 500, 150, 500) else longArrayOf(0, 250)
        val amps = if (danger) intArrayOf(0, 255, 0, 255, 0, 255) else intArrayOf(0, 120)
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amps, -1))
    }
    if (UiPrefs.soundEnabled) {
        // ToneGenerator는 알림 스트림 볼륨을 따른다. 재생 후 해제.
        val tone = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, if (danger) 100 else 60)
        tone.startTone(
            if (danger) android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else android.media.ToneGenerator.TONE_PROP_BEEP,
            if (danger) 1200 else 300
        )
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, if (danger) 1500L else 600L)
    }
}

// ══════════════════════════════════════════════════════════════════════
// 화면 네비게이션 (remember 상태 기반)
// ══════════════════════════════════════════════════════════════════════
sealed class Screen {
    object Onboarding            : Screen()
    object Login                 : Screen()
    object LoginForm             : Screen()
    object Signup                : Screen()
    object SensorConnection      : Screen()
    object SensorManagement      : Screen()
    object MainDriving           : Screen()
    data class TripEnd(val tripId: Long) : Screen()
    object DrivingHistory        : Screen()
    data class DrivingDetail(val tripId: String) : Screen()
    object RiskZones             : Screen()
    object Settings              : Screen()
    object NotificationSettings  : Screen()
    object AccessibilitySettings : Screen()
    object Account               : Screen()
}

// ══════════════════════════════════════════════════════════════════════
// 위험 단계 모델
// ══════════════════════════════════════════════════════════════════════
enum class RiskLevel { SAFE, WARNING, DANGER }
data class AiAnalysisResult(
    val riskGrade: String,
    val riskScore: Int,
    val reason: String,
    val recommendedAction: String
)
// Spring Boot에서 조회한 AI 예측 결과
data class AiPredictionResult(
    val id: Long,
    val measurementId: String,
    val tripId: Long?,
    val modelType: String,
    val modelVersion: String?,
    val predictedRisk: String?,
    val predictedProbability: Double?,
    val ruleRisk: String?,
    val status: String,
    val errorMessage: String?,
    val predictedAt: String?
)

// 실제 AI 예측 결과 조회
suspend fun fetchAiPredictions(tripId: Long): List<AiPredictionResult> {
    return withContext(Dispatchers.IO) {
        val connection = (
                java.net.URL("$API_BASE_URL/api/ai-predictions?tripId=$tripId")
                    .openConnection() as java.net.HttpURLConnection
                )

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 10000

            val statusCode = connection.responseCode

            if (statusCode !in 200..299) {
                throw IllegalStateException(
                    "AI 분석 조회 실패 (HTTP $statusCode)"
                )
            }

            val responseBody = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            // 현재 Spring Boot API는 JSON 배열을 반환함
            val jsonArray = org.json.JSONArray(responseBody)

            List(jsonArray.length()) { index ->
                val item = jsonArray.getJSONObject(index)

                // JSONObject.optString은 JSON null도 문자열로 반환할 수 있으므로 처리
                fun optionalString(key: String): String? {
                    return if (item.isNull(key) || !item.has(key)) {
                        null
                    } else {
                        item.getString(key)
                    }
                }

                AiPredictionResult(
                    id = item.getLong("id"),
                    measurementId = item.getString("measurementId"),
                    tripId = if (item.isNull("tripId")) null
                    else item.getLong("tripId"),
                    modelType = item.getString("modelType"),
                    modelVersion = optionalString("modelVersion"),
                    predictedRisk = optionalString("predictedRisk"),
                    predictedProbability =
                        if (item.isNull("predictedProbability")) null
                        else item.getDouble("predictedProbability"),
                    ruleRisk = optionalString("ruleRisk"),
                    status = item.getString("status"),
                    errorMessage = optionalString("errorMessage"),
                    predictedAt = optionalString("predictedAt")
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
fun parseAiAnalysisResult(json: String): AiAnalysisResult {
    val data = JSONObject(json)

    return AiAnalysisResult(
        riskGrade = data.getString("riskGrade"),
        riskScore = data.getInt("riskScore"),
        reason = data.getString("reason"),
        recommendedAction = data.getString("recommendedAction")
    )
}
fun riskColor(l: RiskLevel) = when (l) {
    RiskLevel.SAFE    -> AppColors.Safe
    RiskLevel.WARNING -> AppColors.Warning
    RiskLevel.DANGER  -> AppColors.Danger
}
fun riskBg(l: RiskLevel) = when (l) {
    RiskLevel.SAFE    -> AppColors.SafeBg
    RiskLevel.WARNING -> AppColors.WarningBg
    RiskLevel.DANGER  -> AppColors.DangerBg
}
fun riskLabel(l: RiskLevel) = when (l) { RiskLevel.SAFE -> "안전"; RiskLevel.WARNING -> "주의"; RiskLevel.DANGER -> "위험" }
fun riskDesc(l: RiskLevel) = when (l) {
    RiskLevel.SAFE    -> "현재 주행 상태가 안전합니다"
    RiskLevel.WARNING -> "전방에 주의가 필요합니다"
    RiskLevel.DANGER  -> "속도를 줄이고 전방을 확인하세요"
}
fun riskGradeBg(g: String)    = when (g) { "HIGH" -> AppColors.DangerBg; "MEDIUM" -> AppColors.WarningBg; else -> AppColors.SafeBg }
fun riskGradeLabel(g: String) = when (g) { "HIGH" -> "높음"; "MEDIUM" -> "보통"; else -> "낮음" }

// ══════════════════════════════════════════════════════════════════════
// 데이터 모델
// ══════════════════════════════════════════════════════════════════════
data class TripRecord(
    val id: String, val date: String, val timeRange: String,
    val duration: String, val dangerCount: Int, val warningCount: Int,
    val maxRisk: RiskLevel
)

data class RiskEvent(
    val id: String, val time: String, val riskLevel: RiskLevel,
    val distanceMm: Int, val location: String,
    val lat: Double? = null, val lng: Double? = null,
    val imageUrl: String? = null   // 위험(4·5단계)일 때 센서가 찍은 사진 경로
)

/** 서버 이미지 경로(/images/...)를 불러와 보여준다. 실패하면 안내 문구. */
@Composable
fun EventPhoto(imageUrl: String, modifier: Modifier = Modifier) {
    val full = if (imageUrl.startsWith("http")) imageUrl else "$API_BASE_URL/${imageUrl.trimStart('/')}"
    // null = 불러오는 중, Result.failure = 실패
    val bitmap by produceState<Result<android.graphics.Bitmap>?>(null, full) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val conn = java.net.URL(full).openConnection() as java.net.HttpURLConnection
                try {
                    conn.connectTimeout = 5000
                    conn.readTimeout = 8000
                    conn.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
                        ?: error("decode failed")
                } finally { conn.disconnect() }
            }
        }
    }
    val r = bitmap
    when {
        r == null -> Box(modifier.height(120.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        r.isSuccess -> Image(
            bitmap = r.getOrThrow().asImageBitmap(),
            contentDescription = "위험 감지 사진",
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = modifier.heightIn(max = 240.dp).clip(RoundedCornerShape(8.dp))
        )
        else -> Text("사진을 불러오지 못했습니다.", fontSize = 12.sp, color = AppColors.TextSub)
    }
}

data class TripDetail(
    val id: String, val date: String, val startTime: String,
    val endTime: String, val duration: String, val events: List<RiskEvent>
)


data class RiskSituation(
    val startTime: String,
    val endTime: String,
    val highestRisk: RiskLevel,
    val eventCount: Int,
    val minDistanceMm: Int?,
    val recovered: Boolean
)

fun summarizeRiskSituations(events: List<RiskEvent>): List<RiskSituation> {
    val situations = mutableListOf<RiskSituation>()
    val sorted = events.sortedBy { it.time }

    var start: RiskEvent? = null
    var last: RiskEvent? = null
    var highest = RiskLevel.WARNING
    var count = 0
    var minDistance: Int? = null

    fun finish(recovered: Boolean) {
        val first = start ?: return
        val end = last ?: first

        situations.add(
            RiskSituation(
                startTime = first.time,
                endTime = end.time,
                highestRisk = highest,
                eventCount = count,
                minDistanceMm = minDistance,
                recovered = recovered
            )
        )

        start = null
        last = null
        highest = RiskLevel.WARNING
        count = 0
        minDistance = null
    }

    for (event in sorted) {
        if (event.riskLevel == RiskLevel.SAFE) {
            finish(recovered = true)
            continue
        }

        val previous = last
        if (previous != null) {
            val gap = try {
                val before = java.time.LocalTime.parse(previous.time)
                val after = java.time.LocalTime.parse(event.time)
                java.time.Duration.between(before, after).seconds
            } catch (e: Exception) {
                Long.MAX_VALUE
            }

            if (gap < 0 || gap > 10) {
                finish(recovered = false)
            }
        }

        if (start == null) start = event

        last = event
        count++

        if (event.riskLevel == RiskLevel.DANGER) {
            highest = RiskLevel.DANGER
        }

        if (event.distanceMm >= 0) {
            minDistance = minDistance?.let {
                minOf(it, event.distanceMm)
            } ?: event.distanceMm
        }
    }

    finish(recovered = false)
    return situations
}

suspend fun fetchTripDetail(tripId: String): TripDetail {
    return withContext(Dispatchers.IO) {
        val connection = (
            java.net.URL("$API_BASE_URL/api/trips/$tripId")
                .openConnection() as java.net.HttpURLConnection
        )

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("주행 정보 조회 실패 (HTTP $status)")
            }

            val response = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            val root = JSONObject(response)
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException("주행 정보를 불러올 수 없습니다.")
            }

            val data = root.getJSONObject("data")
            val startedAt = LocalDateTime.parse(data.getString("startedAt"))
            val endedAt = data.optString("endedAt").takeIf {
                it.isNotBlank() && it != "null"
            }?.let { LocalDateTime.parse(it) }

            val durationMinutes = endedAt?.let {
                java.time.Duration.between(startedAt, it).toMinutes()
            }

            val eventArray = data.getJSONArray("events")
            val events = List(eventArray.length()) { index ->
                val event = eventArray.getJSONObject(index)
                val detectedAt = LocalDateTime.parse(
                    event.getString("detectedAt")
                )

                RiskEvent(
                    id = event.getString("eventId"),
                    time = detectedAt.format(
                        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                    ),
                    riskLevel = when (event.optString("riskLevel")) {
                        "DANGER" -> RiskLevel.DANGER
                        "WARNING" -> RiskLevel.WARNING
                        else -> RiskLevel.SAFE
                    },
                    distanceMm = if (event.isNull("distanceMm")) {
                        -1
                    } else {
                        event.getInt("distanceMm")
                    },
                    location = "위치 정보",
                    lat = if (event.isNull("latitude")) null
                          else event.getDouble("latitude"),
                    lng = if (event.isNull("longitude")) null
                          else event.getDouble("longitude"),
                    imageUrl = event.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" }
                )
            }

            TripDetail(
                id = data.getString("tripId"),
                date = startedAt.format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일")
                ),
                startTime = startedAt.format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                ),
                endTime = endedAt?.format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                ) ?: "주행 중",
                duration = durationMinutes?.let { minutes ->
                    val seconds = java.time.Duration.between(
                        startedAt, endedAt
                    ).seconds.coerceAtLeast(0)

                    if (minutes >= 60) {
                        "${minutes / 60}시간 ${minutes % 60}분"
                    } else if (minutes >= 1) {
                        "${minutes}분"
                    } else {
                        "${seconds}초"
                    }
                } ?: "주행 중",
                events = events
            )
        } finally {
            connection.disconnect()
        }
    }
}

data class RiskZone(
    val id: String, val name: String, val location: String,
    val riskGrade: String, val riskScore: Int, val eventCount: Int,
    val description: String, val lat: Double, val lng: Double
)

// Spring Boot 위험 구간 API 응답
data class ServerRiskZone(
    val zoneId: Long,
    val latitude: Double?,
    val longitude: Double?,
    val riskScore: Int?,
    val riskGrade: String?,
    val zoneEventCount: Int?,
    val reason: String?,
    val updatedAt: String?
)

// 실제 위험 구간 목록 조회
suspend fun fetchRiskZones(): List<ServerRiskZone> {
    return withContext(Dispatchers.IO) {
        val connection = (
                java.net.URL("$API_BASE_URL/api/risk-zones")
                    .openConnection() as java.net.HttpURLConnection
                )

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val statusCode = connection.responseCode

            if (statusCode !in 200..299) {
                throw IllegalStateException(
                    "위험 구간 조회 실패 (HTTP $statusCode)"
                )
            }

            val responseBody = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(responseBody)

            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(
                    root.optString("message", "위험 구간 조회 실패")
                )
            }

            val data = root.getJSONArray("data")

            List(data.length()) { index ->
                val item = data.getJSONObject(index)

                fun optionalDouble(key: String): Double? =
                    if (item.isNull(key)) null else item.getDouble(key)

                fun optionalInt(key: String): Int? =
                    if (item.isNull(key)) null else item.getInt(key)

                fun optionalString(key: String): String? =
                    if (item.isNull(key)) null else item.getString(key)

                ServerRiskZone(
                    zoneId = item.getLong("zoneId"),
                    latitude = optionalDouble("latitude"),
                    longitude = optionalDouble("longitude"),
                    riskScore = optionalInt("riskScore"),
                    riskGrade = optionalString("riskGrade"),
                    zoneEventCount = optionalInt("zoneEventCount"),
                    reason = optionalString("reason"),
                    updatedAt = optionalString("updatedAt")
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// MainActivity
// ══════════════════════════════════════════════════════════════════════
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DongHaengGuardApp() }
    }
}
fun getCurrentLocation(context: Context, maxAgeMs: Long = Long.MAX_VALUE): Pair<Double, Double>? {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!fineLocationGranted && !coarseLocationGranted) {
        return null
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )

    for (provider in providers) {
        val location = try {
            locationManager.getLastKnownLocation(provider)
        } catch (e: SecurityException) {
            null
        }

        if (location != null && System.currentTimeMillis() - location.time <= maxAgeMs) {
            return Pair(location.latitude, location.longitude)
        }
    }

    return null
}
// 2분 이내 위치가 있으면 그것을, 아니면 새 위치를 요청한다 (최대 8초). 못 받으면 오래된 마지막 위치로 대체. 권한이 없으면 null.
suspend fun fetchCurrentLocation(context: Context): Pair<Double, Double>? {
    getCurrentLocation(context, 120_000L)?.let { return it }

    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) return null

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { manager.isProviderEnabled(it) }
    if (providers.isEmpty()) return getCurrentLocation(context)

    val fresh = withTimeoutOrNull(8000) {
        suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    if (cont.isActive) cont.resumeWith(Result.success(Pair(location.latitude, location.longitude)))
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("API 29 이하 호환용")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            try {
                @Suppress("DEPRECATION")
                providers.forEach { manager.requestSingleUpdate(it, listener, android.os.Looper.getMainLooper()) }
            } catch (e: SecurityException) {
                cont.resumeWith(Result.success(null))
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
        }
    }
    return fresh ?: getCurrentLocation(context)
}

// ── 경로 위험도 안내 API (POST /api/routes/recommend) ─────────────────
// 성공 응답은 ApiResponse 래퍼 없이 RouteResponse 객체를 그대로 반환한다.
data class RouteRiskZone(
    val zoneId: Long,
    val latitude: Double?,
    val longitude: Double?,
    val riskScore: Int?,
    val riskGrade: String?,
    val reason: String?,
    val distanceFromRoute: Double?
)

data class SafeRoute(
    val totalDistance: Int?,          // m
    val totalDuration: Int?,          // 초
    val safetyScore: Double?,         // 0~100, 높을수록 안전
    val riskZoneCount: Int?,
    val fallback: Boolean,            // true면 실제 경로가 아닌 대체 경로 (표시하지 않는다)
    val path: List<Pair<Double, Double>>,
    val riskZones: List<RouteRiskZone>
)

enum class RouteErrorKind { NETWORK, SERVER, NO_ROUTE, INVALID_INPUT, BAD_RESPONSE }

sealed class RouteOutcome {
    data class Success(val route: SafeRoute) : RouteOutcome()
    data class Failure(val kind: RouteErrorKind, val message: String) : RouteOutcome()
}

suspend fun fetchSafeRoute(start: Pair<Double, Double>, end: Pair<Double, Double>): RouteOutcome {
    return withContext(Dispatchers.IO) {
        val connection = try {
            java.net.URL("$API_BASE_URL/api/routes/recommend").openConnection() as java.net.HttpURLConnection
        } catch (e: Exception) {
            return@withContext RouteOutcome.Failure(RouteErrorKind.NETWORK, "서버 주소가 올바르지 않습니다.")
        }

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 20000
            connection.doOutput = true

            val body = JSONObject()
                .put("startLat", start.first)
                .put("startLng", start.second)
                .put("endLat", end.first)
                .put("endLng", end.second)
                .toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                // 오류 본문: { "error": "...", "message": "..." }
                val serverMessage = try {
                    JSONObject(text).optString("message").takeIf { it.isNotBlank() }
                } catch (e: Exception) {
                    null
                }
                return@withContext when (code) {
                    400 -> RouteOutcome.Failure(RouteErrorKind.INVALID_INPUT, serverMessage ?: "입력한 위치가 올바르지 않습니다.")
                    404 -> RouteOutcome.Failure(RouteErrorKind.NO_ROUTE, serverMessage ?: "경로를 찾지 못했습니다.")
                    502 -> RouteOutcome.Failure(
                        RouteErrorKind.SERVER,
                        "경로 조회 서비스에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요." + (serverMessage?.let { "\n($it)" } ?: "")
                    )
                    else -> RouteOutcome.Failure(
                        RouteErrorKind.SERVER,
                        "서버 오류 (HTTP $code)" + (serverMessage?.let { ": $it" } ?: "")
                    )
                }
            }

            val root = JSONObject(text)
            val pathJson = root.optJSONArray("path")
                ?: return@withContext RouteOutcome.Failure(RouteErrorKind.BAD_RESPONSE, "응답에 경로 좌표(path)가 없습니다.")

            val path = List(pathJson.length()) { i ->
                val pt = pathJson.getJSONObject(i)
                pt.getDouble("latitude") to pt.getDouble("longitude")
            }

            val zonesJson = root.optJSONArray("riskZones")
            val zones = List(zonesJson?.length() ?: 0) { i ->
                val z = zonesJson!!.getJSONObject(i)
                RouteRiskZone(
                    zoneId = z.getLong("zoneId"),
                    latitude = if (z.isNull("latitude")) null else z.getDouble("latitude"),
                    longitude = if (z.isNull("longitude")) null else z.getDouble("longitude"),
                    riskScore = if (z.isNull("riskScore")) null else z.getInt("riskScore"),
                    riskGrade = if (z.isNull("riskGrade")) null else z.getString("riskGrade"),
                    reason = if (z.isNull("reason")) null else z.getString("reason"),
                    distanceFromRoute = if (z.isNull("distanceFromRoute")) null else z.getDouble("distanceFromRoute")
                )
            }

            val route = SafeRoute(
                totalDistance = if (root.isNull("totalDistance")) null else root.optInt("totalDistance"),
                totalDuration = if (root.isNull("totalDuration")) null else root.optInt("totalDuration"),
                safetyScore = if (root.isNull("safetyScore")) null else root.optDouble("safetyScore"),
                riskZoneCount = if (root.isNull("riskZoneCount")) null else root.optInt("riskZoneCount"),
                fallback = root.optBoolean("fallback", false),
                path = path,
                riskZones = zones
            )

            when {
                path.size < 2 ->
                    RouteOutcome.Failure(RouteErrorKind.NO_ROUTE, "경로를 찾지 못했습니다. 다른 출발지나 목적지를 선택해 주세요.")
                route.fallback ->
                    RouteOutcome.Failure(
                        RouteErrorKind.NO_ROUTE,
                        "실제 경로가 아닌 대체 경로가 반환되어 표시하지 않습니다."
                    )
                else -> RouteOutcome.Success(route)
            }
        } catch (e: org.json.JSONException) {
            RouteOutcome.Failure(RouteErrorKind.BAD_RESPONSE, "서버 응답을 해석할 수 없습니다.")
        } catch (e: java.io.IOException) {
            RouteOutcome.Failure(RouteErrorKind.NETWORK, "서버에 연결할 수 없습니다. 인터넷 연결과 서버 상태를 확인해 주세요.")
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun startTrip(deviceId: String): Long? {
    return withContext(Dispatchers.IO) {
        val url = java.net.URL("$API_BASE_URL/api/trips/start")
        val connection = url.openConnection() as java.net.HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val body = """
                {
                  "userId": 1,
                  "deviceId": "$deviceId",
                  "startedAt": "${LocalDateTime.now().withNano(0)}"
                }
            """.trimIndent()
            android.util.Log.d("TRIP_START", "요청 JSON=$body")

            java.io.OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val statusCode = connection.responseCode
            val responseBody = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText()
            }

            android.util.Log.d(
                "TRIP_START",
                "HTTP=$statusCode, response=$responseBody"
            )

            if (statusCode !in 200..299 || responseBody == null) {
                return@withContext null
            }

            val regex = """"tripId"\s*:\s*(\d+)""".toRegex()
            regex.find(responseBody)?.groupValues?.get(1)?.toLongOrNull()
        } catch (e: Exception) {
            android.util.Log.e("TRIP_START", "주행 시작 API 오류", e)
            null
        }finally {
            connection.disconnect()
        }
    }
}

/** 서버에 연결하지 못한 경우(네트워크·시간 초과)는 알아보기 쉬운 문구로 바꾼다. */
fun friendlyError(e: Exception, fallback: String): String =
    if (e is java.io.IOException) "서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요." else e.message ?: fallback

/** 주행 종료 요청. 결과는 HTTP 코드이고, 서버에 연결하지 못하면 -1. endedAt은 종료를 누른 시각(재전송해도 유지). */
suspend fun endTripCode(tripId: Long, endedAt: LocalDateTime): Int {
    return withContext(Dispatchers.IO) {
        val url = java.net.URL("$API_BASE_URL/api/trips/$tripId/end")
        val connection = url.openConnection() as java.net.HttpURLConnection

        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val body = """
                {
                  "endedAt": "$endedAt",
                  "pendingEventCount": 0
                }
            """.trimIndent()

            java.io.OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val code = connection.responseCode

            val responseBody = try {
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                stream?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (e: Exception) {
                ""
            }

            android.util.Log.d(
                "TRIP_END",
                "tripId=$tripId, HTTP=$code, response=$responseBody"
            )

            code
        } catch (e: Exception) {
            android.util.Log.e("TRIP_END", "tripId=$tripId 종료 요청 실패", e)
            -1
        } finally {
            connection.disconnect()
        }
    }
}


/** 주행 중 스마트폰 GPS 전송. 실패해도 주행에는 영향 없으니 조용히 무시한다. */
suspend fun postTripLocation(tripId: Long, latitude: Double, longitude: Double) {
    withContext(Dispatchers.IO) {
        runCatching {
            val connection = java.net.URL("$API_BASE_URL/api/location/update").openConnection() as java.net.HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.doOutput = true
                java.io.OutputStreamWriter(connection.outputStream).use {
                    it.write("""{"tripId": $tripId, "latitude": $latitude, "longitude": $longitude}""")
                }
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════════════
// 앱 루트 — 네비게이션 상태 관리
// ══════════════════════════════════════════════════════════════════════
@Composable
fun DongHaengGuardApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    // 주행 상태 — 탭 전환 시 유지
    var tripActive by remember { mutableStateOf(false) }
    var riskLevel  by remember { mutableStateOf(RiskLevel.SAFE) }
    var distanceMm by remember { mutableStateOf(1250) }
    var currentTripId by remember { mutableStateOf<Long?>(null) }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    // 폰 뒤로가기: 지나온 화면으로 돌아간다. 하단 탭 화면으로 가면 기록을 비운다(탭에서 뒤로가기 = 앱 종료).
    val backStack = remember { mutableStateListOf<Screen>() }
    fun go(next: Screen) {
        if (next == screen) return
        if (next == Screen.MainDriving || next == Screen.DrivingHistory || next == Screen.RiskZones) backStack.clear()
        else backStack.add(screen)
        screen = next
    }
    BackHandler(enabled = backStack.isNotEmpty()) { screen = backStack.removeAt(backStack.lastIndex) }
    // 주행 홈에서 뒤로가기: 안내 후 2초 안에 한 번 더 누르면 종료. 기록/안전 지도 탭에서는 주행 홈으로.
    val backCtx = LocalContext.current
    var lastBackAt by remember { mutableStateOf(0L) }
    val onTab = screen == Screen.MainDriving || screen == Screen.DrivingHistory || screen == Screen.RiskZones
    BackHandler(enabled = backStack.isEmpty() && onTab) {
        if (screen != Screen.MainDriving) go(Screen.MainDriving)
        else {
            val now = System.currentTimeMillis()
            if (now - lastBackAt < 2000) (backCtx as? android.app.Activity)?.finish()
            else {
                lastBackAt = now
                Toast.makeText(backCtx, "뒤로 버튼을 한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val showBottomNav = screen == Screen.MainDriving ||
            screen == Screen.DrivingHistory ||
            screen == Screen.RiskZones

    val context = LocalContext.current
    UiPrefs.init(context)

    // 서버에 닿지 않아 보류해 둔 주행 종료를 10초마다 다시 보낸다(서버가 켜지면 성공 → 센서도 따라 멈춘다)
    LaunchedEffect(Unit) {
        while (true) {
            UiPrefs.pendingEnd()?.let { (tripId, endedAt) ->
                val code = endTripCode(tripId, LocalDateTime.parse(endedAt))
                // 성공이거나 서버가 거절(4xx: 이미 종료 등)하면 더 보낼 필요가 없다
                if (code in 200..499) {
                    UiPrefs.clearPendingEnd()
                    if (code in 200..299) Toast.makeText(context, "보류했던 주행 종료를 서버에 전송했어요.", Toast.LENGTH_SHORT).show()
                }
            }
            delay(10_000)
        }
    }

    // 앱을 켜면 위치 권한을 한 번 요청한다(이미 허용돼 있으면 팝업 없음)
    val startPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) startPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // 주행 중 위험 단계가 바뀌면(SAFE→WARNING/DANGER 등) 한 번 알림. shortcut: 같은 단계가 계속되면 반복 알림 없음.
    LaunchedEffect(riskLevel) {
        if (tripActive) alertRisk(context, riskLevel)
    }

    // 글씨 크기 설정: 시스템 글씨 크기 위에 배율을 곱한다
    val baseDensity = androidx.compose.ui.platform.LocalDensity.current
    val scaledDensity = androidx.compose.ui.unit.Density(baseDensity.density, baseDensity.fontScale * UiPrefs.fontScale)

    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides scaledDensity) {
    MaterialTheme {
        Scaffold(
            containerColor = AppColors.Background,
            bottomBar = {
                if (showBottomNav) {
                    BottomNavBar(currentScreen = screen) { go(it) }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                val s = screen
                when (s) {
                    Screen.Onboarding            -> OnboardingScreen          { go(it) }
                    Screen.Login                 -> LoginScreen                { go(it) }
                    Screen.LoginForm             -> LoginFormScreen            { go(it) }
                    Screen.Signup                -> SignupScreen               { go(it) }
                    Screen.SensorConnection -> SensorConnectionScreen(
                        onDeviceSelected = { selectedDeviceId = it },
                        onNavigate = { go(it) }
                    )
                    Screen.SensorManagement      -> SensorManagementScreen     { go(it) }
                    Screen.MainDriving -> MainDrivingScreen(
                        tripActive, { tripActive = it },
                        riskLevel, { riskLevel = it },
                        distanceMm, { distanceMm = it },
                        currentTripId, { currentTripId = it },
                        selectedDeviceId
                    ) { go(it) }
                    is Screen.TripEnd -> TripEndScreen(s.tripId) { go(it) }
                    Screen.DrivingHistory        -> DrivingHistoryScreen       { go(it) }
                    is Screen.DrivingDetail      -> DrivingDetailScreen(s.tripId) { go(it) }
                    Screen.RiskZones             -> RiskZonesScreen           { go(it) }
                    Screen.Settings              -> SettingsScreen             { go(it) }
                    Screen.NotificationSettings  -> NotificationSettingsScreen { go(it) }
                    Screen.AccessibilitySettings -> AccessibilitySettingsScreen{ go(it) }
                    Screen.Account               -> AccountScreen              { go(it) }
                }
            }
        }
    }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 하단 네비게이션 바
// ══════════════════════════════════════════════════════════════════════
@Composable
fun BottomNavBar(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    data class Tab(val screen: Screen, val label: String, val icon: String)
    val tabs = listOf(
        Tab(Screen.MainDriving,   "주행",     "▶"),
        Tab(Screen.DrivingHistory,"기록",     "≡"),
        Tab(Screen.RiskZones,     "안전 지도", "!"),
    )
    Surface(color = AppColors.Card, shadowElevation = 8.dp, modifier = Modifier.navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentScreen == tab.screen
                Column(
                    modifier = Modifier
                        .weight(1f).fillMaxHeight()
                        .clickable { onNavigate(tab.screen) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AppColors.PrimaryBg else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab.icon, fontSize = 18.sp,
                            color  = if (selected) AppColors.Primary else AppColors.TextSub,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        tab.label, fontSize = 11.sp,
                        color = if (selected) AppColors.Primary else AppColors.TextSub,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 1. 온보딩 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun OnboardingScreen(onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoMark(size = 96.dp)
            Spacer(Modifier.height(20.dp))
            LogoText(height = 60.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                "전동 휠체어 이용자를 위한\n실시간 위험 감지 서비스",
                fontSize = 14.sp, color = AppColors.TextSub, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            listOf(
                Triple("📶", "Bluetooth", "센서 모듈과 연결하여 실시간 데이터 수신"),
                Triple("📍", "위치",      "위험 발생 위치 기록 및 안전 경로 추천"),
                Triple("🔔", "알림",      "위험 상황 발생 시 즉시 알림 전송"),
            ).forEach { (icon, title, desc) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.PrimaryBg),
                            contentAlignment = Alignment.Center
                        ) { Text(icon, fontSize = 18.sp) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                            Text(desc, fontSize = 12.sp, color = AppColors.TextSub)
                        }
                    }
                }
            }
        }
        Surface(color = AppColors.Card, shadowElevation = 4.dp) {
            Button(
                onClick = { onNavigate(Screen.Login) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) { Text("시작하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 2. 로그인 선택 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun LoginScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppColors.Background).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoMark(size = 96.dp)
        Spacer(Modifier.height(20.dp))
        LogoText(height = 60.dp)
        Spacer(Modifier.height(8.dp))
        Text("안전한 이동을 함께 합니다", fontSize = 14.sp, color = AppColors.TextSub)
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { onNavigate(Screen.SensorConnection) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) { Text("게스트로 시작", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(8.dp))
        // shortcut: 인증 API가 없어 로그인·회원가입 진입을 막음. LoginFormScreen/SignupScreen은 API 생기면 다시 연결.
        Text("로그인 없이 바로 사용할 수 있습니다", fontSize = 12.sp, color = AppColors.TextSub, textAlign = TextAlign.Center)
    }
}

// ══════════════════════════════════════════════════════════════════════
// 3. 로그인 폼 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun LoginFormScreen(onNavigate: (Screen) -> Unit) {
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPw      by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "로그인", showBack = true, onBack = { onNavigate(Screen.Login) })
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormField(label = "이메일") {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    placeholder = { Text("example@email.com", color = AppColors.TextSub) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = dhTextFieldColors()
                )
            }
            FormField(label = "비밀번호") {
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    placeholder = { Text("비밀번호를 입력하세요", color = AppColors.TextSub) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPw = !showPw }) {
                            Text(if (showPw) "숨김" else "보기", fontSize = 12.sp, color = AppColors.TextSub)
                        }
                    },
                    colors = dhTextFieldColors()
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {}) {
                    Text("비밀번호를 잊으셨나요?", fontSize = 12.sp, color = AppColors.Primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        delay(1500)
                        isLoading = false
                        onNavigate(Screen.SensorConnection)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text(if (isLoading) "로그인 중..." else "로그인", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("아직 계정이 없으신가요?", fontSize = 13.sp, color = AppColors.TextSub)
                TextButton(onClick = { onNavigate(Screen.Signup) }) {
                    Text("회원가입하기", color = AppColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
            HDivider()
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("로그인 없이 바로 사용하고 싶으신가요?", fontSize = 12.sp, color = AppColors.TextSub)
                TextButton(onClick = { onNavigate(Screen.SensorConnection) }) {
                    Text("간편 시작으로 돌아가기", color = AppColors.Primary)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 4. 회원가입 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun SignupScreen(onNavigate: (Screen) -> Unit) {
    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPw       by remember { mutableStateOf("") }
    var showPw          by remember { mutableStateOf(false) }
    var showConfirmPw   by remember { mutableStateOf(false) }
    var agreedToTerms   by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "회원가입", showBack = true, onBack = { onNavigate(Screen.Login) })
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FormField(label = "이름") {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        placeholder = { Text("홍길동", color = AppColors.TextSub) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, colors = dhTextFieldColors()
                    )
                }
            }
            item {
                FormField(label = "이메일") {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        placeholder = { Text("example@email.com", color = AppColors.TextSub) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = dhTextFieldColors()
                    )
                }
            }
            item {
                FormField(label = "전화번호 (선택)") {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        placeholder = { Text("010-1234-5678", color = AppColors.TextSub) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = dhTextFieldColors()
                    )
                }
            }
            item {
                FormField(label = "비밀번호 (8자 이상)") {
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        placeholder = { Text("8자 이상 입력하세요", color = AppColors.TextSub) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPw = !showPw }) {
                                Text(if (showPw) "숨김" else "보기", fontSize = 12.sp, color = AppColors.TextSub)
                            }
                        },
                        colors = dhTextFieldColors()
                    )
                }
            }
            item {
                FormField(label = "비밀번호 확인") {
                    OutlinedTextField(
                        value = confirmPw, onValueChange = { confirmPw = it },
                        placeholder = { Text("비밀번호를 다시 입력하세요", color = AppColors.TextSub) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = if (showConfirmPw) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showConfirmPw = !showConfirmPw }) {
                                Text(if (showConfirmPw) "숨김" else "보기", fontSize = 12.sp, color = AppColors.TextSub)
                            }
                        },
                        colors = dhTextFieldColors()
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { agreedToTerms = !agreedToTerms }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (agreedToTerms) AppColors.Primary else AppColors.Card)
                            .border(1.dp, if (agreedToTerms) AppColors.Primary else AppColors.Border, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) { if (agreedToTerms) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Text("이용약관 및 개인정보 처리방침에 동의합니다", fontSize = 14.sp, color = AppColors.Text)
                }
            }
            if (errorMsg.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(AppColors.DangerBg).padding(12.dp)
                    ) { Text(errorMsg, color = AppColors.Danger, fontSize = 13.sp) }
                }
            }
            item {
                Button(
                    onClick = {
                        errorMsg = ""
                        when {
                            !agreedToTerms      -> errorMsg = "이용약관에 동의해주세요."
                            password != confirmPw -> errorMsg = "비밀번호가 일치하지 않습니다."
                            else -> scope.launch {
                                isLoading = true
                                delay(1500)
                                isLoading = false
                                onNavigate(Screen.SensorConnection)
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) { Text(if (isLoading) "가입 중..." else "회원가입", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("이미 계정이 있으신가요?", fontSize = 13.sp, color = AppColors.TextSub)
                    TextButton(onClick = { onNavigate(Screen.LoginForm) }) {
                        Text("로그인하기", color = AppColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 5. 센서 연결 화면
// ══════════════════════════════════════════════════════════════════════
data class ServerDevice(
    val deviceId: String,
    val deviceStatus: String
)

suspend fun fetchRegisteredDevices(userId: Long = 1L): List<ServerDevice> {
    return withContext(Dispatchers.IO) {
        val url = java.net.URL("$API_BASE_URL/api/devices?userId=$userId")
        val connection = url.openConnection() as java.net.HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val statusCode = connection.responseCode

            if (statusCode !in 200..299) {
                throw IllegalStateException("디바이스 조회 실패 (HTTP $statusCode)")
            }

            val response = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            val json = JSONObject(response)

            if (!json.optBoolean("success", false)) {
                throw IllegalStateException(
                    json.optString("message", "디바이스 조회에 실패했습니다.")
                )
            }

            val array = json.getJSONArray("data")

            List(array.length()) { index ->
                val item = array.getJSONObject(index)

                ServerDevice(
                    deviceId = item.getString("deviceId"),
                    deviceStatus = item.optString("deviceStatus", "UNKNOWN")
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}

enum class ConnStatus { IDLE, SEARCHING, FOUND, CONNECTING, CONNECTED, FAILED }

@Composable
fun SensorConnectionScreen(
    onDeviceSelected: (String) -> Unit,
    onNavigate: (Screen) -> Unit
) {
    var status by remember { mutableStateOf(ConnStatus.IDLE) }
    val scope = rememberCoroutineScope()

    var devices by remember { mutableStateOf<List<ServerDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<ServerDevice?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    fun searchDevices() {
        status = ConnStatus.SEARCHING
        errorMessage = ""

        scope.launch {
            try {
                val result = fetchRegisteredDevices()
                devices = result
                status = ConnStatus.FOUND
            } catch (e: Exception) {
                errorMessage = friendlyError(e, "디바이스 조회에 실패했습니다.")
                status = ConnStatus.FAILED
            }
        }
    }
    LaunchedEffect(Unit) {
        searchDevices()
    }
    // 연결 완료 후 자동 이동
    LaunchedEffect(status) {
        if (status == ConnStatus.CONNECTED) {
            delay(1500)
            onNavigate(Screen.MainDriving)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "센서 연결", showBack = true, onBack = { onNavigate(Screen.Login) })
        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
            when (status) {
                ConnStatus.IDLE -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(96.dp).clip(CircleShape).background(AppColors.Surface),
                        contentAlignment = Alignment.Center
                    ) { Text("📶", fontSize = 44.sp) }
                    Spacer(Modifier.height(20.dp))
                    Text("센서를 연결하세요", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                    Spacer(Modifier.height(8.dp))
                    Text("서버에 등록된 동행가드 센서를 조회합니다", fontSize = 14.sp, color = AppColors.TextSub)
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            status = ConnStatus.SEARCHING
                            scope.launch { delay(2000); status = ConnStatus.FOUND }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) { Text("🔍  센서 검색", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                }

                ConnStatus.SEARCHING, ConnStatus.CONNECTING -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(
                        if (status == ConnStatus.SEARCHING) "센서 검색 중..." else "센서 연결 중...",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (status == ConnStatus.SEARCHING) "서버에 등록된 센서를 조회하고 있습니다" else "잠시만 기다려 주세요",
                        fontSize = 14.sp, color = AppColors.TextSub
                    )
                }

                ConnStatus.FOUND -> Column(modifier = Modifier.fillMaxSize()) {
                    Text("발견된 센서", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                    Spacer(Modifier.height(4.dp))
                    Text("연결할 센서를 선택하세요", fontSize = 13.sp, color = AppColors.TextSub)
                    Spacer(Modifier.height(16.dp))
                    devices.forEach { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable {
                                selectedDevice = device
                                onDeviceSelected(device.deviceId)
                                status = ConnStatus.CONNECTED
                            },
                            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📶", fontSize = 20.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        device.deviceId,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.Text
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "서버 등록 상태: ${device.deviceStatus}",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSub,
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { searchDevices() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("다시 검색", color = AppColors.Text) }
                }

                ConnStatus.CONNECTED -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(96.dp).clip(CircleShape).background(AppColors.SafeBg),
                        contentAlignment = Alignment.Center
                    ) { Text("✓", fontSize = 44.sp, color = AppColors.Safe, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(20.dp))
                    Text("센서 선택 완료!", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Safe)
                    Spacer(Modifier.height(8.dp))
                    Text("사용할 센서가 선택되었습니다", fontSize = 14.sp, color = AppColors.TextSub)
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                ConnStatus.FAILED -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(96.dp).clip(CircleShape).background(AppColors.DangerBg),
                        contentAlignment = Alignment.Center
                    ) { Text("✕", fontSize = 44.sp, color = AppColors.Danger, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(20.dp))
                    Text("연결 실패", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Danger)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "서버 센서 목록 조회 실패\n$errorMessage",
                        fontSize = 14.sp,
                        color = AppColors.TextSub,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { searchDevices() },
                        modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) { Text("다시 연결", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { status = ConnStatus.IDLE },
                        modifier = Modifier.fillMaxWidth(0.8f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("다시 검색", color = AppColors.Text) }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 6. 센서 모듈 관리 화면
// ══════════════════════════════════════════════════════════════════════

@Composable
fun SensorManagementScreen(onNavigate: (Screen) -> Unit) {
    var devices by remember {
        mutableStateOf<List<ServerDevice>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null

        try {
            devices = fetchRegisteredDevices()
        } catch (e: Exception) {
            errorMessage = friendlyError(e, "센서 정보를 불러오지 못했습니다.")
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        AppTopBar(
            title = "센서 모듈 관리",
            showBack = true,
            onBack = { onNavigate(Screen.Settings) }
        )

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "센서 정보를 불러오지 못했어요.\n$errorMessage",
                        fontSize = 13.sp,
                        color = AppColors.TextSub,
                        textAlign = TextAlign.Center
                    )
                }
            }

            devices.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "등록된 센서가 없어요.",
                        fontSize = 14.sp,
                        color = AppColors.TextSub
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(devices) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.Card
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "전동 휠체어 센서 모듈",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Text
                                )

                                Text(
                                    text = "센서 ID: ${device.deviceId}",
                                    fontSize = 13.sp,
                                    color = AppColors.TextSub
                                )

                                HDivider()

                                Text(
                                    text = "서버 등록 상태: ${device.deviceStatus}",
                                    fontSize = 13.sp,
                                    color = AppColors.Text
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "실제 블루투스 연결 상태와 배터리 정보는 현재 확인할 수 없습니다.",
                            fontSize = 12.sp,
                            color = AppColors.TextSub
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                onNavigate(Screen.SensorConnection)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("센서 선택 화면으로 이동")
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 7. 메인 주행 화면
// ══════════════════════════════════════════════════════════════════════

data class LatestSensorEvent(
    val riskLevel: RiskLevel,
    val distanceMm: Int?
)

suspend fun fetchLatestSensorEvent(tripId: Long): LatestSensorEvent? {
    return withContext(Dispatchers.IO) {
        val connection = java.net.URL(
            "$API_BASE_URL/api/trips/$tripId"
        ).openConnection() as java.net.HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            if (connection.responseCode !in 200..299) {
                return@withContext null
            }

            val responseBody = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(responseBody)
            val data = root.optJSONObject("data")
                ?: return@withContext null

            val events = data.optJSONArray("events")
                ?: return@withContext null

            if (events.length() == 0) {
                return@withContext null
            }

            // 배열 순서에 의존하지 않고 발생 시각으로 최신 이벤트 선택
            val latestEvent = (0 until events.length())
                .mapNotNull { events.optJSONObject(it) }
                .filter { it.has("detectedAt") && !it.isNull("detectedAt") }
                .maxByOrNull { it.optString("detectedAt") }
                ?: return@withContext null

            val riskLevel = when (latestEvent.optString("riskLevel")) {
                "DANGER" -> RiskLevel.DANGER
                "WARNING" -> RiskLevel.WARNING
                "SAFE" -> RiskLevel.SAFE
                else -> return@withContext null
            }

            // 거리 데이터가 없으면 임의의 1250mm를 만들지 않음
            val distanceMm = if (
                latestEvent.has("distanceMm") &&
                !latestEvent.isNull("distanceMm")
            ) {
                latestEvent.getInt("distanceMm")
            } else {
                null
            }

            LatestSensorEvent(
                riskLevel = riskLevel,
                distanceMm = distanceMm
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "SENSOR_EVENT",
                "최근 센서 이벤트 조회 실패",
                e
            )
            null
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
fun MainDrivingScreen(
    tripActive: Boolean, onTripActiveChange: (Boolean) -> Unit,
    riskLevel: RiskLevel, onRiskLevelChange: (RiskLevel) -> Unit,
    distanceMm: Int, onDistanceMmChange: (Int) -> Unit,
    currentTripId: Long?, onCurrentTripIdChange: (Long?) -> Unit,
    selectedDeviceId: String?,
    onNavigate: (Screen) -> Unit
) {
    var aiPredictions by remember {
        mutableStateOf<List<AiPredictionResult>>(emptyList())
    }

    var aiLoading by remember {
        mutableStateOf(false)
    }

    var aiError by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(tripActive, currentTripId) {
        aiPredictions = emptyList()
        aiError = false
        aiLoading = false

        if (tripActive && currentTripId != null) {
            aiLoading = true

            while (true) {
                try {
                    aiPredictions = fetchAiPredictions(currentTripId)
                    aiError = false
                } catch (e: Exception) {
                    android.util.Log.e(
                        "AI_PREDICTION",
                        "AI 분석 결과 조회 실패",
                        e
                    )
                    aiError = true
                } finally {
                    aiLoading = false
                }

                // AI 추론은 비동기이므로 주기적으로 새 결과 확인
                delay(5000)
            }
        }
    }
    var showEndDialog by remember { mutableStateOf(false) }
    var ending by remember { mutableStateOf(false) }   // 종료 요청 중(재시도 포함)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var permTick by remember { mutableStateOf(0) }   // 권한 응답이 오면 아래 허용 여부를 다시 계산한다
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permTick++ }
    @Suppress("UNUSED_VARIABLE") val permVersion = permTick

    val fineLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val animBg by animateColorAsState(
        targetValue = riskBg(riskLevel),
        animationSpec = tween(300),
        label = "bg"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (riskLevel == RiskLevel.DANGER && tripActive) 1.04f else 1f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "scale"
    )

    LaunchedEffect(currentTripId, tripActive) {
        while (tripActive && currentTripId != null) {
            val latestEvent = fetchLatestSensorEvent(currentTripId)

            if (latestEvent != null) {
                onRiskLevelChange(latestEvent.riskLevel)

                latestEvent.distanceMm?.let { actualDistance ->
                    onDistanceMmChange(actualDistance)
                }
            }

            delay(1000)
        }
    }

    // 주행 중 스마트폰 GPS를 서버에 보낸다. 서버가 센서 이벤트의 위치를 이 값으로 덮어쓴다(POST /api/location/update).
    val locationGranted = fineLocationGranted || coarseLocationGranted
    var latestGps by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    LaunchedEffect(tripActive) {
        if (tripActive && !locationGranted) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    DisposableEffect(tripActive, locationGranted) {
        if (!tripActive || !locationGranted) return@DisposableEffect onDispose { }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                latestGps = location.latitude to location.longitude
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("API 29 이하 호환용")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        getCurrentLocation(context)?.let { latestGps = it }
        try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { manager.isProviderEnabled(it) }
                .forEach { manager.requestLocationUpdates(it, 2000L, 0f, listener, android.os.Looper.getMainLooper()) }
        } catch (e: SecurityException) {
            android.util.Log.e("GPS", "위치 권한 없음", e)
        }
        onDispose { manager.removeUpdates(listener) }
    }

    LaunchedEffect(currentTripId, tripActive, latestGps != null) {
        while (tripActive && currentTripId != null) {
            latestGps?.let { (lat, lng) -> postTripLocation(currentTripId, lat, lng) }
            delay(2000)
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = {
                Text("주행 종료", fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "주행을 종료하시겠습니까?\n주행 기록이 저장됩니다.",
                    color = AppColors.TextSub
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !ending,
                    onClick = {
                        scope.launch {
                            val tripId = currentTripId

                            if (tripId == null) {
                                showEndDialog = false

                                Toast.makeText(
                                    context,
                                    "종료할 주행 정보가 없습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@launch
                            }

                            // 서버가 끊겨 있어도 종료할 수 있게: 연결 실패·서버 오류는 최대 3번 재시도하고,
                            // 그래도 안 되면 종료를 보류로 저장해 두었다가 서버가 켜지면 자동 전송한다.
                            ending = true
                            val endedAt = LocalDateTime.now().withNano(0)
                            var code = endTripCode(tripId, endedAt)
                            var attempt = 1
                            while ((code == -1 || code >= 500) && attempt < 3) {
                                delay(1000L * attempt)
                                code = endTripCode(tripId, endedAt)
                                attempt++
                            }
                            ending = false

                            if (code == -1 || code >= 500) {
                                UiPrefs.savePendingEnd(tripId, endedAt.toString())
                                Toast.makeText(
                                    context,
                                    "서버에 연결할 수 없어 종료를 보류했어요. 연결되면 자동으로 전송됩니다.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (code !in 200..299) {
                                Toast.makeText(
                                    context,
                                    "주행 종료 저장에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            onCurrentTripIdChange(null)
                            onTripActiveChange(false)
                            onRiskLevelChange(RiskLevel.SAFE)
                            onDistanceMmChange(1250)
                            showEndDialog = false
                            onNavigate(Screen.TripEnd(tripId))
                        }
                    }
                ) {
                    Text(
                        if (ending) "종료 중..." else "종료",
                        color = AppColors.Danger,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("계속 주행", color = AppColors.TextSub)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Surface(color = AppColors.Card, shadowElevation = 1.dp) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "주행",
                        modifier = Modifier.padding(start = 12.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Text
                    )

                    TextButton(onClick = { onNavigate(Screen.Settings) }) {
                        Text("⚙", fontSize = 22.sp, color = AppColors.TextSub)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📶", fontSize = 12.sp)

                        Text(
                            text = if (selectedDeviceId != null) {
                                "센서 선택됨"
                            } else {
                                "센서 미선택"
                            },
                            fontSize = 12.sp,
                            color = if (selectedDeviceId != null) {
                                AppColors.Safe
                            } else {
                                AppColors.TextSub
                            }
                        )
                    }


                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📍", fontSize = 12.sp)
                        Text(
                            "위치 기능",
                            fontSize = 12.sp,
                            color = AppColors.TextSub
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tripActive) AppColors.Danger else AppColors.TextSub
                                )
                        )

                        Text(
                            if (tripActive) "주행 중" else "대기 중",
                            fontSize = 12.sp,
                            color = if (tripActive) AppColors.Danger else AppColors.TextSub
                        )
                    }
                }
            }
        }


        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {


            if (!tripActive) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LogoMark(size = 96.dp)

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "주행 시작 준비",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Text
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "사용할 센서가 선택되었습니다",
                        fontSize = 14.sp,
                        color = AppColors.TextSub,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(36.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                val deviceId = selectedDeviceId

                                if (deviceId == null) {
                                    Toast.makeText(
                                        context,
                                        "먼저 센서를 선택해주세요.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                val newTripId = startTrip(deviceId)

                                if (newTripId == null) {
                                    Toast.makeText(
                                        context,
                                        "주행 시작에 실패했습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                onCurrentTripIdChange(newTripId)
                                onTripActiveChange(true)

                                Toast.makeText(
                                    context,
                                    "주행을 시작했습니다. tripId=$newTripId",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        )
                    ) {
                        Text(
                            "▶  주행 시작",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
              // 내용만 스크롤, 주행 종료 버튼은 아래에 고정
              Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(animBg)
                        .border(2.dp, riskColor(riskLevel), RoundedCornerShape(16.dp))
                        .padding(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(riskColor(riskLevel).copy(alpha = 0.15f))
                                .border(3.dp, riskColor(riskLevel), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (riskLevel) {
                                    RiskLevel.SAFE -> "●"
                                    RiskLevel.WARNING -> "▲"
                                    RiskLevel.DANGER -> "!"
                                },
                                fontSize = 30.sp,
                                color = riskColor(riskLevel),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            riskLabel(riskLevel),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = riskColor(riskLevel)
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            riskDesc(riskLevel),
                            fontSize = 14.sp,
                            color = AppColors.Text,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.Card.copy(alpha = 0.8f))
                                .border(1.dp, AppColors.Border, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "전방 거리",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSub
                                )


                                Spacer(Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "$distanceMm",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = AppColors.Text
                                    )

                                    Text(
                                        "  mm",
                                        fontSize = 18.sp,
                                        color = AppColors.TextSub,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // AI 분석 결과 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.Card
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AI 분석 결과",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Text
                        )
                        val tiltPrediction = aiPredictions
                            .filter { it.modelType.equals("TILT", ignoreCase = true) }
                            .maxByOrNull { it.id }

                        val cameraPrediction = aiPredictions
                            .filter { it.modelType.equals("CAMERA", ignoreCase = true) }
                            .maxByOrNull { it.id }

                        fun riskLabel(risk: String?): String {
                            return when (risk?.uppercase()) {
                                "SAFE" -> "안전"
                                "WARNING" -> "주의"
                                "DANGER" -> "위험"
                                else -> "확인 불가"
                            }
                        }

                        fun predictionLabel(prediction: AiPredictionResult?): String {
                            return when {
                                prediction == null -> "분석 결과 없음"
                                prediction.status != "SUCCESS" -> "분석할 수 없음"
                                else -> riskLabel(prediction.predictedRisk)
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "현재 주행 상태",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Text
                            )

                            Text(
                                text = when {
                                    aiLoading && aiPredictions.isEmpty() ->
                                        "AI 분석 결과를 확인하고 있어요."
                                    aiError && aiPredictions.isEmpty() ->
                                        "AI 분석 정보를 불러오지 못했어요."
                                    aiPredictions.isEmpty() ->
                                        "아직 AI 분석 결과가 없어요."
                                    aiPredictions.all { it.status == "TIMEOUT" } ->
                                        "AI 분석 서버에 연결되지 않아 지금은 결과를 볼 수 없어요. 위험 판정은 센서 규칙으로 계속 동작해요."
                                    else ->
                                        "최근 수집된 센서 데이터를 기반으로 분석한 결과예요."
                                },
                                fontSize = 12.sp,
                                color = AppColors.TextSub
                            )

                            // 서버 오류로 이전 AI 결과가 표시되는 경우
                            if (aiError && aiPredictions.isNotEmpty()) {
                                Text(
                                    text = "서버 연결에 문제가 있어 이전에 조회한 AI 결과를 표시하고 있어요.",
                                    fontSize = 12.sp,
                                    color = AppColors.Warning
                                )
                            }

                            // 기울기 AI
                            Text(
                                text = "기울기 AI: ${predictionLabel(tiltPrediction)}",
                                fontSize = 12.sp,
                                color = AppColors.Text
                            )

                            if (tiltPrediction?.status == "SUCCESS") {
                                Text(
                                    text = "모델 점수: ${
                                        tiltPrediction.predictedProbability?.let {
                                            String.format(java.util.Locale.US, "%.3f", it)
                                        } ?: "확인 불가"
                                    } · 규칙 기반: ${riskLabel(tiltPrediction.ruleRisk)}",
                                    fontSize = 11.sp,
                                    color = AppColors.TextSub
                                )
                            }

                            // 카메라 AI
                            Text(
                                text = "카메라 AI: ${predictionLabel(cameraPrediction)}",
                                fontSize = 12.sp,
                                color = AppColors.Text
                            )

                            if (cameraPrediction?.status == "SUCCESS") {
                                Text(
                                    text = "모델 점수: ${
                                        cameraPrediction.predictedProbability?.let {
                                            String.format(java.util.Locale.US, "%.3f", it)
                                        } ?: "확인 불가"
                                    } · 규칙 기반: ${riskLabel(cameraPrediction.ruleRisk)}",
                                    fontSize = 11.sp,
                                    color = AppColors.TextSub
                                )
                            }

                            Text(
                                text = "AI 예측은 실험용 보조 정보이며 모델 점수는 사고 확률이 아니에요. 실제 안전 알림은 기존 규칙 기반 판정을 따릅니다.",
                                fontSize = 10.sp,
                                color = AppColors.TextSub
                            )
                        }

                    } // 기존 AI 카드의 Column 종료
                } // 기존 AI Card 종료
              } // 스크롤 영역 종료

                Button(
                    onClick = {
                        showEndDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Danger
                    )
                ) {
                    Text(
                        "■  주행 종료",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
// ══════════════════════════════════════════════════════════════════════
// 8. 주행 종료 화면
// ══════════════════════════════════════════════════════════════════════

data class TripEndSummary(
    val duration: String,
    val totalCount: Int,
    val dangerCount: Int,
    val warningCount: Int,
    val safeCount: Int,
    val highestRiskLevel: String?
)

suspend fun fetchTripEndSummary(tripId: Long): TripEndSummary {
    return withContext(Dispatchers.IO) {
        val url = java.net.URL("$API_BASE_URL/api/trips/$tripId")
        val connection = url.openConnection() as java.net.HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 10000

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("주행 조회 실패: HTTP ${connection.responseCode}")
            }

            val responseBody = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(responseBody)

            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(
                    root.optString("message", "주행 데이터를 조회할 수 없습니다.")
                )
            }

            val data = root.getJSONObject("data")

            if (data.getString("status") != "ENDED") {
                throw IllegalStateException("아직 종료되지 않은 주행입니다.")
            }

            val startedAt = LocalDateTime.parse(data.getString("startedAt"))
            val endedAt = LocalDateTime.parse(data.getString("endedAt"))

            val durationSeconds = java.time.Duration
                .between(startedAt, endedAt)
                .seconds
                .coerceAtLeast(0)

            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60

            val durationText = if (hours > 0) {
                "${hours}시간 ${minutes}분 ${seconds}초"
            } else if (minutes > 0) {
                "${minutes}분 ${seconds}초"
            } else {
                "${seconds}초"
            }

            val events = data.getJSONArray("events")

            var danger = 0
            var warning = 0
            var safe = 0

            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)

                when (event.optString("riskLevel")) {
                    "DANGER" -> danger++
                    "WARNING" -> warning++
                    "SAFE" -> safe++
                }
            }

            TripEndSummary(
                duration = durationText,
                totalCount = events.length(),
                dangerCount = danger,
                warningCount = warning,
                safeCount = safe,
                highestRiskLevel = data.optString("highestRiskLevel")
                    .takeIf { it.isNotBlank() && it != "null" }
            )
        } finally {
            connection.disconnect()
        }
    }
}
@Composable
fun TripEndScreen(
    tripId: Long,
    onNavigate: (Screen) -> Unit
) {
    var summary by remember(tripId) {
        mutableStateOf<TripEndSummary?>(null)
    }
    var isLoading by remember(tripId) { mutableStateOf(true) }
    var errorMessage by remember(tripId) { mutableStateOf<String?>(null) }

    var aiResults by remember(tripId) {
        mutableStateOf<List<AiPredictionResult>>(emptyList())
    }
    var aiLoading by remember(tripId) { mutableStateOf(true) }
    var aiError by remember(tripId) { mutableStateOf(false) }
    var aiRefreshKey by remember(tripId) { mutableStateOf(0) }

    LaunchedEffect(tripId) {
        isLoading = true
        errorMessage = null

        try {
            summary = fetchTripEndSummary(tripId)
        } catch (e: Exception) {
            errorMessage = friendlyError(e, "주행 리포트를 불러오지 못했습니다.")
        } finally {
            isLoading = false
        }
    }

    
    LaunchedEffect(tripId, aiRefreshKey) {
        aiLoading = true
        aiError = false
        aiResults = emptyList()

        try {
            // 주행 종료 직후 AI 저장이 늦어질 수 있으므로 최대 3회 조회
            for (attempt in 0 until 3) {
                aiResults = fetchAiPredictions(tripId)

                val hasSuccess = aiResults.any {
                    it.status == "SUCCESS"
                }

                if (hasSuccess) break

                if (attempt < 2) {
                    delay(3000)
                }
            }
        } catch (e: Exception) {
            aiError = true
            android.util.Log.e(
                "TRIP_END_AI",
                "AI 결과 조회 실패",
                e
            )
        } finally {
            aiLoading = false
        }
    }


    // 분석에 성공한 결과만 사용
    val successfulAiResults = aiResults.filter {
        it.status == "SUCCESS" && it.predictedRisk in listOf(
            "SAFE", "WARNING", "DANGER"
        )
    }

    fun highestPrediction(modelType: String): AiPredictionResult? {
        return successfulAiResults
            .filter { it.modelType == modelType }
            .maxByOrNull {
                when (it.predictedRisk) {
                    "DANGER" -> 3
                    "WARNING" -> 2
                    "SAFE" -> 1
                    else -> 0
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background).padding(horizontal = 24.dp, vertical = 12.dp)) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(AppColors.SafeBg),
            contentAlignment = Alignment.Center
        ) { Text("✓", fontSize = 28.sp, color = AppColors.Safe, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        Text("주행 완료", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
        Spacer(Modifier.height(4.dp))
        Text(
            "주행 ID: $tripId · 주행 기록이 서버에 저장되었습니다",
            fontSize = 14.sp,
            color = AppColors.TextSub
        )
        Spacer(Modifier.height(28.dp))
        if (isLoading) {
            CircularProgressIndicator(color = AppColors.Primary)
            Spacer(Modifier.height(16.dp))
        }

        if (errorMessage != null) {
            Text(
                errorMessage ?: "",
                color = AppColors.Danger,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
            shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("주행 요약", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕐", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("주행 시간", fontSize = 14.sp, color = AppColors.Text)
                    }
                    Text(
                        summary?.duration ?: "—",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Text
                    )
                }
                HDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "위험 단계별 센서 이벤트",
                    fontSize = 12.sp,
                    color = AppColors.TextSub
                )
                Spacer(Modifier.height(4.dp))

                listOf(
                    Triple(AppColors.Danger, "위험", summary?.dangerCount),
                    Triple(AppColors.Warning, "주의", summary?.warningCount),
                    Triple(AppColors.Safe, "안전", summary?.safeCount)
                ).forEach { (color, label, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp, color = AppColors.Text)
                        }
                        Text(
                            count?.let { "${it}건" } ?: "—",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    }
                }
                HDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", color = AppColors.Safe, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (summary != null) {
                            "주행 종료 확인 · 센서 이벤트 ${summary!!.totalCount}건"
                        } else {
                            "주행 리포트 조회 대기 중"
                        },
                        fontSize = 12.sp,
                        color = if (summary != null) AppColors.Safe else AppColors.TextSub
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("▼ 아래로 스크롤하면 AI 종합 분석 리포트가 있어요", fontSize = 11.sp, color = AppColors.Primary)
        Spacer(Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.Card
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "AI 종합 분석 리포트",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Text
                )

                Text(
                    "이번 주행에서 저장된 AI 예측 결과를 요약합니다.",
                    fontSize = 12.sp,
                    color = AppColors.TextSub
                )

                when {
                    aiLoading -> {
                        CircularProgressIndicator(
                            color = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("AI 분석 결과를 불러오는 중...")
                    }

                    aiError -> {
                        Text(
                            "AI 결과 조회에 실패했습니다.",
                            color = AppColors.Danger,
                            fontSize = 13.sp
                        )

                        OutlinedButton(
                            onClick = { aiRefreshKey++ }
                        ) {
                            Text("다시 조회")
                        }
                    }

                    else -> {
                        if (successfulAiResults.isEmpty()) {
                            Text(
                                if (aiResults.isNotEmpty() && aiResults.all { it.status == "TIMEOUT" })
                                    "AI 분석 서버에 연결되지 않아 이번 주행의 AI 결과가 없어요. 위험 판정은 센서 규칙으로 정상 동작했어요."
                                else "이번 주행의 AI 분석 결과가 아직 없습니다.",
                                fontSize = 13.sp,
                                color = AppColors.TextSub
                            )

                            OutlinedButton(
                                onClick = { aiRefreshKey++ }
                            ) {
                                Text("AI 결과 새로고침")
                            }
                        }
                        listOf(
                            "TILT" to "기울기 AI",
                            "CAMERA" to "카메라 AI"
                        ).forEach { (modelType, label) ->
                            val prediction = highestPrediction(modelType)

                            val riskText = when (prediction?.predictedRisk) {
                                "DANGER" -> "위험"
                                "WARNING" -> "주의"
                                "SAFE" -> "안전"
                                else -> "결과 없음"
                            }

                            val riskTextColor = when (
                                prediction?.predictedRisk
                            ) {
                                "DANGER" -> AppColors.Danger
                                "WARNING" -> AppColors.Warning
                                "SAFE" -> AppColors.Safe
                                else -> AppColors.TextSub
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    label,
                                    fontSize = 14.sp,
                                    color = AppColors.Text
                                )

                                Text(
                                    riskText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = riskTextColor
                                )
                            }
                        }

                        HDivider()

                        Text(
                            "AI 분석 성공: ${successfulAiResults.size}건",
                            fontSize = 12.sp,
                            color = AppColors.TextSub
                        )
                    }
                }

                Text(
                    "AI 예측은 보조 분석 정보이며 실제 사고 발생 확률을 의미하지 않습니다.",
                    fontSize = 11.sp,
                    color = AppColors.TextSub
                )
            }
        }


        
    } // 스크롤 영역 종료 — 아래 버튼은 고정
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onNavigate(Screen.DrivingHistory) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) { Text("≡  기록 보기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onNavigate(Screen.MainDriving) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("메인으로 돌아가기", fontSize = 15.sp, color = AppColors.Text) }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 9. 주행 기록 목록 화면
// ══════════════════════════════════════════════════════════════════════

data class ServerTripRecord(
    val tripId: Long,
    val startedAt: String,
    val endedAt: String?,
    val status: String,
    val eventCount: Int,
    val highestRiskLevel: String,
    val deviceId: String
)


suspend fun fetchTripHistory(userId: Long = 1L): List<ServerTripRecord> {
    return withContext(Dispatchers.IO) {
        val url = java.net.URL("$API_BASE_URL/api/trips?userId=$userId")
        val connection = url.openConnection() as java.net.HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 10000

            val statusCode = connection.responseCode

            if (statusCode !in 200..299) {
                throw IllegalStateException("주행 기록 조회 실패: HTTP $statusCode")
            }

            val responseBody = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(responseBody)

            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(
                    root.optString("message", "주행 기록 조회 실패")
                )
            }

            val array = root.getJSONArray("data")

            val records = List(array.length()) { index ->
                val item = array.getJSONObject(index)

                ServerTripRecord(
                    tripId = item.getLong("tripId"),
                    startedAt = item.getString("startedAt"),
                    endedAt = item.optString("endedAt").takeIf {
                        it.isNotBlank() && it != "null"
                    },
                    status = item.getString("status"),
                    eventCount = item.optInt("eventCount", 0),
                    highestRiskLevel = item.optString(
                        "highestRiskLevel", ""
                    ).takeIf {
                        it in listOf("SAFE", "WARNING", "DANGER")
                    } ?: "",
                    deviceId = item.optString("deviceId", "")
                )
            }

            // 최고 위험 등급이 없는 주행만 이벤트로 계산
            records.map { trip ->
                if (
                    trip.eventCount > 0 &&
                    trip.highestRiskLevel !in listOf(
                        "SAFE", "WARNING", "DANGER"
                    )
                ) {
                    try {
                        val events = fetchTripDetail(
                            trip.tripId.toString()
                        ).events

                        val highest = when {
                            events.any {
                                it.riskLevel == RiskLevel.DANGER
                            } -> "DANGER"

                            events.any {
                                it.riskLevel == RiskLevel.WARNING
                            } -> "WARNING"

                            events.any {
                                it.riskLevel == RiskLevel.SAFE
                            } -> "SAFE"

                            else -> ""
                        }

                        trip.copy(highestRiskLevel = highest)
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "TRIP_HISTORY",
                            "주행 ${trip.tripId} 최고 위험 계산 실패",
                            e
                        )
                        trip
                    }
                } else {
                    trip
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
fun DrivingHistoryScreen(onNavigate: (Screen) -> Unit) {
    var trips by remember {
        mutableStateOf<List<ServerTripRecord>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        try {
            trips = fetchTripHistory()
        } catch (e: Exception) {
            errorMessage = friendlyError(e, "주행 기록을 불러오지 못했습니다.")
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "주행 기록")
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    errorMessage ?: "주행 기록 조회 실패",
                    fontSize = 14.sp,
                    color = AppColors.Danger
                )
            }
        } else if (trips.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("≡", fontSize = 48.sp, color = AppColors.TextSub)
                    Spacer(Modifier.height(16.dp))
                    Text("주행 기록이 없습니다", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                    Text("첫 주행을 시작해보세요", fontSize = 14.sp, color = AppColors.TextSub)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                items(trips) { trip ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.DrivingDetail(trip.tripId.toString())) },
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                        shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    trip.startedAt.substringBefore("T"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Text
                                )
                                Text(">", fontSize = 18.sp, color = AppColors.TextSub)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🕐", fontSize = 12.sp)
                                Text(
                                    "${trip.startedAt.substringAfter("T").take(5)}" +
                                            " · ${if (trip.status == "ENDED") "주행 종료" else "진행 중"}" +
                                            " · 이벤트 ${trip.eventCount}건",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSub
                                )
                            }
                            Spacer(Modifier.height(10.dp))

                            val maxRisk = when (trip.highestRiskLevel) {
                                "DANGER" -> RiskLevel.DANGER
                                "WARNING" -> RiskLevel.WARNING
                                "SAFE" -> RiskLevel.SAFE
                                else -> null
                            }

                            if (maxRisk != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(riskBg(maxRisk))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        "최고 위험: ${riskLabel(maxRisk)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = riskColor(maxRisk)
                                    )
                                }
                            } else {
                                Text(
                                    "최고 위험: 기록 없음",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSub
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 10. 주행 상세 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun DrivingDetailScreen(tripId: String, onNavigate: (Screen) -> Unit) {
    var loading     by remember { mutableStateOf(true) }
    var detail      by remember { mutableStateOf<TripDetail?>(null) }
    var expandedId  by remember { mutableStateOf<String?>(null) }
    var showMap     by remember { mutableStateOf(false) }   // 지도는 기본으로 접혀 있고, 🗺 버튼이나 '지도에서 보기'로 연다
    var showSummaryList by remember { mutableStateOf(false) }   // 위험 요약 상세 목록(드롭다운)
    var focusId     by remember { mutableStateOf<String?>(null) }   // '지도에서 보기'로 고른 이벤트
    val listState   = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope       = rememberCoroutineScope()
    var showRawEvents by remember(tripId) { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tripId) {
        loading = true
        errorMessage = null
        detail = null

        try {
            detail = fetchTripDetail(tripId)
        } catch (e: Exception) {
            android.util.Log.e("TRIP_DETAIL", "주행 상세 조회 실패", e)
            errorMessage = "주행 정보를 불러오지 못했어요."
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        // 헤더
        Surface(color = AppColors.Card, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onNavigate(Screen.DrivingHistory) }) {
                    Text("<  주행 상세", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showMap = !showMap }) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (showMap) AppColors.Primary else AppColors.Surface),
                        contentAlignment = Alignment.Center
                    ) { Text("🗺", fontSize = 16.sp) }
                }
            }
        }

        if (loading) return   // 로딩 표시 없이 헤더만 두고, 조회가 끝나면 내용이 채워진다
        val d = detail

        if (d == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "주행 정보가 없어요.",
                        fontSize = 15.sp,
                        color = AppColors.TextSub
                    )

                    TextButton(
                        onClick = { onNavigate(Screen.DrivingHistory) }
                    ) {
                        Text("주행 기록으로 돌아가기")
                    }
                }
            }
            return
        }

        val riskSituations = summarizeRiskSituations(d.events)

        // 위험 단계가 바뀐 이벤트만 추출 (첫 이벤트는 SAFE가 아닐 때만 포함). prevLevel: 직전 단계
        val prevLevelById = mutableMapOf<String, RiskLevel?>()
        val transitionEvents = mutableListOf<RiskEvent>()
        var prevLevel: RiskLevel? = null
        for (e in d.events.sortedBy { it.time }) {
            val changed = if (prevLevel == null) e.riskLevel != RiskLevel.SAFE else e.riskLevel != prevLevel
            if (changed) {
                transitionEvents.add(e)
                prevLevelById[e.id] = prevLevel
            }
            prevLevel = e.riskLevel
        }
        val shownEvents = if (showRawEvents) d.events else transitionEvents
        val geoMarkers = transitionEvents.mapIndexedNotNull { idx, ev ->
            if (ev.lat == null || ev.lng == null) null
            else GeoMarker(ev.id, ev.lat, ev.lng, "${idx + 1}. ${riskLabel(ev.riskLevel)} (${ev.time})", riskColor(ev.riskLevel))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 날짜·시작·종료·주행 시간 (맨 위)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(d.date, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("시작 시간", fontSize = 12.sp, color = AppColors.TextSub)
                                Text(d.startTime, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("종료 시간", fontSize = 12.sp, color = AppColors.TextSub)
                                Text(d.endTime, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕐", fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(d.duration, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                        }
                    }
                }
            }

            // 지도
            if (showMap) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                        shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🗺", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("위험 발생 위치 지도", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                    Text("위험 단계 변경 지점 ${geoMarkers.size}곳 (좌표 있는 이벤트만)", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                            }
                            if (geoMarkers.isEmpty()) {
                                Text(
                                    "위치 좌표가 기록된 이벤트가 없습니다.",
                                    fontSize = 12.sp, color = AppColors.TextSub,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            } else {
                                OsmMapView(
                                    markers = geoMarkers,
                                    modifier = Modifier.fillMaxWidth().height(240.dp),
                                    onMarkerClick = { id -> expandedId = id },
                                    fitPoints = d.events.firstOrNull { it.id == focusId }
                                        ?.takeIf { it.lat != null && it.lng != null }
                                        ?.let { listOf(it.lat!! to it.lng!!) }
                                )
                            }
                        }
                    }
                }
            }

            // 위험 상황 요약 (주행 종료 화면처럼 단계별 건수 + 짧은 한 줄 요약)
            item {
                val dangerCnt = d.events.count { it.riskLevel == RiskLevel.DANGER }
                val warnCnt = d.events.count { it.riskLevel == RiskLevel.WARNING }
                val safeCnt = d.events.count { it.riskLevel == RiskLevel.SAFE }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("주행 위험 요약", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (riskSituations.isEmpty()) "이번 주행은 주의·위험 상황 없이 안전했어요."
                            else "주의·위험 상황이 ${riskSituations.size}번 있었어요.",
                            fontSize = 13.sp,
                            color = if (riskSituations.isEmpty()) AppColors.Safe else AppColors.Text
                        )
                        HDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("위험 단계별 센서 이벤트", fontSize = 12.sp, color = AppColors.TextSub)
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            Triple(AppColors.Danger, "위험", dangerCnt),
                            Triple(AppColors.Warning, "주의", warnCnt),
                            Triple(AppColors.Safe, "안전", safeCnt)
                        ).forEach { (color, label, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, fontSize = 14.sp, color = AppColors.Text)
                                }
                                Text("${count}건", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
                            }
                        }
                        if (riskSituations.isNotEmpty()) {
                            HDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showSummaryList = !showSummaryList }.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("상황별 자세히 보기 (${riskSituations.size}건)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                Text(if (showSummaryList) "▲" else "▼", fontSize = 12.sp, color = AppColors.TextSub)
                            }
                        }
                        if (showSummaryList) riskSituations.forEachIndexed { _, s ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${s.startTime} 부근 · ${s.minDistanceMm?.let { "가장 가까울 때 ${it}mm" } ?: "거리 정보 없음"}",
                                    fontSize = 12.sp, color = AppColors.TextSub, modifier = Modifier.weight(1f)
                                )
                                Text(riskLabel(s.highestRisk), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = riskColor(s.highestRisk))
                            }
                        }
                    }
                }
            }


            // 이벤트 헤더
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (showRawEvents) "원본 센서 이벤트" else "위험 단계 변경 이벤트",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text
                    )
                    Text("${shownEvents.size}건", fontSize = 13.sp, color = AppColors.TextSub)
                }
            }

            if (shownEvents.isEmpty()) {
                item {
                    Text(
                        "위험 단계 변경 이력이 없습니다.",
                        fontSize = 13.sp, color = AppColors.TextSub,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }

            items(shownEvents, key = { it.id }) { ev ->
                val expanded = expandedId == ev.id
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(riskBg(ev.riskLevel))
                        .border(1.dp, riskColor(ev.riskLevel), RoundedCornerShape(12.dp))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expandedId = if (expanded) null else ev.id }.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (ev.riskLevel) {
                                            RiskLevel.DANGER -> "!"
                                            RiskLevel.WARNING -> "▲"
                                            RiskLevel.SAFE -> "●"
                                        },
                                        fontSize = 16.sp,
                                        color = riskColor(ev.riskLevel),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(riskLabel(ev.riskLevel), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = riskColor(ev.riskLevel))
                                    if (!showRawEvents) {
                                        Text(
                                            prevLevelById[ev.id]?.let { "  (${riskLabel(it)} → ${riskLabel(ev.riskLevel)})" }
                                                ?: "  (기록 시작)",
                                            fontSize = 12.sp, color = AppColors.TextSub
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${if (ev.distanceMm >= 0) "${ev.distanceMm}mm" else "거리 정보 없음"}  ·  ${ev.location}",
                                    fontSize = 12.sp,
                                    color = AppColors.Text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(ev.time, fontSize = 12.sp, color = AppColors.TextSub)
                                Text(if (expanded) "▲" else "▼", fontSize = 12.sp, color = AppColors.TextSub)
                            }
                        }
                        if (expanded) {
                            HDivider()
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.Card.copy(alpha = 0.7f))
                                        .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("전방 거리", fontSize = 11.sp, color = AppColors.TextSub)
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            if (ev.distanceMm >= 0) {
                                                Text(
                                                    "${ev.distanceMm}",
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = AppColors.Text
                                                )
                                                Text(
                                                    " mm",
                                                    fontSize = 14.sp,
                                                    color = AppColors.TextSub,
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )
                                            } else {
                                                Text(
                                                    "거리 정보 없음",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AppColors.TextSub
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("📍", fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text("발생 위치", fontSize = 11.sp, color = AppColors.TextSub)
                                        Text(ev.location, fontSize = 12.sp, color = AppColors.Text)
                                        if (ev.lat != null && ev.lng != null) {
                                            Text(
                                                text = "%.4f, %.4f".format(
                                                    java.util.Locale.US,
                                                    ev.lat,
                                                    ev.lng
                                                ),
                                                fontSize = 11.sp,
                                                color = AppColors.TextSub
                                            )
                                        }
                                    }
                                }
                                ev.imageUrl?.let { url ->
                                    Column {
                                        Text("감지 사진", fontSize = 11.sp, color = AppColors.TextSub)
                                        Spacer(Modifier.height(4.dp))
                                        EventPhoto(url, Modifier.fillMaxWidth())
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        focusId = ev.id
                                        showMap = true
                                        scope.launch { delay(150); listState.animateScrollToItem(1) }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("🗺  지도에서 보기", fontSize = 12.sp, color = AppColors.Text) }
                            }
                        }
                    }
                }
            }

            // 맨 아래: 원본 이벤트 펼치기/접기 + 맨 위로
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showRawEvents = !showRawEvents; expandedId = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showRawEvents) "원본 이벤트 접기" else "원본 이벤트 펼쳐보기 (${d.events.size}건)",
                            color = AppColors.TextSub
                        )
                    }
                    if (showRawEvents) {
                        Button(
                            onClick = { scope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                        ) { Text("▲  맨 위로") }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 11. 위험구간 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun RiskZonesScreen(onNavigate: (Screen) -> Unit) {
    var riskZones by remember {
        mutableStateOf<List<ServerRiskZone>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedZoneId by remember { mutableStateOf<Long?>(null) }
    var showZoneMap by remember { mutableStateOf(false) }
    var routeMode by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        errorMessage = null

        try {
            riskZones = fetchRiskZones()
        } catch (e: Exception) {
            errorMessage = friendlyError(e, "위험 구간을 불러오지 못했습니다.")
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // 다른 화면의 상단 헤더와 같은 흰색 Surface(그림자 1dp) + 18sp SemiBold 제목
        Surface(
            color = AppColors.Card,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "안전 지도",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Text
                )

                if (!routeMode) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "주행 데이터를 기반으로 확인된 위험 구간입니다.",
                        fontSize = 12.sp,
                        color = AppColors.TextSub
                    )
                }

                Spacer(Modifier.height(10.dp))

                SegmentedToggle(
                    options = listOf("위험구간 보기", "경로 위험도 안내"),
                    selected = if (routeMode) 1 else 0,
                    onSelect = { routeMode = it == 1 },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (routeMode) {
            SafeRouteSection(riskZones = riskZones)
            return@Column
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.Primary
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "위험 구간을 불러오는 중...",
                            fontSize = 13.sp,
                            color = AppColors.TextSub
                        )
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "위험 구간 정보를 불러오지 못했어요.\n${errorMessage}",
                            fontSize = 13.sp,
                            color = AppColors.TextSub,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { reloadKey++ }) { Text("다시 시도") }
                    }
                }
            }

            riskZones.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "아직 등록된 위험 구간이 없어요.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Text
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "위험 구간이 등록되면 이곳에 표시됩니다.",
                            fontSize = 12.sp,
                            color = AppColors.TextSub,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    SegmentedToggle(
                        options = listOf("목록", "지도"),
                        selected = if (showZoneMap) 1 else 0,
                        onSelect = { showZoneMap = it == 1 },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    if (showZoneMap) {
                        val zoneMarkers = riskZones.mapNotNull { z ->
                            val lat = z.latitude
                            val lng = z.longitude
                            if (lat == null || lng == null) null
                            else GeoMarker("${z.zoneId}", lat, lng, "위험 구간 #${z.zoneId}", gradeFg(z.riskGrade))
                        }
                        val selectedZone = riskZones.find { it.zoneId == selectedZoneId }

                        if (zoneMarkers.isEmpty()) {
                            Text(
                                "좌표가 있는 위험 구간이 없습니다.",
                                fontSize = 13.sp, color = AppColors.TextSub,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp)
                            )
                        } else {
                            // 지도를 남은 화면 전체에 두고, 선택한 구간의 상세 카드를 지도 위에 겹쳐 표시한다
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                OsmMapView(
                                    markers = zoneMarkers,
                                    modifier = Modifier.fillMaxSize(),
                                    onMarkerClick = { id -> selectedZoneId = id.toLongOrNull() }
                                )
                                selectedZone?.let { z ->
                                    Card(
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "위험 구간 #${z.zoneId}",
                                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                                    color = AppColors.Text, modifier = Modifier.weight(1f)
                                                )
                                                GradePill(z.riskGrade)
                                            }
                                            Text(
                                                "위험 점수 ${z.riskScore?.toString() ?: "정보 없음"} · 위험 발생 ${z.zoneEventCount?.toString() ?: "정보 없음"}회",
                                                fontSize = 12.sp, color = AppColors.TextSub
                                            )
                                            Text(
                                                z.reason?.takeIf { it.isNotBlank() } ?: "상세 분석 정보가 없습니다.",
                                                fontSize = 12.sp, color = AppColors.Text
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(riskZones.sortedByDescending { it.riskScore ?: -1 }) { zone ->
                                val expanded = selectedZoneId == zone.zoneId

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedZoneId = if (expanded) null else zone.zoneId },
                                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "위험 구간 #${zone.zoneId}",
                                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                                color = AppColors.Text, modifier = Modifier.weight(1f)
                                            )
                                            GradePill(zone.riskGrade)
                                        }

                                        Text(
                                            text = "위험 점수 ${zone.riskScore?.toString() ?: "정보 없음"} · 위험 발생 ${zone.zoneEventCount?.toString() ?: "정보 없음"}회",
                                            fontSize = 12.sp,
                                            color = AppColors.TextSub
                                        )

                                        if (expanded) {
                                            HDivider()

                                            Text(
                                                text = zone.reason?.takeIf { it.isNotBlank() } ?: "상세 분석 정보가 없습니다.",
                                                fontSize = 12.sp,
                                                color = AppColors.Text
                                            )

                                            Text(
                                                text = if (zone.latitude != null && zone.longitude != null) {
                                                    "좌표: %.4f, %.4f".format(java.util.Locale.US, zone.latitude, zone.longitude)
                                                } else {
                                                    "위치 정보 없음"
                                                },
                                                fontSize = 11.sp,
                                                color = AppColors.TextSub
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 안전 지도 공용 UI (기존 AppColors·12dp 카드·Primary 버튼 규칙 재사용) ──────────
fun gradeFg(grade: String?): Color = when (grade) {
    "HIGH" -> AppColors.Danger
    "MEDIUM" -> AppColors.Warning
    "LOW" -> AppColors.Safe
    else -> AppColors.TextSub
}

/** 위험 등급 배지: 등급별 배경(…Bg)과 글자색을 기존 위험 단계 색상 체계로 표시 */
@Composable
fun GradePill(grade: String?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(grade?.let { riskGradeBg(it) } ?: AppColors.Surface)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            grade?.let { riskGradeLabel(it) } ?: "등급 없음",
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = gradeFg(grade)
        )
    }
}

/** 하단 탭과 같은 Primary / PrimaryBg 규칙을 쓰는 2~3칸 전환 컨트롤 */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) AppColors.Primary else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) AppColors.OnPrimary else AppColors.TextSub
                )
            }
        }
    }
}

@Composable
fun LinkText(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = if (enabled) AppColors.Primary else AppColors.TextSub,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    )
}

@Composable
fun RoutePointRow(
    dot: Color,
    label: String,
    value: String?,
    onClear: (() -> Unit)?,
    actions: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = AppColors.TextSub)
            Text(
                value ?: "선택 안 됨", fontSize = 13.sp,
                color = if (value == null) AppColors.TextSub else AppColors.Text,
                fontWeight = if (value == null) FontWeight.Normal else FontWeight.Medium,
                maxLines = 1
            )
        }
        actions()
        if (onClear != null && value != null) LinkText("✕", onClick = onClear)
    }
}

@Composable
fun RouteStat(label: String, value: String) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.Surface).padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 10.sp, color = AppColors.TextSub)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
    }
}

// 표시할 점이 없을 때 지도를 여는 기본 위치 (docs/06_API_MAPPING.md 예시 좌표)
const val DEFAULT_MAP_LAT = 35.1796
const val DEFAULT_MAP_LNG = 129.0756
val DEFAULT_MAP_CENTER = DEFAULT_MAP_LAT to DEFAULT_MAP_LNG

// osmdroid 실제 지도 (API 키 불필요, OpenStreetMap 타일 사용)
data class GeoMarker(val id: String, val lat: Double, val lng: Double, val title: String, val color: Color)

@Composable
fun OsmMapView(
    markers: List<GeoMarker>,
    modifier: Modifier = Modifier,
    onMarkerClick: (String) -> Unit = {},
    polyline: List<Pair<Double, Double>> = emptyList(),
    onMapTap: ((Double, Double) -> Unit)? = null,
    // null: 마커·경로가 바뀔 때마다 전체가 보이도록 맞춘다. 값이 있으면 그 점들이 바뀔 때만 맞춘다(비어 있으면 defaultCenter)
    fitPoints: List<Pair<Double, Double>>? = null,
    defaultCenter: Pair<Double, Double> = DEFAULT_MAP_CENTER
) {
    val currentOnClick by rememberUpdatedState(onMarkerClick)
    val currentOnTap by rememberUpdatedState(onMapTap)

    // osmdroid 는 자기 영역 밖(위쪽 헤더)까지 타일을 그리는 경우가 있어서 영역 안으로 잘라 준다
    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { ctx ->
            org.osmdroid.config.Configuration.getInstance().apply {
                userAgentValue = ctx.packageName
                osmdroidBasePath = ctx.cacheDir
                osmdroidTileCache = java.io.File(ctx.cacheDir, "osmdroid-tiles")
            }
            org.osmdroid.views.MapView(ctx).apply {
                setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(org.osmdroid.util.GeoPoint(defaultCenter.first, defaultCenter.second))
                // 스크롤 컨테이너 안에서도 지도 드래그가 가능하도록
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
            }
        },
        update = { map ->
            val key = markers to polyline
            val previous = map.tag as? Pair<*, *>
            if (previous?.first == key && previous.second == fitPoints) return@AndroidView
            // 오버레이가 같으면 다시 그리지 않고, 맞춤 대상이 바뀐 경우만 아래에서 시점을 이동한다
            val refit = fitPoints == null || previous?.second != fitPoints
            map.tag = key to fitPoints
            map.overlays.clear()

            // 지도 탭 좌표 전달 (마커 탭이 우선 처리되도록 맨 아래 레이어에 둔다)
            map.overlays.add(
                org.osmdroid.views.overlay.MapEventsOverlay(object : org.osmdroid.events.MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: org.osmdroid.util.GeoPoint): Boolean {
                        val tap = currentOnTap ?: return false
                        tap(p.latitude, p.longitude)
                        return true
                    }
                    override fun longPressHelper(p: org.osmdroid.util.GeoPoint) = false
                })
            )

            val linePoints = polyline.map { org.osmdroid.util.GeoPoint(it.first, it.second) }
            if (linePoints.size >= 2) {
                map.overlays.add(
                    org.osmdroid.views.overlay.Polyline(map).apply {
                        setPoints(linePoints)
                        outlinePaint.color = AppColors.Primary.toArgb()
                        outlinePaint.strokeWidth = 12f
                    }
                )
            }

            val points = markers.map { org.osmdroid.util.GeoPoint(it.lat, it.lng) }
            markers.forEachIndexed { i, m ->
                val dot = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(m.color.toArgb())
                    setStroke(4, android.graphics.Color.WHITE)
                    setSize(48, 48)
                }
                map.overlays.add(
                    org.osmdroid.views.overlay.Marker(map).apply {
                        position = points[i]
                        icon = dot
                        title = m.title
                        setAnchor(
                            org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                            org.osmdroid.views.overlay.Marker.ANCHOR_CENTER
                        )
                        setOnMarkerClickListener { marker, _ ->
                            marker.showInfoWindow()
                            currentOnClick(m.id)
                            true
                        }
                    }
                )
            }

            val target = fitPoints?.map { org.osmdroid.util.GeoPoint(it.first, it.second) } ?: (points + linePoints)
            if (refit) map.post {
                if (target.isEmpty()) {
                    map.controller.setZoom(15.0)
                    map.controller.setCenter(org.osmdroid.util.GeoPoint(defaultCenter.first, defaultCenter.second))
                } else if (target.size == 1) {
                    map.controller.setZoom(16.0)
                    map.controller.setCenter(target[0])
                } else {
                    val box = org.osmdroid.util.BoundingBox.fromGeoPoints(target)
                    if (linePoints.size >= 2) {
                        // 경로가 있을 때는 위쪽 선택 카드와 아래쪽 결과 시트에 가려지지 않도록 범위를 위아래로 넓혀 맞춘다
                        val span = box.latitudeSpan
                        val padded = org.osmdroid.util.BoundingBox(
                            box.latNorth + span * 0.10, box.lonEast,
                            box.latSouth - span * 0.70, box.lonWest
                        )
                        map.zoomToBoundingBox(padded, false, 60)
                    } else {
                        map.zoomToBoundingBox(box, false, 80)
                    }
                }
            }
            map.invalidate()
        },
        onRelease = { it.onDetach() }
    )
}


// 경로 위험도 안내: 출발지·목적지를 정하면 POST /api/routes/recommend로 경로와 주변 위험구간 정보를 받아 표시한다.
@Composable
fun SafeRouteSection(riskZones: List<ServerRiskZone>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var start by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var destination by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var picking by remember { mutableStateOf<String?>(null) } // "start" | "destination" | null
    var locating by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(true) }   // 결과 시트 접기/펼치기(지도를 넓게 보려고)
    // 현재 위치로 출발지를 정했을 때만 지도를 그 위치로 이동한다 (일반 탭 선택은 시점을 유지)
    var focus by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var routing by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf<SafeRoute?>(null) }
    var routeError by remember { mutableStateOf<RouteOutcome.Failure?>(null) }
    var requestSeq by remember { mutableStateOf(0) } // 출발지·목적지가 바뀌면 늦게 도착한 이전 응답을 버리기 위한 번호
    var controlsOpen by remember { mutableStateOf(true) } // 결과가 나오면 입력 영역을 접어 지도를 넓게 쓴다

    fun resetRoute() {
        requestSeq++
        routing = false
        route = null
        routeError = null
        controlsOpen = true
    }

    fun requestRoute() {
        val s = start ?: return
        val d = destination ?: return
        if (routing) return
        val seq = ++requestSeq
        routing = true
        route = null
        routeError = null
        scope.launch {
            val outcome = fetchSafeRoute(s, d)
            if (seq != requestSeq) return@launch
            routing = false
            when (outcome) {
                is RouteOutcome.Success -> {
                    route = outcome.route
                    controlsOpen = false
                }
                is RouteOutcome.Failure -> routeError = outcome
            }
        }
    }

    fun useCurrentLocation() {
        scope.launch {
            locating = true
            val loc = fetchCurrentLocation(context)
            locating = false
            if (loc == null) {
                Toast.makeText(context, "현재 위치를 가져올 수 없습니다. GPS를 켜고 다시 시도하거나 지도에서 선택해 주세요.", Toast.LENGTH_LONG).show()
            } else {
                start = loc
                focus = loc
                resetRoute()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) useCurrentLocation()
        else Toast.makeText(
            context,
            "위치 권한이 없어 현재 위치를 사용할 수 없습니다. 설정에서 권한을 허용하거나 지도에서 선택해 주세요.",
            Toast.LENGTH_LONG
        ).show()
    }

    fun formatPoint(p: Pair<Double, Double>?) =
        p?.let { "%.5f, %.5f".format(java.util.Locale.US, it.first, it.second) } ?: "선택 안 됨"

    fun assign(lat: Double, lng: Double) {
        when (picking) {
            "start" -> start = lat to lng
            "destination" -> destination = lat to lng
        }
        picking = null
        resetRoute()
    }

    val currentRoute = route
    val markers = buildList {
        riskZones.forEach { z ->
            val lat = z.latitude
            val lng = z.longitude
            if (lat != null && lng != null) {
                val c = when (z.riskGrade) {
                    "HIGH" -> AppColors.Danger
                    "MEDIUM" -> AppColors.Warning
                    "LOW" -> AppColors.Safe
                    else -> AppColors.TextSub
                }
                add(GeoMarker("zone-${z.zoneId}", lat, lng, "위험 구간 #${z.zoneId}", c.copy(alpha = 0.5f)))
            }
        }
        // 경로 주변 위험구간은 진하게 표시
        currentRoute?.riskZones?.forEach { z ->
            val lat = z.latitude
            val lng = z.longitude
            if (lat != null && lng != null) {
                val c = when (z.riskGrade) {
                    "HIGH" -> AppColors.Danger
                    "MEDIUM" -> AppColors.Warning
                    "LOW" -> AppColors.Safe
                    else -> AppColors.TextSub
                }
                add(GeoMarker("route-zone-${z.zoneId}", lat, lng, "경로 주변 위험 구간 #${z.zoneId}", c))
            }
        }
        start?.let { add(GeoMarker("start", it.first, it.second, "출발지", AppColors.Primary)) }
        destination?.let { add(GeoMarker("destination", it.first, it.second, "목적지", AppColors.Text)) }
    }

    // 지도 시점: 경로가 있으면 경로 전체, 현재 위치 선택 시 그 위치, 아니면 위험구간 전체, 없으면 기본 위치
    val fitPoints = currentRoute?.path
        ?: focus?.let { listOf(it) }
        ?: riskZones.mapNotNull { z -> z.latitude?.let { lat -> z.longitude?.let { lng -> lat to lng } } }

    // 지도를 남은 화면 전체에 두고, 위치 선택 카드(위)와 결과 시트(아래)를 지도 위에 겹쳐 표시한다
    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            markers = markers,
            modifier = Modifier.fillMaxSize(),
            // 선택 모드에서 위험구간 마커를 눌러도 그 위치를 선택할 수 있다
            onMarkerClick = { id -> markers.find { it.id == id }?.let { m -> if (picking != null) assign(m.lat, m.lng) } },
            onMapTap = { lat, lng -> assign(lat, lng) },
            polyline = currentRoute?.path ?: emptyList(),
            fitPoints = fitPoints
        )

        // ── 위: 출발지·목적지 선택 카드 ──
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                if (!controlsOpen && start != null && destination != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("출발지 → 목적지 선택됨", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text, modifier = Modifier.weight(1f))
                        LinkText("수정") { controlsOpen = true }
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        RoutePointRow(
                            dot = AppColors.Primary, label = "출발지",
                            value = start?.let { formatPoint(it) },
                            onClear = { start = null; focus = null; resetRoute() }
                        ) {
                            LinkText(if (locating) "확인 중..." else "현재 위치", enabled = !locating) {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                                    PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                                    PackageManager.PERMISSION_GRANTED
                                if (granted) useCurrentLocation()
                                else permissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            }
                            LinkText("지도 선택") { picking = "start" }
                        }
                        HDivider()
                        RoutePointRow(
                            dot = AppColors.Text, label = "목적지",
                            value = destination?.let { formatPoint(it) },
                            onClear = { destination = null; resetRoute() }
                        ) {
                            LinkText("지도 선택") { picking = "destination" }
                        }

                        Spacer(Modifier.height(8.dp))
                        // 출발지와 목적지를 모두 선택해야 하고, 요청 중에는 다시 누를 수 없다
                        Button(
                            onClick = { requestRoute() },
                            enabled = start != null && destination != null && !routing,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Primary,
                                disabledContainerColor = AppColors.Border,
                                disabledContentColor = AppColors.TextSub
                            )
                        ) {
                            if (routing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AppColors.OnPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("경로 확인 중...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(
                                    if (start == null || destination == null) "출발지와 목적지를 선택하세요" else "경로 위험도 확인",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 지도에서 위치를 고르는 중일 때의 안내
            if (picking != null) {
                Surface(color = AppColors.PrimaryBg, shape = RoundedCornerShape(10.dp)) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (picking == "start") "지도를 눌러 출발지를 선택하세요" else "지도를 눌러 목적지를 선택하세요",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Primary,
                            modifier = Modifier.weight(1f)
                        )
                        LinkText("취소") { picking = null }
                    }
                }
            }
        }

        // ── 아래: 결과·오류 시트 (안내 문구는 항상 표시) ──
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = AppColors.Card,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                routeError?.let { err ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppColors.DangerBg).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            when (err.kind) {
                                RouteErrorKind.NETWORK -> "네트워크 오류"
                                RouteErrorKind.SERVER -> "서버 오류"
                                RouteErrorKind.NO_ROUTE -> "경로 없음"
                                RouteErrorKind.INVALID_INPUT -> "입력 오류"
                                RouteErrorKind.BAD_RESPONSE -> "응답 오류"
                            },
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Danger
                        )
                        Text(err.message, fontSize = 12.sp, color = AppColors.Text)
                        if (err.kind == RouteErrorKind.NETWORK || err.kind == RouteErrorKind.SERVER) {
                            OutlinedButton(
                                onClick = { requestRoute() },
                                enabled = !routing,
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("다시 시도", fontSize = 12.sp, color = AppColors.Text) }
                        }
                    }
                }

                if (currentRoute != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { sheetOpen = !sheetOpen }.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("경로 위험도 결과", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                        Text(if (sheetOpen) "▼ 내리기" else "▲ 펼치기", fontSize = 12.sp, color = AppColors.Primary)
                    }
                }
                currentRoute?.takeIf { sheetOpen }?.let { r ->
                    Column(
                        modifier = Modifier.heightIn(max = 230.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("경로 위험도 안내", fontSize = 11.sp, color = AppColors.TextSub)
                                r.safetyScore?.let {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text("%.0f".format(java.util.Locale.US, it), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = AppColors.Primary)
                                        Text(" / 100 안전 점수", fontSize = 12.sp, color = AppColors.TextSub, modifier = Modifier.padding(bottom = 5.dp))
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                r.totalDistance?.let {
                                    RouteStat("이동 거리", if (it >= 1000) "%.1fkm".format(java.util.Locale.US, it / 1000.0) else "${it}m")
                                }
                                // Tmap 보행자 시간(초)을 쓴다. 차량 기준처럼 비현실적으로 빠르면(>2.5m/s≈9km/h) 도보 4km/h(≈1.1m/s)로 거리에서 계산한다.
                                // shortcut: 서버가 보행 시간을 항상 주면 이 대체 계산은 지워도 된다.
                                val walkSec = r.totalDuration?.takeIf { dur ->
                                    dur > 0 && (r.totalDistance ?: 0) / dur.toDouble() <= 2.5
                                } ?: r.totalDistance?.let { (it / 1.1).toInt() } ?: r.totalDuration
                                walkSec?.let {
                                    RouteStat("예상 시간(도보)", if (it < 60) "1분 미만" else "약 ${(it + 30) / 60}분")
                                }
                            }
                        }

                        HDivider()

                        val count = r.riskZoneCount ?: r.riskZones.size
                        Text("경로 주변 위험구간 ${count}곳", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                        if (r.riskZones.isEmpty()) {
                            Text("경로 주변에서 등록된 위험구간이 확인되지 않았습니다.", fontSize = 12.sp, color = AppColors.TextSub)
                        }
                        r.riskZones.forEach { z ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppColors.Surface).padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GradePill(z.riskGrade)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "#${z.zoneId}" +
                                            (z.riskScore?.let { " · 점수 $it" } ?: "") +
                                            (z.distanceFromRoute?.let { " · 경로에서 %.0fm".format(java.util.Locale.US, it) } ?: ""),
                                        fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColors.Text
                                    )
                                    z.reason?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, fontSize = 11.sp, color = AppColors.TextSub)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    "보행자 길찾기(Tmap) 기반 참고 경로로, 휠체어 통행 가능 여부와 실제 안전을 보장하지 않습니다.",
                    fontSize = 11.sp, color = AppColors.TextSub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 12. 설정 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun SettingsScreen(onNavigate: (Screen) -> Unit) {
    var devices by remember {
        mutableStateOf<List<ServerDevice>>(emptyList())
    }

    var deviceLoading by remember {
        mutableStateOf(true)
    }

    var deviceError by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        deviceLoading = true
        deviceError = false

        try {
            devices = fetchRegisteredDevices()
        } catch (e: Exception) {
            deviceError = true
        } finally {
            deviceLoading = false
        }
    }

    val sensorSubtitle = when {
        deviceLoading -> "센서 정보 조회 중..."
        deviceError -> "센서 정보를 불러올 수 없음"
        devices.isEmpty() -> "등록된 센서 없음"
        devices.size == 1 -> devices.first().deviceId
        else -> "등록된 센서 ${devices.size}개"
    }
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "설정", showBack = true, onBack = { onNavigate(Screen.MainDriving) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsSection(title = "디바이스") {
                    SettingsRow(
                        icon = "📶",
                        title = "센서 모듈 관리",
                        subtitle = sensorSubtitle
                    ) {
                        onNavigate(Screen.SensorManagement)
                    }
                }
            }
            item {
                SettingsSection(title = "알림") {
                    SettingsRow(icon = "🔔", title = "위험 알림 설정", subtitle = "진동, 소리, 알림 설정") { onNavigate(Screen.NotificationSettings) }
                }
            }
            item {
                SettingsSection(title = "접근성") {
                    SettingsRow(icon = "♿", title = "화면 및 텍스트", subtitle = "글씨 크기, 대비, 색상") { onNavigate(Screen.AccessibilitySettings) }
                }
            }
            item {
                SettingsSection(title = "사용자") {
                    SettingsRow(icon = "👤", title = "계정 정보", subtitle = "게스트 모드로 사용 중") { onNavigate(Screen.Account) }
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    HDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("동행가드 v1.0.0 (MVP)", fontSize = 12.sp, color = AppColors.TextSub)
                    Text("전동 휠체어 안전 보조 서비스", fontSize = 12.sp, color = AppColors.TextSub)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 13. 위험 알림 설정 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun NotificationSettingsScreen(onNavigate: (Screen) -> Unit) {
    val notifEnabled = UiPrefs.notifEnabled
    val vibrEnabled  = UiPrefs.vibrEnabled
    val soundEnabled = UiPrefs.soundEnabled
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "위험 알림 설정", showBack = true, onBack = { onNavigate(Screen.Settings) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 전체 알림 토글
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.PrimaryBg),
                                contentAlignment = Alignment.Center
                            ) { Text("🔔", fontSize = 18.sp) }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("위험 알림", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                Text(if (notifEnabled) "알림이 활성화되어 있습니다" else "알림이 비활성화되어 있습니다", fontSize = 12.sp, color = AppColors.TextSub)
                            }
                        }
                        DhSwitch(checked = notifEnabled, onCheckedChange = { UiPrefs.saveNotif(it) })
                    }
                }
            }
            // 알림 방식
            item {
                Text("알림 방식", fontSize = 12.sp, color = AppColors.TextSub, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.Surface), contentAlignment = Alignment.Center) {
                                    Text("📳", fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("진동", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                    Text("위험 감지 시 진동으로 알림", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                            }
                            DhSwitch(checked = vibrEnabled && notifEnabled, onCheckedChange = { UiPrefs.saveVibr(it) }, enabled = notifEnabled)
                        }
                        HDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.Surface), contentAlignment = Alignment.Center) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("소리", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                    Text("위험 감지 시 알림음 재생", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                            }
                            DhSwitch(checked = soundEnabled && notifEnabled, onCheckedChange = { UiPrefs.saveSound(it) }, enabled = notifEnabled)
                        }
                    }
                }
            }
            if (!notifEnabled) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(AppColors.WarningBg)
                            .border(1.dp, AppColors.Warning, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) { Text("⚠ 알림이 비활성화되어 있습니다. 안전을 위해 알림을 활성화하는 것을 권장합니다.", fontSize = 13.sp, color = AppColors.Warning) }
                }
            }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Surface).border(1.dp, AppColors.Border, RoundedCornerShape(12.dp)).padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• 위험 단계에 따라 알림 강도가 다릅니다", fontSize = 12.sp, color = AppColors.TextSub)
                        Text("• 위험: 강한 진동 + 큰 소리", fontSize = 12.sp, color = AppColors.Danger)
                        Text("• 주의: 중간 진동 + 보통 소리", fontSize = 12.sp, color = AppColors.Warning)
                        Text("• 안전: 알림 없음", fontSize = 12.sp, color = AppColors.Safe)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { alertRisk(context, RiskLevel.WARNING) }, modifier = Modifier.weight(1f)) { Text("주의 알림 테스트") }
                    OutlinedButton(onClick = { alertRisk(context, RiskLevel.DANGER) }, modifier = Modifier.weight(1f)) { Text("위험 알림 테스트") }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 14. 접근성 설정 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun AccessibilitySettingsScreen(onNavigate: (Screen) -> Unit) {
    val fontSize  = UiPrefs.fontSize
    val contrast  = UiPrefs.contrast
    val colorMode = UiPrefs.colorMode

    val fontSizeOptions  = listOf(Triple("small",       "작게",  "14px"), Triple("medium", "보통", "16px"), Triple("large", "크게", "18px"))
    val contrastOptions  = listOf(Pair("normal", "일반"), Pair("high", "높음"))
    val colorModeOptions = listOf(Triple("default", "기본", "기본 색상 표시"), Triple("redgreen", "적록색약", "적색·녹색 구분이 어려운 경우(적색약·녹색약) 보정"))

    val previewFontSize = when (fontSize) { "small" -> 14.sp; "large" -> 18.sp; else -> 16.sp }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "화면 및 텍스트", showBack = true, onBack = { onNavigate(Screen.Settings) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 글씨 크기
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("T", fontSize = 18.sp, color = AppColors.Primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("글씨 크기", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    fontSizeOptions.forEachIndexed { idx, (value, label, size) ->
                        if (idx > 0) HDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { UiPrefs.saveFontSize(value) }.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                Text(size, fontSize = 12.sp, color = AppColors.TextSub)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("가나다 ABC", fontSize = when(value) { "small" -> 13.sp; "large" -> 17.sp; else -> 15.sp }, color = AppColors.Text)
                                if (fontSize == value) {
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(AppColors.Primary), contentAlignment = Alignment.Center) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 대비
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👁", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("대비", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    contrastOptions.forEachIndexed { idx, (value, label) ->
                        if (idx > 0) HDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { UiPrefs.saveContrast(value) }.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                Text(if (value == "normal") "일반적인 대비" else "높은 대비 (WCAG AAA)", fontSize = 12.sp, color = AppColors.TextSub)
                            }
                            if (contrast == value) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(AppColors.Primary), contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                }
                            }
                        }
                    }
                }
            }
            // 색상 보정
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎨", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("색상 보정", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    colorModeOptions.forEachIndexed { idx, (value, label, desc) ->
                        if (idx > 0) HDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { UiPrefs.saveColorMode(value) }.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                Text(desc, fontSize = 12.sp, color = AppColors.TextSub)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    listOf(AppColors.Danger, AppColors.Warning, AppColors.Safe).forEach { c ->
                                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(c))
                                    }
                                }
                                if (colorMode == value) {
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(AppColors.Primary), contentAlignment = Alignment.Center) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 미리보기
            item {
                Text("미리보기", fontSize = 12.sp, color = AppColors.TextSub, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(Triple(AppColors.SafeBg, AppColors.Safe, "안전 상태"), Triple(AppColors.WarningBg, AppColors.Warning, "주의 필요"), Triple(AppColors.DangerBg, AppColors.Danger, "위험 상태")).forEach { (bg, color, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(bg).border(1.dp, color, RoundedCornerShape(8.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.width(10.dp))
                                Text(label, fontSize = previewFontSize, fontWeight = FontWeight.SemiBold, color = color)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 15. 계정 정보 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun AccountScreen(onNavigate: (Screen) -> Unit) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("게스트 모드를 종료하시겠습니까?", fontWeight = FontWeight.SemiBold) },
            text  = { Text("저장되지 않은 주행 기록은 삭제됩니다", color = AppColors.TextSub) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onNavigate(Screen.Login) }) {
                    Text("종료", color = AppColors.Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("취소", color = AppColors.TextSub) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "계정 정보", showBack = true, onBack = { onNavigate(Screen.Settings) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 게스트 모드 안내 배너
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(AppColors.WarningBg)
                        .border(1.dp, AppColors.Warning, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("⚠", fontSize = 16.sp, color = AppColors.Warning)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("게스트 모드로 사용 중", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Warning)
                            Spacer(Modifier.height(4.dp))
                            Text("계정을 생성하면 주행 기록을 클라우드에 저장하고\n여러 기기에서 동기화할 수 있습니다", fontSize = 12.sp, color = AppColors.TextSub)
                        }
                    }
                }
            }
            // 프로필 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(AppColors.Surface).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(60.dp).clip(CircleShape).background(AppColors.Border),
                                contentAlignment = Alignment.Center
                            ) { Text("👤", fontSize = 28.sp) }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("게스트", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                Text("임시 사용자", fontSize = 12.sp, color = AppColors.TextSub)
                            }
                        }
                        HDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📅", fontSize = 16.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("사용 시작일", fontSize = 12.sp, color = AppColors.TextSub)
                                Text("2026년 5월 28일", fontSize = 14.sp, color = AppColors.Text)
                            }
                        }
                    }
                }
            }
            // 계정 생성 버튼
            item {
                Button(
                    onClick = { onNavigate(Screen.Signup) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) { Text("계정 생성하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
            item {
                Text(
                    "계정을 생성하면 주행 기록이 안전하게 보관됩니다",
                    fontSize = 12.sp, color = AppColors.TextSub,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
            // 로그아웃
            item {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp).border(2.dp, AppColors.Danger, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger)
                ) { Text("← 게스트 모드 종료", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Danger) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 공통 컴포저블
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AppTopBar(
    title:    String,
    showBack: Boolean = false,
    onBack:   (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(color = AppColors.Card, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack && onBack != null) {
                TextButton(onClick = onBack) {
                    Text("<", fontSize = 20.sp, color = AppColors.Text, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Text(
                title,
                modifier = Modifier.weight(1f).padding(start = if (showBack) 0.dp else 4.dp),
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text
            )
            if (trailingContent != null) trailingContent()
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
fun HDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))
}

@Composable
fun FormField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

@Composable
fun dhTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AppColors.Primary,
    unfocusedBorderColor = AppColors.Border,
    cursorColor          = AppColors.Primary
)

@Composable
fun DhSwitch(
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled:         Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor   = Color.White,
            checkedTrackColor   = AppColors.Primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = AppColors.SwitchOff
        )
    )
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 12.sp, color = AppColors.TextSub, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
            shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
        ) { content() }
    }
}

@Composable
fun SettingsRow(
    icon:     String,
    title:    String,
    subtitle: String,
    onClick:  () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.PrimaryBg),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 18.sp) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
            Text(subtitle, fontSize = 12.sp, color = AppColors.TextSub)
        }
        Text(">", fontSize = 16.sp, color = AppColors.TextSub)
    }
}
