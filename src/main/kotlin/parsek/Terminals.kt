package parsek

object Terminals {
    /**
     * Succeeds when at the start of the input (i.e., index equals 0), fails otherwise.
     */
    object Start : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed =
            if (index == 0) succeed(ctx, Unit, index)
            else fail(ctx, index)
    }

    /**
     * Succeeds when at the end of the input (i.e., index equals `input.length`), fails otherwise.
     */
    object End : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed =
            if (index == ctx.input.length) succeed(ctx, Unit, index)
            else fail(ctx, index)
    }

    object Pass : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed = succeed(ctx, Unit, index)
    }

    object Fail : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed = fail(ctx, index)
    }

    data class StringParser(val s: String) : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed =
            if (ctx.input.startsWith(s, index)) succeed(ctx, Unit, index + s.length)
            else fail(ctx, index)
    }

    data class CharParser(val c: Char) : Parser<Unit>() {
        override fun parseRec(ctx: ParserCtx, index: Int): MutableParsed =
            if (ctx.input[index] == c) succeed(ctx, Unit, index + 1)
            else fail(ctx, index)
    }
}
