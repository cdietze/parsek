package parsek

object Terminals {
    /**
     * Succeeds when at the start of the input (i.e., index equals 0), fails otherwise.
     */
    object Start : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> =
            if (index == 0) Parsed.Success(Unit, index)
            else Parsed.Failure(index, this, input)
    }

    /**
     * Succeeds when at the end of the input (i.e., index equals `input.length`), fails otherwise.
     */
    object End : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> {
            return when {
                input.length == index -> Parsed.Success(Unit, index)
                else -> Parsed.Failure(index, this, input)
            }
        }
    }

    object Pass : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> = Parsed.Success(Unit, index)
    }

    object Fail : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> = Parsed.Failure(index, this, input)
    }

    data class StringParser(val s: String) : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> {
            return when {
                input.startsWith(s, index) -> Parsed.Success(Unit, index + s.length)
                else -> Parsed.Failure(index, this, input)
            }
        }
    }

    data class CharParser(val c: Char) : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> {
            // FIXME this will crash when index is beyond the end
            return if (input[index] == c) Parsed.Success(Unit, index + 1)
            else Parsed.Failure(index, this, input)
        }
    }
}
