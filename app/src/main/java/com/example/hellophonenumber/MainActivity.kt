package com.example.hellophonenumber

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hellophonenumber.ui.theme.HelloPhoneNumberTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloPhoneNumberTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegistrationScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    modifier : Modifier = Modifier,
) {
    var isValidPhone by remember { mutableStateOf(false) }
    var e164Format by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Enter your phone number",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        PhoneNumberInput(
            onPhoneNumberChange = { number, valid, e164 ->
                isValidPhone = valid
                e164Format = e164
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Use e164Format for API calls
                Log.d("Phone", "Submitting: $e164Format")
            },
            enabled = isValidPhone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        // Display formatted number
        if (isValidPhone) {
            Text(
                text = "International format: $e164Format",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
