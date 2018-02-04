package parsek

/**
 * Extension interface of [Parser] with some utility functions that
 * should not be part of the public API.
 */
abstract class ParserImpl<out T> : Parser<T>() {

    fun <A> succeed(ctx: ParserCtx, value: A, index: Int, cut: Boolean = false): MutableParseResult.MutableSuccess {
        return ctx.success.apply {
            this.value = value
            this.index = index
            this.cut = cut
        }
    }

    fun fail(ctx: ParserCtx, index: Int, cut: Boolean = false): MutableParseResult.MutableFailure {
        return ctx.failure.apply {
            this.index = index
            this.lastParser = this@ParserImpl
            this.cut = cut
        }
    }
}

sealed class ParseResult<out T> {
    abstract val index: Int

    /**
     * @param index The index *after* this parse completed.
     */
    data class Success<T>(val value: T, override val index: Int) : ParseResult<T>() {
        override val isSuccess: Boolean get() = true
    }

    /**
     * @param index The index where this parse failed.
     */
    data class Failure(override val index: Int, val lastParser: Parser<*>, val input: String) :
        ParseResult<Nothing>() {
        override val isSuccess: Boolean get() = false
        // TODO: only print part of the input
        override fun toString(): String =
            "Failure at :$index, expected: $lastParser\n$input\n${"^".padStart(index + 1)}"
    }

    abstract val isSuccess: Boolean
    val isFailure: Boolean get() = !isSuccess
}

/**
 * If this [ParseResult] is a [ParseResult.Success], it is returned.
 * Otherwise, throws an [IllegalStateException].
 */
fun <T> ParseResult<T>.getOrFail(): ParseResult.Success<T> = when (this) {
    is ParseResult.Success -> this
    else -> error("Parse error: ${this}")
}

/**
 * Value object that is passed along during recursive [Parser.parseRec] calls.
 *
 * Contains constants that are valid during the whole parse (e.g., [input])
 * and mutable instances that may be reused to avoid object allocations.
 */
data class ParserCtx(
    /** The input string that is processed during this parse.
     * This does not change throughout this parse. */
    val input: String,
    /** The current log depth. Whenever a [Combinators.Logged] is parsed this is incremented by 1. */
    var logDepth: Int = 0
) {
    val success = MutableParseResult.MutableSuccess()
    val failure = MutableParseResult.MutableFailure(input)
}

/**
 * The mutable sibling of [ParseResult],
 * it this is used during recursive [Parser.parseRec] calls to avoid object allocations.
 */
sealed class MutableParseResult {
    abstract fun <A> toResult(): ParseResult<A>

    data class MutableSuccess(
        var value: Any? = null,
        var index: Int = 0,
        var cut: Boolean = false
    ) : MutableParseResult() {
        override fun <A> toResult(): ParseResult.Success<A> =
            ParseResult.Success(value as A, index)
    }

    data class MutableFailure(
        val input: String,
        var index: Int = 0,
        var cut: Boolean = false,
        var lastParser: Parser<*>? = null
    ) : MutableParseResult() {
        override fun <A> toResult(): ParseResult.Failure =
            ParseResult.Failure(index, lastParser!!, input)
    }
}
