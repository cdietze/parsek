package parsek

import parsek.JsonParserTest.Js.Val.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonParserTest {
    object Js {
        sealed class Val {
            abstract val value: Any?

            data class Str(override val value: String) : Val()
            data class Obj(override val value: List<Pair<String, Val>> = listOf()) : Val()
            data class Arr(override val value: List<Val> = listOf()) : Val()
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

            operator fun get(index: Int): Val = (this as Arr).value[index]
            operator fun get(field: String): Val = (this as Obj).value.first { it.first == field }.second
        }
    }

    companion object {
        val jsonExpr: Parser<Js.Val> =
            P { space * (obj + array + string + `true` + `false` + `null` + `number`) * space }

        val space = WhileCharIn(" \r\n", min = 0)
        val digits = WhileCharIn("0123456789")
        val exponent = CharIn("eE") * CharIn("+-").opt() * digits
        val fractional = P(".") * digits
        val integral = CharIn("+-").opt() * (P("0") + CharIn("123456789") * digits.opt())
        val number = (CharIn("+-").opt() * integral * fractional.opt() * exponent.opt()).capture()
            .map { Num(it.toDouble()) }

        val `null` = P("null").map { Js.Val.Null }
        val `true` = P("true").map { Js.Val.True }
        val `false` = P("false").map { Js.Val.False }

        val array: Parser<Arr> =
            (P("[") * jsonExpr.rep(sep = P(",")) * space * P("]")).map { Arr(it) }

        val hexDigit = CharIn(('0'..'9') + ('a'..'f') + ('A'..'F'))
        val unicodeEscape = P("u") * hexDigit * hexDigit * hexDigit * hexDigit
        val escape = P("\\") * (CharIn("\"/\\bfnrt") + unicodeEscape)

        val strCharPred: (Char) -> Boolean = { it !in "\"\\" }
        val strChars = CharPred(strCharPred).rep(min = 1)

        val string: Parser<Str> =
            (space * P("\"") * (strChars + escape).rep().capture() * P("\"")).map { Str(it) }

        val pair: Parser<Pair<String, Js.Val>> = (string.map { it.value } * P(":") * jsonExpr)
        val obj: Parser<Obj> =
            (P("{") * pair.rep(sep = P(",")) * space * P("}")).map { Obj(it) }
    }

    @Test
    fun `should parse num`() {
        assertEquals(Num(0.0), number.parse("0").getOrFail().value)
        assertEquals(Num(1230.0), number.parse("1230").getOrFail().value)
    }

    @Test
    fun `should parse string`() {
        assertEquals(Str(""), string.parse("\"\"").getOrFail().value)
        assertEquals(Str("abc"), string.parse("\"abc\"").getOrFail().value)
    }

    @Test
    fun `should parse escaped strings`() {
        assertEquals(Str("a\\tb"), string.parse(""""a\tb"""").getOrFail().value)
        assertEquals(Str("a\\b\\f\\n\\r\\tb"), string.parse(""""a\b\f\n\r\tb"""").getOrFail().value)
        assertEquals(Str("a\\u2665b"), string.parse(""""a\u2665b"""").getOrFail().value)
    }

    @Test
    fun `should fail on invalid escaped strings`() {
        assertTrue(string.parse(""""\x"""").isFailure)
        assertTrue(string.parse(""""\uxxxx"""").isFailure)
        assertTrue(string.parse(""""\uab"""").isFailure)
    }

    @Test
    fun `should parse flat expressions`() {
        assertEquals(Obj(listOf()), jsonExpr.parse("{}").getOrFail().value)
        assertEquals(Obj(listOf("a" to Str("b"))), jsonExpr.parse("""{"a": "b"}""").getOrFail().value)
        assertEquals(
            Obj(listOf("a" to Str("b"), "c" to Num(5.0))),
            jsonExpr.parse("""{"a": "b", "c": 5}""").getOrFail().value
        )
        assertEquals(Arr(listOf()), jsonExpr.parse("""[]""").getOrFail().value)
        assertEquals(Arr(listOf(Num(1.0))), jsonExpr.parse("""[1]""").getOrFail().value)
        assertEquals(
            Arr(listOf(Num(1.0), Num(2.0))),
            jsonExpr.parse("""[1, 2]""").getOrFail().value
        )
    }

    @Test
    fun `should parse nested expressions`() {
        assertEquals(Arr(listOf(Obj())), jsonExpr.parse("""[{}]""").getOrFail().value)
        assertEquals(Arr(listOf(Obj(), Obj())), jsonExpr.parse("""[{},{}]""").getOrFail().value)
        assertEquals(Obj(listOf("a" to Str("b"))), jsonExpr.parse("""{"a":"b"}""").getOrFail().value)
    }
}
