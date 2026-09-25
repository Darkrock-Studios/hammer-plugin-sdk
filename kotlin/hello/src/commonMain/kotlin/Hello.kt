// One small example of each kind of plugin, for RECIPES.md. Each function is an export's body; the
// wasm exports in Exports.kt pass it the request.
import hammer.ActionReply
import hammer.ActionRequest
import hammer.CommandRequest
import hammer.DiagnoseRequest
import hammer.Diagnostic
import hammer.ExportRequest
import hammer.Fix
import hammer.call
import hammer.input
import hammer.progress
import hammer.settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
class HelloSettings(val greeting: String)

// An export format: the file's text.
fun helloExport(request: ExportRequest): String {
	val greeting = request.settings<HelloSettings>().greeting
	return buildString {
		appendLine("$greeting, ${request.projectName}!")
		request.chapters.forEach { appendLine("${it.name}: ${it.scenes.size} scenes") }
	}
}

// A `message` action: a line the user sees briefly.
fun greet(request: ActionRequest): String = "${request.settings<HelloSettings>().greeting}, ${request.project}!"

@Serializable
class SceneTree(val nodes: List<Node>) {
	@Serializable
	class Node(val id: Int, val name: String, val kind: String, val children: List<Node> = emptyList())
}

private fun tree(project: String) = call<SceneTree>("scene.tree", buildJsonObject { put("project", project) })

private fun SceneTree.Node.flatten(): List<SceneTree.Node> = listOf(this) + children.flatMap { it.flatten() }

// A `document` action: markdown the user can copy or save as a note. It calls Hammer's operations.
fun outline(request: ActionRequest): String = buildString {
	appendLine("# ${request.project}")
	fun write(nodes: List<SceneTree.Node>, depth: Int) {
		nodes.forEach { node ->
			appendLine("  ".repeat(depth) + "- ${node.name}")
			write(node.children, depth + 1)
		}
	}
	write(tree(request.project).nodes, 0)
}

@Serializable
class CountInput(val scenes: List<Int> = emptyList())

@Serializable
private class SceneText(val markdown: String)

// An action with a field, run from Project Home or a scene, that reports its progress.
fun countWords(request: ActionRequest): String {
	val here = request.context.id.takeIf { request.context.place == "scene" }
	val chosen = request.input<CountInput>().scenes.ifEmpty { listOfNotNull(here) }
	val scenes = tree(request.project).nodes.flatMap { it.flatten() }
		.filter { it.kind == "scene" && (chosen.isEmpty() || it.id in chosen) }
	return buildString {
		appendLine("# Word counts")
		scenes.forEachIndexed { i, scene ->
			progress(i.toDouble() / scenes.size, "Counting ${scene.name}")
			val text = call<SceneText>("scene.read", buildJsonObject {
				put("project", request.project)
				put("id", scene.id)
			})
			appendLine("- ${scene.name}: ${text.markdown.split(' ', '\n', '\t').count { it.isNotBlank() }}")
		}
	}
}

// An `interactive` action: markdown with buttons, each calling the action again with its id. What it
// writes itself is in the UI's language, where it has one.
fun wave(request: ActionRequest): ActionReply {
	val french = request.locale?.startsWith("fr") == true
	return when (request.button) {
		null -> ActionReply(
			markdown = if (french) "Bonjour de **${request.project}**. Rendre le salut ?" else "Hello from **${request.project}**. Wave back?",
			buttons = listOf(
				ActionReply.Button("once", if (french) "Saluer" else "Wave"),
				ActionReply.Button("twice", if (french) "Saluer deux fois" else "Wave twice"),
			),
		)
		"twice" -> ActionReply(message = if (french) "Vous avez salué deux fois" else "You waved twice")
		else -> ActionReply(message = if (french) "Vous avez salué" else "You waved")
	}
}

// A text check: underlines each "very", offering to remove it. Kotlin/Wasm's Regex is slow in Hammer's
// interpreter, so it searches by hand.
fun findVery(request: DiagnoseRequest): List<Diagnostic> = request.paragraphs.flatMapIndexed { paragraph, text ->
	val found = mutableListOf<Diagnostic>()
	var at = text.indexOf("very", ignoreCase = true)
	while (at >= 0) {
		val end = at + 4
		val word = (at == 0 || !text[at - 1].isLetter()) && (end == text.length || !text[end].isLetter())
		if (word) {
			val space = if (end < text.length && text[end] == ' ') 1 else 0
			found += Diagnostic(paragraph, at, end + space, "“Very” rarely helps", listOf(Fix("", "Remove “very”")))
		}
		at = text.indexOf("very", end, ignoreCase = true)
	}
	found
}

// A CLI command: one line of standard output for each line of input.
fun helloCommand(request: CommandRequest): String = "${request.settings<HelloSettings>().greeting}, ${request.line}!"
