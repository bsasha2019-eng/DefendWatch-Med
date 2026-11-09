package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var historyList by remember { mutableStateOf(emptyList<HistoryItem>()) }
    val coroutineScope = rememberCoroutineScope()

    // Завантажуємо історію при першому відкритті екрана
    LaunchedEffect(Unit) {
        try {
            historyList = RetrofitClient.apiService.getHistory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
                        
                        // Оновлюємо історію після успішного отримання відповіді
                        try {
                            historyList = RetrofitClient.apiService.getHistory()
                        } catch (he: Exception) {
                            he.printStackTrace()
                        }
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Заголовок історії та кнопка очищення
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Історія порад (${historyList.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (historyList.isNotEmpty()) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                RetrofitClient.apiService.clearHistory()
                                historyList = emptyList()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                ) {
                    Text("Очистити", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Список карток історії
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (historyList.isEmpty()) {
                item {
                    Text(
                        text = "Історія поки що порожня.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(historyList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = item.timestamp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Запит: ${item.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.advice,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
