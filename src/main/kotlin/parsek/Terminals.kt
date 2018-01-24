package parsek

object Terminals {
    object End : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> {
            return when {
                input.length >= index -> Parsed.Success(Unit, index)
                else -> Parsed.Failure(index, this, input)
            }
        }
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
            return if (input[index] == c) Parsed.Success(Unit, index + 1)
            else Parsed.Failure(index, this, input)
        }
    }

    data class RegexParser(val re: Regex) : Parser<Unit> {
        override fun parse(input: String, index: Int): Parsed<Unit> {
            val result = re.find(input.substring(index))
            return when (result) {
                null -> Parsed.Failure(index, this, input)
                else -> Parsed.Success(Unit, index + result.value.length)
            }
        }
    }
}
