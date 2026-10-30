package ru.vorontsov.ivanmozg

import io.github.openspacedrepetition.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LogicTest {
    @Test fun stats() { assertEquals(2.5, Stats.median(listOf(4.0, 1.0, 2.0, 3.0)), 1e-9); assertEquals(listOf(200.0), Stats.cleanRt(listOf(50.0, 200.0, 2000.0))) }
    @Test fun nbackMatchesConsistent() {
        repeat(50) { s -> val n = 1 + s % 4; val t = NBack.generate(n, rnd = Random(s))
            t.forEachIndexed { i, x -> if (i >= n) { assertEquals(x.posMatch, x.pos == t[i - n].pos); assertEquals(x.letterMatch, x.letter == t[i - n].letter) } }
            val perfect = NBack.accuracy(t, n, t.indices.filter { t[it].posMatch }.toSet(), t.indices.filter { t[it].letterMatch }.toSet())
            assertEquals(1.0, perfect, 1e-9) }
        assertEquals(3, NBack.nextLevel(2, 0.95)); assertEquals(1, NBack.nextLevel(2, 0.5)); assertEquals(1, NBack.nextLevel(1, 0.1))
    }
    @Test fun matchRateReasonable() { val t = (0 until 200).flatMap { NBack.generate(2, rnd = Random(it)) }; val r = t.count { it.posMatch } / t.size.toDouble(); assertTrue(r in 0.2..0.4) }
    @Test fun span() { assertEquals(6, Span.next(5, true)); assertEquals(2, Span.next(2, false)); val d = Span.digits(9, Random(1)); assertEquals(9, d.size); for (i in 1 until d.size) assertNotEquals(d[i], d[i - 1]) }
    @Test fun stroopBalanced() { val r = Random(3); val c = (0 until 2000).count { Stroop.trial(r).congruent }; assertTrue(c in 850..1150) }
    @Test fun switchRules() { val s = TaskSwitch.sequence(500, Random(2)); assertTrue(s.none { it.digit == 5 }); assertTrue(SwitchTrial(4, true, false).answerLeft); assertFalse(SwitchTrial(7, false, false).answerLeft) }
    @Test fun loci() { assertEquals(2, Loci.score(listOf("ёлка", "кит", "лодка"), listOf("Елка ", "кот", "лодка"))) }
    @Test fun fsrsIntervalsGrow() {
        val s = Scheduler.builder().enableFuzzing(false).build()
        var c = Card.builder().build(); var now = java.time.Instant.now(); var prev = 0L
        repeat(5) { c = s.reviewCard(c, Rating.GOOD, now).card(); val gap = c.due.epochSecond - now.epochSecond; assertTrue(gap >= prev); prev = gap; now = c.due }
        assertTrue(prev > 86400 * 10)
        assertEquals(c.due, Card.fromJson(c.toJson()).due)
    }
}
