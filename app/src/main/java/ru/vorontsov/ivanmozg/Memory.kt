package ru.vorontsov.ivanmozg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun SrsScreen(store: Store) {
    var tick by remember { mutableIntStateOf(0) }
    val due = remember(tick) { store.due() }
    val all = remember(tick) { store.cards() }
    var show by remember(tick) { mutableStateOf(false) }
    var front by remember { mutableStateOf("") }; var back by remember { mutableStateOf("") }
    Page("Карточки (FSRS)") {
        val c = due.firstOrNull()
        if (c == null) Note(if (all.isEmpty()) "Карточек пока нет. Добавь ниже: вопрос — ответ. Сначала вспоминай сам, потом смотри ответ — это и есть «самопроверка»."
            else "На сегодня всё повторено 👍 Следующее повторение: ${next(all)}")
        else {
            Text("Осталось: ${due.size}")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(c.front, fontSize = 22.sp, textAlign = TextAlign.Center)
                    if (show) { HorizontalDivider(Modifier.padding(vertical = 12.dp)); Text(c.back, fontSize = 20.sp, textAlign = TextAlign.Center) }
                }
            }
            if (!show) Big("Вспомнил(а)? Показать ответ") { show = true }
            else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Снова", "Трудно", "Хорошо", "Легко").forEachIndexed { i, t ->
                    Button({ store.review(c.id, i + 1); tick++ }, Modifier.weight(1f).heightIn(min = 52.dp), contentPadding = PaddingValues(2.dp)) { Text(t) }
                }
            }
        }
        HorizontalDivider()
        Text("Новая карточка", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(front, { front = it }, Modifier.fillMaxWidth(), label = { Text("Вопрос") })
        OutlinedTextField(back, { back = it }, Modifier.fillMaxWidth(), label = { Text("Ответ") })
        Big("Добавить", onClick = { store.addCard(front.trim(), back.trim()); front = ""; back = ""; tick++ }, enabled = front.isNotBlank() && back.isNotBlank())
        if (all.isNotEmpty()) {
            Text("Все карточки (${all.size})", style = MaterialTheme.typography.titleMedium)
            all.forEach { f -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${f.front} — ${f.back}", Modifier.weight(1f)); TextButton({ store.deleteCard(f.id); tick++ }) { Text("Удалить") } } }
        }
    }
}

private fun next(all: List<Flash>): String {
    val d = all.minOf { it.card.due }; val h = Duration.between(Instant.now(), d).toHours()
    return if (h < 1) "меньше чем через час" else if (h < 48) "через $h ч" else "через ${h / 24} дн."
}

@Composable
fun LociScreen(store: Store) {
    var stage by remember { mutableIntStateOf(0) } // 0 места, 1 запоминание, 2 вспоминание, 3 итог
    var placesText by remember { mutableStateOf(store.lociPlaces.joinToString("\n")) }
    val places = placesText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.take(15)
    var words by remember { mutableStateOf(listOf<String>()) }
    var idx by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateListOf<String>() }
    Page("Метод локусов") {
        when (stage) {
            0 -> {
                Note("Метод «дворца памяти»: идёшь мысленно по знакомому маршруту и в каждое место «кладёшь» яркий, странный, " +
                    "движущийся образ слова. Проверенная мнемотехника: метаанализ 13 РКИ, g≈0,65 (Twomey & Kroneisen, 2021); 6 недель тренировки — запоминание списка слов более чем вдвое (Dresler et al., 2017).")
                OutlinedTextField(placesText, { placesText = it }, Modifier.fillMaxWidth(), label = { Text("Твой маршрут: одно место на строку") }, minLines = 6)
                Big("Начать (${places.size} слов)", onClick = {
                    store.lociPlaces = places; words = Loci.pick(places.size); idx = 0; answers.clear(); answers.addAll(List(places.size) { "" }); stage = 1
                }, enabled = places.size >= 3)
            }
            1 -> {
                Text("${idx + 1} / ${words.size}")
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(places[idx], fontSize = 18.sp); Text(words[idx].uppercase(), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                } }
                Note("Представь: ${words[idx]} в месте «${places[idx]}» — огромный, шумный, необычный. 5–10 секунд.")
                Big(if (idx < words.size - 1) "Дальше" else "Вспоминать") { if (idx < words.size - 1) idx++ else stage = 2 }
            }
            2 -> {
                Text("Пройди маршрут и напиши слова")
                places.forEachIndexed { i, pl -> OutlinedTextField(answers[i], { answers[i] = it }, Modifier.fillMaxWidth(), label = { Text(pl) }, singleLine = true) }
                Big("Проверить") { store.add("loci", Loci.score(words, answers).toDouble() / words.size * 100); stage = 3 }
            }
            else -> {
                val s = Loci.score(words, answers)
                Text("Верно: $s из ${words.size}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                words.forEachIndexed { i, w -> Text("${places[i]}: $w ${if (Loci.score(listOf(w), listOf(answers[i])) == 1) "✅" else "❌ (${answers[i]})"}") }
                Note("Через час и завтра пройди маршрут ещё раз мысленно — отсроченное вспоминание закрепляет сильнее.")
                Big("Ещё раз") { stage = 0 }
            }
        }
    }
}

@Composable
fun SpanScreen(store: Store) {
    var len by remember { mutableIntStateOf(store.spanLevel) }
    var seq by remember { mutableStateOf(listOf<Int>()) }
    var shown by remember { mutableIntStateOf(-1) }
    var stage by remember { mutableIntStateOf(0) } // 0 старт, 1 показ, 2 ввод, 3 результат
    var input by remember { mutableStateOf("") }
    var last by remember { mutableStateOf<Boolean?>(null) }
    var trials by remember { mutableIntStateOf(0) }; var best by remember { mutableIntStateOf(0) }
    LaunchedEffect(stage, seq) {
        if (stage == 1) { for (i in seq.indices) { shown = i; delay(800); shown = -1; delay(200) }; stage = 2 }
    }
    Page("Объём памяти: цифры") {
        Text("Длина: $len   Попыток: $trials/8   Лучшая: $best")
        when (stage) {
            0, 3 -> {
                if (last != null) Text(if (last == true) "Верно ✅" else "Ошибка ❌ (было ${seq.joinToString("")})", fontSize = 20.sp)
                if (trials >= 8) {
                    Text("Сеанс окончен. Объём: $best", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Big("Новый сеанс") { trials = 0; best = 0; last = null }
                } else Big(if (trials == 0) "Начать" else "Дальше") { seq = Span.digits(len); input = ""; stage = 1 }
                Note("Цифры показываются по одной (1 в секунду). Затем введи их в том же порядке. Совет: группируй по 3–4 (чанкинг). " +
                    "Тренировка повышает именно этот навык; на интеллект в целом переносится слабо (Melby-Lervåg et al., 2016).")
            }
            1 -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(if (shown >= 0) seq[shown].toString() else "", fontSize = 96.sp, fontWeight = FontWeight.Bold) }
            2 -> {
                OutlinedTextField(input, { s -> input = s.filter { it.isDigit() } }, Modifier.fillMaxWidth(), label = { Text("Цифры по порядку") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                Big("Готово") {
                    val ok = input == seq.joinToString(""); last = ok; trials++
                    if (ok) best = maxOf(best, len)
                    len = Span.next(len, ok); store.spanLevel = len; stage = 3
                    if (trials >= 8) store.add("span", best.toDouble())
                }
            }
        }
    }
}
