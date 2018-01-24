package parsek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SequenceCombinator {
    @Test
    fun `should succeed simple samples`() {
        val p = p("a") * p("b")
        assertEquals(Parsed.Success(Unit, 2), p.parse("ab"))
    }

    @Test
    fun `should fail simple samples`() {
        val p = p("a") * p("b")
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("a").isFailure)
        assertTrue(p.parse("b").isFailure)
        assertTrue(p.parse("ba").isFailure)
        assertTrue(p.parse("aab").isFailure)
    }
}

class EitherCombinator {
    @Test
    fun `should succeed simple samples`() {
        val p = (p("a") + p("b")).capture()
        assertEquals(Parsed.Success("a", 1), p.parse("a"))
        assertEquals(Parsed.Success("b", 1), p.parse("b"))
        assertEquals(Parsed.Success("a", 1), p.parse("aa"))
        assertEquals(Parsed.Success("b", 1), p.parse("ba"))
    }

    @Test
    fun `should fail simple samples`() {
        val p = p("a") + p("b")
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("c").isFailure)
        assertTrue(p.parse("ca").isFailure)
    }
}
