package com.julen.clases.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.julen.clases.AppViewModel
import com.julen.clases.UiState
import com.julen.clases.data.AttendanceRepository
import com.julen.clases.ui.components.PeriodPickerDialog
import com.julen.clases.ui.formatDateUtc
import com.julen.clases.ui.theme.AppColors
import com.julen.clases.ui.theme.heroGradient

private const val STEPS = 3

@Composable
fun OnboardingScreen(state: UiState, viewModel: AppViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(state.userName) }
    var limit by rememberSaveable { mutableFloatStateOf(AttendanceRepository.DEFAULT_LIMIT) }
    var showPeriodPicker by remember { mutableStateOf(false) }
    val white = Color.White

    val finish = {
        viewModel.setUserName(name)
        viewModel.setAbsenceLimit(limit)
        viewModel.completeOnboarding()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(heroGradient())),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepDots(current = step)
                Spacer(Modifier.weight(1f))
                if (step < STEPS - 1) {
                    TextButton(onClick = finish) { Text("Saltar", color = white.copy(alpha = 0.85f)) }
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                    }
                },
                label = "onboarding",
                modifier = Modifier.weight(1f),
            ) { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 32.dp),
                ) {
                    when (current) {
                        0 -> {
                            BigIcon(Icons.Filled.AutoAwesome)
                            Title("Tu curso, bajo control")
                            Body("Horario en directo, faltas, notas, exámenes y apuntes en un solo sitio.")
                            Spacer(Modifier.height(24.dp))
                            Feature(Icons.Filled.CalendarMonth, "Clase actual y próxima en tiempo real")
                            Feature(Icons.Filled.FactCheck, "Cuántas faltas te quedan en cada asignatura")
                            Feature(Icons.Filled.School, "Media ponderada de tus notas")
                            Feature(Icons.Filled.EventNote, "Agenda con cuenta atrás de exámenes")
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(30) },
                                label = { Text("¿Cómo te llamas? (opcional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = white,
                                    unfocusedTextColor = white,
                                    focusedBorderColor = white,
                                    unfocusedBorderColor = white.copy(alpha = 0.5f),
                                    focusedLabelColor = white,
                                    unfocusedLabelColor = white.copy(alpha = 0.75f),
                                    cursorColor = white,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        1 -> {
                            BigIcon(Icons.Filled.CalendarMonth)
                            Title("¿Cuándo es la evaluación?")
                            Body("Con las fechas de inicio y fin calculo las horas reales de cada asignatura según tu horario.")
                            Spacer(Modifier.height(24.dp))
                            val period = state.period
                            Button(
                                onClick = { showPeriodPicker = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = white.copy(alpha = 0.18f), contentColor = white),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                Text(
                                    if (period != null) {
                                        "${formatDateUtc(period.startUtc, "d MMM")} – ${formatDateUtc(period.endUtc, "d MMM yyyy")}"
                                    } else {
                                        "Elegir fechas"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Body("Puedes cambiarlas más tarde en Ajustes, y añadir festivos para que no cuenten.")
                        }

                        else -> {
                            BigIcon(Icons.Filled.Tune)
                            Title("Límite de faltas")
                            Body("Porcentaje máximo de horas que puedes faltar por asignatura antes de perder la evaluación continua.")
                            Spacer(Modifier.height(28.dp))
                            Text(
                                "${limit.toInt()}%",
                                color = white,
                                style = MaterialTheme.typography.displayLarge,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                            Slider(
                                value = limit,
                                onValueChange = { limit = it },
                                valueRange = 5f..30f,
                                steps = 24,
                                colors = SliderDefaults.colors(
                                    thumbColor = white,
                                    activeTrackColor = white,
                                    inactiveTrackColor = white.copy(alpha = 0.25f),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                ),
                            )
                            Body("Lo habitual es entre el 15 % y el 20 %. Consulta la normativa de tu centro.")
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) {
                    TextButton(onClick = { step-- }) { Text("Atrás", color = white) }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { if (step < STEPS - 1) step++ else finish() },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = heroGradient().first()),
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(
                        if (step < STEPS - 1) "Continuar" else "Empezar",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }

    if (showPeriodPicker) {
        PeriodPickerDialog(
            initial = state.period,
            onDismiss = { showPeriodPicker = false },
            onConfirm = { start, end ->
                viewModel.savePeriod(start, end)
                showPeriodPicker = false
            },
        )
    }
}

@Composable
private fun StepDots(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(STEPS) { index ->
            val width by animateDpAsState(if (index == current) 28.dp else 8.dp, label = "dot")
            val color by animateColorAsState(
                if (index <= current) Color.White else Color.White.copy(alpha = 0.35f),
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun BigIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Title(text: String) {
    Text(text, color = Color.White, style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
