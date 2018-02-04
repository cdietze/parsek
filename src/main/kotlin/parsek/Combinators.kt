package parsek

import parsek.MutableParseResult.MutableFailure
import parsek.MutableParseResult.MutableSuccess

object Combinators {

    data class Mapped<A, out B>(val p: Parser<A>, val f: (A) -> B) : ParserImpl<B> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> r.apply { value = f(r.value as A) }
                is MutableFailure -> r
            }
        }

        override fun toString(): String = p.toString()
    }

    data class FlatMapped<A, out B>(val p: Parser<A>, val f: (A) -> Parser<B>) : ParserImpl<B> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> f(r.value as A).parseRec(ctx, r.index)
                is MutableFailure -> r
            }
        }

        override fun toString(): String = p.toString()
    }

    data class Filtered<A>(val p: Parser<A>, val pred: (A) -> Boolean) : ParserImpl<A> {
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

    data class Capturing(val p: Parser<*>) : ParserImpl<String> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> succeed(ctx, ctx.input.substring(index, r.index), r.index)
                is MutableFailure -> r
            }
        }

        override fun toString(): String = p.toString()
    }

    data class Seq<out A, out B>(val a: Parser<A>, val b: Parser<B>) : ParserImpl<Pair<A, B>> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val ra = a.parseRec(ctx, index)
            return when (ra) {
                is MutableSuccess -> {
                    val raIndex = ra.index
                    val raValue = ra.value
                    val raCut = ra.cut
                    // Shadow current results var before we call parseRec which may override its mutable content
                    @Suppress("NAME_SHADOWING", "UNUSED_VARIABLE")
                    val ra = null
                    val rb = b.parseRec(ctx, raIndex)
                    return when (rb) {
                        is MutableSuccess -> succeed(ctx, Pair(raValue, rb.value), rb.index, cut = raCut || rb.cut)
                        is MutableFailure -> rb.apply { cut = raCut }
                    }
                }
                is MutableFailure -> fail(ctx, index, cut = ra.cut)
            }
        }

        // FIXME: add parentheses to keep correct precedence, e.g. currently jsonExpr is printed as
        // `space * obj + array + string + P("true") + P("false") + P("null") + number * space`
        override fun toString(): String = "$a * $b"
    }

    data class Either<out A>(val ps: List<Parser<A>>) : ParserImpl<A> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            tailrec fun loop(parserIndex: Int): MutableParseResult {
                if (parserIndex >= ps.size) return fail(ctx, index)
                val parser = ps[parserIndex]
                val r = parser.parseRec(ctx, index)
                return when (r) {
                    is MutableSuccess -> r
                    is MutableFailure -> if (r.cut)
                        fail(ctx, index, cut = true) else
                        loop(parserIndex + 1)
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
    data class Not(val p: Parser<*>) : ParserImpl<Unit> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> fail(ctx, index)
                is MutableFailure -> succeed(ctx, Unit, index)
            }
        }

        override fun toString(): String = "$p.not()"
    }

    data class Rule<A>(override val name: String, val p: () -> Parser<A>) : NamedParser<A> {
        val pCache: Parser<A> by lazy(p)

        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult =
            pCache.parseRec(ctx, index)

        override fun toString(): String = name
    }

    data class Logged<A>(val p: Parser<A>, val name: String, val output: (String) -> Unit) : Parser<A> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val thisLogDepth = ctx.logDepth
            ctx.logDepth += 1
            val indent = "  ".repeat(thisLogDepth)
            output("$indent+$name:$index")
            val r = p.parseRec(ctx, index)
            ctx.logDepth = thisLogDepth
            when (r) {
                is MutableSuccess -> output(
                    "$indent-$name:$index Success(:${r.index},'${ctx.input.substring(index, r.index)}')"
                )
                is MutableFailure -> output(
                    "$indent-$name:$index Failure(:${r.index})"
                )
            }
            return r
        }

        override fun toString(): String = p.toString()
    }

    data class Optional<out A>(val p: Parser<A>) : ParserImpl<A?> {
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
    ) : ParserImpl<List<A>> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val result = mutableListOf<A>()
            var lastIndex = index
            tailrec fun loop(sep: Parser<*>, count: Int, cut: Boolean): MutableParseResult {
                if (count >= max) {
                    return succeed(ctx, result, lastIndex)
                }
                val sepResult = sep.parseRec(ctx, lastIndex)
                return when (sepResult) {
                    is MutableSuccess -> {
                        val sepResultIndex = sepResult.index
                        val sepResultCut = sepResult.cut
                        // Shadow current results var before we call parseRec which may override its mutable content
                        @Suppress("NAME_SHADOWING", "UNUSED_VARIABLE")
                        val sepResult = null
                        val parseResult = p.parseRec(ctx, sepResultIndex)
                        when (parseResult) {
                            is MutableSuccess -> {
                                result.add(parseResult.value as A)
                                lastIndex = parseResult.index
                                loop(separator, count + 1, cut || sepResultCut || parseResult.cut)
                            }
                            is MutableFailure -> {
                                if (min <= count) succeed(ctx, result, lastIndex, cut)
                                else fail(ctx, index, cut)
                            }
                        }
                    }
                    is MutableFailure -> {
                        if (min <= count) succeed(ctx, result, lastIndex, cut = cut)
                        else fail(ctx, index, cut = cut)
                    }
                }
            }
            return loop(sep = Terminals.Pass, count = 0, cut = false)
        }

        override fun toString(): String {
            // TODO (toString): Append non-default parameters
            return "$p.rep()"
        }
    }

    data class Cut<out A>(val p: Parser<A>) : ParserImpl<A> {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            val r = p.parseRec(ctx, index)
            return when (r) {
                is MutableSuccess -> r.apply { cut = true }
                is MutableFailure -> r
            }
        }

        override fun toString(): String = "$p.cut()"
    }
}
