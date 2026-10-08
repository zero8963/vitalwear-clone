package com.example.vitalwearclonev1.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Refresh
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

/**
 * Fitness (2026-10-07): body-map in VIEW mode — regions color-coded by
 * recovery state. Tapping a region shows what the app knows about that
 * muscle. Front/back toggle up top.
 */
@Composable
fun BodyMapTabContent(
    store: WorkoutLogStore,
    logVersion: Int
) {
    var showFront by remember { mutableStateOf(true) }
    // Recompute when a workout is logged (logVersion bumps).
    val statuses = remember(logVersion) { RecoveryEngine.getStatuses(store) }
    val stateByMuscle = remember(statuses) { statuses.associate { it.muscle to it.state } }
    var tapped by remember { mutableStateOf<MuscleStatus?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { showFront = !showFront },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 90, 160)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(if (showFront) "Front — tap for back" else "Back — tap for front", color = Color.White)
                }
            }
        }
        item {
            BodyMapView(
                mode = BodyMapMode.VIEW,
                statuses = stateByMuscle,
                selected = emptySet(),
                showFront = showFront,
                onMuscleTap = { muscle ->
                    tapped = statuses.firstOrNull { it.muscle == muscle }
                },
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
        item {
            tapped?.let { s ->
                val whenText = when {
                    s.daysSinceTrained == null -> "never logged"
                    s.daysSinceTrained == 0L -> "today"
                    s.daysSinceTrained == 1L -> "yesterday"
                    else -> "${s.daysSinceTrained} days ago"
                }
                val stateText = when (s.state) {
                    RecoveryState.FRIED -> "fried — rest it"
                    RecoveryState.RECOVERING -> "recovering"
                    RecoveryState.READY -> "ready to train"
                    RecoveryState.NEVER -> "not trained yet"
                }
                Text(
                    "${s.muscle.displayName}: last trained $whenText ($stateText)",
                    color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            } ?: Text(
                "Tap a muscle to see its status",
                color = Color.Gray, fontSize = 12.sp
            )
        }
        item {
            RecoveryLegend()
        }
    }
}

@Composable
private fun RecoveryLegend() {
    Card(
        backgroundColor = Color(0, 40, 80),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendDot(Color(0xFFE53935), "Trained <24h")
            LegendDot(Color(0xFFFFA000), "Recovering")
            LegendDot(Color(0xFF3D5A4C), "Ready")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(14.dp).padding(end = 6.dp)) {
            drawCircle(color, radius = 14f)
        }
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

/**
 * Fitness (2026-10-07): the Coach tab — today's headline, "hit these" and
 * "let these rest" lists from the RecoveryEngine.
 */
@Composable
fun CoachTabContent(store: WorkoutLogStore, logVersion: Int) {
    val statuses = remember(logVersion) { RecoveryEngine.getStatuses(store) }
    val headline = remember(statuses) { RecoveryEngine.getHeadline(statuses) }
    val (hit, rest) = remember(statuses) { RecoveryEngine.getSuggestions(statuses) }

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
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Today's Call", color = Color.Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(headline, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Text("Hit these", color = Color.Green, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (hit.isEmpty()) {
            item { Text("Nothing due right now.", color = Color.Gray, fontSize = 14.sp) }
        } else {
            items(hit) { s -> SuggestionRow(s, Color.Green) }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Let these rest", color = Color(0xFFFFA000), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (rest.isEmpty()) {
            item { Text("Nothing recovering — everything's fresh.", color = Color.Gray, fontSize = 14.sp) }
        } else {
            items(rest) { s -> SuggestionRow(s, Color(0xFFFFA000)) }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SuggestionRow(s: CoachSuggestion, accent: Color) {
    Card(
        backgroundColor = Color(20, 60, 100),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (accent == Color.Green) Icons.Default.CheckCircle else Icons.Default.Hotel,
                null, tint = accent
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(s.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(s.detail, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
