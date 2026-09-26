package hammer

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResourceTest {
	private val directory = Files.createTempDirectory("resources").toFile().apply {
		resolve("words").mkdir()
		resolve("words/en.txt").writeText("apple\nbanana")
		resolve("empty.txt").writeText("")
		parentFile.resolve("outside.txt").writeText("secret")
	}

	@Test
	fun `the fake reads files inside its directory, as Hammer reads resources`() {
		host = FakeHammerHost(resourceDirectory = directory)
		assertEquals("apple\nbanana", resourceText("words/en.txt"))
		listOf("words", "empty.txt", "missing.txt", "../outside.txt", "words/../words/en.txt", "/words/en.txt", "").forEach {
			assertNull(resource(it), it)
		}
	}
}
