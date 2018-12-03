package parsek

import kotlin.test.Test
import kotlin.test.assertEquals

class MapTests {

    private val char = CharPred { true }.capture()
    private val digit = CharIn("0123456789").capture()

    @Test
    fun `map with 2 params should work`() {
        val p = (char * char).map { a, b -> a + b }
        assertEquals("ab", p.parse("ab").getOrFail().value)
    }

    @Test
    fun `map with 3 params should work`() {
        val p = (char * char * char).map { a, b, c -> a + b + c }
        assertEquals("abc", p.parse("abc").getOrFail().value)
    }

    @Test
    fun `map with 4 params should work`() {
        val p = (char * char * char * char).map { a, b, c, d -> a + b + c + d }
        assertEquals("abcd", p.parse("abcd").getOrFail().value)
    }

    @Test
    fun `map with 5 params should work`() {
        val p = (char * char * char * char * char).map { a, b, c, d, e -> a + b + c + d + e }
        assertEquals("abcde", p.parse("abcde").getOrFail().value)
    }
}
