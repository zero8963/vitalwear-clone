package com.example.vitalwearclonev1.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Fitness (2026-10-07): the workout logging flow — pick a template chip
 * (pre-fills the body map), tap the map to fine-tune, Save writes to the
 * log. "Custom" starts from an empty map.
 */
@Composable
fun LogWorkoutSheet(
    onDismiss: () -> Unit,
    onSave: (templateName: String, muscles: Set<MuscleGroup>) -> Unit
) {
    var pickedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var selected by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    var showFront by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            backgroundColor = Color(0, 30, 60),
            shape = RoundedCornerShape(20.dp),
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text("Log Workout", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Pick a template, then tap the map to fine-tune.",
                        color = Color.Gray, fontSize = 12.sp
                    )
                }
                // Template chips: 3 rows x 2.
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkoutTemplate.values().toList().chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { t ->
                                    val isPicked = pickedTemplate == t
                                    Button(
                                        onClick = {
                                            pickedTemplate = t
                                            selected = t.muscles.toSet()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = if (isPicked) Color.Cyan else Color(20, 60, 100)
                                        )
                                    ) {
                                        Text(
                                            t.displayName,
                                            color = if (isPicked) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                // Pad odd rows so buttons keep their width.
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    pickedTemplate = null
                                    selected = emptySet()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (pickedTemplate == null && selected.isEmpty())
                                        Color.Cyan else Color(20, 60, 100)
                                )
                            ) {
                                Text(
                                    "Custom",
                                    color = if (pickedTemplate == null && selected.isEmpty())
                                        Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showFront = !showFront }) {
                            Text(
                                if (showFront) "Showing front — tap for back" else "Showing back — tap for front",
                                color = Color.Cyan, fontSize = 13.sp
                            )
                        }
                    }
                }
                item {
                    BodyMapView(
                        mode = BodyMapMode.LOG,
                        statuses = emptyMap(),
                        selected = selected,
                        showFront = showFront,
                        onMuscleTap = { muscle ->
                            selected = if (selected.contains(muscle)) selected - muscle
                            else selected + muscle
                        },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }
                item {
                    Text(
                        if (selected.isEmpty()) "Tap muscles you worked"
                        else "Selected: " + selected.sortedBy { it.name }
                            .joinToString(", ") { it.displayName },
                        color = if (selected.isEmpty()) Color.Gray else Color.Cyan,
                        fontSize = 13.sp
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(60, 60, 70)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val name = if (selected == pickedTemplate?.muscles && pickedTemplate != null)
                                    pickedTemplate!!.displayName else "Custom"
                                onSave(name, selected)
                            },
                            enabled = selected.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Workout", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
