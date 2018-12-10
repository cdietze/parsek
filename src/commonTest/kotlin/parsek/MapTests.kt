package parsek

import kotlin.test.Test
import kotlin.test.assertEquals

class MapTests {

    private val char = CharPred { true }.capture()
    private val digit = CharIn("0123456789").capture()

    @Test
    fun `map_with_2_params_should_work`() {
        val p = (char * char).map { a, b -> a + b }
        assertEquals("ab", p.parse("ab").getOrFail().value)
    }

    @Test
    fun `map_with_3_params_should_work`() {
        val p = (char * char * char).map { a, b, c -> a + b + c }
        assertEquals("abc", p.parse("abc").getOrFail().value)
    }

    @Test
    fun `map_with_4_params_should_work`() {
        val p = (char * char * char * char).map { a, b, c, d -> a + b + c + d }
        assertEquals("abcd", p.parse("abcd").getOrFail().value)
    }

    @Test
    fun `map_with_5_params_should_work`() {
        val p = (char * char * char * char * char).map { a, b, c, d, e -> a + b + c + d + e }
        assertEquals("abcde", p.parse("abcde").getOrFail().value)
    }
}
