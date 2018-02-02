package parsek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BasicTest {

    @Test
    fun `should succeed simple samples`() {
        val p = P("a")
        assertEquals(ParseResult.Success(Unit, 1), p.parse("a"))
        assertEquals(ParseResult.Success(Unit, 1), p.parse("ab"))
        assertEquals(ParseResult.Success(Unit, 1), p.parse("aa"))
    }

    @Test
    fun `should fail simple samples`() {
        val p = P("a")
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("b").isFailure)
        assertTrue(p.parse("ba").isFailure)
    }

    @Test
    fun `should handle Start correctly`() {
        assertTrue(Start.parse("").isSuccess)
        assertTrue(Start.parse("a").isSuccess)
        assertTrue(Start.parse("a", 1).isFailure)
        assertTrue(Start.parse("ab", 1).isFailure)
    }

    @Test
    fun `should handle End correctly`() {
        assertTrue(End.parse("").isSuccess)
        assertTrue(End.parse("a").isFailure)

        val p = P("ab") * End
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("a").isFailure)
        assertTrue(p.parse("ab").isSuccess)
        assertTrue(p.parse("abc").isFailure)
    }
}
