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

    fun <A> Parser<A>.log(name: String, output: (String) -> Unit): Parser<A> = object : Parser<A> {
        override fun parse(input: String, index: Int): Parsed<A> {
            output("+$name:$index")
            val result = this@log.parse(input, index)
            result.match(
                { output("-$name:$index Success(:${it.index})") },
                { output("-$name:$index Failure") }
            )
            return result
        }
    }

    data class Optional<out A>(val p: Parser<A>) : Parser<A?> {
        override fun parse(input: String, index: Int): Parsed<A?> {
            return p.parse(input, index).match({ it }, { Parsed.Success(null, index) })
        }
    }

    data class Repeat<out A>(val p: Parser<A>, val separator: Parser<*>) : Parser<List<A>> {
        override fun parse(input: String, index: Int): Parsed<List<A>> {
            val result = mutableListOf<A>()
            var lastIndex = index
            // TODO: make sure loop is tail recursive
            fun loop(sep: Parser<*>): Parsed<List<A>> {
                val sepResult = sep.parse(input, lastIndex)
                return sepResult.match(
                    {
                        val parseResult = p.parse(input, it.index)
                        parseResult.match(
                            {
                                result.add(it.value)
                                lastIndex = it.index
                                loop(separator)
                            },
                            { Parsed.Success(result, lastIndex) }
                        )
                    },
                    { Parsed.Success(result, lastIndex) }
                )
            }
            return loop(Terminals.Pass)
        }
    }
}
