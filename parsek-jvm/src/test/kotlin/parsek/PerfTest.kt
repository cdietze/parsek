package parsek

import org.junit.Ignore
import org.junit.Test
import kotlin.system.measureTimeMillis

class JsonPerfTest {

    val warmupCycles = 0
    val testCycles = 1

    @Ignore
    @Test
    fun `benchmark JSON parser`() {
        val jsonString = javaClass.getResource("/test.json").readText()
        println("Running warmup")
        (1..warmupCycles).forEach {
            JsonParserTest.jsonExpr.parse(jsonString)
        }
        println("Running benchmark")
        val millis = measureTimeMillis {
            (1..testCycles).forEach {
                JsonParserTest.jsonExpr.parse(jsonString)
            }
        }
        println("Ran $testCycles cycle in ${millis}ms")
    }
}
