package parsek

sealed class Parsed<out T> {
    abstract val index: Int

    /**
     * @param index The index *after* this parse completed.
     */
    data class Success<T>(val value: T, override val index: Int) : Parsed<T>() {
        override val isSuccess: Boolean get() = true
    }

    /**
     * @param index The index where this parse failed.
     */
    data class Failure(override val index: Int, val lastParser: Parser<*>, val input: String) : Parsed<Nothing>() {
        override val isSuccess: Boolean get() = false
        override fun toString(): String =
            "Parse error while processing $lastParser:\n$input\n${"^".padStart(index + 1)}"
    }

    abstract val isSuccess: Boolean
    val isFailure: Boolean get() = !isSuccess
}

fun <T> Parsed<T>.getOrFail(): Parsed.Success<T> = when (this) {
    is Parsed.Success -> this
    else -> error("Parse error: ${this}")
}

fun <T, R> Parsed<T>.match(
    onSuccess: (Parsed.Success<T>) -> R,
    onFailure: (Parsed.Failure) -> R
): R = when (this) {
    is Parsed.Success -> onSuccess(this)
    is Parsed.Failure -> onFailure(this)
}

fun <T, R> Parsed<T>.flatMap(f: (Parsed.Success<T>) -> Parsed<R>): Parsed<R> = match(f, { it })
fun <T, R> Parsed<T>.map(f: (T) -> R): Parsed<R> = this.match({ s -> s.map(f) }, { it })
fun <T, R> Parsed.Success<T>.map(f: (T) -> R): Parsed.Success<R> = Parsed.Success(f(value), index)

data class ParserCtx(val input: String) {
    val success = MutableSuccess()
    val failure = MutableFailure(input)
}

sealed class MutableParsed() {
    abstract fun <A> toResult(): Parsed<A>
}

data class MutableSuccess(
    var value: Any? = null,
    var index: Int = 0
) : MutableParsed() {
    override fun <A> toResult(): Parsed.Success<A> =
        Parsed.Success(value as A, index)
}

data class MutableFailure(
    val input: String,
    var index: Int = 0,
    var lastParser: Parser<*>? = null
) : MutableParsed() {
    override fun <A> toResult(): Parsed.Failure =
        Parsed.Failure(index, lastParser!!, input)
}
