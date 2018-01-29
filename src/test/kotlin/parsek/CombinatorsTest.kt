package parsek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SequenceCombinator {
    @Test
    fun `should succeed simple samples`() {
        val p = P("a") * P("b")
        assertEquals(Parsed.Success(Unit, 2), p.parse("ab"))
    }

    @Test
    fun `should fail simple samples`() {
        val p = P("a") * P("b")
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
        val p = (P("a") + P("b")).capture()
        assertEquals(Parsed.Success("a", 1), p.parse("a"))
        assertEquals(Parsed.Success("b", 1), p.parse("b"))
        assertEquals(Parsed.Success("a", 1), p.parse("aa"))
        assertEquals(Parsed.Success("b", 1), p.parse("ba"))
    }

    @Test
    fun `should fail simple samples`() {
        val p = P("a") + P("b")
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("c").isFailure)
        assertTrue(p.parse("ca").isFailure)
    }
}

class RepeatTests {
    @Test
    fun `should succeed simple samples`() {
        val p: Parser<Unit> = P("a").rep()
        assertEquals(Parsed.Success(Unit, 0), p.parse(""))
        assertEquals(Parsed.Success(Unit, 1), p.parse("a"))
        assertEquals(Parsed.Success(Unit, 2), p.parse("aa"))
        assertEquals(Parsed.Success(Unit, 3), p.parse("aaa"))
        assertEquals(Parsed.Success(Unit, 2), p.parse("aaba"))
    }

    @Test
    fun `should honor min`() {
        val p: Parser<Unit> = P("a").rep(min = 2)
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("a").isFailure)
        assertTrue(p.parse("aa").isSuccess)
        assertTrue(p.parse("aaa").isSuccess)
    }

    @Test
    fun `should honor max`() {
        val p: Parser<Unit> = P("a").rep(max = 2)
        assertEquals(Parsed.Success(Unit, 0), p.parse(""))
        assertEquals(Parsed.Success(Unit, 1), p.parse("a"))
        assertEquals(Parsed.Success(Unit, 2), p.parse("aa"))
        assertEquals(Parsed.Success(Unit, 2), p.parse("aaa"))
        assertEquals(Parsed.Success(Unit, 2), p.parse("aaaa"))
    }

    @Test
    fun `should honor separator`() {
        val p: Parser<Unit> = P("a").rep(sep = P(","))
        assertEquals(Parsed.Success(Unit, 0), p.parse(""))
        assertEquals(Parsed.Success(Unit, 1), p.parse("a"))
        assertEquals(Parsed.Success(Unit, 3), p.parse("a,a"))
        assertEquals(Parsed.Success(Unit, 5), p.parse("a,a,a"))
        assertEquals(Parsed.Success(Unit, 1), p.parse("a,"))
    }
}
