package ru.vorontsov.ivanmozg

import android.content.Context
import io.github.openspacedrepetition.Card
import io.github.openspacedrepetition.Rating
import io.github.openspacedrepetition.Scheduler
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Result(val type: String, val ts: Long, val value: Double)
data class Flash(val id: Long, val front: String, val back: String, val card: Card)

/** Локальное хранилище: всё офлайн, ничего не покидает телефон. */
class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("mozg", Context.MODE_PRIVATE)
    private val scheduler: Scheduler = Scheduler.builder().build() // FSRS, desired retention 0.9

    fun results(): List<Result> {
        val a = JSONArray(p.getString("results", "[]"))
        return (0 until a.length()).map { a.getJSONObject(it).let { o -> Result(o.getString("t"), o.getLong("ts"), o.getDouble("v")) } }
    }
    fun add(type: String, value: Double) {
        val a = JSONArray(p.getString("results", "[]"))
        a.put(JSONObject().put("t", type).put("ts", System.currentTimeMillis()).put("v", value))
        val trimmed = if (a.length() > 3000) JSONArray((a.length() - 3000 until a.length()).map { a.get(it) }) else a
        p.edit().putString("results", trimmed.toString()).apply()
    }
    fun doneToday(type: String): Boolean {
        val today = LocalDate.now()
        return results().any { it.type == type && Instant.ofEpochMilli(it.ts).atZone(ZoneId.systemDefault()).toLocalDate() == today }
    }
    fun markDone(key: String) = add(key, 1.0)

    var nbackLevel: Int
        get() = p.getInt("nback", 2)
        set(v) { p.edit().putInt("nback", v).apply() }
    var spanLevel: Int
        get() = p.getInt("span", 5)
        set(v) { p.edit().putInt("span", v).apply() }
    var lociPlaces: List<String>
        get() = p.getString("loci", null)?.split("\n")?.filter { it.isNotBlank() } ?: listOf(
            "Входная дверь", "Прихожая", "Вешалка", "Ванная", "Кухня: плита", "Кухня: холодильник", "Стол", "Диван", "Окно", "Кровать")
        set(v) { p.edit().putString("loci", v.joinToString("\n")).apply() }
    var theme: Int // 0 системная, 1 светлая, 2 тёмная
        get() = p.getInt("theme", 0)
        set(v) { p.edit().putInt("theme", v).apply() }

    fun cards(): List<Flash> {
        val a = JSONArray(p.getString("cards", "[]"))
        return (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            runCatching { Flash(o.getLong("id"), o.getString("f"), o.getString("b"), Card.fromJson(o.getString("c"))) }.getOrNull()
        }
    }
    private fun save(list: List<Flash>) {
        val a = JSONArray(); list.forEach { a.put(JSONObject().put("id", it.id).put("f", it.front).put("b", it.back).put("c", it.card.toJson())) }
        p.edit().putString("cards", a.toString()).apply()
    }
    fun addCard(front: String, back: String) {
        val id = System.currentTimeMillis()
        save(cards() + Flash(id, front, back, Card.builder().cardId((id % Int.MAX_VALUE).toInt()).build()))
    }
    fun deleteCard(id: Long) = save(cards().filter { it.id != id })
    fun due(): List<Flash> { val now = Instant.now(); return cards().filter { !it.card.due.isAfter(now) }.sortedBy { it.card.due } }
    /** rating 1..4 = Снова/Трудно/Хорошо/Легко. */
    fun review(id: Long, rating: Int) {
        val r = when (rating) { 1 -> Rating.AGAIN; 2 -> Rating.HARD; 3 -> Rating.GOOD; else -> Rating.EASY }
        save(cards().map { if (it.id == id) it.copy(card = scheduler.reviewCard(it.card, r).card()) else it })
        add("srs", if (rating > 1) 1.0 else 0.0)
    }
}
