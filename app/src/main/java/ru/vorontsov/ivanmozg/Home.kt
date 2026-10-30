package ru.vorontsov.ivanmozg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

data class PlanItem(val key: String, val route: String, val title: String, val hint: String)

@Composable
fun HomeScreen(store: Store, go: (String) -> Unit, theme: Int, setTheme: (Int) -> Unit) {
    val odd = LocalDate.now().dayOfYear % 2 == 1
    val due = remember { store.due().size }
    val plan = listOf(
        PlanItem("move", "move", "Движение 10 минут", "Самое надёжное по науке: быстрая ходьба, лестница, прыжки"),
        PlanItem("srs", "srs", "Карточки (FSRS)", "К повторению: $due"),
        if (odd) PlanItem("loci", "loci", "Метод локусов", "Запомни 10 слов по маршруту")
        else PlanItem("span", "span", "Объём памяти (цифры)", "Адаптивная длина"),
        PlanItem("nback", "nback", "Двойной n-back", "Уровень: ${store.nbackLevel}"),
        PlanItem("rt_choice", "rt_choice", "Реакция с выбором", "20 попыток, точность до кадра"),
        if (odd) PlanItem("stroop", "stroop", "Струп", "Торможение лишнего") else PlanItem("switch", "switch", "Переключение задач", "Гибкость внимания"),
        PlanItem("sleep", "sleep", "Сон сегодня", "7–9 ч: память закрепляется во сне"),
    )
    Page("ИванМозг") {
        Text("План на сегодня (~25 мин)", style = MaterialTheme.typography.titleMedium)
        plan.forEach { item ->
            val ok = store.doneToday(item.key)
            Card(Modifier.fillMaxWidth().clickable { go(item.route) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ok) "✅" else "⬜", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column { Text(item.title, style = MaterialTheme.typography.titleMedium); Text(item.hint, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        Text("Все упражнения", style = MaterialTheme.typography.titleMedium)
        val all = listOf("srs" to "Карточки", "loci" to "Локусы", "span" to "Цифры", "nback" to "N-back",
            "rt_simple" to "Простая реакция", "rt_choice" to "Реакция с выбором", "stroop" to "Струп", "switch" to "Переключение",
            "progress" to "Прогресс", "science" to "Наука")
        all.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (r, t) -> OutlinedButton({ go(r) }, Modifier.weight(1f).heightIn(min = 48.dp)) { Text(t) } }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Тема: ")
            listOf("Системная", "Светлая", "Тёмная").forEachIndexed { i, t ->
                FilterChip(theme == i, { setTheme(i) }, { Text(t) }, Modifier.padding(end = 6.dp))
            }
        }
        Note("Честно: «экстренного» ускорения мозга не существует. Упражнения на экране в основном улучшают сами эти упражнения. " +
            "Лучше всего доказаны: интервальные повторения, самопроверка, физическая нагрузка и сон. Подробности — в разделе «Наука».")
    }
}

@Composable
fun MoveScreen(done: () -> Unit) = Page("Движение 10 минут") {
    Note("Варианты (выбери один):\n• Быстрая ходьба 10 мин так, чтобы трудно было петь.\n• Лестница вверх-вниз 5–10 мин.\n" +
        "• Интервалы: 30 с приседаний/прыжков + 30 с отдыха × 10.\n\nОднократная нагрузка 10–20 мин даёт небольшое кратковременное " +
        "улучшение внимания и исполнительных функций (метаанализ Chang et al., 2012). Для регулярных тренировок у здоровых " +
        "людей эффект на когнитивные функции небольшой и после поправки на публикационное смещение почти исчезает (Ciria et al., 2023), " +
        "но польза для сердца, сна и настроения бесспорна — а они влияют на мышление.")
    Note("При болезнях сердца, давлении, травмах — сначала с врачом.")
    Big("Сделал ✅", onClick = done)
}

@Composable
fun SleepScreen(done: () -> Unit) = Page("Сон") {
    Note("Почему важно: во сне память консолидируется; недосып на 1 ночь заметно ухудшает внимание и время реакции — " +
        "этот эффект заметно больше, чем у любой «тренировки мозга» (Lim & Dinges, 2010).\n\nСоветы (CBT-I / гигиена сна):\n• 7–9 ч, подъём в одно и то же время.\n" +
        "• Кофеин — не позже чем за 8 ч до сна.\n• Утром — дневной свет 10–20 мин.\n• Последний час — без яркого экрана и работы.\n" +
        "• Повтори карточки вечером — сон закрепит.\n• Не можешь уснуть 20 мин — встань и вернись, когда захочется спать.")
    Big("Понял, сегодня ложусь вовремя ✅", onClick = done)
}
