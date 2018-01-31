package parsek

import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonPerfTest {
    val jsonString = File("src/test/resources/test.json").readText()

    val warmupCycles = 100
    val testCycles = 100

    @Test
    @Ignore
    fun `benchmark JSON parser`() {
        println("Running warmup")
        (1..warmupCycles).forEach {
            JsonParserTest.jsonExpr.parse(jsonString)
        }
        println("Running benchmark")
        val millis = measureTimeMillis {
            (1..testCycles).forEach {
                val result = JsonParserTest.jsonExpr.parse(jsonString)
                assertTrue(result.isSuccess)
            }
        }
        println("Ran $testCycles cycles in ${millis}ms")
    }

    @Test
    fun `should parse test file`() {
        val result = JsonParserTest.jsonExpr.parse(jsonString)
        assertTrue(result.isSuccess)
        assertEquals("Susan White", result.getOrFail().value[200]["friends"][1]["name"].value)
    }
}
