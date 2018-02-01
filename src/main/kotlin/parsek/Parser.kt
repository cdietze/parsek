package parsek

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
        override fun toString(): String =
            "Parse error while processing $lastParser:\n$input\n${"^".padStart(index + 1)}"
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
data class ParserCtx(val input: String) {
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
        var index: Int = 0
    ) : MutableParseResult() {
        override fun <A> toResult(): ParseResult.Success<A> =
            ParseResult.Success(value as A, index)
    }

    data class MutableFailure(
        val input: String,
        var index: Int = 0,
        var lastParser: Parser<*>? = null
    ) : MutableParseResult() {
        override fun <A> toResult(): ParseResult.Failure =
            ParseResult.Failure(index, lastParser!!, input)
    }
}
