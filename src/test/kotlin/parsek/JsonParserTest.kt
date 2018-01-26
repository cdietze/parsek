package parsek

import parsek.Combinators.log
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

        // TODO: add unicode support
        val string: Parser<Js.Val.Str> =
            (space * p("\"") * p("^[^\"]*".toRegex()).capture() * p("\"")).map { Js.Val.Str(it) }
                .log("string", ::println)

        val pair: Parser<Pair<String, Js.Val>> = (string.map { it.value } * p(":") * jsonExpr).log("pair", ::println)
        val obj: Parser<Js.Val.Obj> =
            (p("{") * pair.rep(sep = p(",")) * space * p("}")).map { Js.Val.Obj(it) }.log("obj", ::println)
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
