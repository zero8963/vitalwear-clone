package com.example.vitalwearclonev1.card

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.TextButton
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.cfogrady.vb.dim.adventure.AdventureLevels
import com.github.cfogrady.vb.dim.card.Card
import com.github.cfogrady.vb.dim.card.DimReader
import com.github.cfogrady.vb.dim.card.DimWriter
import com.github.cfogrady.vb.dim.character.CharacterStats
import com.github.cfogrady.vb.dim.fusion.AttributeFusions
import com.github.cfogrady.vb.dim.fusion.SpecificFusions
import com.github.cfogrady.vb.dim.header.DimHeader
import com.github.cfogrady.vb.dim.transformation.TransformationRequirements
import com.example.vitalwearclonev1.lab.DigimonLabActivity
import com.example.vitalwearclonev1.common.communication.ChannelTypes
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.Charset
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp
import com.example.vitalwearclonev1.common.SpriteBitmapHandler
import com.github.cfogrady.vb.dim.card.BemCard

/**
 * Standalone activity to import DIM cards directly from storage to the Watch
 * without relying on the physical hardware or internal app databases.
 */
class StandaloneImportCardActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    enum class ImportState {
        PickFile,
        NameOrUnique,
        LoadFile,
        ImportCard,
        Success
    }

    private lateinit var filePickLauncher: ActivityResultLauncher<Array<String>>
    private val importState = MutableStateFlow(ImportState.PickFile)
    private var uri = MutableStateFlow<Uri?>(null)
    private var cardName = MutableStateFlow("")
    private var uniqueSprites = MutableStateFlow(false)
    private var nfcAdapter: NfcAdapter? = null
    private var isWatchDetected = MutableStateFlow(false)
    private var card: Card<out DimHeader, out CharacterStats<out CharacterStats.CharacterStatsEntry>, out TransformationRequirements<out TransformationRequirements.TransformationRequirementsEntry>, out AdventureLevels<out AdventureLevels.AdventureLevel>, out AttributeFusions, out SpecificFusions<out SpecificFusions.SpecificFusionEntry>>? = null
    private var previewSprite = MutableStateFlow<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filePickLauncher = buildFilePickLauncher()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            val state by importState.collectAsState()
            when(state) {
                ImportState.PickFile -> WelcomeScreen()
                ImportState.NameOrUnique -> NameOrUnique()
                ImportState.LoadFile -> {
                    Box(Modifier.fillMaxSize().background(Color(0, 80, 150)), contentAlignment = Alignment.Center) {
                        Text(text = "Loading Card Image...", color = Color.White)
                    }
                    LaunchedEffect(Unit) {
                        loadCard()
                    }
                }
                ImportState.ImportCard -> {
                    Box(Modifier.fillMaxSize().background(Color(0, 80, 150)), contentAlignment = Alignment.Center) {
                        Text(text = "Transferring to Watch...", color = Color.White)
                    }
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            importCard()
                            importState.value = ImportState.Success
                        }
                    }
                }
                ImportState.Success -> {
                    Box(Modifier.fillMaxSize().background(Color(0, 80, 150)), contentAlignment = Alignment.Center) {
                        Text(text = "Transfer Successful!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    LaunchedEffect(Unit) {
                        Handler(Looper.getMainLooper()!!).postDelayed({
                            finish()
                        }, 1000)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val options = Bundle()
        // NFC_READER_FLAGS: FLAG_READER_NFC_A | FLAG_READER_NFC_B | FLAG_READER_SKIP_NDEF_CHECK | FLAG_READER_NO_PLATFORM_SOUNDS
        nfcAdapter?.enableReaderMode(this, this, 
            NfcAdapter.FLAG_READER_NFC_A or 
            NfcAdapter.FLAG_READER_NFC_B or 
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or 
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, 
            options)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        try {
            Timber.d("NFC Tag Discovered via ReaderMode")
            isWatchDetected.value = true
            
            // Send Battle message to watch
            val messageClient = Wearable.getMessageClient(this)
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/BATTLE", null)
                }
            }

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Watch detected! Battle command sent.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling NFC tag")
        }
    }

    @Composable
    private fun WelcomeScreen() {
        val selectedUri by uri.collectAsState()
        val name by cardName.collectAsState()
        val watchDetected by isWatchDetected.collectAsState()
        val preview by previewSprite.collectAsState()
        
        Box(Modifier.fillMaxSize().background(Color(0, 80, 150))) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "VitalWear Clone", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = "DIM Transfer Utility", fontSize = 16.sp, color = Color.LightGray, modifier = Modifier.padding(bottom = 20.dp))
                
                if (selectedUri == null) {
                    Button(onClick = { filePickLauncher.launch(arrayOf("*/*")) }) {
                        Text(text = "Select DIM File")
                    }
                } else {
                    Box(modifier = Modifier.size(100.dp).background(Color.Black.copy(alpha = 0.2f)).padding(10.dp), contentAlignment = Alignment.Center) {
                        preview?.let {
                            Image(it.asImageBitmap(), "Preview", modifier = Modifier.fillMaxSize())
                        } ?: Text("Loading...", color = Color.Gray, fontSize = 12.sp)
                    }

                    Text(text = "Selected: $name", color = Color.Green, modifier = Modifier.padding(vertical = 10.dp))
                    Button(onClick = { importState.value = ImportState.NameOrUnique }) {
                        Text(text = "Sync & Transfer")
                    }
                    Button(modifier = Modifier.padding(top = 10.dp), onClick = { filePickLauncher.launch(arrayOf("*/*")) }) {
                        Text(text = "Change File", fontSize = 12.sp)
                    }
                }
                
                Text(
                    text = if (watchDetected) "Watch Detected!" else "Waiting for Watch (NFC)...", 
                    color = if (watchDetected) Color.Green else Color.White.copy(alpha = 0.7f), 
                    modifier = Modifier.padding(top = 40.dp),
                    fontSize = 3.em,
                    fontWeight = if (watchDetected) FontWeight.Bold else FontWeight.Normal
                )

                Button(modifier = Modifier.padding(top = 20.dp), onClick = {
                    startActivity(Intent(this@StandaloneImportCardActivity, DigimonLabActivity::class.java))
                }) {
                    Text(text = "View Digimon Lab")
                }
            }
        }
    }

    @Composable
    private fun NameOrUnique() {
        val name by cardName.collectAsState()
        val unique by uniqueSprites.collectAsState()
        Box(Modifier.background(Color(0, 80, 150))) { // Blue background for standalone mode
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Standalone DIM Transfer", fontSize = 6.em, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Button(modifier = Modifier.padding(10.dp), onClick = { importState.value = ImportState.PickFile }) {
                        Text(text = "Pick Again")
                    }
                    Text(text = name, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Name: ", modifier = Modifier.padding(10.dp), fontSize = 5.em, fontWeight = FontWeight.Bold, color = Color.White)
                    TextField(value = name, onValueChange = {
                        cardName.value = it
                    })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Unique Sprites:", fontSize = 5.em, fontWeight = FontWeight.Bold, color = Color.White)
                    Checkbox(checked = unique, onCheckedChange = {uniqueSprites.value = it})
                }
                
                Button(modifier = Modifier.padding(top = 20.dp), onClick = {
                    importState.value = ImportState.LoadFile
                }) {
                    Text(text = "Transfer to Watch")
                }

                TextButton(modifier = Modifier.padding(top = 10.dp), onClick = {
                    importState.value = ImportState.LoadFile
                }) {
                    Text(text = "Save to Phone Only", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }

    private fun buildFilePickLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if(it != null) {
                uri.value = it
                val path = it.path ?: ""
                var name = path.substring(path.lastIndexOf("/")+1)
                if(name.contains(".")) {
                    name = name.substring(0, name.lastIndexOf("."))
                }
                cardName.value = name
            }
        }
    }

    private suspend fun loadCard() {
        withContext(Dispatchers.IO) {
            try {
                val selectedUri = uri.value ?: throw Exception("No file selected")
                contentResolver.openInputStream(selectedUri).use {
                    val loadedCard = try {
                        DimReader().readCard(it!!, false)
                    } catch (t: Throwable) {
                        contentResolver.openInputStream(selectedUri).use { is2 ->
                            DimReader().readCard(is2!!, true)
                        }
                    }
                    card = loadedCard
                    
                    // Generate preview sprite
                    val isBem = loadedCard is BemCard
                    val sprites = loadedCard.spriteData.sprites
                    val baseIdx = if (isBem) 54 else 10 // First character/egg
                    if (baseIdx + 1 < sprites.size) {
                        previewSprite.value = SpriteBitmapHandler.getBitmap(sprites[baseIdx + 1])
                    }
                    
                    importState.value = ImportState.ImportCard
                }
            } catch (t: Throwable) {
                Timber.e(t, "Error reading DIM file")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StandaloneImportCardActivity, "Failed to read DIM file: ${t.message}", Toast.LENGTH_LONG).show()
                    importState.value = ImportState.PickFile
                }
            }
        }
    }

    private suspend fun importCard() {
        val currentCard = card
        if (currentCard == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@StandaloneImportCardActivity, "Card data not loaded!", Toast.LENGTH_LONG).show()
                importState.value = ImportState.PickFile
            }
            return
        }
        try {
            // Save card locally for Adventure Mode
            val cardManager = CardManager(this)
            cardManager.saveCard(cardName.value, currentCard)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@StandaloneImportCardActivity, "DIM Saved Locally for Adventure!", Toast.LENGTH_SHORT).show()
            }

            val channelClient = Wearable.getChannelClient(this)
            val nodes = try {
                Wearable.getNodeClient(this).connectedNodes.await()
            } catch (e: Exception) {
                Timber.e(e, "Error getting connected nodes")
                emptyList()
            }
            
            if (nodes.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StandaloneImportCardActivity, "DIM Saved to Phone. (No watch found)", Toast.LENGTH_LONG).show()
                }
                // Don't return, just skip the node loop
            }

            // Prepare card payload bytes
            val cardWriter = DimWriter()
            val cardPayload = ByteArrayOutputStream().use { outputStream ->
                cardWriter.writeCard(currentCard, outputStream)
                outputStream.toByteArray()
            }
            
            val cardNameBytes = cardName.value.toByteArray(Charset.defaultCharset())
            
            for (node in nodes) {
                Timber.d("Opening channel to node: ${node.displayName} (${node.id})")
                val channel = try {
                    channelClient.openChannel(node.id, ChannelTypes.CARD_DATA).await()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to open channel to ${node.displayName}")
                    continue
                }

                try {
                    channelClient.getOutputStream(channel).await().use { os ->
                        delay(500) // connection warm-up
                        val output = DataOutputStream(os.buffered())
                        // Protocol: Name (null terminated), unique, convertToBem, payloadSize (Int), payload
                        output.write(cardNameBytes)
                        output.writeByte(0) // Null terminator for name
                        output.writeByte(if(uniqueSprites.value) 1 else 0)
                        output.writeByte(if(currentCard is BemCard) 1 else 0)
                        output.writeInt(cardPayload.size)
                        output.write(cardPayload)
                        output.flush()
                        delay(500) // connection cool-down
                        Timber.d("Card data written to ${node.displayName}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error writing data to channel for ${node.displayName}")
                    throw e // Re-throw to be caught by outer try-catch
                } finally {
                    channelClient.close(channel).await()
                }
            }
            
            // Sync to VBHelper
            VBHelperCardSync.syncCard(applicationContext, cardPayload, cardName.value)
            
        } catch (e: Exception) {
            Timber.e(e, "Error transferring to watch")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@StandaloneImportCardActivity, "Transfer failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                importState.value = ImportState.PickFile
            }
        }
    }
}
