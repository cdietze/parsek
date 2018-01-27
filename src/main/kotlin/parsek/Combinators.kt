package parsek

object Combinators {

    data class Capturing(val p: Parser<*>) : Parser<String> {
        override fun parse(input: String, index: Int): Parsed<String> {
            val res = p.parse(input, index)
            return res.flatMap { oldSuccess: Parsed.Success<Any?> ->
                Parsed.Success(
                    input.substring(
                        index,
                        oldSuccess.index
                    ), oldSuccess.index
                )
            }
        }
    }

    data class Seq<A, B>(val a: Parser<A>, val b: Parser<B>) : Parser<Pair<A, B>> {
        override fun parse(input: String, index: Int): Parsed<Pair<A, B>> =
            a.flatMap { aValue -> b.map { bValue -> Pair(aValue, bValue) } }.parse(input, index)
    }

    data class Either<A>(val ps: List<Parser<A>>) : Parser<A> {
        override fun parse(input: String, index: Int): Parsed<A> {
            fun loop(parserIndex: Int): Parsed<A> {
                return if (parserIndex >= ps.size) Parsed.Failure(index, this, input)
                else ps[parserIndex].parse(input, index).match({ it }, { loop(parserIndex + 1) })
            }
            return loop(0)
        }
    }

    data class Logged<A>(val p: Parser<A>, val name: String, val output: (String) -> Unit) : Parser<A> {
        override fun parse(input: String, index: Int): Parsed<A> {
            output("+$name:$index")
            val result = p.parse(input, index)
            result.match(
                { output("-$name:$index Success(:${it.index},'${input.substring(index, it.index)}')") },
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
