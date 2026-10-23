// One small example of each kind of plugin, for RECIPES.md. Each function is an export's body; the
// wasm exports in Exports.kt pass it the request.
import hammer.ActionReply
import hammer.ActionReply.Button
import hammer.ActionReply.Field
import hammer.ActionReply.Page
import hammer.ActionRequest
import hammer.CommandRequest
import hammer.DiagnoseRequest
import hammer.Diagnostic
import hammer.EntityRef
import hammer.ExportRequest
import hammer.Fix
import hammer.input
import hammer.ops.Hammer
import hammer.ops.SceneKind
import hammer.ops.SceneNode
import hammer.progress
import hammer.resourceText
import hammer.settings
import hammer.state
import hammer.stateOf
import hammer.words
import kotlinx.serialization.Serializable
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

// An action that replies with a message: a line the user sees briefly.
fun greet(request: ActionRequest): ActionReply = ActionReply.message("${request.settings<HelloSettings>().greeting}, ${request.project}!")

private fun SceneNode.flatten(): List<SceneNode> = listOf(this) + children.flatMap { it.flatten() }

// An action that replies with a document: markdown the user can copy or save as a note. It calls
// Hammer's operations.
fun outline(request: ActionRequest): ActionReply = ActionReply.document(
	buildString {
		appendLine("# ${request.project}")
		fun write(nodes: List<SceneNode>, depth: Int) {
			nodes.forEach { node ->
				appendLine("  ".repeat(depth) + "- ${node.name}")
				write(node.children, depth + 1)
			}
		}
		write(Hammer.scene.tree(request.project).nodes, 0)
	},
)

@Serializable
class CountInput(val scenes: List<Int> = emptyList())

// An action with a field, run from Project Home or a scene, that reports its progress.
fun countWords(request: ActionRequest): ActionReply {
	val here = request.context.id.takeIf { request.context.place == "scene" }
	val chosen = request.input<CountInput>().scenes.ifEmpty { listOfNotNull(here) }
	val scenes = Hammer.scene.tree(request.project).nodes.flatMap { it.flatten() }
		.filter { it.kind == SceneKind.Scene && (chosen.isEmpty() || it.id in chosen) }
	val counts = buildString {
		appendLine("# Word counts")
		scenes.forEachIndexed { i, scene ->
			progress(i.toDouble() / scenes.size, "Counting ${scene.name}")
			val text = Hammer.scene.read(request.project, scene.id)
			appendLine("- ${scene.name}: ${text.markdown.split(' ', '\n', '\t').count { it.isNotBlank() }}")
		}
	}
	return ActionReply.document(counts)
}

@Serializable
class AboutInput(val who: EntityRef)

// An action with an entity field: a character the user picks by name, read with entry.read.
fun about(request: ActionRequest): ActionReply {
	val who = request.input<AboutInput>().who
	val entry = Hammer.entry.read(request.project, who.id)
	val scenes = entry.referencedBy.size
	return ActionReply.document(
		buildString {
			appendLine("# ${entry.name}")
			if (entry.aliases.isNotEmpty()) appendLine("Also known as ${entry.aliases.joinToString()}.")
			appendLine("In ${if (scenes == 1) "1 scene" else "$scenes scenes"}.")
		},
	)
}

// Reads a file the package ships in resources/: one tip a line, picked at random.
fun tip(): ActionReply =
	ActionReply.message(resourceText("tips.txt").orEmpty().lines().filter { it.isNotBlank() }.randomOrNull() ?: "No tips today")

@Serializable
class WaveName(val name: String)

@Serializable
class WaveTimes(val times: Int)

@Serializable
class Wave(val name: String)

// An action of two pages. The first asks for a name, which the plugin checks itself, answering with
// errors when it refuses one. The second carries the name in its state, and has Back, which skips
// Hammer's checks of its field, and Wave, which ends the action with a message. What it writes itself is
// in the UI's language, where it has one.
fun wave(request: ActionRequest): ActionReply? = when (request.button) {
	null, "back" -> ActionReply(
		Page(
			markdown = request.say("Hello from **${request.project}**. Who is waving?", "Bonjour de **${request.project}**. Qui salue ?"),
			fields = listOf(
				Field.text("name", request.say("Your name", "Votre nom"), required = true, default = request.state<Wave>()?.name),
			),
			buttons = listOf(Button("next", request.say("Next", "Suivant"), primary = true)),
		),
	)
	"next" -> {
		val name = request.input<WaveName>().name.trim()
		if (name.any { it.isDigit() }) {
			ActionReply(errors = mapOf("name" to request.say("A name has no digits", "Un nom n’a pas de chiffres")))
		} else {
			ActionReply(
				Page(
					markdown = request.say("Hello, **$name**. Wave back?", "Bonjour, **$name**. Rendre le salut ?"),
					fields = listOf(Field.int("times", request.say("Waves", "Saluts"), min = 1, max = 3)),
					buttons = listOf(
						Button("back", request.say("Back", "Retour"), validate = false),
						Button("wave", request.say("Wave", "Saluer"), primary = true),
					),
					state = stateOf(Wave(name)),
				),
			)
		}
	}
	"wave" -> {
		val name = request.state<Wave>()?.name.orEmpty()
		val times = request.input<WaveTimes>().times
		ActionReply.message(
			if (times == 1) request.say("$name waved", "$name a salué")
			else request.say("$name waved $times times", "$name a salué $times fois"),
		)
	}
	else -> null
}

private fun ActionRequest.say(english: String, french: String) = if (locale?.startsWith("fr") == true) french else english

// A text check: underlines each "very", offering to remove it. Kotlin/Wasm's Regex is slow in Hammer's
// interpreter, so it searches by hand.
fun findVery(request: DiagnoseRequest): List<Diagnostic> = request.paragraphs.flatMapIndexed { paragraph, text ->
	text.words().filter { it.text.equals("very", ignoreCase = true) }.map { word ->
		val end = if (text.getOrNull(word.end) == ' ') word.end + 1 else word.end
		Diagnostic(paragraph, word.start, end, "“Very” rarely helps", listOf(Fix("", "Remove “very”")))
	}
}

// A CLI command: one line of standard output for each line of input.
fun helloCommand(request: CommandRequest): String = "${request.settings<HelloSettings>().greeting}, ${request.line}!"
