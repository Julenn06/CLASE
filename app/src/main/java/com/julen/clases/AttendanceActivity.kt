package com.julen.clases

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.datepicker.MaterialDatePicker

class AttendanceActivity : BaseActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(typography = AppTheme.Typography) {
                AttendanceScreen(
                    viewModel = viewModel,
                    onSelectDatesClick = { showDatePicker() },
                ) { finish() }
            }
        }
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText(getString(R.string.selecciona_el_rango_de_fechas))
        
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { range ->
            val start = range.first
            val end = range.second
            if ((start != null) && (end != null)) {
                viewModel.saveDates(start, end)
            }
        }
        picker.show(supportFragmentManager, "date_picker")
    }
}

@Composable
fun AttendanceScreen(
    viewModel: AppViewModel,
    onSelectDatesClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // Top Bar Premium
        AttendanceTopBar(onBackClick)

        // Rango de fechas de evaluación estilizado tipo Banner Glassmorphic
        EvaluationRangeCard(uiState.dateRangeText, onSelectDatesClick)

        Spacer(modifier = Modifier.height(20.dp))

        // Lista de Asignaturas
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = uiState.subjectList,
                key = { it.name }
            ) { subjectState ->
                SubjectAttendanceCard(subjectState = subjectState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AttendanceTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .background(Color.White, CircleShape)
                .shadow(1.dp, CircleShape)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.back),
                tint = AppTheme.Primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.attendance_title),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun EvaluationRangeCard(dateRangeText: String, onSelectDatesClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.periodo_lectivo),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
                Text(
                    text = dateRangeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.Primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Button(
                onClick = onSelectDatesClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.configurar),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SubjectAttendanceCard(subjectState: AppViewModel.SubjectState, viewModel: AppViewModel) {
    val progressAnim by animateFloatAsState(
        targetValue = subjectState.percentage / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "SubjectProgress"
    )

    val colorStatus by animateColorAsState(
        targetValue = when {
            subjectState.percentage < 10f -> AppTheme.StatusSafe
            subjectState.percentage < 15f -> AppTheme.StatusWarning
            else -> AppTheme.StatusDanger
        },
        label = "StatusColor"
    )

    val badgeText = when {
        subjectState.percentage < 10f -> stringResource(R.string.status_safe)
        subjectState.percentage < 15f -> stringResource(R.string.status_warning)
        else -> stringResource(R.string.status_danger)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0xFF0F172A).copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = subjectState.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = AppTheme.Primary,
                        letterSpacing = (-0.5).sp
                    )
                    Surface(
                        color = colorStatus.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = colorStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.label_percentage, subjectState.percentage),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colorStatus,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progressAnim.coerceIn(0f, 1f) },
                color = colorStatus,
                trackColor = Color(0xFFF1F5F9),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Controles de Acción Rediseñados (Modern & Tactile)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Caja Retrasos
                ModernCounter(
                    label = stringResource(R.string.retrasos),
                    value = stringResource(R.string.tardy_format, subjectState.tardies % 3),
                    onDecrement = { viewModel.removeTardy(subjectState.name) },
                    onIncrement = { viewModel.addTardy(subjectState.name) },
                    decrementContentDescription = stringResource(R.string.remove_tardy),
                    incrementContentDescription = stringResource(R.string.add_tardy),
                    modifier = Modifier.weight(1f)
                )

                // Caja Ausencias
                ModernCounter(
                    label = stringResource(R.string.ausencias),
                    value = subjectState.absences.toString(),
                    onDecrement = { viewModel.removeAbsence(subjectState.name) },
                    onIncrement = { viewModel.addAbsence(subjectState.name) },
                    decrementContentDescription = stringResource(R.string.remove_absence),
                    incrementContentDescription = stringResource(R.string.add_absence),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ModernCounter(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementContentDescription: String,
    incrementContentDescription: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Menos
            CounterActionButton(
                icon = ImageVector.vectorResource(R.drawable.ic_remove),
                contentDescription = decrementContentDescription,
                onClick = onDecrement,
                modifier = Modifier.weight(1f)
            )

            // Valor Central
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.5f))
            )

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppTheme.Primary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.5f))
            )

            // Botón Más
            CounterActionButton(
                icon = ImageVector.vectorResource(R.drawable.ic_add),
                contentDescription = incrementContentDescription,
                onClick = onIncrement,
                modifier = Modifier.weight(1f),
                isIncrement = true
            )
        }
    }
}

@Composable
fun CounterActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isIncrement: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (isIncrement) AppTheme.Accent else Color.Gray),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = if (isIncrement) AppTheme.Accent else Color(0xFF64748B)
        )
    }
}
