package parsek

import kotlin.jvm.JvmName

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

interface Parser<out T> {
    fun parse(input: String, index: Int = 0): Parsed<T>
}

fun <T, R> Parser<T>.map(f: (T) -> R) = Combinators.Mapped(this, f)
fun <T, R> Parser<T>.flatMap(f: (T) -> Parser<R>): Parser<R> = Combinators.FlatMapped(this, f)
fun <T> Parser<T>.filter(pred: (T) -> Boolean): Parser<T> = Combinators.Filtered(this, pred)

fun P(c: Char): Parser<Unit> = Terminals.CharParser(c)
fun P(s: String): Parser<Unit> = Terminals.StringParser(s)
fun <A> P(p: () -> Parser<A>): Parser<A> = Combinators.Rule(p)

val Start: Parser<Unit> = Terminals.Start
val End: Parser<Unit> = Terminals.End

fun CharIn(chars: Iterable<Char>): Parser<Unit> = Intrinsics.CharIn(chars)
fun CharIn(chars: String): Parser<Unit> = Intrinsics.CharIn(chars.asIterable())
fun CharPred(pred: (Char) -> Boolean): Parser<Unit> = Intrinsics.CharPred(pred)
fun WhileCharIn(chars: Iterable<Char>, min: Int = 1): Parser<Unit> = Intrinsics.WhileCharIn(chars, min)
fun WhileCharIn(chars: String, min: Int = 1): Parser<Unit> = Intrinsics.WhileCharIn(chars.asIterable(), min)

fun Parser<Any?>.capture(): Parser<String> = Combinators.Capturing(this)

@JvmName("\$timesUU")
operator fun Parser<Unit>.times(b: Parser<Unit>): Parser<Unit> = Combinators.Seq(this, b).map { Unit }

@JvmName("\$timesUA")
operator fun <A> Parser<Unit>.times(b: Parser<A>): Parser<A> = Combinators.Seq(this, b).map { it.second }

@JvmName("\$timesAU")
operator fun <A> Parser<A>.times(b: Parser<Unit>): Parser<A> = Combinators.Seq(this, b).map { it.first }

@JvmName("\$timesAB")
operator fun <A, B> Parser<A>.times(b: Parser<B>): Parser<Pair<A, B>> = Combinators.Seq(this, b)

operator fun <A> Parser<A>.plus(b: Parser<A>): Parser<A> = Combinators.Either(listOf(this, b))

operator fun <A> Parser<A>.not(): Parser<Unit> = Combinators.Not(this)

fun <A> Parser<A>.log(name: String, output: (String) -> Unit): Parser<A> = Combinators.Logged(this, name, output)

@JvmName("\$repU")
fun Parser<Unit>.rep(min: Int = 0, max: Int = Int.MAX_VALUE, sep: Parser<*> = Terminals.Pass): Parser<Unit> =
    Combinators.Repeat(this, min, max, sep).map { Unit }

@JvmName("\$repA")
fun <A> Parser<A>.rep(min: Int = 0, max: Int = Int.MAX_VALUE, sep: Parser<*> = Terminals.Pass): Parser<List<A>> =
    Combinators.Repeat(this, min, max, sep)

fun <A> Parser<A>.opt(): Parser<A?> = Combinators.Optional(this)

