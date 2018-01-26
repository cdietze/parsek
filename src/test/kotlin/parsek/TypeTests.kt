package parsek

/**
 * Check that type inference works as expected.
 * These are checked at compile-time, so no need to "run" these tests.
 */
object TypeTests {
    fun seqUU(a: Parser<Unit>, b: Parser<Unit>): Parser<Unit> = a * b
    fun <A> seqAU(a: Parser<A>, b: Parser<Unit>): Parser<A> = a * b
    fun <A> seqUA(a: Parser<Unit>, b: Parser<A>): Parser<A> = a * b
    fun <A, B> seqAB(a: Parser<A>, b: Parser<B>): Parser<Pair<A, B>> = a * b

    fun eitherUU(a: Parser<Unit>, b: Parser<Unit>): Parser<Unit> = a + b
    fun <A> eitherAA(a: Parser<A>, b: Parser<A>): Parser<A> = a + b
    fun <A> eitherAAA(a: Parser<A>, b: Parser<A>, c: Parser<A>): Parser<A> = a + b + c
}
