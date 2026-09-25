package com.example.vitalwearclonev1.card

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.github.cfogrady.vb.dim.card.BemCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class CardBrowserEntry(
    val name: String,
    val charCount: Int,
    val isBem: Boolean,
    val thumb: Bitmap?
)

class ManageCardsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManageCardsScreen(onBack = { finish() })
        }
    }
}

@Composable
fun ManageCardsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardManager = remember { CardManager(context) }
    var entries by remember { mutableStateOf<List<CardBrowserEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var cardToDelete by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    fun loadCards() {
        scope.launch {
            loading = true
            val list = withContext(Dispatchers.IO) {
                val out = mutableListOf<CardBrowserEntry>()
                try {
                    for (name in cardManager.listCards().filter { it.isNotBlank() }) {
                        try {
                            val card = cardManager.getCard(name) ?: continue
                            val count = try { card.characterStats.characterEntries.size } catch (e: Exception) { 0 }
                            val isBem = card is BemCard
                            val thumb = try {
                                val sprites = card.spriteData.sprites
                                val base = if (isBem) 54 else 10 // charBaseIndex(0, isBem)
                                sprites.getOrNull(base + 1)?.let { SpriteBitmapHandler.getBitmap(it) }
                            } catch (e: Exception) { null }
                            out.add(CardBrowserEntry(name, count, isBem, thumb))
                        } catch (e: Exception) {
                            Timber.w(e, "Skipping unreadable card $name")
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Card list failed")
                }
                out.sortedBy { it.name.lowercase() }
            }
            entries = list
            loading = false
        }
    }

    // Reload whenever we come back (e.g. after importing a DIM).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(refreshTick) { loadCards() }

    val shown = entries.filter { it.name.contains(query, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DIM Card Library") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color(0, 50, 100),
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0, 20, 40))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (loading) "Loading..." else "${entries.size} card${if (entries.size == 1) "" else "s"} installed",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Button(
                    onClick = { context.startActivity(Intent(context, StandaloneImportCardActivity::class.java)) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0, 150, 80))
                ) {
                    Text("Import DIM", color = Color.White)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search cards") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedLabelColor = Color.Cyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedBorderColor = Color.Cyan,
                    cursorColor = Color.Cyan
                )
            )
            if (!loading && shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (entries.isEmpty()) "No DIM cards saved on phone yet.\nTap Import DIM to add one."
                        else "No cards match \"$query\".",
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(shown, key = { it.name }) { entry ->
                        CardBrowserRow(entry, onDelete = { cardToDelete = entry.name })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        cardToDelete?.let { name ->
            AlertDialog(
                onDismissRequest = { cardToDelete = null },
                title = { Text("Delete DIM Card?") },
                text = { Text("Are you sure you want to delete '$name'? This will free up space but you won't be able to hatch new Digimon from it until you re-import it.") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (cardManager.deleteCard(name)) {
                                Toast.makeText(context, "Deleted $name", Toast.LENGTH_SHORT).show()
                                loadCards()
                            } else {
                                Toast.makeText(context, "Failed to delete $name", Toast.LENGTH_SHORT).show()
                            }
                            cardToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CardBrowserRow(entry: CardBrowserEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0, 80, 150),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                if (entry.thumb != null) {
                    Image(entry.thumb.asImageBitmap(), entry.name, modifier = Modifier.size(44.dp))
                } else {
                    Text("?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(entry.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${entry.charCount} character${if (entry.charCount == 1) "" else "s"} \u00b7 ${if (entry.isBem) "BEM" else "DIM"}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(255, 130, 130))
            }
        }
    }
}
