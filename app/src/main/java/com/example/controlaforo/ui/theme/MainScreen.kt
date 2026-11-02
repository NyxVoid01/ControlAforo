package com.example.controlaforo.ui.theme

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // Estados locales sincronizados con Firestore
    var personasActuales by remember { mutableStateOf(0) }
    var aforoMaximo by remember { mutableStateOf(3) }
    var estadoBloqueo by remember { mutableStateOf(false) }
    var luzIndicadora by remember { mutableStateOf("verde") }

    // Escuchar cambios en tiempo real desde Cloud Firestore
    DisposableEffect(Unit) {
        val docRef = db.collection("control_presencia").document("espacio_principal")
        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                personasActuales = snapshot.getLong("personas_actuales")?.toInt() ?: 0
                aforoMaximo = snapshot.getLong("aforo_maximo")?.toInt() ?: 3
                estadoBloqueo = snapshot.getBoolean("estado_bloqueo") ?: false
                luzIndicadora = snapshot.getString("luz_indicadora") ?: "verde"
            }
        }
        onDispose {
            listener.remove()
        }
    }

    // Función para actualizar el aforo en Firestore
    fun actualizarAforo(nuevoValor: Int) {
        val bloqueo = nuevoValor >= aforoMaximo
        val luz = if (bloqueo) "roja" else "verde"

        val datos = mapOf(
            "personas_actuales" to nuevoValor,
            "estado_bloqueo" to bloqueo,
            "luz_indicadora" to luz
        )

        db.collection("control_presencia").document("espacio_principal")
            .update(datos)
            .addOnFailureListener {
                Toast.makeText(context, "Error al actualizar el aforo", Toast.LENGTH_SHORT).show()
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Control de Aforo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Indicador visual luminoso (Círculo de luz)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = if (luzIndicadora == "verde") Color(0xFF4CAF50) else Color(0xFFF44336),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = luzIndicadora.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Contador actual / máximo
            Text(
                text = "$personasActuales / $aforoMaximo",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Personas en el espacio principal",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Botones de control (Entrada / Salida)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (personasActuales > 0) {
                            actualizarAforo(personasActuales - 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(text = "Salida (-1)", fontSize = 16.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        if (personasActuales < aforoMaximo) {
                            actualizarAforo(personasActuales + 1)
                        }
                    },
                    enabled = !estadoBloqueo && personasActuales < aforoMaximo,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(text = "Entrada (+1)", fontSize = 16.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Botón para cerrar sesión
            TextButton(onClick = onLogout) {
                Text(text = "Cerrar Sesión", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}