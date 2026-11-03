package com.julen.clases

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ScheduleActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(typography = AppTheme.Typography) {
                ScheduleScreen(onBackClick = { finish() })
            }
        }
    }
}

@Composable
fun ScheduleScreen(onBackClick: () -> Unit) {
    val days = listOf("Hora", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
    
    val gridData = listOf(
        listOf("8:00-9:00", "OP1", "PMDM", "AD", "AD", "PSP"),
        listOf("9:00-10:00", "OP1", "PMDM", "AD", "AD", "PSP"),
        listOf("10:00-11:00", "IPEII", "PMDM", "OP2", "IPEII", "PSP"),
        listOf("RECREO", "RECREO", "RECREO", "RECREO", "RECREO", "RECREO"),
        listOf("11:30-12:30", "IPEII", "DI", "OP2", "SI", "SGE"),
        listOf("12:30-13:30", "SI", "DI", "DI", "SI", "SGE"),
        listOf("13:30-14:30", "SI", "DI", "DI", "SI", "SGE")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background)
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar Premium
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
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Volver", tint = AppTheme.Primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Horario Escolar",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .shadow(6.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Cabecera de Días
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        days.forEach { day ->
                            CellItem(text = day, isHeader = true)
                        }
                    }

                    // Filas de Horas y Asignaturas
                    gridData.forEach { rowData ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isRecreoRow = rowData[0] == "RECREO"
                            if (isRecreoRow) {
                                CellItem(text = "11:00-11:30", isHour = true)
                                Box(
                                    modifier = Modifier
                                        .width(574.dp) // Sincronizado para las 5 columnas con espaciado
                                        .height(58.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "R E C R E O",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFF64748B),
                                        fontSize = 13.sp,
                                        letterSpacing = 2.sp
                                    )
                                }
                            } else {
                                rowData.forEachIndexed { index, text ->
                                    CellItem(
                                        text = text,
                                        isHeader = false,
                                        isHour = index == 0
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

@Composable
fun CellItem(text: String, isHeader: Boolean = false, isHour: Boolean = false) {
    val backgroundColor = when {
        isHeader -> AppTheme.Primary
        isHour -> Color(0xFFF8FAFC)
        text == "SI" -> Color(0xFFFFEBEE)
        text == "SGE" -> Color(0xFFE0F2F1)
        text == "PMDM" -> Color(0xFFE3F2FD)
        text == "AD" -> Color(0xFFFFF3E0)
        text == "PSP" -> Color(0xFFF3E5F5)
        text == "DI" -> Color(0xFFE8F5E9)
        text == "IPEII" -> Color(0xFFFFFDE7)
        else -> Color(0xFFF1F5F9)
    }

    val textColor = when {
        isHeader -> Color.White
        isHour -> Color(0xFF64748B)
        text == "SI" -> Color(0xFFC62828)
        text == "SGE" -> Color(0xFF00695C)
        text == "PMDM" -> Color(0xFF1565C0)
        text == "AD" -> Color(0xFFEF6C00)
        text == "PSP" -> Color(0xFF6A1B9A)
        text == "DI" -> Color(0xFF2E7D32)
        text == "IPEII" -> Color(0xFFF57F17)
        else -> Color(0xFF334155)
    }

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(58.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isHeader || isHour || text.length <= 5) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = if (isHour) 11.sp else 13.sp,
            textAlign = TextAlign.Center,
            letterSpacing = if (isHeader) 0.5.sp else 0.sp
        )
    }
}
