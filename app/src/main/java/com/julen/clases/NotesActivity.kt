package com.julen.clases

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class NotesActivity : BaseActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(typography = AppTheme.Typography) {
                NotesScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onShowToast = { text -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }
}

@Composable
fun NotesScreen(viewModel: AppViewModel, onBackClick: () -> Unit, onShowToast: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
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
                text = "Notas y Tareas",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selector de Asignaturas Premium
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedSubject,
                onValueChange = {},
                readOnly = true,
                label = { Text("Asignatura", fontWeight = FontWeight.Bold) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expandir",
                        modifier = Modifier.clickable { expanded = true },
                        tint = AppTheme.Primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.Accent,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.White)
            ) {
                viewModel.subjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(subject, fontWeight = FontWeight.Bold, color = AppTheme.Primary) },
                        onClick = {
                            selectedSubject = subject
                            noteText = viewModel.getNote(subject)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Área de Notas con diseño de "Papel Digital"
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            placeholder = { Text("Escribe aquí tus tareas, fechas de exámenes o apuntes importantes...", color = Color(0xFF94A3B8)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.Accent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (selectedSubject.isNotEmpty()) {
                    viewModel.saveNote(selectedSubject, noteText)
                    onShowToast("Notas guardadas para $selectedSubject")
                } else {
                    onShowToast("Selecciona una asignatura primero")
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = AppTheme.Primary.copy(alpha = 0.4f))
        ) {
            Text(text = "GUARDAR CAMBIOS", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
