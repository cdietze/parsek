package parsek

import kotlin.jvm.JvmName

sealed class Parsed<out T> {
    /**
     * @param index The index *after* this parse completed.
     */
    data class Success<T>(val value: T, val index: Int) : Parsed<T>() {
        override val isSuccess: Boolean get() = true
    }

    /**
     * @param index The index where this parse failed.
     */
    data class Failure(val index: Int, val lastParser: Parser<*>, val input: String) : Parsed<Nothing>() {
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

interface Parser<out T> {
    fun parse(input: String, index: Int = 0): Parsed<T>
}

fun <T, R> Parser<T>.flatMap(f: (T) -> Parser<R>): Parser<R> = object : Parser<R> {
    override fun parse(input: String, index: Int): Parsed<R> {
        val parsed = this@flatMap.parse(input, index)
        return when (parsed) {
            is Parsed.Success -> f(parsed.value).parse(input, parsed.index)
            is Parsed.Failure -> parsed
        }
    }
}

fun <T, R> Parser<T>.map(f: (T) -> R) = object : Parser<R> {
    override fun parse(input: String, index: Int): Parsed<R> {
        val parsed = this@map.parse(input, index)
        return when (parsed) {
            is Parsed.Success -> parsed.map(f)
            is Parsed.Failure -> parsed
        }
    }
}

fun p(s: String): Parser<Unit> = Terminals.StringParser(s)
fun p(c: Char): Parser<Unit> = Terminals.CharParser(c)
fun p(r: Regex): Parser<Unit> = Terminals.RegexParser(r)

val End: Parser<Unit> = Terminals.End

fun Parser<Any?>.capture(): Parser<String> = Combinators.Capturing(this)

@JvmName("\$timesUU")
operator fun Parser<Unit>.times(b: Parser<Unit>): Parser<Unit> = Combinators.sequence(this, b).map { Unit }

@JvmName("\$timesUA")
operator fun <A> Parser<Unit>.times(b: Parser<A>): Parser<A> = Combinators.sequence(this, b).map { it.second }

@JvmName("\$timesAU")
operator fun <A> Parser<A>.times(b: Parser<Unit>): Parser<A> = Combinators.sequence(this, b).map { it.first }

@JvmName("\$timesAB")
operator fun <A, B> Parser<A>.times(b: Parser<B>): Parser<Pair<A, B>> = Combinators.sequence(this, b)

operator fun <A> Parser<A>.plus(b: Parser<A>): Parser<A> = Combinators.either(listOf(this, b))

fun <A> Parser<A>.rep(sep: Parser<*> = Terminals.Pass): Parser<List<A>> = Combinators.Repeat(this, sep)
fun <A> Parser<A>.opt(): Parser<A?> = Combinators.Optional(this)

fun <A> P(p: () -> Parser<A>): Parser<A> = object : Parser<A> {
    override fun parse(input: String, index: Int): Parsed<A> = p().parse(input, index)
}
