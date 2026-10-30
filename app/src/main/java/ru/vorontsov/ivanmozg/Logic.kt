package ru.vorontsov.ivanmozg

import kotlin.math.sqrt
import kotlin.random.Random

/** Чистая логика без Android — покрыта unit-тестами. */
object Stats {
    fun mean(x: List<Double>) = if (x.isEmpty()) 0.0 else x.sum() / x.size
    fun median(x: List<Double>): Double {
        if (x.isEmpty()) return 0.0
        val s = x.sorted(); val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2
    }
    fun sd(x: List<Double>): Double {
        if (x.size < 2) return 0.0
        val m = mean(x); return sqrt(x.sumOf { (it - m) * (it - m) } / (x.size - 1))
    }
    /** Убираем явные промахи: упреждения (<100 мс) и отвлечения (>1500 мс) — стандартная практика в RT-исследованиях. */
    fun cleanRt(x: List<Double>) = x.filter { it in 100.0..1500.0 }
}

/** Шаг одного n-back испытания. */
data class NBackTrial(val pos: Int, val letter: Int, val posMatch: Boolean, val letterMatch: Boolean)

object NBack {
    /** Генерирует последовательность с ~30% совпадений по каждому каналу (как у Jaeggi et al. 2008). */
    fun generate(n: Int, len: Int = 20 + n, rnd: Random = Random.Default, matchRate: Double = 0.3): List<NBackTrial> {
        val pos = IntArray(len); val let = IntArray(len)
        for (i in 0 until len) {
            pos[i] = if (i >= n && rnd.nextDouble() < matchRate) pos[i - n] else rnd.nextInt(8).let { p -> if (i >= n && p == pos[i - n]) (p + 1) % 8 else p }
            let[i] = if (i >= n && rnd.nextDouble() < matchRate) let[i - n] else rnd.nextInt(8).let { l -> if (i >= n && l == let[i - n]) (l + 1) % 8 else l }
        }
        return (0 until len).map { i -> NBackTrial(pos[i], let[i], i >= n && pos[i] == pos[i - n], i >= n && let[i] == let[i - n]) }
    }
    /** Точность сеанса по обоим каналам (верные нажатия + верные пропуски). */
    fun accuracy(trials: List<NBackTrial>, n: Int, pressPos: Set<Int>, pressLet: Set<Int>): Double {
        var ok = 0; var tot = 0
        for (i in n until trials.size) {
            tot += 2
            if (trials[i].posMatch == (i in pressPos)) ok++
            if (trials[i].letterMatch == (i in pressLet)) ok++
        }
        return if (tot == 0) 0.0 else ok.toDouble() / tot
    }
    /** Адаптация уровня: ≥90% → n+1, <75% → n−1 (правило, близкое к Brain Workshop). */
    fun nextLevel(n: Int, acc: Double) = when { acc >= 0.9 -> n + 1; acc < 0.75 -> maxOf(1, n - 1); else -> n }
}

object Span {
    /** Адаптивная лестница 1-up/1-down: верно → длина+1, ошибка → длина−1. */
    fun next(len: Int, correct: Boolean) = if (correct) len + 1 else maxOf(2, len - 1)
    fun digits(len: Int, rnd: Random = Random.Default): List<Int> {
        val out = ArrayList<Int>()
        while (out.size < len) { val d = rnd.nextInt(10); if (out.isEmpty() || out.last() != d) out += d }
        return out
    }
}

/** Струп: слово-цвет и цвет шрифта; конгруэнтные 50%. */
data class StroopTrial(val word: Int, val ink: Int) { val congruent get() = word == ink }
object Stroop {
    val names = listOf("КРАСНЫЙ", "ЗЕЛЁНЫЙ", "СИНИЙ", "ЖЁЛТЫЙ")
    fun trial(rnd: Random = Random.Default): StroopTrial {
        val ink = rnd.nextInt(4)
        return if (rnd.nextBoolean()) StroopTrial(ink, ink) else StroopTrial((ink + 1 + rnd.nextInt(3)) % 4, ink)
    }
}

/** Переключение задач: цифра 1–9 кроме 5; правило «чёт/нечет» или «больше/меньше 5». */
data class SwitchTrial(val digit: Int, val ruleParity: Boolean, val switched: Boolean) {
    /** true = левая кнопка («чётное» / «меньше 5»). */
    val answerLeft get() = if (ruleParity) digit % 2 == 0 else digit < 5
}
object TaskSwitch {
    fun sequence(len: Int, rnd: Random = Random.Default): List<SwitchTrial> {
        var rule = rnd.nextBoolean(); val out = ArrayList<SwitchTrial>()
        repeat(len) { i ->
            val sw = i > 0 && rnd.nextDouble() < 0.4
            if (sw) rule = !rule
            var d: Int; do { d = 1 + rnd.nextInt(9) } while (d == 5)
            out += SwitchTrial(d, rule, sw)
        }
        return out
    }
}

/** Метод локусов: слова для запоминания (конкретные, легко представить). */
object Loci {
    val words = listOf("якорь","арбуз","барабан","ведро","гитара","дракон","ёлка","жираф","замок","зонт","игла","кактус","лимон","маяк","носорог",
        "облако","пианино","ракета","самовар","тигр","утюг","фонарь","хлеб","цирк","чайник","шляпа","щука","эскимо","юла","яблоко",
        "велосипед","глобус","дельфин","книга","лампа","мост","ножницы","орёл","пингвин","радуга","свеча","телефон","улитка","флаг","часы","шар","кит","корона","лодка","молоток")
    fun pick(n: Int, rnd: Random = Random.Default) = words.shuffled(rnd).take(n)
    /** Засчитываем слово, если оно на своём месте (порядок важен — суть метода). */
    fun score(target: List<String>, answer: List<String>) = target.indices.count { i ->
        answer.getOrNull(i)?.trim()?.lowercase()?.replace('ё', 'е') == target[i].lowercase().replace('ё', 'е') }
}
