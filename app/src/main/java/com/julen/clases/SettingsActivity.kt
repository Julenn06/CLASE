package com.julen.clases

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : BaseActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(typography = AppTheme.Typography) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onShowToast = { text -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBackClick: () -> Unit, onShowToast: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

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
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Zona de Peligro",
            style = MaterialTheme.typography.titleLarge,
            color = AppTheme.StatusDanger,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Tarjeta destructiva premium
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Restablecer almacenamiento",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.Primary
                )
                Text(
                    text = "Esta acción borrará de manera inmediata e irreversible todos tus registros de ausencias, retrasos e información de apuntes de todas las asignaturas.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                Button(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.StatusDanger),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "BORRAR DATOS AL COMPLETO", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White,
                title = { Text("¿Confirmar Reset?", fontWeight = FontWeight.Black, color = AppTheme.Primary) },
                text = { Text("¿Estás completamente seguro de eliminar el historial lectivo? Los datos de SharedPreferences serán eliminados.", color = Color(0xFF334155)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetAllData()
                            showDialog = false
                            onShowToast("Todos los datos han sido borrados")
                        }
                    ) {
                        Text("SÍ, BORRAR", color = AppTheme.StatusDanger, fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("CANCELAR", color = AppTheme.Primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
