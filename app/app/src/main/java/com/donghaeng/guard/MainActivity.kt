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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ══════════════════════════════════════════════════════════
// 색상 시스템
// ══════════════════════════════════════════════════════════
object AppColors {
    val Background      = Color(0xFFF8FAFC)
    val CardBackground  = Color(0xFFFFFFFF)
    val Primary         = Color(0xFF2563EB)
    val PrimaryLight    = Color(0xFFDBEAFE)
    val OnPrimary       = Color(0xFFFFFFFF)
    val Surface         = Color(0xFFF1F5F9)
    val TextPrimary     = Color(0xFF111827)
    val TextSecondary   = Color(0xFF6B7280)
    val Border          = Color(0xFFE5E7EB)

    val Safe            = Color(0xFF22C55E)
    val SafeBg          = Color(0xFFDCFCE7)
    val Warning         = Color(0xFFF59E0B)
    val WarningBg       = Color(0xFFFEF3C7)
    val Danger          = Color(0xFFEF4444)
    val DangerBg        = Color(0xFFFEE2E2)
}

// ══════════════════════════════════════════════════════════
// 화면 정의 (remember 상태 기반 내비게이션)
// ══════════════════════════════════════════════════════════
sealed class Screen {
    object Home    : Screen()
    object Driving : Screen()
    object History : Screen()
}

// ══════════════════════════════════════════════════════════
// 위험 단계 모델
// ══════════════════════════════════════════════════════════
enum class RiskLevel { SAFE, WARNING, DANGER }

data class RiskUiState(
    val level:           RiskLevel,
    val emoji:           String,
    val label:           String,
    val description:     String,
    val color:           Color,
    val backgroundColor: Color,
    val borderColor:     Color,
)

fun riskUiState(level: RiskLevel): RiskUiState = when (level) {
    RiskLevel.SAFE    -> RiskUiState(level, "🛡", "안전",
        "현재 주행 상태가 안전합니다",
        AppColors.Safe,    AppColors.SafeBg,    AppColors.Safe)
    RiskLevel.WARNING -> RiskUiState(level, "⚠",  "주의",
        "전방에 주의가 필요합니다",
        AppColors.Warning, AppColors.WarningBg, AppColors.Warning)
    RiskLevel.DANGER  -> RiskUiState(level, "!",  "위험",
        "속도를 줄이고 전방을 확인하세요",
        AppColors.Danger,  AppColors.DangerBg,  AppColors.Danger)
}

// ══════════════════════════════════════════════════════════
// 목 데이터
// ══════════════════════════════════════════════════════════
data class TripRecord(
    val id:            String,
    val date:          String,
    val timeRange:     String,
    val duration:      String,
    val dangerCount:   Int,
    val warningCount:  Int,
    val maxRiskLevel:  RiskLevel,
)

val mockTripHistory = listOf(
    TripRecord("1", "2026년 5월 28일", "14:30 - 15:45", "1시간 15분", 3,  8, RiskLevel.DANGER),
    TripRecord("2", "2026년 5월 27일", "10:20 - 11:05", "45분",       1,  4, RiskLevel.DANGER),
    TripRecord("3", "2026년 5월 26일", "16:00 - 16:30", "30분",       0,  2, RiskLevel.WARNING),
    TripRecord("4", "2026년 5월 25일", "09:00 - 09:40", "40분",       0,  0, RiskLevel.SAFE),
)

// ══════════════════════════════════════════════════════════
// MainActivity
// ══════════════════════════════════════════════════════════
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DongHaengGuardApp() }
    }
}

// ══════════════════════════════════════════════════════════
// 앱 루트 — 화면 전환 상태 관리
// ══════════════════════════════════════════════════════════
@Composable
fun DongHaengGuardApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    MaterialTheme {
        Scaffold(
            containerColor = AppColors.Background,
            bottomBar = {
                BottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate    = { currentScreen = it }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    is Screen.Home    -> HomeScreen(
                        onStartDriving = { currentScreen = Screen.Driving }
                    )
                    is Screen.Driving -> DrivingScreen()
                    is Screen.History -> HistoryScreen()
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// 하단 내비게이션 바
// ══════════════════════════════════════════════════════════
@Composable
fun BottomNavBar(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    data class NavItem(val screen: Screen, val emoji: String, val label: String)

    val items = listOf(
        NavItem(Screen.Home,    "○",  "홈"),
        NavItem(Screen.Driving, "▶",  "주행"),
        NavItem(Screen.History, "☰",  "기록"),
    )

    Surface(
        color          = AppColors.CardBackground,
        shadowElevation = 8.dp,
        modifier       = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier                = Modifier.height(64.dp),
            horizontalArrangement   = Arrangement.SpaceEvenly,
            verticalAlignment       = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentScreen::class == item.screen::class
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(item.screen) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text       = item.emoji,
                        fontSize   = 22.sp,
                        color      = if (selected) AppColors.Primary else AppColors.TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold   else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text       = item.label,
                        fontSize   = 11.sp,
                        color      = if (selected) AppColors.Primary else AppColors.TextSecondary,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(AppColors.Primary)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// 홈 화면
// ══════════════════════════════════════════════════════════
@Composable
fun HomeScreen(onStartDriving: () -> Unit) {
    val bleConnected    by remember { mutableStateOf(true) }
    val gpsActive       by remember { mutableStateOf(true) }
    val sensorBattery   by remember { mutableStateOf(87) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // 헤더
        AppTopBar(title = "동행가드")

        LazyColumn(
            modifier           = Modifier.weight(1f),
            contentPadding     = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 앱 소개 카드
            item {
                Card(
                    colors    = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                    shape     = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(vertical = 28.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier            = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AppColors.PrimaryLight),
                            contentAlignment    = Alignment.Center
                        ) {
                            Text("🛡", fontSize = 40.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "동행가드",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "전동 휠체어 이용자를 위한\n실시간 위험 감지 서비스",
                            fontSize  = 13.sp,
                            color     = AppColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 센서 연결 상태
            item {
                SectionLabel("센서 연결 상태")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SensorStatusChip(
                        emoji     = "📶",
                        label     = "BLE",
                        value     = if (bleConnected) "연결됨" else "미연결",
                        connected = bleConnected,
                        modifier  = Modifier.weight(1f)
                    )
                    SensorStatusChip(
                        emoji     = "📍",
                        label     = "GPS",
                        value     = if (gpsActive) "활성" else "비활성",
                        connected = gpsActive,
                        modifier  = Modifier.weight(1f)
                    )
                    SensorStatusChip(
                        emoji     = "🔋",
                        label     = "배터리",
                        value     = "$sensorBattery%",
                        connected = sensorBattery > 20,
                        modifier  = Modifier.weight(1f)
                    )
                }
            }

            // 주요 기능 안내
            item { SectionLabel("주요 기능") }

            item {
                FeatureRow(
                    emoji       = "📡",
                    title       = "실시간 위험 감지",
                    description = "ToF + 초음파 센서로 전방 장애물을 실시간으로 감지합니다"
                )
            }
            item {
                FeatureRow(
                    emoji       = "🗺",
                    title       = "위험 이벤트 기록",
                    description = "GPS 위치와 함께 DANGER / WARNING 이벤트를 자동으로 저장합니다"
                )
            }
            item {
                FeatureRow(
                    emoji       = "🔔",
                    title       = "즉시 알림",
                    description = "위험 단계별 진동 및 소리 알림으로 즉각 대응이 가능합니다"
                )
            }
        }

        // 주행 시작 버튼
        Surface(
            color           = AppColors.CardBackground,
            shadowElevation = 4.dp
        ) {
            Button(
                onClick  = onStartDriving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    contentColor   = AppColors.OnPrimary
                )
            ) {
                Text("▶  주행 시작", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color      = AppColors.TextPrimary
    )
}

@Composable
fun SensorStatusChip(
    emoji:     String,
    label:     String,
    value:     String,
    connected: Boolean,
    modifier:  Modifier = Modifier
) {
    Card(
        colors   = CardDefaults.cardColors(
            containerColor = if (connected) AppColors.SafeBg else AppColors.DangerBg
        ),
        shape    = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(label, fontSize = 10.sp, color = AppColors.TextSecondary)
            Text(
                value,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (connected) AppColors.Safe else AppColors.Danger
            )
        }
    }
}

@Composable
fun FeatureRow(emoji: String, title: String, description: String) {
    Card(
        colors    = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier            = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }
            Column {
                Text(
                    title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color    = AppColors.TextSecondary
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// 주행 중 화면
// ══════════════════════════════════════════════════════════
@Composable
fun DrivingScreen() {
    var tripActive      by remember { mutableStateOf(false) }
    var riskLevel       by remember { mutableStateOf(RiskLevel.SAFE) }
    var distanceMm      by remember { mutableStateOf(1250) }
    val bleConnected    by remember { mutableStateOf(true) }
    val gpsActive       by remember { mutableStateOf(true) }
    var showEndDialog   by remember { mutableStateOf(false) }

    val riskState = riskUiState(riskLevel)

    val animatedBg by animateColorAsState(
        targetValue    = riskState.backgroundColor,
        animationSpec  = tween(300),
        label          = "bg"
    )

    // DANGER 상태일 때 펄스 효과
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue   = 1f,
        targetValue    = if (riskLevel == RiskLevel.DANGER) 1.04f else 1f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // 주행 종료 확인 다이얼로그
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = {
                Text("주행 종료", fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
            },
            text = {
                Text(
                    "주행을 종료하시겠습니까?\n주행 기록이 저장됩니다.",
                    color    = AppColors.TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    tripActive   = false
                    riskLevel    = RiskLevel.SAFE
                    distanceMm   = 1250
                    showEndDialog = false
                }) {
                    Text("종료", color = AppColors.Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("계속 주행", color = AppColors.TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // 헤더 + 연결 상태 바
        Surface(color = AppColors.CardBackground, shadowElevation = 1.dp) {
            Column {
                AppTopBar(title = "주행")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ConnStatusBadge(
                        emoji   = "📶",
                        label   = if (bleConnected) "BLE 연결됨" else "BLE 미연결",
                        active  = bleConnected
                    )
                    ConnStatusBadge(
                        emoji   = "📍",
                        label   = if (gpsActive) "GPS 활성" else "GPS 비활성",
                        active  = gpsActive
                    )
                    ConnStatusBadge(
                        emoji        = if (tripActive) "●" else "○",
                        label        = if (tripActive) "주행 중" else "대기 중",
                        active       = tripActive,
                        activeColor  = AppColors.Danger
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!tripActive) {
                // ── 주행 대기 상태 ──────────────────────────────
                Column(
                    modifier            = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier         = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(AppColors.PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡", fontSize = 50.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "주행 준비 완료",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "센서가 연결되었습니다\n주행을 시작하면 실시간으로 위험을 감지합니다",
                        fontSize  = 14.sp,
                        color     = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    Button(
                        onClick  = { tripActive = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor   = AppColors.OnPrimary
                        )
                    ) {
                        Text("▶  주행 시작", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // ── 주행 중 상태 ────────────────────────────────

                // 위험 상태 카드
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(animatedBg)
                        .border(2.dp, riskState.borderColor, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 위험 레벨 아이콘 (Box로 직접 그리기)
                        Box(
                            modifier         = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(riskState.color.copy(alpha = 0.15f))
                                .border(3.dp, riskState.color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = riskState.emoji,
                                fontSize   = 40.sp,
                                color      = riskState.color,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            riskState.label,
                            fontSize   = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = riskState.color
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            riskState.description,
                            fontSize  = 14.sp,
                            color     = AppColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 전방 거리값 표시
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.75f))
                                .border(1.dp, AppColors.Border, RoundedCornerShape(12.dp))
                                .padding(vertical = 14.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "전방 거리",
                                    fontSize = 12.sp,
                                    color    = AppColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "$distanceMm",
                                        fontSize     = 38.sp,
                                        fontWeight   = FontWeight.Bold,
                                        fontFamily   = FontFamily.Monospace,
                                        color        = AppColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "mm",
                                        fontSize  = 18.sp,
                                        color     = AppColors.TextSecondary,
                                        modifier  = Modifier.padding(bottom = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 임시 센서 데이터 전송 (데모) 버튼
                OutlinedButton(
                    onClick = {
                        val risks     = RiskLevel.values()
                        val distances = listOf(1250, 450, 150)
                        val next      = (riskLevel.ordinal + 1) % risks.size
                        riskLevel  = risks[next]
                        distanceMm = distances[next]
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.TextSecondary
                    )
                ) {
                    Text("[데모] 임시 센서 데이터 전송", fontSize = 13.sp)
                }

                // 주행 종료 버튼
                Button(
                    onClick  = { showEndDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Danger,
                        contentColor   = Color.White
                    )
                ) {
                    Text("■  주행 종료", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 하단 상태 디버그 바 (시안 검토용)
        if (tripActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text     = "상태: 주행중  |  ${riskLevel.name}  |  ${distanceMm}mm",
                    fontSize = 11.sp,
                    color    = AppColors.TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ConnStatusBadge(
    emoji:       String,
    label:       String,
    active:      Boolean,
    activeColor: Color = AppColors.Safe
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 13.sp)
        Text(
            label,
            fontSize = 12.sp,
            color    = if (active) activeColor else AppColors.TextSecondary
        )
    }
}

// ══════════════════════════════════════════════════════════
// 주행 기록 화면
// ══════════════════════════════════════════════════════════
@Composable
fun HistoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        AppTopBar(title = "주행 기록")

        if (mockTripHistory.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "주행 기록이 없습니다",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "첫 주행을 시작해보세요",
                        fontSize = 14.sp,
                        color    = AppColors.TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier           = Modifier.fillMaxSize(),
                contentPadding     = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mockTripHistory) { trip ->
                    TripCard(trip)
                }
            }
        }
    }
}

@Composable
fun TripCard(trip: TripRecord) {
    val rs = riskUiState(trip.maxRiskLevel)

    Card(
        colors    = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 날짜 + 화살표
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    trip.date,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary
                )
                Text(">", fontSize = 18.sp, color = AppColors.TextSecondary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 시간 + 소요 시간
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("○", fontSize = 11.sp, color = AppColors.TextSecondary)
                Text(
                    "${trip.timeRange}  ·  ${trip.duration}",
                    fontSize = 12.sp,
                    color    = AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 위험 / 주의 횟수
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                RiskCountBadge(color = AppColors.Danger,  label = "위험", count = trip.dangerCount)
                RiskCountBadge(color = AppColors.Warning, label = "주의", count = trip.warningCount)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 최고 위험 배지
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(rs.backgroundColor)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    "최고 위험: ${rs.label}",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = rs.color
                )
            }
        }
    }
}

@Composable
fun RiskCountBadge(color: Color, label: String, count: Int) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text("$label  \${count}회", fontSize = 12.sp, color = AppColors.TextSecondary)
    }
}

// ══════════════════════════════════════════════════════════
// 공통 컴포넌트
// ══════════════════════════════════════════════════════════
@Composable
fun AppTopBar(title: String) {
    Surface(color = AppColors.CardBackground, shadowElevation = 1.dp) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                title,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = AppColors.TextPrimary
            )
            Text("⚙", fontSize = 20.sp, color = AppColors.TextSecondary)
        }
    }
}
