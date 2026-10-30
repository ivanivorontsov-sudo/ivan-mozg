package ru.vorontsov.ivanmozg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Тайминг: момент показа стимула = время кадра Choreographer (withFrameNanos), в котором стимул впервые отрисован;
 * момент ответа = MotionEvent.eventTime касания (PointerInputChange.uptimeMillis). Оба — CLOCK_MONOTONIC.
 * Погрешность ≈ 1 кадр (6,9 мс при 144 Гц) + задержка сенсора — одинакова для всех попыток, поэтому сравнения корректны.
 */
@Composable
fun TapBox(modifier: Modifier, color: Color, onDown: (Long) -> Unit, content: @Composable BoxScope.() -> Unit = {}) {
    val cb by rememberUpdatedState(onDown)
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(color).pointerInput(Unit) {
        awaitPointerEventScope { while (true) {
            val e = awaitPointerEvent()
            e.changes.firstOrNull { it.pressed && !it.previousPressed }?.let { it.consume(); cb(it.uptimeMillis) }
        } }
    }, contentAlignment = Alignment.Center, content = content)
}

@Composable
fun ReactionScreen(store: Store, choice: Boolean) {
    val total = 20
    var phase by remember { mutableStateOf("idle") } // idle, wait, stim, done
    var target by remember { mutableIntStateOf(0) }
    var onset by remember { mutableDoubleStateOf(0.0) }
    var trial by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }
    val rts = remember { mutableStateListOf<Double>() }
    var errors by remember { mutableIntStateOf(0) }
    LaunchedEffect(phase, trial) {
        if (phase == "wait") {
            delay(Random.nextLong(900, 2600)) // случайный предпериод — нельзя предугадать
            target = if (choice) Random.nextInt(4) else 0
            phase = "stim"
            withFrameNanos { onset = it / 1e6 }
        }
    }
    fun press(which: Int, t: Long) {
        when (phase) {
            "wait" -> { msg = "Фальстарт! Жди сигнал"; trial++ }
            "stim" -> {
                if (onset == 0.0) return
                val rt = t - onset
                if (which == target) { rts += rt; msg = ms(rt) } else { errors++; msg = "Не тот квадрат" }
                onset = 0.0
                if (rts.size + errors >= total) {
                    phase = "done"
                    store.add(if (choice) "rt_choice" else "rt_simple", Stats.median(Stats.cleanRt(rts)))
                } else { phase = "wait"; trial++ }
            }
        }
    }
    val title = if (choice) "Реакция с выбором" else "Простая реакция"
    Page(title, scroll = false) {
        when (phase) {
            "idle" -> {
                Note(if (choice) "Загорится один из 4 квадратов — коснись именно его как можно быстрее. $total попыток."
                     else "Когда поле станет зелёным — коснись как можно быстрее. $total попыток.")
                Note("Честно: простое время реакции почти не тренируется (в основном это скорость нервной системы, сна, кофеина, возраста). " +
                     "Реакция с выбором улучшается с практикой, но в основном для этой задачи. Используй как измеритель состояния: " +
                     "если сегодня на 30+ мс медленнее обычного — вероятно, недосып или усталость.")
                Big("Старт") { rts.clear(); errors = 0; msg = ""; phase = "wait"; trial++ }
            }
            "done" -> {
                val c = Stats.cleanRt(rts)
                Text("Медиана: ${ms(Stats.median(c))}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Среднее: ${ms(Stats.mean(c))}   Разброс (SD): ${ms(Stats.sd(c))}")
                Text("Лучшее: ${ms(c.minOrNull() ?: 0.0)}   Ошибки: $errors")
                Text("Ориентир: простая реакция на касание у взрослых обычно 200–300 мс, с выбором из 4 — 350–500 мс (с учётом задержки экрана).",
                    style = MaterialTheme.typography.bodySmall)
                Big("Ещё раз") { phase = "idle" }
            }
            else -> {
                Text("${rts.size + errors + 1} / $total   $msg", fontSize = 18.sp)
                if (!choice) TapBox(Modifier.fillMaxWidth().weight(1f), if (phase == "stim") Color(0xFF2ECC71) else Color(0xFF555B66), { press(0, it) }) {
                    Text(if (phase == "stim") "ЖМИ!" else "Жди…", color = Color.White, fontSize = 32.sp)
                } else Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (r in 0..1) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (c in 0..1) { val i = r * 2 + c
                            TapBox(Modifier.weight(1f).fillMaxHeight(), if (phase == "stim" && target == i) Color(0xFFF1C40F) else Color(0xFF555B66), { press(i, it) })
                        }
                    }
                }
            }
        }
    }
}

private val inks = listOf(Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFFDD835))

@Composable
fun StroopScreen(store: Store) {
    val total = 24
    var running by remember { mutableStateOf(false) }
    var t by remember { mutableStateOf<StroopTrial?>(null) }
    var onset by remember { mutableDoubleStateOf(0.0) }
    var n by remember { mutableIntStateOf(0) }
    val cong = remember { mutableStateListOf<Double>() }; val inc = remember { mutableStateListOf<Double>() }
    var errors by remember { mutableIntStateOf(0) }
    LaunchedEffect(running, n) {
        if (running && n < total) { t = null; delay(500); t = Stroop.trial(); withFrameNanos { onset = it / 1e6 } }
        if (running && n >= total) { running = false; store.add("stroop", Stats.median(Stats.cleanRt(inc)) - Stats.median(Stats.cleanRt(cong))) }
    }
    Page("Струп", scroll = false) {
        if (!running) {
            if (n >= total) {
                val i = Stats.median(Stats.cleanRt(inc)); val c = Stats.median(Stats.cleanRt(cong))
                Text("Эффект Струпа: ${ms(i - c)}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Совпадающие: ${ms(c)}   Конфликтные: ${ms(i)}   Ошибки: $errors")
                Text("Меньше разница — лучше подавление лишнего.", style = MaterialTheme.typography.bodySmall)
            }
            Note("Нажимай цвет ШРИФТА, а не то, что написано. Это тест торможения (исполнительные функции). Улучшение в основном специфично для задачи.")
            Big("Старт") { cong.clear(); inc.clear(); errors = 0; n = 0; running = true }
        } else {
            Text("${n + 1} / $total")
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                t?.let { Text(Stroop.names[it.word], color = inks[it.ink], fontSize = 48.sp, fontWeight = FontWeight.Black) }
            }
            for (r in 0..1) Row(Modifier.fillMaxWidth().height(90.dp).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0..1) { val k = r * 2 + c
                    TapBox(Modifier.weight(1f).fillMaxHeight(), inks[k], { time ->
                        val cur = t ?: return@TapBox
                        if (onset == 0.0) return@TapBox
                        val rt = time - onset; onset = 0.0
                        if (k == cur.ink) (if (cur.congruent) cong else inc) += rt else errors++
                        n++
                    })
                }
            }
        }
    }
}

@Composable
fun SwitchScreen(store: Store) {
    val total = 30
    var seq by remember { mutableStateOf(listOf<SwitchTrial>()) }
    var running by remember { mutableStateOf(false) }
    var n by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    var onset by remember { mutableDoubleStateOf(0.0) }
    val sw = remember { mutableStateListOf<Double>() }; val rep = remember { mutableStateListOf<Double>() }
    var errors by remember { mutableIntStateOf(0) }
    LaunchedEffect(running, n) {
        if (running && n < total) { visible = false; delay(400); visible = true; withFrameNanos { onset = it / 1e6 } }
        if (running && n >= total) { running = false; store.add("switch", Stats.median(Stats.cleanRt(sw)) - Stats.median(Stats.cleanRt(rep))) }
    }
    fun answer(left: Boolean, time: Long) {
        if (!visible || onset == 0.0) return
        val tr = seq[n]; val rt = time - onset; onset = 0.0
        if (tr.answerLeft == left) { if (n > 0) (if (tr.switched) sw else rep) += rt } else errors++
        n++
    }
    Page("Переключение задач", scroll = false) {
        if (!running) {
            if (n >= total && seq.isNotEmpty()) {
                Text("Цена переключения: ${ms(Stats.median(Stats.cleanRt(sw)) - Stats.median(Stats.cleanRt(rep)))}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Повтор правила: ${ms(Stats.median(Stats.cleanRt(rep)))}   Смена: ${ms(Stats.median(Stats.cleanRt(sw)))}   Ошибки: $errors")
            }
            Note("Синяя рамка «ЧЁТНОСТЬ»: чётное — влево, нечётное — вправо.\nОранжевая «ВЕЛИЧИНА»: меньше 5 — влево, больше 5 — вправо.\n" +
                "Правило меняется непредсказуемо. Тренирует когнитивную гибкость; перенос на повседневные задачи ограничен.")
            Big("Старт") { seq = TaskSwitch.sequence(total); n = 0; sw.clear(); rep.clear(); errors = 0; running = true }
        } else {
            val tr = seq[n]
            val col = if (tr.ruleParity) Color(0xFF1E88E5) else Color(0xFFFB8C00)
            Text("${n + 1} / $total")
            Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp).clip(RoundedCornerShape(24.dp)).background(col.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                if (visible) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (tr.ruleParity) "ЧЁТНОСТЬ" else "ВЕЛИЧИНА", color = col, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(tr.digit.toString(), fontSize = 96.sp, fontWeight = FontWeight.Black)
                }
            }
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TapBox(Modifier.weight(1f).fillMaxHeight(), Color(0xFF555B66), { answer(true, it) }) { Text("Чётное\n< 5", color = Color.White, fontSize = 20.sp) }
                TapBox(Modifier.weight(1f).fillMaxHeight(), Color(0xFF555B66), { answer(false, it) }) { Text("Нечётное\n> 5", color = Color.White, fontSize = 20.sp) }
            }
        }
    }
}

private val letters = "БКЛМНРСТ"
private val cells = listOf(0, 1, 2, 3, 5, 6, 7, 8)

@Composable
fun NBackScreen(store: Store) {
    var n by remember { mutableIntStateOf(store.nbackLevel) }
    var running by remember { mutableStateOf(false) }
    var trials by remember { mutableStateOf(listOf<NBackTrial>()) }
    var cur by remember { mutableIntStateOf(-1) }
    var visible by remember { mutableStateOf(false) }
    val pp = remember { mutableStateListOf<Int>() }; val pl = remember { mutableStateListOf<Int>() }
    var result by remember { mutableStateOf("") }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        for (i in trials.indices) { cur = i; visible = true; delay(1000); visible = false; delay(1800) }
        val acc = NBack.accuracy(trials, n, pp.toSet(), pl.toSet())
        val nn = NBack.nextLevel(n, acc)
        store.add("nback", n.toDouble())
        result = "Точность ${(acc * 100).toInt()}% на уровне $n → следующий уровень $nn"
        n = nn; store.nbackLevel = nn; running = false; cur = -1
    }
    Page("Двойной $n-back", scroll = false) {
        if (!running) {
            if (result.isNotEmpty()) Text(result, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Note("Каждые ~3 с в клетке появляется буква. Жми «Позиция», если место совпадает с тем, что было $n шагов назад; " +
                "«Буква» — если совпадает буква. Уровень подстраивается: ≥90% — выше, <75% — ниже.")
            Note("Честно: n-back улучшает сам n-back и близкие задачи на рабочую память; перенос на интеллект и «жизнь» " +
                "в крупных метаанализах не подтверждён (Melby-Lervåg, Redick & Hulme, 2016; Simons et al., 2016).")
            Big("Старт (${20 + n} шагов)") { trials = NBack.generate(n); pp.clear(); pl.clear(); running = true }
        } else {
            Text("Шаг ${cur + 1} / ${trials.size}")
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(Modifier.aspectRatio(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (r in 0..2) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0..2) { val cell = r * 3 + c
                            val tr = trials.getOrNull(cur)
                            val on = visible && tr != null && cells[tr.pos] == cell
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp))
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                if (on) Text(letters[tr!!.letter].toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button({ if (cur >= 0) pp.add(cur) }, Modifier.weight(1f).fillMaxHeight(), enabled = cur !in pp) { Text("Позиция", fontSize = 20.sp) }
                Button({ if (cur >= 0) pl.add(cur) }, Modifier.weight(1f).fillMaxHeight(), enabled = cur !in pl) { Text("Буква", fontSize = 20.sp) }
            }
        }
    }
}
