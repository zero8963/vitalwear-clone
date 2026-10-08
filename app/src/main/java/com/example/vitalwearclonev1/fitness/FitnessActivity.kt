package com.example.vitalwearclonev1.fitness

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.vitalwearclonev1.communication.PhoneHealthSyncManager
import java.time.LocalDate

/**
 * Fitness (2026-10-07): the Training section's home — three tabs:
 * Vitals (watch data via Health Connect), Body Map (muscle recovery +
 * tap-to-log), Coach (rest-day suggestions).
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
            if (tab == 1) {
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
                listOf("Vitals", "Body Map", "Coach").forEachIndexed { i, title ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { Text(title, color = if (tab == i) Color.Cyan else Color.Gray) }
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> VitalsTab(syncManager)
                    1 -> BodyMapTabContent(logStore, logVersion)
                    2 -> CoachTabContent(logStore, logVersion)
                }
            }
        }
    }

    if (showLogSheet) {
        LogWorkoutSheet(
            onDismiss = { showLogSheet = false },
            onSave = { templateName, muscles ->
                logStore.logWorkout(LocalDate.now(), templateName, muscles)
                logVersion++
                showLogSheet = false
                Toast.makeText(
                    context,
                    "Workout logged: ${muscles.size} muscle groups",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}
