package parsek

object Intrinsics {
    // TODO optimize using a BitSet
    data class CharIn(val chars: Iterable<Char>) : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed {
            return if (index < ctx.input.length && ctx.input[index] in chars) {
                succeed(ctx, Unit, index + 1)
            } else {
                fail(ctx, index)
            }
        }
    }

    data class CharPred(val pred: (Char) -> Boolean) : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed {
            return if (index < ctx.input.length && pred(ctx.input[index])) {
                succeed(ctx, Unit, index + 1)
            } else {
                fail(ctx, index)
            }
        }
    }

    // TODO optimize using a BitSet
    data class WhileCharIn(val chars: Iterable<Char>, val min: Int) : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed {
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
    }
}
