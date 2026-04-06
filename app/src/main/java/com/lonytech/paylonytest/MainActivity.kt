package com.lonytech.paylonytest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.a5starcompany.topwisemp35p.emvreader.emv.CardReadState
import com.lonytech.Terminal
import com.lonytech.paylonytest.ui.theme.PaylonytestTheme
import com.lonytech.Paylony
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var terminal: Terminal? = null

    private var pinEntryLauncher: ActivityResultLauncher<Intent>? = null

    private val text = mutableStateOf("")
    private val showDialog = mutableStateOf(false)
    private val title = mutableStateOf("Loading...")
    private val message = mutableStateOf("Please wait...")

    private var printMap: Map<String, String?> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val printdata = applicationContext.assets.open("printdata.json").bufferedReader().use { it.readText() }
            val jsonBack = JSONObject(printdata)
            printMap = jsonBack.keys().asSequence()
                .associateWith { key -> jsonBack.optString(key, "") }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading printdata.json", e)
        }

        terminal = Terminal(
            feeBearer = "agent",
            feeType = "percent",
            flatFee = "100",
            feeCent = "0.55",
            feeCap = "100",
            terminalId = "2LON00A1",
            token = "1016|FM81d1FIwwzMyp3DZDHCeSQ1Bzk8mmvErdYMxiHQ30ebe97e",
            rrn = getReference()
        )

        pinEntryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val pin = result.data?.getStringExtra("PIN_RESULT")
                if (pin != null) {
                    runOnUiThread {
                        showDialog.value = true
                        title.value = "Processing"
                        message.value = "Sending PIN..."
                    }
                    paylonysdk?.enterPin(pin, "SAVINGS")
                }
                Log.d("MainActivity", "Received PIN: $pin")
            }
        }

        val print: Print? = paylonysdk?.let { Print(it, this) }

        enableEdgeToEdge()
        setContent {
            PaylonytestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(16.dp)
                            .focusable(true)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    val key = keyEvent.key
                                    if (key == Key.Backspace && text.value.isNotEmpty()) {
                                        text.value = text.value.dropLast(1)
                                    } else if (key == Key.Enter && text.value.isNotEmpty()) {
                                        startCardRead()
                                    } else if (text.value.length < 10 && key.nativeKeyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
                                        text.value += (key.nativeKeyCode - KeyEvent.KEYCODE_0).toString()
                                    }
                                }
                                true
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        paylonysdk?.let {
                            Greeting(name = it.serialNumber)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Amount: NGN${text.value}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { startCardRead() },
                                enabled = text.value.isNotEmpty() && text.value != "0"
                            ) {
                                Text("Make Payment")
                            }

                            Button(onClick = { print?.startPrint(printMap) }) {
                                Text("Print")
                            }
                        }
                    }
                }
                ProgressDialog(showDialog = showDialog.value) { showDialog.value = false }
            }
        }
    }

    private fun startCardRead() {
        runOnUiThread {
            showDialog.value = true
            title.value = "Loading..."
            message.value = "Please insert or tap your card"
        }
        paylonysdk?.readCard(text.value, "cashout", "")
    }

    private val paylonysdk by lazy {
        terminal?.let { t ->
            Paylony(applicationContext, terminal = t) { res ->
                runOnUiThread {
                    if (res == null) return@runOnUiThread
                    Log.d("MainActivity", "State: ${res.state}, Message: ${res.message}")

                    when (res.state) {
                        CardReadState.Loading -> {
                            showDialog.value = true
                            title.value = "Processing"
                            message.value = res.message ?: "Please wait..."
                        }
                        CardReadState.CardDetected -> {
                            showDialog.value = false
                            val intent = Intent(this@MainActivity, Cardpin::class.java)
                            intent.putExtra("amount", text.value)
                            intent.putExtra("pan", res.message)
                            pinEntryLauncher?.launch(intent)
                        }
                        CardReadState.CardData -> {
                            showDialog.value = true
                            title.value = "Card Result"
                            message.value = res.message ?: "Card data captured"
                        }
                        CardReadState.CallBackTransResult -> {
                            showDialog.value = true
                            title.value = if (res.status == true) "Success" else "Failed"
                            message.value = res.message ?: ""
                        }
                        CardReadState.CallBackError -> {
                            showDialog.value = true
                            title.value = "Error"
                            message.value = res.message ?: "An error occurred"
                        }
                        CardReadState.CallBackCanceled -> {
                            showDialog.value = true
                            title.value = "Canceled"
                            message.value = "Transaction was canceled"
                        }
                        else -> {
                            // Other states
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ProgressDialog(showDialog: Boolean, onDismiss: () -> Unit) {
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { onDismiss() },
                modifier = Modifier.padding(16.dp),
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                title = {
                    Text(text = title.value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (title.value == "Loading..." || title.value == "Processing") {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Text(text = message.value, fontSize = 16.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { onDismiss() }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    private fun getReference(): String {
        return SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(Date())
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PaylonytestTheme {
        Greeting("Android")
    }
}
