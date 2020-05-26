package parsek

/**
 * Check that type inference works as expected.
 * These are checked at compile-time, so no need to "run" these tests.
 */
@Suppress("unused", "RedundantLambdaArrow")
object TypeTests {
    fun seqUU(a: Parser<Unit>, b: Parser<Unit>): Parser<Unit> = a * b
    fun <A> seqAU(a: Parser<A>, b: Parser<Unit>): Parser<A> = a * b
    fun <A> seqUA(a: Parser<Unit>, b: Parser<A>): Parser<A> = a * b
    fun <A, B> seqAB(a: Parser<A>, b: Parser<B>): Parser<Pair<A, B>> = a * b

    fun eitherUU(a: Parser<Unit>, b: Parser<Unit>): Parser<Unit> = a + b
    fun <A> eitherAA(a: Parser<A>, b: Parser<A>): Parser<A> = a + b
    fun <A> eitherAAA(a: Parser<A>, b: Parser<A>, c: Parser<A>): Parser<A> = a + b + c

    fun <A> map1(a: Parser<A>): Parser<Int> =
        (a).map { _: A -> 0 }

    fun <A, B> map2(a: Parser<A>, b: Parser<B>): Parser<Int> =
        (a * b).map { _: A, _: B -> 0 }

    fun <A, B, C> map3(a: Parser<A>, b: Parser<B>, c: Parser<C>): Parser<Int> =
        (a * b * c).map { _: A, _: B, _: C -> 0 }

    fun <A, B, C, D> map4(a: Parser<A>, b: Parser<B>, c: Parser<C>, d: Parser<D>): Parser<Int> =
        (a * b * c * d).map { _: A, _: B, _: C, _: D -> 0 }

    fun shouldBeAbleToSkipOpt(numParser: Parser<Int>): Parser<Int> =
        P("x").opt() * numParser * P("y").opt()
}
