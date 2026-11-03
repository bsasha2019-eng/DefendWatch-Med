package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AiGuideScreen()
            }
        }
    }
}

@Composable
fun AiGuideScreen() {
    var inputText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("ШІ-Довідник DefendWatch", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Опишіть ситуацію (напр. сильна кровотеча)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (inputText.isBlank()) {
                    resultText = "Будь ласка, введіть опис."
                    return@Button
                }
                isLoading = true
                isError = false
                resultText = ""

                coroutineScope.launch {
                    try {
                        val response = RetrofitClient.apiService.getAiAdvice(MedicalRequest(inputText))
                        resultText = "Категорія: ${response.category}\n\nПорада:\n${response.advice}"
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isError = true
                        val errorDetail = e.localizedMessage ?: e.message ?: e.toString()
                        resultText = "Помилка (${e.javaClass.simpleName}): $errorDetail"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Завантаження..." else "Отримати пораду")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (resultText.isNotEmpty()) {
            Text(
                text = resultText,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
