package parsek

object Intrinsics {
    // TODO optimize using a BitSet
    data class CharIn(val chars: Iterable<Char>) : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> {
            return if (index < input.length && input[index] in chars) {
                Parsed.Success(Unit, index + 1)
            } else {
                Parsed.Failure(index, this, input)
            }
        }
    }

    data class CharPred(val pred: (Char) -> Boolean) : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> {
            return if (index < input.length && pred(input[index])) {
                Parsed.Success(Unit, index + 1)
            } else {
                Parsed.Failure(index, this, input)
            }
        }
    }

    // TODO optimize using a BitSet
    data class WhileCharIn(val chars: Iterable<Char>, val min: Int) : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> {
            var curIndex = index
            while (curIndex < input.length && input[curIndex] in chars) {
                curIndex++
            }
            return if (curIndex - index >= min) {
                Parsed.Success(Unit, curIndex)
            } else {
                Parsed.Failure(index, this, input)
            }
        }
    }
}
