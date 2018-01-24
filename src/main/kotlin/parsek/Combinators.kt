package parsek

object Combinators {
    fun <A, B> sequence(a: Parser<A>, b: Parser<B>): Parser<Pair<A, B>> =
            a.flatMap { aValue -> b.map { bValue -> Pair(aValue, bValue) } }

    fun <A> either(ps: List<Parser<A>>): Parser<A> = object : Parser<A> {
        override fun parse(input: String, index: Int): Parsed<A> {
            fun loop(parserIndex: Int): Parsed<A> {
                return if (parserIndex >= ps.size) Parsed.Failure(index, this, input)
                else ps[parserIndex].parse(input, index).match({ it }, { loop(parserIndex + 1) })
            }
            return loop(0)
        }
    }
}
