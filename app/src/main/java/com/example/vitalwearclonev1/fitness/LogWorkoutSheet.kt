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
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
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
 *
 * Fitness (2026-10-08): exercise picker added. Searching and tapping an
 * exercise auto-selects its muscles (primary = full cyan, secondary-only =
 * lighter shade). Manual map taps override the auto-fill afterwards:
 * tapping a selected muscle removes it, tapping an empty one adds it.
 * Templates behave exactly as before (a template pick clears exercises and
 * pre-fills the template's muscles).
 */
@Composable
fun LogWorkoutSheet(
    onDismiss: () -> Unit,
    onSave: (templateName: String, muscles: Set<MuscleGroup>, exerciseCount: Int) -> Unit
) {
    var pickedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var pickedExercises by remember { mutableStateOf<Set<Exercise>>(emptySet()) }
    // Manual layer: muscles the user tapped on/off themselves.
    var manual by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    // Muscles the user explicitly tapped OFF (overrides the auto-fill).
    var excluded by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    var exerciseQuery by remember { mutableStateOf("") }
    var showFront by remember { mutableStateOf(true) }

    // Muscles contributed by the picked exercises (recomputed on add/remove).
    val exerciseDerived: Set<MuscleGroup> = remember(pickedExercises) {
        pickedExercises.flatMap { it.primary + it.secondary }.toSet()
    }
    val exercisePrimary: Set<MuscleGroup> = remember(pickedExercises) {
        pickedExercises.flatMap { it.primary }.toSet()
    }
    val exerciseSecondaryOnly: Set<MuscleGroup> = remember(pickedExercises) {
        pickedExercises.flatMap { it.secondary }.toSet() - exercisePrimary
    }

    // The effective selection shown on the map and saved to the log.
    val selected: Set<MuscleGroup> = (exerciseDerived - excluded) + manual
    // Secondary-only muscles that are currently selected → lighter shade.
    val secondarySelected: Set<MuscleGroup> = exerciseSecondaryOnly.intersect(selected)

    fun addExercise(e: Exercise) {
        if (!pickedExercises.contains(e)) pickedExercises = pickedExercises + e
        exerciseQuery = ""
    }

    fun removeExercise(e: Exercise) {
        pickedExercises = pickedExercises - e
    }

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
                        "Pick a template or search exercises, then tap the map to fine-tune.",
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
                                            pickedExercises = emptySet()
                                            manual = t.muscles.toSet()
                                            excluded = emptySet()
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
                                    pickedExercises = emptySet()
                                    manual = emptySet()
                                    excluded = emptySet()
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
                // Exercise picker: searchable, tap to add as a removable chip.
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Exercises — auto-fill muscles",
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                        TextField(
                            value = exerciseQuery,
                            onValueChange = { exerciseQuery = it },
                            placeholder = {
                                Text(
                                    "Search ${EXERCISES.size} exercises…",
                                    color = Color.Gray, fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color(20, 60, 100),
                                textColor = Color.White,
                                cursorColor = Color.Cyan,
                                focusedIndicatorColor = Color.Cyan,
                                unfocusedIndicatorColor = Color.Gray,
                                placeholderColor = Color.Gray
                            )
                        )
                        val matches = remember(exerciseQuery, pickedExercises) {
                            if (exerciseQuery.isBlank()) emptyList()
                            else EXERCISES.filter {
                                it.name.contains(exerciseQuery, ignoreCase = true) &&
                                    it !in pickedExercises
                            }.take(6)
                        }
                        matches.forEach { e ->
                            Button(
                                onClick = { addExercise(e) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0, 90, 110)
                                )
                            ) {
                                Text(
                                    "+ ${e.name}",
                                    color = Color.White, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        pickedExercises.forEach { e ->
                            Card(
                                backgroundColor = Color(0, 100, 130),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        e.name,
                                        color = Color.White, fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { removeExercise(e) }) {
                                        Text("✕", color = Color(255, 120, 120), fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                        if (pickedExercises.isNotEmpty()) {
                            Text(
                                "Dark cyan = primary muscle, light cyan = secondary. " +
                                    "Tap the map to adjust.",
                                color = Color.Gray, fontSize = 11.sp
                            )
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
                            if (selected.contains(muscle)) {
                                manual = manual - muscle
                                excluded = excluded + muscle
                            } else {
                                excluded = excluded - muscle
                                manual = manual + muscle
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.9f),
                        secondarySelected = secondarySelected
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
                                onSave(name, selected, pickedExercises.size)
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
