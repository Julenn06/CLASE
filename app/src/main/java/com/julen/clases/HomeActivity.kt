package com.julen.clases

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HomeActivity : BaseActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(typography = AppTheme.Typography) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSchedule = { startActivity(Intent(this, ScheduleActivity::class.java)) },
                    onNavigateToAttendance = { startActivity(Intent(this, AttendanceActivity::class.java)) },
                    onNavigateToNotes = { startActivity(Intent(this, NotesActivity::class.java)) },
                    onNavigateToSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDashboard()
    }
}

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToSchedule: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Animación suave de entrada de los elementos
    val animProgress by animateFloatAsState(
        targetValue = uiState.progress / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "DashboardProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background)
            .systemBarsPadding()
            .padding(20.dp)
    ) {
        // Encabezado Minimalista Premium
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mi Progreso",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape)
                    .shadow(1.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Perfil",
                    tint = AppTheme.Primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta Principal de Estado - Gradiente Líquido Oscuro Neumórfico
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = AppTheme.Primary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(AppTheme.PrimaryGradient))
                    .padding(26.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTADO DE LA EVALUACIÓN",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Activo",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "${uiState.remainingDays} días restantes",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Barra de progreso estilizada
                LinearProgressIndicator(
                    progress = { animProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = AppTheme.StatusSafe,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Panel interno de información premium (Glassmorphism look)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TOTAL FALTAS",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = uiState.totalAbsences.toString(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(1.dp, 36.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        Column(modifier = Modifier.weight(1.5f).padding(start = 16.dp)) {
                            Text(
                                text = "CLASE ACTUAL",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = uiState.currentSubject,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Accesos Rápidos",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Grid Premium estilizado asimétrico
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                MenuGridCard(
                    text = "Horario",
                    subText = "Calendario escolar",
                    icon = Icons.Default.DateRange,
                    gradient = AppTheme.AccentGradient,
                    onClick = onNavigateToSchedule,
                    modifier = Modifier.weight(1f)
                )
                MenuGridCard(
                    text = "Asistencia",
                    subText = "Control de límites",
                    icon = Icons.Default.CheckCircle,
                    gradient = listOf(Color(0xFF34D399), Color(0xFF059669)),
                    onClick = onNavigateToAttendance,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                MenuGridCard(
                    text = "Notas",
                    subText = "Tareas y apuntes",
                    icon = Icons.Default.Edit,
                    gradient = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                    onClick = onNavigateToNotes,
                    modifier = Modifier.weight(1f)
                )
                MenuGridCard(
                    text = "Ajustes",
                    subText = "Limpieza de datos",
                    icon = Icons.Default.Settings,
                    gradient = listOf(Color(0xFF94A3B8), Color(0xFF475569)),
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MenuGridCard(
    text: String,
    subText: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Brush.linearGradient(gradient), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Column {
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppTheme.Primary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
