package parsek

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Port of the mathematical expression parser from Fastparse,
 * https://github.com/lihaoyi/fastparse/blob/master/fastparse/shared/src/test/scala/fastparse/MathTests.scala#L19-L29
 */
class ExprParserTest {
    val int: Parser<Int> = (CharIn("+-").opt() * WhileCharIn("0123456789")).capture().map { it.toInt() }

    val parens: Parser<Int> = Rule("parens") { P("(") * addSub * P(")") }
    val factor: Parser<Int> = int + parens

    val divMul: Parser<Int> = (factor * ((P("*") + P("/")).capture() * factor).rep()).map(::eval)
    val addSub: Parser<Int> = (divMul * ((P("+") + P("-")).capture() * divMul).rep()).map(::eval)

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
        assertEquals(ParseResult.Success(1, 1), int.parse("1"))
        assertEquals(ParseResult.Success(42, 2), int.parse("42"))
        assertEquals(ParseResult.Success(42, 3), int.parse("+42"))
        assertEquals(ParseResult.Success(-42, 3), int.parse("-42"))
    }

    @Test
    fun `should eval simple math expressions`() {
        assertEquals(ParseResult.Success(123, 3), expr.parse("123"))
        assertEquals(ParseResult.Success(2, 3), expr.parse("1+1"))
        assertEquals(ParseResult.Success(2, 3), expr.parse("3-1"))
        assertEquals(ParseResult.Success(6, 3), expr.parse("2*3"))
        assertEquals(ParseResult.Success(3, 3), expr.parse("6/2"))
        assertEquals(ParseResult.Success(2, 3), expr.parse("(2)"))
    }

    @Test
    fun `should eval nested math expressions`() {
        assertEquals(7, expr.parse("1+2*3").getOrFail().value)
        assertEquals(7, expr.parse("2*3+1").getOrFail().value)
        assertEquals(9, expr.parse("(1+2)*3").getOrFail().value)
        assertEquals(8, expr.parse("2*(3+1)").getOrFail().value)
        assertEquals(8, expr.parse("2*(3+1)/2*(1+1)").getOrFail().value)
        assertEquals(16, expr.parse("2*(2*(1+1)+4)").getOrFail().value)
    }
}
