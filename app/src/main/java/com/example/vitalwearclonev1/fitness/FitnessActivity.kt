package com.example.vitalwearclonev1.fitness

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import com.example.vitalwearclonev1.communication.PhoneHealthSyncManager
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Fitness (2026-10-07): the Training section's home — three tabs:
 * Vitals (watch data via Health Connect), Body Map (muscle recovery +
 * tap-to-log), Coach (rest-day suggestions).
 *
 * Fitness (2026-10-27): four tabs. Exercises is the manual log's home —
 * the searchable exercise list with the sets x reps x weight commit
 * form; Body Map is now purely the recovery viewer (the old log sheet
 * still opens from its + button, templates and map fine-tune intact).
 */
class FitnessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessRoot()
        }
    }
}

@Composable
fun FitnessRoot() {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)
    val syncManager = remember { PhoneHealthSyncManager(context) }
    val logStore = remember { WorkoutLogStore(context) }

    var tab by remember { mutableIntStateOf(0) }
    var showLogSheet by remember { mutableStateOf(false) }
    // Bumped every time a workout is saved so the map + coach recompute.
    var logVersion by remember { mutableIntStateOf(0) }

    // 2026-10-07: explicit Health Connect permission request for the Fitness
    // section. The home screen's auto-request never fired for the newly added
    // heart-rate/sleep permissions, and Health Connect won't list a permission
    // type until the app has requested it at least once — so this button is
    // currently the ONLY way to grant them. null = still checking.
    val scope = rememberCoroutineScope()
    var permsGranted by remember { mutableStateOf<Boolean?>(null) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        // Contract result is unreliable for partial grants; re-read the truth.
        scope.launch { permsGranted = syncManager.hasAllPermissions() }
    }
    // Re-check on launch and every time the Vitals tab opens — this also
    // picks up grants the user made manually in Health Connect settings.
    LaunchedEffect(tab) {
        permsGranted = syncManager.hasAllPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fitness") },
                backgroundColor = Color(0, 100, 150),
                contentColor = Color.White,
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            // 2026-10-27: the + button lives on the Body Map viewer tab
            // (now index 2) and still opens the template/map log sheet.
            if (tab == 2) {
                FloatingActionButton(
                    onClick = { showLogSheet = true },
                    backgroundColor = Color(0, 150, 100)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .background(Color(0, 20, 40))
        ) {
            TabRow(
                selectedTabIndex = tab,
                backgroundColor = Color(0, 40, 80),
                contentColor = Color.Cyan
            ) {
                listOf("Vitals", "Exercises", "Body Map", "Coach").forEachIndexed { i, title ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { Text(title, color = if (tab == i) Color.Cyan else Color.Gray) }
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> VitalsTab(
                        syncManager,
                        permsGranted = permsGranted,
                        onGrantPermissions = {
                            healthPermissionLauncher.launch(syncManager.permissions)
                        }
                    )
                    1 -> ExercisesTabContent(
                        store = logStore,
                        logVersion = logVersion,
                        onCommit = { exercise, muscles, sets, reps, weightLb ->
                            // 2026-10-27: per-exercise commit — the same
                            // log + credit path as the sheet save (one
                            // trophy/VP per exercise, 2h dedup window via
                            // markInAppExerciseLog), plus the sets x reps
                            // x weight detail in the stored entry.
                            logStore.logExercise(
                                LocalDate.now(), exercise, muscles,
                                sets, reps, weightLb
                            )
                            logStore.markInAppExerciseLog()
                            PhoneMonsterManager(context).recordExerciseCompleted()
                            logVersion++
                            val effort = "$sets x $reps" +
                                (weightLb?.let { " @ ${formatWeightLb(it)} lb" }
                                    ?: " (bodyweight)")
                            Toast.makeText(
                                context,
                                "Logged: ${exercise.name} $effort (+1 trophy)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    2 -> BodyMapTabContent(logStore, logVersion, syncManager) { logVersion++ }
                    3 -> CoachTabContent(logStore, logVersion)
                }
            }
        }
    }

    if (showLogSheet) {
        LogWorkoutSheet(
            onDismiss = { showLogSheet = false },
            onSave = { templateName, muscles, exerciseCount ->
                logStore.logWorkout(LocalDate.now(), templateName, muscles)
                if (exerciseCount > 0) {
                    // 2026-10-08: per-exercise workout credit — every
                    // exercise logged via the picker earns the same
                    // trophy/VP credit as a Health Connect session, through
                    // the exact same recordExerciseCompleted() path (no
                    // rebalancing). The Health Connect auto-credit denies
                    // sessions overlapping this log's 2h window, so the
                    // same workout never double-counts — while separate
                    // sessions (morning run vs afternoon gym) each count.
                    val monsterManager = PhoneMonsterManager(context)
                    repeat(exerciseCount) { monsterManager.recordExerciseCompleted() }
                    logStore.markInAppExerciseLog()
                }
                logVersion++
                showLogSheet = false
                Toast.makeText(
                    context,
                    "Workout logged: ${muscles.size} muscle groups" +
                        if (exerciseCount > 0) " (+$exerciseCount trophies)" else "",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}
