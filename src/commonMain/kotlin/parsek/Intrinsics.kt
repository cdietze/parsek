package parsek

internal object Intrinsics {
    // TODO optimize using a BitSet
    data class CharIn(val chars: Iterable<Char>) : ParserImpl<Char>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            return if (index < ctx.input.length && ctx.input[index] in chars) {
                succeed(ctx, ctx.input[index], index + 1)
            } else {
                fail(ctx, index)
            }
        }

        // TODO (toString): Append [chars] in readable way
        override fun toString(): String = "CharIn(...)"
    }

    data class CharPred(val pred: (Char) -> Boolean) : ParserImpl<Char>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            return if (index < ctx.input.length && pred(ctx.input[index])) {
                succeed(ctx, ctx.input[index], index + 1)
            } else {
                fail(ctx, index)
            }
        }

        // TODO (toString): Append [pred] in readable way
        override fun toString(): String = "CharPred(...)"
    }

    // TODO optimize using a BitSet
    data class WhileCharIn(val chars: Iterable<Char>, val min: Int) : ParserImpl<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParseResult {
            var curIndex = index
            while (curIndex < ctx.input.length && ctx.input[curIndex] in chars) {
                curIndex++
            }
            return if (curIndex - index >= min) {
                succeed(ctx, Unit, curIndex)
            } else {
                fail(ctx, index)
            }
        }

        override fun toString(): String {
            // TODO (toString): Append [chars] in readable way
            // TODO (toString): Append non-default parameters
            return "WhileCharIn(...)"
        }
    }
}
