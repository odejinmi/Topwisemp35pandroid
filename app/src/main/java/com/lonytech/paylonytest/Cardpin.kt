package com.lonytech.paylonytest

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class Cardpin : ComponentActivity() {

    private var mCardNo: String? = null
    private var mAmount: String? = null
    private val finalPan = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAmount = intent.getStringExtra("amount")
        mCardNo = intent.getStringExtra("pan")
        
        mCardNo?.let { pan ->
            if (pan.length > 9) {
                val stars = "*".repeat(pan.length - 9)
                finalPan.value = pan.take(5) + stars + pan.takeLast(4)
            } else {
                finalPan.value = pan
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            PinEntryScreen()
        }
    }

    @Composable
    fun PinEntryScreen() {
        var pin by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    // Use KeyDown for immediate response
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val key = keyEvent.key
                        val nativeCode = key.nativeKeyCode

                        when {
                            key == Key.Backspace -> {
                                if (pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                }
                                true
                            }
                            key == Key.Enter -> {
                                if (pin.length >= 4) {
                                    submitPin(pin)
                                }
                                true
                            }
                            nativeCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                                if (pin.length < 4) {
                                    pin += (nativeCode - KeyEvent.KEYCODE_0).toString()
                                }
                                true
                            }
                            else -> false
                        }
                    } else {
                        // Consume KeyUp events for digits/backspace/enter to prevent double-processing or default behavior
                        val key = keyEvent.key
                        key == Key.Backspace || key == Key.Enter || key.nativeKeyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9
                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Amount: NGN$mAmount",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Masked Pan: ${finalPan.value}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Enter Your PIN",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PIN Display with masking
            Text(
                text = "*".repeat(pin.length).padEnd(4, '•'),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { submitPin(pin) },
                enabled = pin.length == 4,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Confirm")
            }
        }
    }

    private fun submitPin(pin: String) {
        val intent = Intent().apply {
            putExtra("PIN_RESULT", pin)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}