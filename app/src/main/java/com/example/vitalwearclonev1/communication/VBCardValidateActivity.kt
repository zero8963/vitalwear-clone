package com.example.vitalwearclonev1.communication

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

/**
 * Verifies a DIM/BEM card ID against a REAL Vital Bracelet (BE family,
 * including the Vital Hero) using the bracelet's Connect -> App Loglink mode.
 *
 * In that mode the bracelet presents itself as a Mifare Ultralight tag, so the
 * phone acts as a plain NFC reader (see [VBBraceletTag]). Two taps:
 *   1. Tap the bracelet (in App Loglink) -> phone writes the CHECK_DIM request.
 *      The bracelet shows the insert-card icon.
 *   2. Insert the physical card into the bracelet, wait for it to load, tap
 *      again -> phone reads back the bracelet's answer and validates the ID.
 *
 * Ported from 457R0/VitalWear's ValidateCardActivity (release tag "companion"),
 * proven against real hardware. Launch with [EXTRA_DIM_ID] (Int); on success
 * returns [RESULT_OK] with [RESULT_VALIDATED_DIM_ID].
 *
 * NOTE: the bracelet needs an active character (from any card) on it during
 * validation, per the original project's README.
 */
class VBCardValidateActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    companion object {
        const val EXTRA_DIM_ID = "extra_dim_id"
        const val EXTRA_CARD_NAME = "extra_card_name"
        const val RESULT_VALIDATED_DIM_ID = "validated_dim_id"
    }

    private enum class CardValidationState {
        WaitingForVBConnect,
        ValidateCardOnVB,
        Success,
    }

    private var nfcAdapter: NfcAdapter? = null
    private val validationStateFlow = MutableStateFlow(CardValidationState.WaitingForVBConnect)
    private var cardIdToValidate: UShort = 0u
    private var cardName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dimId = intent.getIntExtra(EXTRA_DIM_ID, -1)
        if (dimId < 0) {
            Timber.e("VBCardValidateActivity launched without a card ID")
            finish()
            return
        }
        cardIdToValidate = dimId.toUShort()
        cardName = intent.getStringExtra(EXTRA_CARD_NAME).orEmpty()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC on this device!", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent { ValidateScreen() }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) {
            Toast.makeText(this, "NFC must be enabled", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        } else {
            val options = Bundle()
            // Work around for some broken NFC firmware that polls the tag too fast.
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
            adapter.enableReaderMode(
                this, this,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
            )
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        try {
            when (validationStateFlow.value) {
                CardValidationState.WaitingForVBConnect -> {
                    val nfc = MifareUltralight.get(tag)
                    if (nfc == null) {
                        Timber.w("Discovered tag is not MifareUltralight; ignoring")
                        runOnUiThread {
                            Toast.makeText(this, "That's not the bracelet — tap your Hero in App Loglink mode.", Toast.LENGTH_LONG).show()
                        }
                        return
                    }
                    nfc.connect()
                    nfc.use {
                        VBBraceletTag(nfc).writeCardCheck(nfc, cardIdToValidate)
                    }
                    Timber.d("CHECK_DIM request written for DIM $cardIdToValidate")
                    validationStateFlow.value = CardValidationState.ValidateCardOnVB
                }
                CardValidationState.ValidateCardOnVB -> {
                    val nfc = MifareUltralight.get(tag) ?: return
                    nfc.connect()
                    val ok = nfc.use {
                        VBBraceletTag(nfc).wasCardIdValidated(cardIdToValidate)
                    }
                    if (ok) {
                        validationStateFlow.value = CardValidationState.Success
                    } else {
                        Timber.w("Bracelet answer did not validate DIM $cardIdToValidate")
                        runOnUiThread {
                            Toast.makeText(this, "Not validated — is the right card inserted and loaded?", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                CardValidationState.Success -> {
                    Timber.w("Tag discovered after success; ignoring")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "NFC error during bracelet validation")
            runOnUiThread {
                Toast.makeText(this, "NFC error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Composable
    private fun ValidateScreen() {
        val state by validationStateFlow.collectAsState()
        val title = if (cardName.isNotBlank()) "Verify \"$cardName\"" else "Verify card $cardIdToValidate"
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0, 20, 40)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                when (state) {
                    CardValidationState.WaitingForVBConnect -> {
                        CircularProgressIndicator(color = Color.Cyan)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "On your bracelet go to:\nConnect → App Loglink\n\nThen tap the bracelet to the phone.",
                            color = Color.LightGray, fontSize = 15.sp, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your bracelet needs an active character on it for this to work.",
                            color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center
                        )
                    }
                    CardValidationState.ValidateCardOnVB -> {
                        CircularProgressIndicator(color = Color.Cyan)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Request sent!\n\nInsert the physical card into the bracelet, wait for it to load, then tap the bracelet to the phone again.",
                            color = Color.LightGray, fontSize = 15.sp, textAlign = TextAlign.Center
                        )
                    }
                    CardValidationState.Success -> {
                        LaunchedEffect(Unit) {
                            Handler(Looper.getMainLooper()!!).postDelayed({
                                val result = Intent().putExtra(RESULT_VALIDATED_DIM_ID, cardIdToValidate.toInt())
                                setResult(RESULT_OK, result)
                                finish()
                            }, 1200)
                        }
                        Text("✓", color = Color.Green, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Card validated with your bracelet!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { finish() }) {
                    Text("Cancel")
                }
            }
        }
    }
}
