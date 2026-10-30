package ru.vorontsov.ivanmozg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private data class Metric(val key: String, val title: String, val unit: String, val lowerBetter: Boolean)
private val metrics = listOf(
    Metric("rt_simple", "Простая реакция (медиана)", "мс", true),
    Metric("rt_choice", "Реакция с выбором (медиана)", "мс", true),
    Metric("stroop", "Эффект Струпа", "мс", true),
    Metric("switch", "Цена переключения", "мс", true),
    Metric("nback", "Уровень n-back", "", false),
    Metric("span", "Объём памяти на цифры", "", false),
    Metric("loci", "Локусы, % верно", "%", false),
    Metric("srs", "Карточки: доля вспомненных", "", false),
)

@Composable
fun ProgressScreen(store: Store) {
    val res = remember { store.results() }
    Page("Прогресс") {
        val days = res.map { Instant.ofEpochMilli(it.ts).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
        var streak = 0; var d = LocalDate.now(); while (d in days) { streak++; d = d.minusDays(1) }
        Text("Дней подряд: $streak   Всего дней: ${days.size}", fontWeight = FontWeight.Bold)
        metrics.forEach { m ->
            val v = if (m.key == "srs") res.filter { it.type == "srs" }.chunked(10).map { c -> c.sumOf { it.value } / c.size * 100 }
                    else res.filter { it.type == m.key }.map { it.value }
            if (v.isEmpty()) return@forEach
            val last = v.takeLast(30)
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(m.title, style = MaterialTheme.typography.titleMedium)
                Text("Последнее: ${last.last().toInt()} ${m.unit}   Лучшее: ${(if (m.lowerBetter) last.min() else last.max()).toInt()} ${m.unit}   Сеансов: ${v.size}" +
                    (if (m.lowerBetter) "  (меньше — лучше)" else ""), style = MaterialTheme.typography.bodySmall)
                Chart(last)
            } }
        }
        if (res.isEmpty()) Note("Пока нет данных — пройди упражнения из плана.")
        Note("Не пугайся колебаний: реакция зависит от сна, кофеина и времени суток. Смотри на тренд за 2–3 недели.")
    }
}

@Composable
private fun Chart(v: List<Double>) {
    val c = MaterialTheme.colorScheme.primary; val g = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp)) {
        val lo = v.min(); val hi = v.max(); val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
        drawLine(g, Offset(0f, size.height), Offset(size.width, size.height))
        val pts = v.mapIndexed { i, x ->
            Offset(if (v.size == 1) size.width / 2 else i * size.width / (v.size - 1), (size.height - ((x - lo) / span * (size.height - 8)) - 4).toFloat()) }
        for (i in 1 until pts.size) drawLine(c, pts[i - 1], pts[i], strokeWidth = 5f)
        pts.forEach { drawCircle(c, 7f, it) }
    }
}
