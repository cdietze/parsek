package parsek

import parsek.MutableParseResult.MutableFailure
import parsek.MutableParseResult.MutableSuccess

object Combinators {

    data class Mapped<A, out B>(val p: Parser<A>, val f: (A) -> B) : Parser<B>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> succeed(ctx, f(r.value as A), r.index)
                is MutableFailure -> r
            }
        }

        override fun toString(): String = p.toString()
    }

    data class FlatMapped<A, out B>(val p: Parser<A>, val f: (A) -> Parser<B>) : Parser<B>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> f(r.value as A).parseRec(ctx, r.index)
                is MutableFailure -> r
            }
        }

        override fun toString(): String = p.toString()
    }

    data class Filtered<A>(val p: Parser<A>, val pred: (A) -> Boolean) : Parser<A>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> if (pred(r.value as A)) r else fail(ctx, index)
                is MutableFailure -> r
            }
        }

        // TODO (toString): Append [pred] in readable way
        override fun toString(): String = "$p.filter(...)"
    }

    data class Capturing(val p: Parser<*>) : Parser<String>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> succeed(ctx, ctx.input.substring(index, r.index), r.index)
                is MutableFailure -> r
            }
        }

        override fun toString(): String = p.toString()
    }

    data class Seq<out A, out B>(val a: Parser<A>, val b: Parser<B>) : Parser<Pair<A, B>>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val ra = a.parseRec(ctx, index)
            return when (ra) {
                is MutableSuccess -> {
                    val raValue = ra.value
                    val rb = b.parseRec(ctx, ra.index)
                    return when (rb) {
                        is MutableSuccess -> succeed(ctx, Pair(raValue, rb.value), rb.index)
                        is MutableFailure -> rb
                    }
                }
                is MutableFailure -> fail(ctx, index)
            }
        }

        override fun toString(): String = "$a * $b"
    }

    data class Either<out A>(val ps: List<Parser<A>>) : Parser<A>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            tailrec fun loop(parserIndex: Int): MutableParseResult {
                if (parserIndex >= ps.size) return fail(ctx, index)
                val parser = ps[parserIndex]
                val r = parser.parseRec(ctx, index)
                return when (r) {
                    is MutableSuccess -> r
                    is MutableFailure -> loop(parserIndex + 1)
                }
            }
            return loop(0)
        }

        override fun toString(): String = ps.joinToString(" + ")
    }

    /**
     * Wraps another parser and succeeds if it fails and fails if it succeeds.
     * Does not consume any input in either case.
     */
    data class Not(val p: Parser<*>) : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> fail(ctx, index)
                is MutableFailure -> succeed(ctx, Unit, index)
            }
        }

        override fun toString(): String = "$p.not()"
    }

    data class Rule<A>(val name: String, val p: () -> Parser<A>) : Parser<A>() {
        val pCache: Parser<A> by lazy(p)

        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult =
            pCache.parseRec(ctx, index)

        override fun toString(): String = name
    }

    data class Logged<A>(val p: Parser<A>, val name: String, val output: (String) -> Unit) : Parser<A>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            output("+$name:$index")
            val r = p.parseRec(ctx, index)
            when (r) {
                is MutableSuccess -> output(
                    "-$name:$index Success(:${r.index},'${ctx.input.substring(index, r.index)}')"
                )
                is MutableFailure -> output(
                    "-$name:$index Failure"
                )
            }
            return r
        }

        override fun toString(): String = p.toString()
    }

    data class Optional<out A>(val p: Parser<A>) : Parser<A?>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> r
                is MutableFailure -> succeed(ctx, null, index)
            }
        }

        override fun toString(): String = "$p.opt()"
    }

    data class Repeat<out A>(
        val p: Parser<A>,
        val min: Int,
        val max: Int,
        val separator: Parser<*>
    ) : Parser<List<A>>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val result = mutableListOf<A>()
            var lastIndex = index
            tailrec fun loop(sep: Parser<*>, count: Int): MutableParseResult {
                if (count >= max) {
                    return succeed(ctx, result, lastIndex)
                }
                val sepResult = sep.parseRec(ctx, lastIndex)
                return when (sepResult) {
                    is MutableSuccess -> {
                        val parseResult = p.parseRec(ctx, sepResult.index)
                        when (parseResult) {
                            is MutableSuccess -> {
                                result.add(parseResult.value as A)
                                lastIndex = parseResult.index
                                loop(separator, count + 1)
                            }
                            is MutableFailure -> {
                                if (min <= count) succeed(ctx, result, lastIndex)
                                else fail(ctx, index)
                            }
                        }
                    }
                    is MutableFailure -> {
                        if (min <= count) succeed(ctx, result, lastIndex)
                        else fail(ctx, index)
                    }
                }
            }
            return loop(Terminals.Pass, 0)
        }

        override fun toString(): String {
            // TODO (toString): Append non-default parameters
            return "$p.rep()"
        }
    }
}
