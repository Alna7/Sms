package com.example.smsapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue

class MainActivity : ComponentActivity() {
    private val SMS_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmsApp()
        }

        // درخواست مجوز خواندن پیامک
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), SMS_PERMISSION_CODE)
        }
    }
}

@Composable
fun SmsApp() {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    var isStarted by remember { mutableStateOf(false) }

    // BroadcastReceiver برای دریافت پیامک
    DisposableEffect(isStarted) {
        if (isStarted) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                        val bundle = intent.extras
                        if (bundle != null) {
                            val pdus = bundle.get("pdus") as Array<*>
                            for (pdu in pdus) {
                                val sms = android.telephony.SmsMessage.createFromPdu(pdu as ByteArray, bundle.getString("format"))
                                if (sms.originatingAddress?.contains(phoneNumber.text) == true) {
                                    val message = sms.messageBody
                                    val url = "https://tetra.alnafun.ir/api/sms?smsbank=$message"
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(browserIntent)
                                }
                            }
                        }
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
            onDispose {
                context.unregisterReceiver(receiver)
            }
        } else {
            onDispose { }
        }
    }

    // UI با Jetpack Compose
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("شماره تلفن") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { isStarted = !isStarted },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isStarted) "توقف" else "شروع")
        }
    }
}
