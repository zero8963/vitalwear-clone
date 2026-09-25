package com.example.vitalwearclonev1.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.communication.PhoneHealthSyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkoutRoot()
        }
    }
}

@Composable
fun WorkoutRoot() {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)
    val syncManager = remember { PhoneHealthSyncManager(context) }
    val workoutManager = remember { WorkoutManager(context) }
    
    var currentScreen by remember { mutableStateOf("DASHBOARD") } // "DASHBOARD", "ACTIVE", "CREATOR"
    var activeRoutine by remember { mutableStateOf<WorkoutRoutine?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(when(currentScreen) {
                        "CREATOR" -> "New Routine"
                        "ACTIVE" -> "Workout"
                        else -> "Vital Workout"
                    }) 
                },
                backgroundColor = Color(0, 100, 150),
                contentColor = Color.White,
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentScreen != "DASHBOARD") currentScreen = "DASHBOARD"
                        else activity?.finish()
                    }) {
                        Icon(if (currentScreen != "DASHBOARD") Icons.Default.ArrowBack else Icons.Default.FitnessCenter, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0, 20, 40))) {
            when (currentScreen) {
                "DASHBOARD" -> {
                    WorkoutDashboard(syncManager, workoutManager, onCreateCustom = {
                        currentScreen = "CREATOR"
                    }) { routine ->
                        activeRoutine = routine
                        currentScreen = "ACTIVE"
                    }
                }
                "CREATOR" -> {
                    CustomWorkoutCreator(workoutManager) {
                        currentScreen = "DASHBOARD"
                    }
                }
                "ACTIVE" -> {
                    activeRoutine?.let {
                        ActiveWorkoutScreen(it) {
                            currentScreen = "DASHBOARD"
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutDashboard(syncManager: PhoneHealthSyncManager, workoutManager: WorkoutManager, onCreateCustom: () -> Unit, onStartRoutine: (WorkoutRoutine) -> Unit) {
    val context = LocalContext.current
    var steps by remember { mutableLongStateOf(0L) }
    var calories by remember { mutableIntStateOf(0) }
    // TEMP DIAG (2026-09-24): shows where the step number came from. REMOVE later.
    var diagText by remember { mutableStateOf("diag: loading...") }
    // Samsung Health workout counter (2026-09-25): verifies Samsung workouts
    // are reaching the app through Health Connect.
    var samsungCount by remember { mutableIntStateOf(0) }
    var samsungCals by remember { mutableIntStateOf(0) }
    var samsungDiag by remember { mutableStateOf("samsung workouts: loading...") }
    
    // Use a trigger to force re-fetch of custom routines
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val customRoutines = remember(refreshTrigger) { workoutManager.getCustomRoutines() }

    LaunchedEffect(Unit) {
        val (stats, diag) = syncManager.getDailyStatsWithDiag(includeOrigins = true)
        steps = stats.first
        calories = stats.second
        diagText = diag
        val sw = syncManager.getSamsungWorkoutsToday()
        samsungCount = sw.count
        samsungCals = sw.caloriesKcal
        samsungDiag = sw.diag
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                backgroundColor = Color(0, 40, 80),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Activity", color = Color.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ActivityStat(label = "Steps", value = steps.toString(), icon = Icons.Default.DirectionsWalk, color = Color.Green)
                        ActivityStat(label = "Calories", value = calories.toString(), icon = Icons.Default.Whatshot, color = Color.Red)
                    }
                    // TEMP DIAG (2026-09-24): on-screen read-path diagnostics. REMOVE later.
                    Spacer(Modifier.height(8.dp))
                    Text(diagText, color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        item {
            Card(
                backgroundColor = Color(0, 40, 80),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Samsung Health Workouts", color = Color.Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ActivityStat(label = "Workouts", value = samsungCount.toString(), icon = Icons.Default.FitnessCenter, color = Color.Yellow)
                        ActivityStat(label = "Calories", value = samsungCals.toString(), icon = Icons.Default.Whatshot, color = Color.Red)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(samsungDiag, color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        item {
            Text("Featured Routines", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        item {
            RoutineItem(workoutManager.getMilitaryRoutine()) { onStartRoutine(it) }
        }

        if (customRoutines.isNotEmpty()) {
            item {
                Text("Custom Routines", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            items(customRoutines) { routine ->
                RoutineItem(
                    routine = routine,
                    onDelete = {
                        workoutManager.deleteCustomRoutine(routine.id)
                        refreshTrigger++
                        android.widget.Toast.makeText(context, "Routine Deleted", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onClick = { onStartRoutine(it) }
                )
            }
        }
        
        item {
            Button(
                onClick = onCreateCustom,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Create Custom Workout", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActivityStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun RoutineItem(routine: WorkoutRoutine, onDelete: (() -> Unit)? = null, onClick: (WorkoutRoutine) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(routine) },
        backgroundColor = Color(20, 60, 100),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.Green, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(routine.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${routine.exercises.size} Exercises", color = Color.Cyan.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun CustomWorkoutCreator(workoutManager: WorkoutManager, onComplete: () -> Unit) {
    val context = LocalContext.current
    var routineName by remember { mutableStateOf("") }
    val exercises = remember { mutableStateListOf<Exercise>() }
    
    // Exercise inputs
    var exName by remember { mutableStateOf("") }
    var exDesc by remember { mutableStateOf("") }
    var exTargetValue by remember { mutableStateOf("") }
    var isTimeBased by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Create Custom Routine", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = routineName,
                onValueChange = { routineName = it },
                label = { Text("Routine Name (e.g. Morning Burn)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
            )
        }

        item {
            Card(backgroundColor = Color(20, 40, 80).copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add New Exercise", color = Color.Cyan, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = exName,
                        onValueChange = { exName = it },
                        label = { Text("Exercise Name (Required)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White)
                    )
                    OutlinedTextField(
                        value = exDesc,
                        onValueChange = { exDesc = it },
                        label = { Text("Brief Description", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text(if (isTimeBased) "Target: Seconds" else "Target: Reps", color = Color.White, modifier = Modifier.weight(1f))
                        Switch(checked = isTimeBased, onCheckedChange = { isTimeBased = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan))
                        Text("Timed", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }

                    OutlinedTextField(
                        value = exTargetValue,
                        onValueChange = { if (it.all { c -> c.isDigit() }) exTargetValue = it },
                        label = { Text(if (isTimeBased) "Seconds" else "Reps", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White)
                    )

                    Button(
                        onClick = {
                            if (exName.isBlank()) {
                                android.widget.Toast.makeText(context, "Exercise name is required", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val target = exTargetValue.toIntOrNull()
                            if (target == null || target <= 0) {
                                android.widget.Toast.makeText(context, "Enter a valid target number", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            exercises.add(Exercise(
                                name = exName,
                                description = exDesc,
                                targetReps = if (!isTimeBased) target else null,
                                targetSeconds = if (isTimeBased) target else null
                            ))
                            exName = ""; exDesc = ""; exTargetValue = ""
                            android.widget.Toast.makeText(context, "Added!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(top = 16.dp).align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100))
                    ) {
                        Text("Add to List", color = Color.White)
                    }
                }
            }
        }

        item {
            Text("List of Exercises (${exercises.size})", color = Color.White, fontWeight = FontWeight.Bold)
        }

        items(exercises) { ex ->
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(255, 255, 255, 10), RoundedCornerShape(8.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ex.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (ex.targetSeconds != null) "${ex.targetSeconds}s" else "${ex.targetReps} reps", color = Color.Cyan, fontSize = 12.sp)
                }
                IconButton(onClick = { exercises.remove(ex) }) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (routineName.isBlank()) {
                        android.widget.Toast.makeText(context, "Routine name is required", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (exercises.isEmpty()) {
                        android.widget.Toast.makeText(context, "Add at least one exercise", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    workoutManager.saveCustomRoutine(WorkoutRoutine(
                        id = "custom_${System.currentTimeMillis()}",
                        name = routineName,
                        exercises = exercises.toList()
                    ))
                    android.widget.Toast.makeText(context, "Routine Saved!", android.widget.Toast.LENGTH_SHORT).show()
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Cyan)
            ) {
                Text("SAVE ROUTINE", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ActiveWorkoutScreen(routine: WorkoutRoutine, onComplete: () -> Unit) {
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var completedCount by remember { mutableIntStateOf(0) }
    val exercise = routine.exercises[currentExerciseIndex]
    
    var timeRemaining by remember { mutableIntStateOf(exercise.targetSeconds ?: 0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val syncManager = remember { com.example.vitalwearclonev1.communication.PhoneHealthSyncManager(context) }
    val monsterManager = remember { com.example.vitalwearclonev1.monster.PhoneMonsterManager(context) }
    val scope = rememberCoroutineScope()

    fun moveToNext(isCompleted: Boolean) {
        if (isCompleted) {
            completedCount++
            // A finished exercise heals critical condition (15 min off the timer).
            if (monsterManager.recordExerciseCompleted()) {
                android.widget.Toast.makeText(context, "Your Digimon has recovered from critical condition!", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        
        if (currentExerciseIndex < routine.exercises.size - 1) {
            currentExerciseIndex++
            timeRemaining = routine.exercises[currentExerciseIndex].targetSeconds ?: 0
            isTimerRunning = false
        } else {
            scope.launch {
                // Adjust calories based on completion percentage (Base 150)
                val totalExercises = routine.exercises.size
                val ratio = if (totalExercises > 0) completedCount.toFloat() / totalExercises else 0f
                val finalCalories = (150 * ratio).toInt()
                
                // Power up the local monster
                monsterManager.applyWorkoutPowerUp(ratio)
                
                // Sync with watch
                syncManager.sendWorkoutSession(routine.name, finalCalories.coerceAtLeast(10)) 
                onComplete()
            }
        }
    }

    LaunchedEffect(isTimerRunning, currentExerciseIndex) {
        if (isTimerRunning && timeRemaining > 0) {
            while (timeRemaining > 0 && isTimerRunning) {
                delay(1000)
                timeRemaining--
            }
            if (timeRemaining == 0) {
                isTimerRunning = false
                // Auto-complete timed exercises? Or let user click?
                // For now, let's just stop the timer.
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(routine.name, color = Color.Gray, fontSize = 14.sp)
        Text("Exercise ${currentExerciseIndex + 1}/${routine.exercises.size}", color = Color.Cyan, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        
        Text(exercise.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(exercise.description, color = Color.Cyan, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        
        Spacer(Modifier.height(48.dp))
        
        if (exercise.targetSeconds != null) {
            Text("${timeRemaining}s", color = Color.Yellow, fontSize = 64.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { isTimerRunning = !isTimerRunning },
                colors = ButtonDefaults.buttonColors(backgroundColor = if (isTimerRunning) Color.Red else Color.Green)
            ) {
                Text(if (isTimerRunning) "PAUSE" else "START TIMER", fontWeight = FontWeight.Bold)
            }
        } else if (exercise.targetReps != null) {
            Text("${exercise.targetReps} REPS", color = Color.Yellow, fontSize = 64.sp, fontWeight = FontWeight.Black)
            Text("TARGET", color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(Modifier.height(64.dp))

        // Complete Button (Main Action)
        Button(
            onClick = { moveToNext(isCompleted = true) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 200, 100))
        ) {
            Text("COMPLETE EXERCISE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (currentExerciseIndex > 0) {
                TextButton(onClick = { currentExerciseIndex-- }) {
                    Text("PREVIOUS", color = Color.Gray)
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            
            TextButton(onClick = { moveToNext(isCompleted = false) }) {
                Text(if (currentExerciseIndex < routine.exercises.size - 1) "SKIP" else "FINISH WITHOUT CREDIT", color = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}
