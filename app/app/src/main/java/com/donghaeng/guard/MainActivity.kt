package com.donghaeng.guard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════
// 색상 시스템 (theme.css → AppColors)
// ══════════════════════════════════════════════════════════════════════
object AppColors {
    val Background  = Color(0xFFF8FAFC)
    val Card        = Color(0xFFFFFFFF)
    val Primary     = Color(0xFF2563EB)
    val PrimaryBg   = Color(0xFFDBEAFE)
    val OnPrimary   = Color(0xFFFFFFFF)
    val Surface     = Color(0xFFF1F5F9)
    val Text        = Color(0xFF111827)
    val TextSub     = Color(0xFF6B7280)
    val Border      = Color(0xFFE5E7EB)
    val Destructive = Color(0xFFEF4444)
    val Safe        = Color(0xFF22C55E)
    val SafeBg      = Color(0xFFDCFCE7)
    val Warning     = Color(0xFFF59E0B)
    val WarningBg   = Color(0xFFFEF3C7)
    val Danger      = Color(0xFFEF4444)
    val DangerBg    = Color(0xFFFEE2E2)
    val SwitchOff   = Color(0xFFD1D5DB)
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
    object TripEnd               : Screen()
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
fun riskGradeColor(g: String) = when (g) { "HIGH" -> AppColors.Danger; "MEDIUM" -> AppColors.Warning; else -> AppColors.Safe }
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
    val lat: Double? = null, val lng: Double? = null
)

data class TripDetail(
    val id: String, val date: String, val startTime: String,
    val endTime: String, val duration: String, val events: List<RiskEvent>
)

data class RiskZone(
    val id: String, val name: String, val location: String,
    val riskGrade: String, val riskScore: Int, val eventCount: Int,
    val description: String, val lat: Double, val lng: Double
)

data class MapMarker(
    val id: String, val x: Float, val y: Float,
    val label: String, val riskGrade: String,
    val onClick: () -> Unit = {}
)

// ══════════════════════════════════════════════════════════════════════
// Mock 데이터
// ══════════════════════════════════════════════════════════════════════
val mockTrips = listOf(
    TripRecord("1", "2026년 5월 28일", "14:30 - 15:45", "1시간 15분", 3, 8, RiskLevel.DANGER),
    TripRecord("2", "2026년 5월 27일", "10:20 - 11:05", "45분",       1, 4, RiskLevel.DANGER),
    TripRecord("3", "2026년 5월 26일", "16:00 - 16:30", "30분",       0, 2, RiskLevel.WARNING),
    TripRecord("4", "2026년 5월 25일", "09:00 - 09:40", "40분",       0, 0, RiskLevel.SAFE),
)

val mockTripDetail = TripDetail(
    id = "1", date = "2026년 5월 28일", startTime = "14:30",
    endTime = "15:45", duration = "1시간 15분",
    events = listOf(
        RiskEvent("1", "14:35", RiskLevel.WARNING, 450,  "서울시 강남구 역삼동 테헤란로 231",      37.5012, 127.0396),
        RiskEvent("2", "14:52", RiskLevel.DANGER,  180,  "서울시 강남구 삼성동 코엑스몰 앞",       37.5115, 127.0595),
        RiskEvent("3", "15:10", RiskLevel.WARNING, 420,  "서울시 강남구 대치동 은마아파트 사거리", 37.4947, 127.0630),
        RiskEvent("4", "15:25", RiskLevel.DANGER,  150,  "서울시 강남구 도곡동 매봉역 2번 출구",   37.5015, 127.0544),
    )
)

val mockRiskZones = listOf(
    RiskZone("1", "역삼역 사거리",      "서울시 강남구 역삼동 테헤란로 231",   "HIGH",   85, 12, "급경사 및 단차 발생 빈도 높음. 보도 경사가 심하고 턱이 많아 주의가 필요합니다.",     37.5012, 127.0396),
    RiskZone("2", "삼성역 2번 출구",    "서울시 강남구 삼성동 봉은사로 524",   "MEDIUM", 62,  7, "보도 폭이 좁고 장애물 다수. 유동인구가 많아 주의가 필요합니다.",                   37.5115, 127.0595),
    RiskZone("3", "대치동 은행사거리",  "서울시 강남구 대치동 선릉로 428",     "MEDIUM", 58,  5, "횡단보도 진입 시 턱 높이가 불규칙하여 주의가 필요합니다.",                         37.4947, 127.0630),
    RiskZone("4", "도곡동 매봉역 앞",   "서울시 강남구 도곡동 남부순환로 2947","LOW",    38,  3, "일부 구간에서 경사 감지. 대체로 안전한 편입니다.",                                 37.5015, 127.0544),
)

// ══════════════════════════════════════════════════════════════════════
// MainActivity
// ══════════════════════════════════════════════════════════════════════
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DongHaengGuardApp() }
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

    val showBottomNav = screen == Screen.MainDriving ||
            screen == Screen.DrivingHistory ||
            screen == Screen.RiskZones

    MaterialTheme {
        Scaffold(
            containerColor = AppColors.Background,
            bottomBar = {
                if (showBottomNav) {
                    BottomNavBar(currentScreen = screen) { screen = it }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                val s = screen
                when (s) {
                    Screen.Onboarding            -> OnboardingScreen          { screen = it }
                    Screen.Login                 -> LoginScreen                { screen = it }
                    Screen.LoginForm             -> LoginFormScreen            { screen = it }
                    Screen.Signup                -> SignupScreen               { screen = it }
                    Screen.SensorConnection      -> SensorConnectionScreen     { screen = it }
                    Screen.SensorManagement      -> SensorManagementScreen     { screen = it }
                    Screen.MainDriving           -> MainDrivingScreen(
                        tripActive, { tripActive = it },
                        riskLevel,  { riskLevel  = it },
                        distanceMm, { distanceMm = it }
                    ) { screen = it }
                    Screen.TripEnd               -> TripEndScreen             { screen = it }
                    Screen.DrivingHistory        -> DrivingHistoryScreen       { screen = it }
                    is Screen.DrivingDetail      -> DrivingDetailScreen(s.tripId) { screen = it }
                    Screen.RiskZones             -> RiskZonesScreen           { screen = it }
                    Screen.Settings              -> SettingsScreen             { screen = it }
                    Screen.NotificationSettings  -> NotificationSettingsScreen { screen = it }
                    Screen.AccessibilitySettings -> AccessibilitySettingsScreen{ screen = it }
                    Screen.Account               -> AccountScreen              { screen = it }
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
        Tab(Screen.RiskZones,     "위험구간", "!"),
    )
    Surface(color = AppColors.Card, shadowElevation = 8.dp) {
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
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).background(AppColors.PrimaryBg),
                contentAlignment = Alignment.Center
            ) { Text("🛡", fontSize = 48.sp) }
            Spacer(Modifier.height(20.dp))
            Text("동행가드", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
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
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(AppColors.PrimaryBg),
            contentAlignment = Alignment.Center
        ) { Text("🛡", fontSize = 48.sp) }
        Spacer(Modifier.height(20.dp))
        Text("동행가드", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
        Spacer(Modifier.height(8.dp))
        Text("안전한 이동을 함께 합니다", fontSize = 14.sp, color = AppColors.TextSub)
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { onNavigate(Screen.SensorConnection) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) { Text("🛡  간편 시작", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(8.dp))
        Text("로그인 없이 바로 사용할 수 있습니다", fontSize = 12.sp, color = AppColors.TextSub, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HDivider(modifier = Modifier.weight(1f))
            Text("  또는  ", fontSize = 12.sp, color = AppColors.TextSub)
            HDivider(modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TextButton(onClick = { onNavigate(Screen.LoginForm) }) {
                Text("로그인", color = AppColors.Primary, fontWeight = FontWeight.Medium)
            }
            Text("|", color = AppColors.Border, fontSize = 16.sp)
            TextButton(onClick = { onNavigate(Screen.Signup) }) {
                Text("회원가입", color = AppColors.Primary, fontWeight = FontWeight.Medium)
            }
        }
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
enum class ConnStatus { IDLE, SEARCHING, FOUND, CONNECTING, CONNECTED, FAILED }

@Composable
fun SensorConnectionScreen(onNavigate: (Screen) -> Unit) {
    var status by remember { mutableStateOf(ConnStatus.IDLE) }
    val scope  = rememberCoroutineScope()

    val mockDevices = listOf(
        Pair("DongHaeng-ESP32-A1B2", -45),
        Pair("DongHaeng-ESP32-C3D4", -60),
    )

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
                    Text("주변의 동행가드 센서 모듈을 검색합니다", fontSize = 14.sp, color = AppColors.TextSub)
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
                        if (status == ConnStatus.SEARCHING) "주변의 BLE 디바이스를 찾고 있습니다" else "잠시만 기다려 주세요",
                        fontSize = 14.sp, color = AppColors.TextSub
                    )
                }

                ConnStatus.FOUND -> Column(modifier = Modifier.fillMaxSize()) {
                    Text("발견된 센서", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                    Spacer(Modifier.height(4.dp))
                    Text("연결할 센서를 선택하세요", fontSize = 13.sp, color = AppColors.TextSub)
                    Spacer(Modifier.height(16.dp))
                    mockDevices.forEach { (name, rssi) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable {
                                status = ConnStatus.CONNECTING
                                scope.launch {
                                    delay(2000)
                                    status = ConnStatus.CONNECTED
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📶", fontSize = 20.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("신호 세기: $rssi dBm", fontSize = 12.sp, color = AppColors.TextSub, modifier = Modifier.padding(start = 32.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            status = ConnStatus.SEARCHING
                            scope.launch { delay(2000); status = ConnStatus.FOUND }
                        },
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
                    Text("연결 완료!", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Safe)
                    Spacer(Modifier.height(8.dp))
                    Text("센서 모듈과 성공적으로 연결되었습니다", fontSize = 14.sp, color = AppColors.TextSub)
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
                    Text("센서 모듈 연결에 실패했습니다\n다시 시도해 주세요", fontSize = 14.sp, color = AppColors.TextSub, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            status = ConnStatus.CONNECTING
                            scope.launch { delay(2000); status = ConnStatus.CONNECTED }
                        },
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
    var isConnected     by remember { mutableStateOf(true) }
    var isDisconnecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "센서 모듈 관리", showBack = true, onBack = { onNavigate(Screen.Settings) })
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isConnected) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(AppColors.SafeBg)
                            .border(1.dp, AppColors.Safe, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✓", color = AppColors.Safe, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text("센서 연결됨", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Safe)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("센서가 정상적으로 연결되어 있습니다", fontSize = 12.sp, color = AppColors.TextSub)
                        }
                    }
                }
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
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.PrimaryBg),
                                    contentAlignment = Alignment.Center
                                ) { Text("📶", fontSize = 22.sp) }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("DongHaeng-ESP32-A1B2", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                    Text("전동 휠체어 센서 모듈", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                            }
                            HDivider()
                            listOf(
                                Triple("신호 강도", "강함", AppColors.Safe),
                                Triple("배터리",    "87%",  AppColors.Text),
                                Triple("마지막 동기화", "방금 전", AppColors.Text),
                            ).forEach { (key, value, valueColor) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key,   fontSize = 14.sp, color = AppColors.Text)
                                    Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
                                }
                                HDivider()
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                isDisconnecting = true
                                delay(1000)
                                isConnected = false
                                isDisconnecting = false
                            }
                        },
                        enabled = !isDisconnecting,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
                    ) {
                        Text(
                            if (isDisconnecting) "연결 해제 중..." else "⏻  센서 연결 해제",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                item {
                    Text(
                        "연결을 해제하면 실시간 위험 감지 기능을 사용할 수 없습니다",
                        fontSize = 12.sp, color = AppColors.TextSub,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                        shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(CircleShape).background(AppColors.Surface),
                                contentAlignment = Alignment.Center
                            ) { Text("📶", fontSize = 28.sp) }
                            Spacer(Modifier.height(16.dp))
                            Text("센서 연결 해제됨", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                            Spacer(Modifier.height(8.dp))
                            Text("다른 센서를 찾으시겠습니까?", fontSize = 13.sp, color = AppColors.TextSub, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { onNavigate(Screen.SensorConnection) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                            ) { Text("🔍  센서 찾기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
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
@Composable
fun MainDrivingScreen(
    tripActive:         Boolean,   onTripActiveChange: (Boolean) -> Unit,
    riskLevel:          RiskLevel, onRiskLevelChange:  (RiskLevel) -> Unit,
    distanceMm:         Int,       onDistanceMmChange: (Int) -> Unit,
    onNavigate: (Screen) -> Unit
) {
    var showEndDialog by remember { mutableStateOf(false) }

    val animBg by animateColorAsState(targetValue = riskBg(riskLevel), animationSpec = tween(300), label = "bg")
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (riskLevel == RiskLevel.DANGER && tripActive) 1.04f else 1f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "scale"
    )

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("주행 종료", fontWeight = FontWeight.SemiBold) },
            text  = { Text("주행을 종료하시겠습니까?\n주행 기록이 저장됩니다.", color = AppColors.TextSub) },
            confirmButton = {
                TextButton(onClick = {
                    onTripActiveChange(false)
                    onRiskLevelChange(RiskLevel.SAFE)
                    onDistanceMmChange(1250)
                    showEndDialog = false
                    onNavigate(Screen.TripEnd)
                }) { Text("종료", color = AppColors.Danger, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("계속 주행", color = AppColors.TextSub) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        // 헤더
        Surface(color = AppColors.Card, shadowElevation = 1.dp) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("주행", modifier = Modifier.padding(start = 12.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                    TextButton(onClick = { onNavigate(Screen.Settings) }) {
                        Text("⚙", fontSize = 22.sp, color = AppColors.TextSub)
                    }
                }
                // 연결 상태 바
                Row(
                    modifier = Modifier.fillMaxWidth().background(AppColors.Surface).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📶", fontSize = 12.sp)
                        Text("BLE 연결됨", fontSize = 12.sp, color = AppColors.Safe)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📍", fontSize = 12.sp)
                        Text("GPS 활성", fontSize = 12.sp, color = AppColors.Safe)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (tripActive) AppColors.Danger else AppColors.TextSub))
                        Text(if (tripActive) "주행 중" else "대기 중", fontSize = 12.sp, color = if (tripActive) AppColors.Danger else AppColors.TextSub)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!tripActive) {
                // 대기 상태
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(AppColors.PrimaryBg),
                        contentAlignment = Alignment.Center
                    ) { Text("🛡", fontSize = 48.sp) }
                    Spacer(Modifier.height(20.dp))
                    Text("주행 준비 완료", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "센서가 연결되었습니다\n주행을 시작하면 실시간으로 위험을 감지합니다",
                        fontSize = 14.sp, color = AppColors.TextSub, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(36.dp))
                    Button(
                        onClick = { onTripActiveChange(true) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) { Text("▶  주행 시작", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                }
            } else {
                // 주행 중 - 위험 상태 카드
                Box(
                    modifier = Modifier.fillMaxWidth().scale(pulseScale).clip(RoundedCornerShape(16.dp))
                        .background(animBg).border(2.dp, riskColor(riskLevel), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(88.dp).clip(CircleShape)
                                .background(riskColor(riskLevel).copy(alpha = 0.15f))
                                .border(3.dp, riskColor(riskLevel), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (riskLevel) { RiskLevel.SAFE -> "●"; RiskLevel.WARNING -> "▲"; RiskLevel.DANGER -> "!" },
                                fontSize = 40.sp, color = riskColor(riskLevel), fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(riskLabel(riskLevel), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = riskColor(riskLevel))
                        Spacer(Modifier.height(4.dp))
                        Text(riskDesc(riskLevel), fontSize = 14.sp, color = AppColors.Text, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        // 거리값
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(AppColors.Card.copy(alpha = 0.8f))
                                .border(1.dp, AppColors.Border, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("전방 거리", fontSize = 12.sp, color = AppColors.TextSub)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "$distanceMm",
                                        fontSize = 38.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace, color = AppColors.Text
                                    )
                                    Text("  mm", fontSize = 18.sp, color = AppColors.TextSub, modifier = Modifier.padding(bottom = 4.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // 데모: 임시 센서 데이터 전송
                OutlinedButton(
                    onClick = {
                        val levels = RiskLevel.values()
                        val dists  = listOf(1250, 450, 150)
                        val next   = (riskLevel.ordinal + 1) % levels.size
                        onRiskLevelChange(levels[next])
                        onDistanceMmChange(dists[next])
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("[데모] 임시 센서 데이터 전송", fontSize = 13.sp, color = AppColors.TextSub) }

                // 주행 종료
                Button(
                    onClick = { showEndDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
                ) { Text("■  주행 종료", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 8. 주행 종료 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun TripEndScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppColors.Background)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(AppColors.SafeBg),
            contentAlignment = Alignment.Center
        ) { Text("✓", fontSize = 48.sp, color = AppColors.Safe, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(20.dp))
        Text("주행 완료", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
        Spacer(Modifier.height(8.dp))
        Text("안전한 주행이었습니다", fontSize = 14.sp, color = AppColors.TextSub)
        Spacer(Modifier.height(28.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
            shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("주행 요약", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕐", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("주행 시간", fontSize = 14.sp, color = AppColors.Text)
                    }
                    Text("1시간 15분", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                }
                HDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("위험 발생 횟수", fontSize = 12.sp, color = AppColors.TextSub)
                Spacer(Modifier.height(10.dp))
                listOf(
                    Triple(AppColors.Danger,  "위험", "3회"),
                    Triple(AppColors.Warning, "주의", "8회"),
                    Triple(AppColors.Safe,    "안전", "142회"),
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
                        Text(count, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
                    }
                }
                HDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", color = AppColors.Safe, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("서버에 저장되었습니다", fontSize = 12.sp, color = AppColors.Safe)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onNavigate(Screen.DrivingHistory) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) { Text("≡  기록 보기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(10.dp))
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
@Composable
fun DrivingHistoryScreen(onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "주행 기록")
        if (mockTrips.isEmpty()) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mockTrips) { trip ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.DrivingDetail(trip.id)) },
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                        shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(trip.date, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                Text(">", fontSize = 18.sp, color = AppColors.TextSub)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🕐", fontSize = 12.sp)
                                Text("${trip.timeRange}  ·  ${trip.duration}", fontSize = 12.sp, color = AppColors.TextSub)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.Danger))
                                    Text("위험 ${trip.dangerCount}", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.Warning))
                                    Text("주의 ${trip.warningCount}", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(riskBg(trip.maxRisk))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("최고 위험: ${riskLabel(trip.maxRisk)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = riskColor(trip.maxRisk))
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
    var showMap     by remember { mutableStateOf(false) }

    LaunchedEffect(tripId) {
        loading = true
        delay(1000)
        detail  = mockTripDetail
        loading = false
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

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("주행 정보를 불러오는 중...", fontSize = 14.sp, color = AppColors.TextSub)
                }
            }
            return
        }
        val d = detail ?: return

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 주행 요약
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
                                    Text("${d.events.size}개의 위험 이벤트 위치", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                                VirtualMapView(
                                    markers = d.events.mapIndexed { idx, ev ->
                                        val positions = listOf(
                                            Pair(23f, 35f), Pair(45f, 28f), Pair(38f, 52f), Pair(62f, 41f)
                                        )
                                        val (x, y) = positions[idx % positions.size]
                                        MapMarker(
                                            id = ev.id, x = x, y = y,
                                            label = "${idx + 1}. ${riskLabel(ev.riskLevel)} (${ev.time})",
                                            riskGrade = when (ev.riskLevel) {
                                                RiskLevel.DANGER -> "HIGH"; RiskLevel.WARNING -> "MEDIUM"; else -> "LOW"
                                            },
                                            onClick = { expandedId = ev.id; showMap = false }
                                        )
                                    }
                                )
                            }
                            Text(
                                "[향후 실제 지도 연동 예정]",
                                fontSize = 11.sp, color = AppColors.TextSub,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }
                }
            }

            // 이벤트 헤더
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("위험 이벤트", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                    Text("${d.events.size}건", fontSize = 13.sp, color = AppColors.TextSub)
                }
            }

            items(d.events) { ev ->
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
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("${ev.distanceMm}mm  ·  ${ev.location}", fontSize = 12.sp, color = AppColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                            Text("${ev.distanceMm}", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = AppColors.Text)
                                            Text(" mm", fontSize = 14.sp, color = AppColors.TextSub, modifier = Modifier.padding(bottom = 2.dp))
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
                                            Text("${ev.lat.toBigDecimal().setScale(4)}, ${ev.lng.toBigDecimal().setScale(4)}", fontSize = 11.sp, color = AppColors.TextSub)
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showMap = true },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("🗺  지도에서 보기", fontSize = 12.sp, color = AppColors.Text) }
                            }
                        }
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
    var loading        by remember { mutableStateOf(true) }
    var viewMode       by remember { mutableStateOf("list") } // "map" | "list"
    var selectedZoneId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(1200)
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        Surface(color = AppColors.Card, shadowElevation = 1.dp) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("위험 구간", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.Surface).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("map" to "🗺", "list" to "≡").forEach { (mode, icon) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (viewMode == mode) AppColors.Card else Color.Transparent)
                                    .clickable { viewMode = mode }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(icon, fontSize = 16.sp) }
                        }
                    }
                }
                // 안내 배너
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp)).background(AppColors.PrimaryBg)
                        .border(1.dp, AppColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("ℹ", fontSize = 14.sp, color = AppColors.Primary)
                        Spacer(Modifier.width(8.dp))
                        Text("여러 사용자의 주행 데이터를 AI가 분석하여 위험도가 높은 순으로 제공합니다", fontSize = 12.sp, color = AppColors.Text)
                    }
                }
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("AI 분석 중...", fontSize = 14.sp, color = AppColors.TextSub)
                }
            }
            return
        }

        when (viewMode) {
            "map" -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                        shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🗺", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("위험 구간 지도", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                    Text("${mockRiskZones.size}개의 위험 구간", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                val mapPositions = listOf(Pair(23f,35f), Pair(45f,28f), Pair(38f,52f), Pair(62f,41f))
                                VirtualMapView(
                                    markers = mockRiskZones.mapIndexed { i, z ->
                                        val (x, y) = mapPositions[i % mapPositions.size]
                                        MapMarker(id = z.id, x = x, y = y, label = "${z.name} (위험도: ${z.riskScore})", riskGrade = z.riskGrade, onClick = { selectedZoneId = z.id })
                                    }
                                )
                            }
                            Text("[향후 실제 지도 연동 예정]", fontSize = 11.sp, color = AppColors.TextSub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
                        }
                    }
                    // 선택된 구간 정보
                    val sel = mockRiskZones.find { it.id == selectedZoneId }
                    if (sel != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                            shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sel.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📍", fontSize = 11.sp)
                                            Text(sel.location, fontSize = 11.sp, color = AppColors.TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    TextButton(onClick = { selectedZoneId = null }) { Text("✕", color = AppColors.TextSub) }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(riskGradeBg(sel.riskGrade)).padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Text(riskGradeLabel(sel.riskGrade), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = riskGradeColor(sel.riskGrade)) }
                                    Text("위험도 ${sel.riskScore}  ·  발생 ${sel.eventCount}회", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(sel.description, fontSize = 12.sp, color = AppColors.TextSub)
                            }
                        }
                    }
                }
            }
            else -> { // list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mockRiskZones.sortedByDescending { it.riskScore }) { zone ->
                        val isExpanded = selectedZoneId == zone.id
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedZoneId = if (isExpanded) null else zone.id },
                            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                            shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(zone.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text)
                                        Spacer(Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📍", fontSize = 11.sp)
                                            Text(zone.location, fontSize = 11.sp, color = AppColors.TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                            .background(riskGradeBg(zone.riskGrade))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Text(riskGradeLabel(zone.riskGrade), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = riskGradeColor(zone.riskGrade)) }
                                }
                                Spacer(Modifier.height(12.dp))
                                // 위험도 바
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.Surface).padding(12.dp)
                                ) {
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("위험도", fontSize = 12.sp, color = AppColors.TextSub)
                                            Text("${zone.riskScore}", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = AppColors.Text)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(AppColors.Border)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(zone.riskScore / 100f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(riskGradeColor(zone.riskGrade))
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("!", fontSize = 12.sp, color = AppColors.TextSub)
                                    Spacer(Modifier.width(4.dp))
                                    Text("위험 발생 ${zone.eventCount}회", fontSize = 12.sp, color = AppColors.TextSub)
                                }
                                if (isExpanded) {
                                    HDivider(modifier = Modifier.padding(vertical = 10.dp))
                                    Box(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .background(AppColors.Surface).padding(10.dp)
                                    ) { Text(zone.description, fontSize = 12.sp, color = AppColors.TextSub) }
                                    Spacer(Modifier.height(6.dp))
                                    Text("좌표: ${"%.4f".format(zone.lat)}, ${"%.4f".format(zone.lng)}", fontSize = 11.sp, color = AppColors.TextSub)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 12. 설정 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun SettingsScreen(onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        AppTopBar(title = "설정", showBack = true, onBack = { onNavigate(Screen.MainDriving) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsSection(title = "디바이스") {
                    SettingsRow(icon = "📶", title = "센서 모듈 관리", subtitle = "DongHaeng-ESP32-A1B2") { onNavigate(Screen.SensorManagement) }
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
    var notifEnabled  by remember { mutableStateOf(true) }
    var vibrEnabled   by remember { mutableStateOf(true) }
    var soundEnabled  by remember { mutableStateOf(true) }

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
                        DhSwitch(checked = notifEnabled, onCheckedChange = { notifEnabled = it })
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
                            DhSwitch(checked = vibrEnabled && notifEnabled, onCheckedChange = { vibrEnabled = it }, enabled = notifEnabled)
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
                            DhSwitch(checked = soundEnabled && notifEnabled, onCheckedChange = { soundEnabled = it }, enabled = notifEnabled)
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
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 14. 접근성 설정 화면
// ══════════════════════════════════════════════════════════════════════
@Composable
fun AccessibilitySettingsScreen(onNavigate: (Screen) -> Unit) {
    var fontSize    by remember { mutableStateOf("medium") }
    var contrast    by remember { mutableStateOf("normal") }
    var colorMode   by remember { mutableStateOf("default") }

    val fontSizeOptions  = listOf(Triple("small",       "작게",  "14px"), Triple("medium", "보통", "16px"), Triple("large", "크게", "18px"))
    val contrastOptions  = listOf(Pair("normal", "일반"), Pair("high", "높음"))
    val colorModeOptions = listOf(Triple("default", "기본", "기본 색상 표시"), Triple("protanopia", "적색약", "적색 인식 보정"), Triple("deuteranopia", "녹색약", "녹색 인식 보정"))

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
                            modifier = Modifier.fillMaxWidth().clickable { fontSize = value }.padding(16.dp),
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
                            modifier = Modifier.fillMaxWidth().clickable { contrast = value }.padding(16.dp),
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
                            modifier = Modifier.fillMaxWidth().clickable { colorMode = value }.padding(16.dp),
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
// 16. 가상 지도 컴포저블 (Canvas 기반)
// ══════════════════════════════════════════════════════════════════════
@Composable
fun VirtualMapView(
    markers: List<MapMarker>,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9FAFB))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(zoom)
        ) {
            val w = size.width
            val h = size.height

            drawRect(color = Color(0xFFF9FAFB))

            listOf(0.20f, 0.45f, 0.70f).forEach { r ->
                drawRect(
                    color = Color(0xFFD1D5DB),
                    topLeft = Offset(0f, h * r),
                    size = Size(w, h * 0.025f)
                )
            }

            listOf(0.25f, 0.55f, 0.82f).forEach { r ->
                drawRect(
                    color = Color(0xFFD1D5DB),
                    topLeft = Offset(w * r, 0f),
                    size = Size(w * 0.025f, h)
                )
            }

            drawCircle(
                color = Color(0xFFDCFCE7),
                radius = h * 0.09f,
                center = Offset(w * 0.12f, h * 0.33f)
            )

            drawCircle(
                color = Color(0xFFDCFCE7),
                radius = h * 0.07f,
                center = Offset(w * 0.71f, h * 0.80f)
            )

            drawRect(
                color = Color(0xFFBFDBFE),
                topLeft = Offset(w * 0.30f, h * 0.06f),
                size = Size(w * 0.18f, h * 0.11f)
            )

            drawRect(
                color = Color(0xFFBFDBFE),
                topLeft = Offset(w * 0.60f, h * 0.52f),
                size = Size(w * 0.17f, h * 0.10f)
            )
        }

        // mock 마커 표시
        MapMarkerDot(
            grade = "MEDIUM",
            label = "▲",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 92.dp, top = 130.dp)
        )

        MapMarkerDot(
            grade = "HIGH",
            label = "!",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 170.dp, top = 210.dp)
        )

        MapMarkerDot(
            grade = "MEDIUM",
            label = "▲",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 230.dp, top = 290.dp)
        )

        MapMarkerDot(
            grade = "HIGH",
            label = "!",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 300.dp, top = 170.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Card)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                    .clickable { zoom = (zoom + 0.25f).coerceAtMost(3f) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Card)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                    .clickable { zoom = (zoom - 0.25f).coerceAtLeast(0.5f) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Card)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${(zoom * 100).toInt()}%", fontSize = 9.sp, color = AppColors.TextSub)
            }
        }
    }
}
@Composable
fun MapMarkerDot(
    grade: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(riskGradeColor(grade))
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
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
