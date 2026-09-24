package com.example.vitalwearclonev1.card

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import timber.log.Timber
import java.io.File

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
    val cardManager = remember { CardManager(context) }
    var cards by remember { mutableStateOf(cardManager.listCards()) }
    var cardToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Saved DIMs") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0, 20, 40))
        ) {
            if (cards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No DIM cards saved on phone.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(cards) { cardName ->
                        CardItem(
                            name = cardName,
                            onDelete = { cardToDelete = cardName }
                        )
                        Spacer(Modifier.height(8.dp))
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
                                    cards = cardManager.listCards()
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
}

@Composable
fun CardItem(name: String, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0, 80, 150),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                // We could add file size here if we wanted
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
