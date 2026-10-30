package ru.vorontsov.ivanmozg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Page(title: String, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val m = Modifier.fillMaxSize().padding(16.dp)
    Column(if (scroll) m.verticalScroll(rememberScrollState()) else m, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable fun Note(text: String) = Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }

@Composable fun Big(text: String, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true, onClick: () -> Unit) =
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled) { Text(text, fontSize = 17.sp) }

fun ms(x: Double) = "${x.toInt()} мс"
