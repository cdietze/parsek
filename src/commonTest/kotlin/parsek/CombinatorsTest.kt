package parsek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterCombinator {
    @Test
    fun `shouldWork`() {
        val p = CharIn("ab").capture().filter { it == "a" }
        assertTrue(p.parse("a").isSuccess)
        assertTrue(p.parse("b").isFailure)
        assertTrue(p.parse("c").isFailure)
    }
}

class SequenceCombinator {
    @Test
    fun `shouldSucceedSimpleSamples`() {
        val p = P("a") * P("b")
        assertEquals(ParseResult.Success(Unit, 2), p.parse("ab"))
    }

    @Test
    fun `shouldFailSimpleSamples`() {
        val p = P("a") * P("b")
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("a").isFailure)
        assertTrue(p.parse("b").isFailure)
        assertTrue(p.parse("ba").isFailure)
        assertTrue(p.parse("aab").isFailure)
    }

    @Test
    fun `shouldContainCorrectValue`() {
        val p = P("a").capture() * P("b").capture()
        assertEquals(ParseResult.Success(Pair("a", "b"), 2), p.parse("ab"))
    }
}

class EitherCombinator {
    @Test
    fun `shouldSucceedSimpleSamples`() {
        val p = (P("a") + P("b")).capture()
        assertEquals(ParseResult.Success("a", 1), p.parse("a"))
        assertEquals(ParseResult.Success("b", 1), p.parse("b"))
        assertEquals(ParseResult.Success("a", 1), p.parse("aa"))
        assertEquals(ParseResult.Success("b", 1), p.parse("ba"))
    }

    @Test
    fun `shouldFailSimpleSamples`() {
        val p = P("a") + P("b")
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("c").isFailure)
        assertTrue(p.parse("ca").isFailure)
    }
}

class RepeatTests {
    @Test
    fun `shouldSucceedSimpleSamples`() {
        val p: Parser<Unit> = P("a").rep()
        assertEquals(ParseResult.Success(Unit, 0), p.parse(""))
        assertEquals(ParseResult.Success(Unit, 1), p.parse("a"))
        assertEquals(ParseResult.Success(Unit, 2), p.parse("aa"))
        assertEquals(ParseResult.Success(Unit, 3), p.parse("aaa"))
        assertEquals(ParseResult.Success(Unit, 2), p.parse("aaba"))
    }

    @Test
    fun `shouldHonorMin`() {
        val p: Parser<Unit> = P("a").rep(min = 2)
        assertTrue(p.parse("").isFailure)
        assertTrue(p.parse("a").isFailure)
        assertTrue(p.parse("aa").isSuccess)
        assertTrue(p.parse("aaa").isSuccess)
    }

    @Test
    fun `shouldHonorMax`() {
        val p: Parser<Unit> = P("a").rep(max = 2)
        assertEquals(ParseResult.Success(Unit, 0), p.parse(""))
        assertEquals(ParseResult.Success(Unit, 1), p.parse("a"))
        assertEquals(ParseResult.Success(Unit, 2), p.parse("aa"))
        assertEquals(ParseResult.Success(Unit, 2), p.parse("aaa"))
        assertEquals(ParseResult.Success(Unit, 2), p.parse("aaaa"))
    }

    @Test
    fun `shouldHonorSeparator`() {
        val p: Parser<Unit> = P("a").rep(sep = P(","))
        assertEquals(ParseResult.Success(Unit, 0), p.parse(""))
        assertEquals(ParseResult.Success(Unit, 1), p.parse("a"))
        assertEquals(ParseResult.Success(Unit, 3), p.parse("a,a"))
        assertEquals(ParseResult.Success(Unit, 5), p.parse("a,a,a"))
        assertEquals(ParseResult.Success(Unit, 1), p.parse("a,"))
    }
}

class NotTests {
    @Test
    fun `shouldWork`() {
        val p: Parser<Unit> = P("a").not()
        assertTrue(p.parse("").let { it.isSuccess && it.index == 0 })
        assertTrue(p.parse("a").let { it.isFailure && it.index == 0 })
        assertTrue(p.parse("b").let { it.isSuccess && it.index == 0 })
    }
}

class CutTests {

    @Test
    fun `shouldSetCutFlagOnSuccessfulParse`() {
        assertTrue(
            P("a").cut().parseRec(ParserCtx("a"), 0).let {
                it is MutableParseResult.MutableSuccess && it.cut
            })
    }

    @Test
    fun `shouldNotSetCutFlagOnFailedParse`() {
        assertTrue(
            P("a").cut().parseRec(ParserCtx("b"), 0).let {
                it is MutableParseResult.MutableFailure && !it.cut
            })
    }

    @Test
    fun `shouldKeepCutFlagInSuccessfulSequences`() {
        assertTrue(
            (P("(").cut() * P("a")).parseRec(ParserCtx("(a"), 0).let {
                it is MutableParseResult.MutableSuccess && it.cut
            })
    }

    @Test
    fun `shouldKeepCutFlagInFailedSequences`() {
        assertTrue(
            (P("(").cut() * P("a")).parseRec(ParserCtx("(b"), 0).let {
                it is MutableParseResult.MutableFailure && it.cut
            })
    }

    @Test
    fun `shouldNotTryAlternativeWhenCut`() {
        val p = P("(").cut() * P("a") + P("(b")
        assertTrue(p.parse("(a").isSuccess)
        assertTrue(p.parse("(b").isFailure)
    }

    @Test
    fun `shouldNotTryAlternativeWhenRepeatIsCut`() {
        val p = P("(").rep().cut() * P("a") + P("(b") + P("b")
        assertTrue(p.parse("(a").isSuccess)
        assertTrue(p.parse("(b").isFailure)
        assertTrue(p.parse("b").isFailure)
    }

    @Test
    fun `shouldNotTryAlternativeWhenCutIsRepeated`() {
        val p = P("(").cut().rep() * P("a") + P("(b") + P("b")
        assertTrue(p.parse("(a").isSuccess)
        assertTrue(p.parse("(b").isFailure)
        assertTrue(p.parse("b").isSuccess)
    }
}
