package com.example.vitalwearclonev1.game

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.example.vitalwearclonev1.ui.HealthBar
import com.github.cfogrady.vb.dim.card.BemCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EducationalGameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra("EXTRA_MODE") ?: "MATH"
        val isLesson = intent.getBooleanExtra("EXTRA_IS_LESSON", false)
        setContent {
            EducationalGameRoot(mode, isLesson)
        }
    }
}

@Composable
fun EducationalGameRoot(mode: String, isLesson: Boolean) {
    var selectedGrade by remember { mutableStateOf<String?>(null) }
    var isAdjusted by remember { mutableStateOf(false) }
    var currentFloor by remember { mutableStateOf<Int?>(null) }
    var selectedFormulaUnit by remember { mutableStateOf<FormulaUnit?>(null) }
    var selectedSpellingUnit by remember { mutableStateOf<SpellingUnit?>(null) }
    val context = LocalContext.current
    val progressManager = remember { MathGameProgressManager(context) }

    if (selectedGrade == null) {
        GradeSelectionScreen(mode, isAdjusted, onAdjustedToggle = { isAdjusted = it }) { grade ->
            selectedGrade = grade
        }
    } else if (isLesson) {
        InstructionalLessonScreen(mode, selectedGrade!!, isAdjusted) {
            selectedGrade = null
        }
    } else if (mode == "FORMULA" && selectedFormulaUnit == null) {
        FormulaUnitSelectionScreen(mode, selectedGrade!!) { unit ->
            selectedFormulaUnit = unit
        }
    } else if (mode == "FORMULA") {
        GamePlayScreen(mode, selectedGrade!!, 1, isAdjusted, progressManager, formulaUnit = selectedFormulaUnit) {
            selectedFormulaUnit = null
        }
    } else if (mode == "SPELLING" && selectedSpellingUnit == null) {
        SpellingUnitSelectionScreen(mode, selectedGrade!!) { unit ->
            selectedSpellingUnit = unit
        }
    } else if (mode == "SPELLING") {
        GamePlayScreen(mode, selectedGrade!!, 1, isAdjusted, progressManager, spellingUnit = selectedSpellingUnit) {
            selectedSpellingUnit = null
        }
    } else if (currentFloor == null) {
        FloorSelectionScreen(mode, selectedGrade!!, progressManager) { floor ->
            currentFloor = floor
        }
    } else {
        GamePlayScreen(mode, selectedGrade!!, currentFloor!!, isAdjusted, progressManager) {
            currentFloor = null
        }
    }
}

@Composable
fun rememberProblemRepository(grade: String, floor: Int, isAdjusted: Boolean): ProblemRepository {
    return remember(grade, floor, isAdjusted) {
        val mathGen = MathProblemGenerator(grade, floor, isAdjusted)
        val readingGen = ReadingProblemGenerator(grade, floor, isAdjusted)
        val historyGen = HistoryProblemGenerator(grade, floor, isAdjusted)
        val scienceGen = ScienceProblemGenerator(grade, floor, isAdjusted)
        val geometryGen = GeometryProblemGenerator(grade, floor, isAdjusted)
        val lifeScienceGen = LifeScienceProblemGenerator(grade, floor, isAdjusted)

        val offline = OfflineProblemProvider(mathGen, readingGen, historyGen, scienceGen, geometryGen, lifeScienceGen)
        ProblemRepository(offline)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InstructionalLessonScreen(mode: String, grade: String, isAdjusted: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val monsterManager = remember { PhoneMonsterManager(context) }
    val formulaMasteryManager = remember { FormulaMasteryManager(context) }
    val spellingMasteryManager = remember { SpellingMasteryManager(context) }
    val scope = rememberCoroutineScope()

    val repository = rememberProblemRepository(grade, 1, isAdjusted)

    var history by remember { mutableStateOf(listOf<Lesson>()) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var isLoading by remember { mutableStateOf(false) }
    val lessonHistory = remember { LessonHistoryManager(context) }

    fun generateNew() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            // Session-seen titles plus titles seen in previous sessions (persisted).
            val sessionSeen = history.take(currentIndex + 1).map { it.title }
            var seenTitles = (sessionSeen + lessonHistory.getSeenTitles(mode, grade)).distinct()
            var newLesson = repository.getLesson(mode, grade, seenTitles)
            if (newLesson.title in seenTitles) {
                // Every lesson in the pool has been seen: start a fresh cycle instead of
                // repeating at random. Avoid immediately repeating the on-screen lesson.
                lessonHistory.clearHistory(mode, grade)
                val avoid = history.getOrNull(currentIndex)?.title
                seenTitles = (sessionSeen + listOfNotNull(avoid)).distinct()
                newLesson = repository.getLesson(mode, grade, seenTitles)
            }
            lessonHistory.markTitleSeen(mode, grade, newLesson.title)
            val newHistory = history.take(currentIndex + 1) + newLesson
            history = newHistory
            currentIndex = history.size - 1
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (currentIndex == -1) {
            generateNew()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$mode Lessons (Grade $grade)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                backgroundColor = Color(0, 100, 80),
                contentColor = Color.White
            )
        }
    ) { padding ->
        val lesson = if (currentIndex >= 0 && currentIndex < history.size) history[currentIndex] else null
        
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(10, 20, 30))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = lesson,
                transitionSpec = {
                    if (targetState != initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        fadeIn() togetherWith fadeOut()
                    }
                }
            ) { targetLesson ->
                if (targetLesson != null) {
                    Card(
                        backgroundColor = Color(30, 50, 70),
                        shape = RoundedCornerShape(16.dp),
                        elevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = targetLesson.title,
                                color = Color.Cyan,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = targetLesson.explanation,
                                color = Color.White,
                                fontSize = 18.sp,
                                lineHeight = 26.sp
                            )
                            
                            if (targetLesson.steps.isNotEmpty()) {
                                Spacer(Modifier.height(24.dp))
                                Text("How to do it:", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                targetLesson.steps.forEach { step ->
                                    Text(
                                        text = "• $step",
                                        color = Color.LightGray,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                            
                            targetLesson.example?.let {
                                Spacer(Modifier.height(24.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0, 0, 0, 100), RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text("Example:", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(it, color = Color.White, fontSize = 16.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                }
                            }

                            targetLesson.practiceQuestion?.let { question ->
                                Spacer(Modifier.height(32.dp))
                                var userAnswer by remember(targetLesson) { mutableStateOf<String?>(null) }
                                var showSolveSteps by remember(targetLesson) { mutableStateOf(false) }
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(40, 60, 80), RoundedCornerShape(12.dp))
                                        .padding(16.dp)
                                ) {
                                    Text("Practice Time!", color = Color.Cyan, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(question, color = Color.White, fontSize = 20.sp)
                                    
                                    Spacer(Modifier.height(16.dp))

                                    if (targetLesson.practiceOptions.isNotEmpty()) {
                                        targetLesson.practiceOptions.forEach { option ->
                                            Button(
                                                onClick = { 
                                                    if (userAnswer == null) {
                                                        userAnswer = option
                                                        val correct = option == targetLesson.practiceAnswer
                                                        if (correct) {
                                                            monsterManager.addXp(100)
                                                        }
                                                        // 2026-09-27: lessons grant the secret crit-CHANCE bonus.
                                                        // Roll 1-5%, minus 1% if the answer was wrong (never below 0).
                                                        val gain = monsterManager.grantLessonCritBonus(if (correct) 0 else 1)
                                                        val total = monsterManager.getSecretCritChanceBonus()
                                                        Toast.makeText(context, if (gain > 0)
                                                            "Secret Crit Chance +$gain%! (total +$total%)"
                                                            else "No crit bonus - wrong answer.",
                                                            Toast.LENGTH_LONG).show()
                                                        // Formula lessons also feed mastery (NAME form:
                                                        // refreshes last-seen, levels only on APPLY).
                                                        if (mode == "FORMULA") {
                                                            formulaCardByName(targetLesson.title)?.let { card ->
                                                                formulaMasteryManager.recordAnswer(card.id, FormulaForm.NAME, correct)
                                                            }
                                                        }
                                                        // Spelling lessons: multiple-choice correct feeds
                                                        // mastery as PICK (last-seen only, like formula NAME).
                                                        if (mode == "SPELLING") {
                                                            spellingCardByWord(targetLesson.title)?.let { card ->
                                                                spellingMasteryManager.recordAnswer(card.id, SpellingForm.PICK, correct)
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    backgroundColor = when {
                                                        userAnswer == null -> Color(60, 80, 120)
                                                        option == targetLesson.practiceAnswer -> Color.Green.copy(alpha = 0.6f)
                                                        option == userAnswer -> Color.Red.copy(alpha = 0.6f)
                                                        else -> Color(60, 80, 120)
                                                    }
                                                ),
                                                enabled = userAnswer == null
                                            ) {
                                                Text(option, color = Color.White)
                                            }
                                        }
                                    } else {
                                        var textInput by remember(targetLesson) { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = textInput,
                                            onValueChange = { textInput = it },
                                            label = { Text("Your Answer", color = Color.Gray) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White),
                                            enabled = userAnswer == null
                                        )
                                        Button(
                                            onClick = { 
                                                userAnswer = textInput
                                                val correct = textInput.trim().equals(targetLesson.practiceAnswer, ignoreCase = true)
                                                if (correct) {
                                                    monsterManager.addXp(100)
                                                }
                                                // 2026-09-27: lessons grant the secret crit-CHANCE bonus.
                                                val gain = monsterManager.grantLessonCritBonus(if (correct) 0 else 1)
                                                val total = monsterManager.getSecretCritChanceBonus()
                                                Toast.makeText(context, if (gain > 0)
                                                    "Secret Crit Chance +$gain%! (total +$total%)"
                                                    else "No crit bonus - wrong answer.",
                                                    Toast.LENGTH_LONG).show()
                                                // Spelling lessons: typed correct feeds mastery as
                                                // SPELL (levels on two consecutive correct SPELLs,
                                                // mirroring formula APPLY). This text-input branch
                                                // is first used by spelling lessons.
                                                if (mode == "SPELLING") {
                                                    spellingCardByWord(targetLesson.title)?.let { card ->
                                                        spellingMasteryManager.recordAnswer(card.id, SpellingForm.SPELL, correct)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            enabled = userAnswer == null
                                        ) {
                                            Text("Check Answer")
                                        }
                                    }
                                    
                                    userAnswer?.let {
                                        val isCorrect = it.trim().equals(targetLesson.practiceAnswer, ignoreCase = true)
                                        Text(
                                            text = if (isCorrect) "Correct! +100 XP" else "Not quite!",
                                            color = if (isCorrect) Color.Green else Color.Yellow,
                                            modifier = Modifier.padding(top = 16.dp),
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (targetLesson.solveSteps.isNotEmpty()) {
                                            Button(
                                                onClick = { showSolveSteps = !showSolveSteps },
                                                modifier = Modifier.padding(top = 8.dp),
                                                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                                                elevation = null
                                            ) {
                                                Text(if (showSolveSteps) "Hide Steps" else "Show me how", color = Color.Cyan)
                                            }

                                            if (showSolveSteps) {
                                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                                    targetLesson.solveSteps.forEach { step ->
                                                        Text("→ $step", color = Color.LightGray, fontSize = 14.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Formula Card section (2026-11-01): for FORMULA lessons, show
            // the card banner + memory trick + mastery level under the lesson
            // card, reusing the existing lesson card UI.
            if (mode == "FORMULA") {
                lesson?.let { currentLesson ->
                    formulaCardByName(currentLesson.title)?.let { card ->
                        val masteryLevel = formulaMasteryManager.level(card.id)
                        Spacer(Modifier.height(16.dp))
                        Card(
                            backgroundColor = Color(60, 50, 10),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("★ FORMULA CARD", color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))
                                Text(card.formulaSymbols, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))
                                Text("Remember: ${card.memoryTrick}", color = Color.LightGray, fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (masteryLevel >= 3) "★ MASTERED" else "Mastery level $masteryLevel / 3",
                                    color = if (masteryLevel >= 3) Color.Yellow else Color.Gray,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Spelling Card section (2026-11-01): for SPELLING lessons, show
            // the card banner + breakdown + memory trick + mastery level
            // under the lesson card, mirroring the Formula Card banner.
            if (mode == "SPELLING") {
                lesson?.let { currentLesson ->
                    spellingCardByWord(currentLesson.title)?.let { card ->
                        val masteryLevel = spellingMasteryManager.level(card.id)
                        Spacer(Modifier.height(16.dp))
                        Card(
                            backgroundColor = Color(60, 50, 10),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("★ SPELLING CARD", color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))
                                Text(card.word, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))
                                Text("Breakdown: ${card.breakdown}", color = Color.LightGray, fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Remember: ${card.memoryTrick}", color = Color.LightGray, fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (masteryLevel >= 3) "★ MASTERED" else "Mastery level $masteryLevel / 3",
                                    color = if (masteryLevel >= 3) Color.Yellow else Color.Gray,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(100, 60, 40))
                ) {
                    Text("PREVIOUS", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (currentIndex < history.size - 1) {
                            currentIndex++
                        } else {
                            generateNew()
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 100))
                ) {
                    Text(if (currentIndex < history.size - 1) "NEXT" else if (isLoading) "LOADING..." else "NEW CONCEPT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(100, 100, 100))
            ) {
                Text("RETURN TO MENU", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "Education Mode is designed to help you maintain these skills for life.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun GradeSelectionScreen(mode: String, isAdjusted: Boolean, onAdjustedToggle: (Boolean) -> Unit, onGradeSelected: (String) -> Unit) {
    val grades = listOf("K") + (1..12).map { it.toString() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$mode - Select Grade") },
                backgroundColor = Color(0, 50, 100),
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(10, 10, 20))) {
            // Subtle adjusted mode toggle at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(20, 20, 40))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Paced Learning", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Focus on core concepts with extra time", color = Color.Gray, fontSize = 12.sp)
                }
                Switch(
                    checked = isAdjusted,
                    onCheckedChange = onAdjustedToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(grades.size) { index ->
                    val grade = grades[index]
                    GradeCard(grade) { onGradeSelected(grade) }
                }
            }
        }
    }
}

@Composable
fun GradeCard(grade: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(60, 40, 100))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("Grade $grade", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FloorSelectionScreen(mode: String, grade: String, progressManager: MathGameProgressManager, onFloorSelected: (Int) -> Unit) {
    val highest = progressManager.getHighestClearedFloor(mode, grade)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$mode (Grade $grade) - Select Floor") },
                backgroundColor = Color(0, 50, 100),
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(10, 10, 20))) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(20) { index ->
                    val floor = index + 1
                    val isUnlocked = floor <= highest + 1
                    FloorCard(floor, isUnlocked) {
                        if (isUnlocked) onFloorSelected(floor)
                    }
                }
            }
        }
    }
}

@Composable
fun FormulaUnitSelectionScreen(mode: String, grade: String, onUnitSelected: (FormulaUnit) -> Unit) {
    val context = LocalContext.current
    val masteryManager = remember { FormulaMasteryManager(context) }
    val entryBand = formulaBandForGrade(grade)
    val unlockedIds = masteryManager.unlockedUnits(entryBand).map { it.id }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formula Cards (Grade $grade)") },
                backgroundColor = Color(0, 50, 100),
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(10, 10, 20)).padding(16.dp)) {
            Text(
                "Master a unit to unlock the next one — even in a higher grade band. Gold means mastered.",
                color = Color.Gray, fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(FORMULA_UNITS.size) { index ->
                    val unit = FORMULA_UNITS[index]
                    val isUnlocked = unit.id in unlockedIds
                    val isMastered = masteryManager.isUnitMastered(unit)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isMastered) Color(150, 120, 0)
                                else if (isUnlocked) Color(30, 60, 120)
                                else Color.DarkGray
                            )
                            .clickable(enabled = isUnlocked) { if (isUnlocked) onUnitSelected(unit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                unit.name,
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (isMastered) "★ MASTERED"
                                else if (isUnlocked) "${unit.cardIds.size} cards"
                                else "LOCKED",
                                color = if (isMastered) Color.Yellow else Color.Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpellingUnitSelectionScreen(mode: String, grade: String, onUnitSelected: (SpellingUnit) -> Unit) {
    val context = LocalContext.current
    val masteryManager = remember { SpellingMasteryManager(context) }
    val entryBand = spellingBandForGrade(grade)
    val unlockedIds = masteryManager.unlockedUnits(entryBand).map { it.id }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spelling Cards (Grade $grade)") },
                backgroundColor = Color(0, 50, 100),
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(10, 10, 20)).padding(16.dp)) {
            Text(
                "Master a unit to unlock the next one — even in a higher grade band. Gold means mastered.",
                color = Color.Gray, fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(SPELLING_UNITS.size) { index ->
                    val unit = SPELLING_UNITS[index]
                    val isUnlocked = unit.id in unlockedIds
                    val isMastered = masteryManager.isUnitMastered(unit)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isMastered) Color(150, 120, 0)
                                else if (isUnlocked) Color(30, 60, 120)
                                else Color.DarkGray
                            )
                            .clickable(enabled = isUnlocked) { if (isUnlocked) onUnitSelected(unit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                unit.name,
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (isMastered) "★ MASTERED"
                                else if (isUnlocked) "${unit.cardIds.size} cards"
                                else "LOCKED",
                                color = if (isMastered) Color.Yellow else Color.Gray,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloorCard(floor: Int, isUnlocked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isUnlocked) Color(30, 60, 120) else Color.DarkGray)
            .clickable(enabled = isUnlocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isUnlocked) "Floor $floor" else "LOCKED",
            color = if (isUnlocked) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * Battle-form mapping for spelling mastery: battles have no typed
 * input, so a battle PICK is the battle's SPELL performance (see the
 * SpellingMasteryState note). FILL and USE record as themselves.
 */
private fun spellingBattleForm(form: SpellingForm): SpellingForm =
    if (form == SpellingForm.PICK) SpellingForm.SPELL else form

@Composable
fun GamePlayScreen(mode: String, grade: String, floor: Int, isAdjusted: Boolean, progressManager: MathGameProgressManager, formulaUnit: FormulaUnit? = null, spellingUnit: SpellingUnit? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val cardManager = remember { CardManager(context) }
    val monsterManager = remember { PhoneMonsterManager(context) }
    // Formula battle variant (2026-11-01): when formulaUnit is set, problems
    // come from that unit's deck only, and answers feed mastery. All other
    // mechanics (HP, timer, enemies, credit) are the existing ones.
    val isFormula = formulaUnit != null
    val formulaMasteryManager = remember { FormulaMasteryManager(context) }
    val formulaGenerator = remember { FormulaProblemGenerator(isAdjusted) }
    var currentFormulaCard by remember { mutableStateOf<FormulaCard?>(null) }
    var currentFormulaForm by remember { mutableStateOf<FormulaForm?>(null) }
    val formulaUnlockedBefore = remember {
        if (isFormula) formulaMasteryManager.unlockedUnits(formulaBandForGrade(grade)).map { it.id }.toSet() else emptySet()
    }
    // Spelling battle variant (2026-11-01): mirrors the formula variant.
    // Battles have no typed input, so a correct battle PICK (choosing
    // the exact spelling among misspellings) is recorded as SPELL —
    // the battle's spelling performance — while lesson multiple-choice
    // correct stays PICK (last-seen only).
    val isSpelling = spellingUnit != null
    val spellingMasteryManager = remember { SpellingMasteryManager(context) }
    val spellingGenerator = remember { SpellingProblemGenerator(isAdjusted) }
    var currentSpellingCard by remember { mutableStateOf<SpellingCard?>(null) }
    var currentSpellingForm by remember { mutableStateOf<SpellingForm?>(null) }
    val spellingUnlockedBefore = remember {
        if (isSpelling) spellingMasteryManager.unlockedUnits(spellingBandForGrade(grade)).map { it.id }.toSet() else emptySet()
    }

    val repository = rememberProblemRepository(grade, floor, isAdjusted)

    var currentProblem by remember { mutableStateOf<Problem?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Initialize first problem
    LaunchedEffect(Unit) {
        isLoading = true
        if (isFormula) {
            val fp = formulaGenerator.nextProblem(formulaUnit!!.cards())
            currentFormulaCard = fp.card
            currentFormulaForm = fp.form
            currentProblem = fp.problem
        } else if (isSpelling) {
            val sp = spellingGenerator.nextProblem(spellingUnit!!.cards())
            currentSpellingCard = sp.card
            currentSpellingForm = sp.form
            currentProblem = sp.problem
        } else {
            currentProblem = repository.getProblem(mode, grade, floor, isAdjusted)
        }
        isLoading = false
    }

    var playerHP by remember { mutableFloatStateOf(500f) }
    var enemyHP by remember { mutableFloatStateOf(500f) }
    val maxHP = 500f
    val damagePerCorrect = 100f
    val damagePerWrong = 100f
    
    val baseTimer = if (isAdjusted) 60 else 30
    var timer by remember { mutableIntStateOf(baseTimer) }
    var isGameOver by remember { mutableStateOf(false) }
    var isWin by remember { mutableStateOf(false) }
    
    var enemiesDefeated by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) } // wrong answers + timeouts this floor
    val enemiesPerFloor = 3

    val mySprites = remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    val enemySprites = remember { mutableStateOf<Map<String, Bitmap?>>(emptyMap()) }
    val currentFrame = remember { mutableIntStateOf(0) }
    
    val playerOffset = remember { Animatable(0f) }
    val enemyOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    /** Next problem from the right source: the unit deck for formula/spelling battles, the repository otherwise. */
    fun requestNextProblem() {
        scope.launch {
            isLoading = true
            if (isFormula) {
                val fp = formulaGenerator.nextProblem(formulaUnit!!.cards())
                currentFormulaCard = fp.card
                currentFormulaForm = fp.form
                currentProblem = fp.problem
            } else if (isSpelling) {
                val sp = spellingGenerator.nextProblem(spellingUnit!!.cards())
                currentSpellingCard = sp.card
                currentSpellingForm = sp.form
                currentProblem = sp.problem
            } else {
                currentProblem = repository.getProblem(mode, grade, floor, isAdjusted)
            }
            isLoading = false
        }
    }

    fun loadRandomEnemy() {
        val cardNames = cardManager.listCards()
        if (cardNames.isNotEmpty()) {
            val randomCardName = cardNames.random()
            val card = cardManager.getCard(randomCardName)
            card?.let {
                val isBem = it is BemCard
                val s = it.spriteData.sprites
                val characterEntries = it.characterStats.characterEntries
                if (characterEntries.isNotEmpty()) {
                    val charIdx = (0 until characterEntries.size).random()
                    enemySprites.value = mapOf(
                        "IDLE1" to SpriteBitmapHandler.getBitmap(s[monsterManager.getIdleSpriteIndices(charIdx, isBem)[0]]),
                        "IDLE2" to SpriteBitmapHandler.getBitmap(s[monsterManager.getIdleSpriteIndices(charIdx, isBem)[1]]),
                        "ATK" to SpriteBitmapHandler.getBitmap(s[monsterManager.getBattleSpriteIndex(charIdx, isBem)]),
                        "WIN" to SpriteBitmapHandler.getBitmap(s[monsterManager.getWinSpriteIndex(charIdx, isBem)]),
                        "LOSE" to SpriteBitmapHandler.getBitmap(s[monsterManager.getLoseSpriteIndex(charIdx, isBem)])
                    )
                }
            }
        }
    }

    // Animation Loop
    LaunchedEffect(Unit) {
        while(true) {
            currentFrame.intValue = (currentFrame.intValue + 1) % 2
            delay(500)
        }
    }

    // Timer Loop
    LaunchedEffect(isGameOver) {
        if (!isGameOver) {
            while (timer > 0 && !isGameOver) {
                delay(1000)
                timer--
            }
            if (timer == 0 && !isGameOver) {
                // Timeout = Penalty (and a formula miss, for mastery + requeue).
                playerHP -= damagePerWrong
                mistakes++
                timer = baseTimer

                if (isFormula && currentFormulaCard != null && currentFormulaForm != null) {
                    formulaMasteryManager.recordAnswer(currentFormulaCard!!.id, currentFormulaForm!!, false)
                    formulaGenerator.recordResult(currentFormulaCard!!, false)
                }
                if (isSpelling && currentSpellingCard != null && currentSpellingForm != null) {
                    spellingMasteryManager.recordAnswer(currentSpellingCard!!.id, spellingBattleForm(currentSpellingForm!!), false)
                    spellingGenerator.recordResult(currentSpellingCard!!, false)
                }
                requestNextProblem()
                
                if (playerHP <= 0) {
                    isGameOver = true
                    isWin = false
                }
            }
        }
    }

    // Load Sprites
    LaunchedEffect(Unit) {
        val myState = monsterManager.getCurrentMonster()
        if (myState != null) {
            val card = cardManager.getCard(myState.cardName)
            card?.let {
                val isBem = it is BemCard
                val s = it.spriteData.sprites
                mySprites.value = mapOf(
                    "IDLE1" to SpriteBitmapHandler.getBitmap(s[monsterManager.getIdleSpriteIndices(myState.characterId, isBem)[0]]),
                    "IDLE2" to SpriteBitmapHandler.getBitmap(s[monsterManager.getIdleSpriteIndices(myState.characterId, isBem)[1]]),
                    "ATK" to SpriteBitmapHandler.getBitmap(s[monsterManager.getBattleSpriteIndex(myState.characterId, isBem)]),
                    "WIN" to SpriteBitmapHandler.getBitmap(s[monsterManager.getWinSpriteIndex(myState.characterId, isBem)]),
                    "LOSE" to SpriteBitmapHandler.getBitmap(s[monsterManager.getLoseSpriteIndex(myState.characterId, isBem)])
                )
            }
        }
        loadRandomEnemy()
    }

    fun handleAnswer(isCorrect: Boolean) {
        if (isGameOver) return

        // Formula battles feed mastery + the missed-card requeue on every answer.
        if (isFormula && currentFormulaCard != null && currentFormulaForm != null) {
            formulaMasteryManager.recordAnswer(currentFormulaCard!!.id, currentFormulaForm!!, isCorrect)
            formulaGenerator.recordResult(currentFormulaCard!!, isCorrect)
        }
        // Spelling battles feed mastery + the missed-card requeue on every answer.
        if (isSpelling && currentSpellingCard != null && currentSpellingForm != null) {
            spellingMasteryManager.recordAnswer(currentSpellingCard!!.id, spellingBattleForm(currentSpellingForm!!), isCorrect)
            spellingGenerator.recordResult(currentSpellingCard!!, isCorrect)
        }

        if (isCorrect) {
            // Player Attacks
            scope.launch {
                playerOffset.animateTo(50f, tween(200))
                enemyHP -= damagePerCorrect
                playerOffset.animateTo(0f, tween(100))
                
                if (enemyHP <= 0) {
                    enemiesDefeated++
                    if (enemiesDefeated >= enemiesPerFloor) {
                        // Formula/spelling battles skip the floor key: unit
                        // progress is mastery, not floors. Credit (trophy +
                        // crit bonus) is identical to a normal floor clear.
                        if (!isFormula && !isSpelling) progressManager.markFloorCleared(mode, grade, floor)
                        // 2026-09-27: practice floors grant the secret crit-DAMAGE bonus.
                        // Roll 1-5%, minus 1% per mistake on this floor (never below 0).
                        val gain = monsterManager.grantPracticeCritBonus(mistakes)
                        val total = monsterManager.getSecretCritDamageBonus()
                        // 2026-09-29: education stands in for bracelet missions —
                        // each cleared floor earns a trophy toward evolution.
                        monsterManager.recordLessonCompleted()
                        val unlockNote = if (isFormula) {
                            val unlockedAfter = formulaMasteryManager.unlockedUnits(formulaBandForGrade(grade)).map { it.id }.toSet()
                            val newly = unlockedAfter - formulaUnlockedBefore
                            if (newly.isNotEmpty()) {
                                val names = newly.mapNotNull { id -> FORMULA_UNITS.firstOrNull { it.id == id }?.name }
                                " You mastered ${formulaUnit!!.name} — ${names.joinToString(", ")} unlocked!"
                            } else ""
                        } else if (isSpelling) {
                            val unlockedAfter = spellingMasteryManager.unlockedUnits(spellingBandForGrade(grade)).map { it.id }.toSet()
                            val newly = unlockedAfter - spellingUnlockedBefore
                            if (newly.isNotEmpty()) {
                                val names = newly.mapNotNull { id -> SPELLING_UNITS.firstOrNull { it.id == id }?.name }
                                " You mastered ${spellingUnit!!.name} — ${names.joinToString(", ")} unlocked!"
                            } else ""
                        } else ""
                        Toast.makeText(context, (if (gain > 0)
                            "Secret Crit Damage +$gain%! (total +$total%)"
                            else "Floor cleared! No crit bonus - too many mistakes.") + unlockNote,
                            Toast.LENGTH_LONG).show()
                        isWin = true
                        isGameOver = true
                    } else {
                        enemyHP = maxHP
                        loadRandomEnemy()
                        requestNextProblem()
                        timer = baseTimer
                    }
                } else {
                    requestNextProblem()
                    timer = baseTimer
                }
            }
        } else {
            // Enemy Attacks (wrong answer = mistake)
            mistakes++
            scope.launch {
                enemyOffset.animateTo(-50f, tween(200))
                playerHP -= damagePerWrong
                enemyOffset.animateTo(0f, tween(100))
                
                if (playerHP <= 0) {
                    isWin = false
                    isGameOver = true
                } else {
                    requestNextProblem()
                    timer = baseTimer
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFormula) "FORMULA ${formulaUnit!!.name} - Enemy ${enemiesDefeated + 1}/$enemiesPerFloor" else if (isSpelling) "SPELLING ${spellingUnit!!.name} - Enemy ${enemiesDefeated + 1}/$enemiesPerFloor" else "$mode G$grade F$floor - Enemy ${enemiesDefeated + 1}/$enemiesPerFloor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                backgroundColor = Color(0, 50, 100),
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            
            // Battle Scene (Mini)
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HealthBar(playerHP, maxHP)
                        val myImg = if (currentFrame.intValue == 0) mySprites.value["IDLE1"] else mySprites.value["IDLE2"]
                        myImg?.let { Image(it.asImageBitmap(), "Me", Modifier.size(80.dp).offset(x = playerOffset.value.dp)) }
                    }
                    Text(" VS ", color = Color.White, fontWeight = FontWeight.Bold)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HealthBar(enemyHP, maxHP)
                        val enImg = if (currentFrame.intValue == 0) enemySprites.value["IDLE1"] else enemySprites.value["IDLE2"]
                        enImg?.let { Image(it.asImageBitmap(), "Enemy", Modifier.size(80.dp).offset(x = enemyOffset.value.dp)) }
                    }
                }
            }

            if (!isGameOver && currentProblem != null) {
                Text("TIME: $timer", color = if (timer < 10) Color.Red else Color.Yellow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.height(16.dp))
                
                val question = currentProblem!!.question
                val options = currentProblem!!.options
                val correct = currentProblem!!.correctAnswer

                Text(question, color = Color.White, fontSize = if (mode == "MATH") 48.sp else 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 16.dp))
                
                Spacer(Modifier.height(32.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(options.size) { index ->
                        val option = options[index]
                        Button(
                            onClick = { handleAnswer(option == correct) },
                            modifier = Modifier.fillMaxWidth().height(if (mode == "MATH") 80.dp else 60.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(40, 40, 80)),
                            enabled = !isLoading
                        ) {
                            Text(option, color = Color.White, fontSize = if (mode == "MATH") 24.sp else 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (!isGameOver && isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Cyan)
                }
            } else if (isGameOver) {
                Spacer(Modifier.height(64.dp))
                Text(
                    if (isWin && isFormula && formulaMasteryManager.isUnitMastered(formulaUnit!!)) "VICTORY! UNIT MASTERED!"
                    else if (isWin && isSpelling && spellingMasteryManager.isUnitMastered(spellingUnit!!)) "VICTORY! UNIT MASTERED!"
                    else if (isWin) "VICTORY!" else "GAME OVER",
                    color = if (isWin) Color.Green else Color.Red, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = onBack) {
                    Text("Return to Menu")
                }
            }
        }
    }
}
