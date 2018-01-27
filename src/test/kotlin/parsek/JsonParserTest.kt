package parsek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonParserTest {
    object Js {
        sealed class Val {
            abstract val value: Any?

            data class Str(override val value: String) : Val()
            data class Obj(override val value: List<Pair<String, Val>>) : Val()
            data class Arr(override val value: List<Val>) : Val()
            data class Num(override val value: Double) : Val()

            object False : Val() {
                override val value = false
            }

            object True : Val() {
                override val value = false
            }

            object Null : Val() {
                override val value: Any? = null
            }
        }
    }

    companion object {
        val jsonExpr: Parser<Js.Val> =
            P { space * (obj + array + string + `true` + `false` + `null` + `number`) * space }

        val space = p("^[ \\r\\n]*".toRegex())
        val digits = p("^[0-9]+".toRegex())
        val exponent = p("[eE][+-]?".toRegex()) * digits
        val fractional = p(".") * digits
        val integral = p("^[+\\-]?(0|([1-9][0-9]*))".toRegex())
        val number = (p("[+-]?".toRegex()) * integral * fractional.opt() * exponent.opt()).capture()
            .map { Js.Val.Num(it.toDouble()) }

        val `null` = p("null").map { Js.Val.Null }
        val `true` = p("true").map { Js.Val.True }
        val `false` = p("false").map { Js.Val.False }

        val array: Parser<Js.Val.Arr> = (p("[") * jsonExpr.rep(sep = p(",")) * space * p("]")).map { Js.Val.Arr(it) }

        val escape = p("""^\\((u[0-9a-fA-F]{4})|[bfnrt])""".toRegex())
        val strChars = p("""^[^"\\]+""".toRegex())

        val string: Parser<Js.Val.Str> =
            (space * p("\"") * (strChars + escape).rep().capture() * p("\"")).map { Js.Val.Str(it) }

        val pair: Parser<Pair<String, Js.Val>> = (string.map { it.value } * p(":") * jsonExpr)
        val obj: Parser<Js.Val.Obj> =
            (p("{") * pair.rep(sep = p(",")) * space * p("}")).map { Js.Val.Obj(it) }
    }

    @Test
    fun `should parse num`() {
        assertEquals(Js.Val.Num(0.0), number.parse("0").getOrFail().value)
        assertEquals(Js.Val.Num(1230.0), number.parse("1230").getOrFail().value)
    }

    @Test
    fun `should parse string`() {
        assertEquals(Js.Val.Str(""), string.parse("\"\"").getOrFail().value)
        assertEquals(Js.Val.Str("abc"), string.parse("\"abc\"").getOrFail().value)
    }

    @Test
    fun `should parse escaped strings`() {
        assertEquals(Js.Val.Str("a\\tb"), string.parse(""""a\tb"""").getOrFail().value)
        assertEquals(Js.Val.Str("a\\b\\f\\n\\r\\tb"), string.parse(""""a\b\f\n\r\tb"""").getOrFail().value)
        assertEquals(Js.Val.Str("a\\u2665b"), string.parse(""""a\u2665b"""").getOrFail().value)
    }

    @Test
    fun `should fail on invalid escaped strings`() {
        assertTrue(string.parse(""""\x"""").isFailure)
        assertTrue(string.parse(""""\uxxxx"""").isFailure)
        assertTrue(string.parse(""""\uab"""").isFailure)
    }

    @Test
    fun `should parse flat expressions`() {
        assertEquals(Js.Val.Obj(listOf()), jsonExpr.parse("{}").getOrFail().value)
        assertEquals(Js.Val.Obj(listOf("a" to Js.Val.Str("b"))), jsonExpr.parse("""{"a": "b"}""").getOrFail().value)
        assertEquals(
            Js.Val.Obj(listOf("a" to Js.Val.Str("b"), "c" to Js.Val.Num(5.0))),
            jsonExpr.parse("""{"a": "b", "c": 5}""").getOrFail().value
        )
        assertEquals(Js.Val.Arr(listOf()), jsonExpr.parse("""[]""").getOrFail().value)
        assertEquals(Js.Val.Arr(listOf(Js.Val.Num(1.0))), jsonExpr.parse("""[1]""").getOrFail().value)
        assertEquals(
            Js.Val.Arr(listOf(Js.Val.Num(1.0), Js.Val.Num(2.0))),
            jsonExpr.parse("""[1, 2]""").getOrFail().value
        )
    }

    @Test
    fun `should parse nested expressions`() {
        assertTrue(jsonExpr.parse("""[{}]""").getOrFail().isSuccess)
        assertTrue(jsonExpr.parse("""[{},{}]""").getOrFail().isSuccess)
    }
}
