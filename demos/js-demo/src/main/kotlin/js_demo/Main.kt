import kotlinx.html.dom.append
import kotlinx.html.js.button
import kotlinx.html.js.input
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.p
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import parsek.*
import kotlin.browser.document

val int: Parser<Int> = Rule("int") { (CharIn("+-").opt() * WhileCharIn("0123456789")).capture().map { it.toInt() } }

val parens: Parser<Int> = Rule("parens") { P("(") * addSub * P(")") }
val factor: Parser<Int> = Rule("factor") { int + parens }

val divMul: Parser<Int> = Rule("divMul") { (factor * ((P("*") + P("/")).capture() * factor).rep()).map(::eval) }
val addSub: Parser<Int> = Rule("addSub") { (divMul * ((P("+") + P("-")).capture() * divMul).rep()).map(::eval) }

val expr: Parser<Int> = Rule("expr") { addSub * End }

private fun eval(it: Pair<Int, List<Pair<String, Int>>>): Int {
    return it.second.fold(it.first, { acc, p ->
        when (p.first) {
            "+" -> acc + p.second
            "-" -> acc - p.second
            "*" -> acc * p.second
            "/" -> acc / p.second
            else -> error("unknown operatore: ${p.first}")
        }
    })
}

fun main(args: Array<String>) {
    println("Hi from parsek js-demo")
    document.body!!.append {
        val i: HTMLInputElement = input {
            placeholder = "Enter expression here"
            value = "2*3+4*(5+1)"
        }
        lateinit var output: HTMLElement
        button {
            +"Parse"
            onClickFunction = { event ->
                val s = i.value
                output.innerText = expr.parse(s).toString()
            }
        }
        output = p { }
    }
}
