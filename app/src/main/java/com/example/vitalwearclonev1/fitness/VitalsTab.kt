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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.communication.PhoneHealthSyncManager
import java.time.Instant

/**
 * Fitness (2026-10-07): today's vitals from Health Connect — heart-rate
 * trace from the watch, steps, calories, last night's sleep. Every read is
 * failure-safe; empty states explain what's missing instead of crashing.
 */
@Composable
fun VitalsTab(
    syncManager: PhoneHealthSyncManager,
    permsGranted: Boolean?,
    onGrantPermissions: () -> Unit
) {
    var samples by remember { mutableStateOf<List<Pair<Instant, Int>>>(emptyList()) }
    var steps by remember { mutableLongStateOf(0L) }
    var calories by remember { mutableStateOf(0) }
    var sleepHours by remember { mutableStateOf<Double?>(null) }
    var loaded by remember { mutableStateOf(false) }

    // Keyed on permsGranted so data reloads right after the user grants —
    // the first pass (null) loads with whatever was already granted.
    LaunchedEffect(permsGranted) {
        try {
            samples = syncManager.getHeartRateToday()
        } catch (e: Exception) { samples = emptyList() }
        try {
            val stats = syncManager.getDailyStats()
            steps = stats.first
            calories = stats.second
        } catch (e: Exception) { /* keep zeros */ }
        try {
            sleepHours = syncManager.getSleepLastNightHours()
        } catch (e: Exception) { sleepHours = null }
        loaded = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 2026-10-07: Health Connect only lists permission types the app has
        // requested at least once, so until the user taps this, heart rate /
        // sleep can't be granted at all — this button is the way in.
        if (permsGranted == false) {
            item {
                Card(
                    backgroundColor = Color(0, 60, 100),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(
                            "Heart rate + sleep need permission",
                            color = Color.Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Steps and workouts are syncing, but the new heart-rate " +
                                "and sleep permissions haven't been granted yet. " +
                                "Tap below to allow them in Health Connect.",
                            color = Color.LightGray, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onGrantPermissions,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant health permissions", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        item {
            HeartRateCard(samples, loaded)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalMiniCard(
                    label = "Steps",
                    value = if (loaded) steps.toString() else "…",
                    icon = { Icon(Icons.Default.DirectionsWalk, null, tint = Color.Green) },
                    modifier = Modifier.weight(1f)
                )
                VitalMiniCard(
                    label = "Calories",
                    value = if (loaded) calories.toString() else "…",
                    icon = { Icon(Icons.Default.Whatshot, null, tint = Color.Red) },
                    modifier = Modifier.weight(1f)
                )
                VitalMiniCard(
                    label = "Sleep",
                    value = if (!loaded) "…" else sleepHours?.let { "%.1fh".format(it) } ?: "—",
                    icon = { Icon(Icons.Default.Bedtime, null, tint = Color(0xFF90CAF9)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (loaded && samples.isEmpty()) {
            item {
                Text(
                    "No heart-rate data today — Samsung Health > Settings > Health Connect > " +
                        "\"Share\" needs heart rate turned on, and the watch needs to be worn.",
                    color = Color.Gray, fontSize = 12.sp
                )
            }
        }
        if (loaded && sleepHours == null) {
            item {
                Text(
                    "No sleep session found — wear the watch overnight with sleep tracking on.",
                    color = Color.Gray, fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun VitalMiniCard(
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = Color(0, 40, 80),
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HeartRateCard(samples: List<Pair<Instant, Int>>, loaded: Boolean) {
    // One point per ~5 minutes keeps the trace smooth without thousands of dots.
    val trace = remember(samples) { downsample(samples) }
    val latest = samples.lastOrNull()?.second
    val resting = samples.minOfOrNull { it.second }
    val avg = if (samples.isNotEmpty()) samples.map { it.second }.average().toInt() else null

    Card(
        backgroundColor = Color(0, 40, 80),
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = Color.Red)
                Spacer(Modifier.width(8.dp))
                Text("Heart Rate Today", color = Color.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            if (!loaded) {
                Text("Loading…", color = Color.Gray, fontSize = 14.sp)
            } else if (trace.size < 2) {
                Text("Not enough data yet today.", color = Color.Gray, fontSize = 14.sp)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HrStat("Now", latest?.toString() ?: "—", Color.Red)
                    HrStat("Resting", resting?.toString() ?: "—", Color.Green)
                    HrStat("Average", avg?.toString() ?: "—", Color.Yellow)
                }
                Spacer(Modifier.height(12.dp))
                HeartRateGraph(trace, modifier = Modifier.fillMaxWidth().height(160.dp))
            }
        }
    }
}

@Composable
private fun HrStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

/** Bucket samples into ~5-minute averages, oldest first. */
private fun downsample(samples: List<Pair<Instant, Int>>): List<Pair<Instant, Int>> {
    if (samples.isEmpty()) return emptyList()
    return samples.groupBy { it.first.epochSecond / 300L }
        .entries.sortedBy { it.key }
        .map { e -> Instant.ofEpochSecond(e.key * 300L) to e.value.map { it.second }.average().toInt() }
}

@Composable
private fun HeartRateGraph(trace: List<Pair<Instant, Int>>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val leftPad = 44f
        val botPad = 24f
        val topPad = 12f
        val plotW = size.width - leftPad - 8f
        val plotH = size.height - topPad - botPad
        if (plotW <= 0 || plotH <= 0) return@Canvas

        val minBpm = (trace.minOf { it.second } - 5).coerceAtLeast(30)
        val maxBpm = trace.maxOf { it.second } + 5
        val t0 = trace.first().first.epochSecond.toFloat()
        val t1 = trace.last().first.epochSecond.toFloat().coerceAtLeast(t0 + 1f)

        fun x(t: Instant) = leftPad + (t.epochSecond - t0) / (t1 - t0) * plotW
        fun y(bpm: Int) = topPad + (1f - (bpm - minBpm).toFloat() / (maxBpm - minBpm)) * plotH

        // gridlines at quartiles
        val gridColor = Color(255, 255, 255, 24)
        for (f in listOf(0f, 0.5f, 1f)) {
            val gy = topPad + f * plotH
            drawLine(gridColor, Offset(leftPad, gy), Offset(leftPad + plotW, gy), strokeWidth = 1f)
        }

        val path = Path()
        trace.forEachIndexed { i, (t, bpm) ->
            val p = Offset(x(t), y(bpm))
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, Color(0xFFE53935), style = Stroke(width = 3f))
    }
}
