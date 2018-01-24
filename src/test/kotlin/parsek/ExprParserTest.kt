package parsek

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Port of the mathematical expression parser from Fastparse,
 * https://github.com/lihaoyi/fastparse/blob/master/fastparse/shared/src/test/scala/fastparse/MathTests.scala#L19-L29
 */
class ExprParserTest {
    val int: Parser<Int> = p("^([+\\-])?\\d+".toRegex()).capture().map { it.toInt() }

    val parens: Parser<Int> = P { p("(") * addSub * p(")") }
    val factor: Parser<Int> = int + parens

    val divMul: Parser<Int> = (factor * ((p("*") + p("/")).capture() * factor).rep()).map(::eval)
    val addSub: Parser<Int> = (divMul * ((p("+") + p("-")).capture() * divMul).rep()).map(::eval)

    val expr: Parser<Int> = addSub * End

    private fun eval(it: Pair<Int, List<Pair<String, Int>>>): Int {
        return it.second.fold(it.first, { acc, p ->
            when (p.first) {
                "+" -> acc + p.second
                "-" -> acc - p.second
                "*" -> acc * p.second
                "/" -> acc / p.second
                else -> error("unknown operatore: ${p.first}")
            }
        })
    }

    @Test
    fun `should parse int expressions`() {
        assertEquals(Parsed.Success(1, 1), int.parse("1"))
        assertEquals(Parsed.Success(42, 2), int.parse("42"))
        assertEquals(Parsed.Success(42, 3), int.parse("+42"))
        assertEquals(Parsed.Success(-42, 3), int.parse("-42"))
    }

    @Test
    fun `should eval math expressions`() {
        assertEquals(Parsed.Success(123, 3), expr.parse("123"))
        assertEquals(Parsed.Success(2, 3), expr.parse("1+1"))
        assertEquals(Parsed.Success(2, 3), expr.parse("3-1"))
        assertEquals(Parsed.Success(6, 3), expr.parse("2*3"))
        assertEquals(Parsed.Success(3, 3), expr.parse("6/2"))
        assertEquals(Parsed.Success(2, 3), expr.parse("(2)"))
    }
}
