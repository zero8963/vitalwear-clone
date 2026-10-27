package com.example.vitalwearclonev1.fitness

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Fitness (2026-10-27): the Exercises tab — the full exercise list with
 * search. Manual logging is the source of truth for muscles (Health
 * Connect sessions never carry per-exercise detail), so tapping an
 * exercise opens the sets x reps x weight commit form. Committing logs
 * the exercise's muscles to the body map and recovery state through the
 * same store + credit path as every other log.
 */
@Composable
fun ExercisesTabContent(
    store: WorkoutLogStore,
    logVersion: Int,
    onCommit: (exercise: Exercise, muscles: Set<MuscleGroup>, sets: Int, reps: Int, weightLb: Float?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Exercise?>(null) }
    // Recents refresh whenever a commit bumps logVersion.
    val recentNames = remember(logVersion) { store.getRecentExerciseNames() }
    val recentExercises = remember(recentNames) {
        recentNames.mapNotNull { name -> EXERCISES.firstOrNull { it.name == name } }
    }
    val matches = remember(query) {
        if (query.isBlank()) EXERCISES
        else EXERCISES.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { query = it },
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
        if (recentExercises.isNotEmpty()) {
            Text(
                "Recent — tap to log again",
                color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentExercises.forEach { e ->
                    Button(
                        onClick = { selected = e },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0, 90, 110)
                        )
                    ) {
                        Text(e.name, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
        if (matches.isEmpty()) {
            Text(
                "No exercises match \"$query\".",
                color = Color.Gray, fontSize = 13.sp
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(matches, key = { it.name }) { exercise ->
                ExerciseRow(exercise = exercise, onTap = { selected = exercise })
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    selected?.let { exercise ->
        ExerciseLogFormDialog(
            exercise = exercise,
            onDismiss = { selected = null },
            onCommit = { muscles, sets, reps, weightLb ->
                onCommit(exercise, muscles, sets, reps, weightLb)
                selected = null
            }
        )
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onTap: () -> Unit) {
    Card(
        backgroundColor = Color(0, 40, 80),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onTap,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0, 40, 80)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    exercise.name,
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
                val primary = exercise.primary.joinToString(", ") { it.displayName }
                val secondary = exercise.secondary.joinToString(", ") { it.displayName }
                Text(
                    primary + if (secondary.isNotEmpty()) "  •  $secondary" else "",
                    color = Color.Gray, fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Sets x reps x weight commit form. Weight is optional — blank means
 * bodyweight. Muscles default to the exercise's mapping; tapping a chip
 * toggles that group out (the manual override, same idea as the map
 * fine-tune in the log sheet).
 */
@Composable
private fun ExerciseLogFormDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onCommit: (muscles: Set<MuscleGroup>, sets: Int, reps: Int, weightLb: Float?) -> Unit
) {
    var setsText by remember { mutableStateOf("3") }
    var repsText by remember { mutableStateOf("10") }
    var weightText by remember { mutableStateOf("") }
    val allMuscles = remember(exercise) { exercise.primary + exercise.secondary }
    var excluded by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    val selectedMuscles = allMuscles - excluded

    val sets = setsText.toIntOrNull()?.takeIf { it > 0 }
    val reps = repsText.toIntOrNull()?.takeIf { it > 0 }
    val weightLb = weightText.trim().ifBlank { null }?.toFloatOrNull()
    val weightValid = weightText.isBlank() || weightLb != null
    val canCommit = sets != null && reps != null && weightValid &&
        selectedMuscles.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = Color(0, 30, 60),
            shape = RoundedCornerShape(20.dp),
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    exercise.name,
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = "Sets",
                        value = setsText,
                        onValueChange = { setsText = it.filter { c -> c.isDigit() }.take(2) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "Reps",
                        value = repsText,
                        onValueChange = { repsText = it.filter { c -> c.isDigit() }.take(3) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "Weight (lb)",
                        value = weightText,
                        onValueChange = {
                            weightText = it.filter { c -> c.isDigit() || c == '.' }.take(7)
                        },
                        keyboardType = KeyboardType.Decimal,
                        placeholder = "Bodyweight",
                        modifier = Modifier.weight(1.2f)
                    )
                }
                if (sets != null && reps != null) {
                    Text(
                        "$sets x $reps" + (weightLb?.let { " @ ${formatWeightLb(it)} lb" }
                            ?: " (bodyweight)"),
                        color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Muscles hit — tap to remove one:",
                    color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allMuscles.sortedBy { it.name }.forEach { muscle ->
                        val on = selectedMuscles.contains(muscle)
                        Button(
                            onClick = {
                                excluded = if (on) excluded + muscle else excluded - muscle
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (on) Color(0, 100, 130)
                                else Color(60, 60, 70)
                            )
                        ) {
                            Text(
                                muscle.displayName,
                                color = Color.White, fontSize = 13.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(60, 60, 70)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            onCommit(selectedMuscles, sets!!, reps!!, weightLb)
                        },
                        enabled = canCommit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0, 150, 100)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Commit", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    placeholder: String? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it, color = Color.Gray, fontSize = 13.sp) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
    }
}
