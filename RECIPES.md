# Plugin recipes

Hello-world versions of each kind of Hammer plugin, in Kotlin. Every recipe is part of one working
plugin, [`kotlin/hello`](kotlin/hello), so they build and are tested; copy the parts you need.
hammer-editor's `docs/CLIENT-PLUGINS.md` has the full rules for each.

## A new plugin

A plugin is a `.hammerplugin` package: `manifest.toml`, the compiled module, and optionally
`settings.toml`, translations in `locales/`, and files to read at run time in `resources/`. In `kotlin/`:

1. Make a directory, say `kotlin/greeter`, with `build.gradle.kts`:

   ```kotlin
   plugins {
   	id("hammer.plugin")
   }
   ```

2. Add `"greeter"` to the `include(...)` in `kotlin/settings.gradle.kts`.
3. Write `manifest.toml`. Every plugin starts the same way; the recipes below add to it.

   ```toml
   id = "greeter"          # lowercase, and what its settings and exports are named after
   name = "Greeter"
   version = "0.1.0"
   api = 1

   [permissions]
   operations = ["scene.tree", "scene.read"]  # what it may call; see Calling Hammer
   ```

4. Put the plugin's logic in `src/commonMain/kotlin`, where JVM tests can reach it, and its exports in
   `src/wasmWasiMain/kotlin/Exports.kt`.
5. Build with `gradle :greeter:hammerPlugin`, which writes `greeter/build/greeter.hammerplugin`, and
   install it in Hammer from Settings, Plugins.

A module has one export of each kind (`export`, `action`, `diagnose`, `command`), so a plugin with two
actions tells them apart by name in its one `action` export. `kotlin/hello`'s
[Exports.kt](kotlin/hello/src/wasmWasiMain/kotlin/Exports.kt) wires up every recipe here.

## Settings

`settings.toml` declares settings Hammer shows as a form in Settings, Plugins; every request carries
their values. Types are `bool`, `int` (with `min` and `max`), `string` (with `multiline`), and `choice`
(with `options`).

```toml
[[setting]]
key = "greeting"
type = "string"
label = "Greeting"
default = "Hello"
```

Read them into a class with a property for each:

```kotlin
@Serializable
class HelloSettings(val greeting: String)

val greeting = request.settings<HelloSettings>().greeting
```

## An export format

Adds a format to the Export dialog. The export gets the story's chapters and returns the file's bytes.

```toml
[[exporters]]
format = "hello.txt"   # starts with the plugin's id
extension = "txt"
mime = "text/plain"
label = "Hello (TXT)"
```

```kotlin
fun helloExport(request: ExportRequest): String {
	val greeting = request.settings<HelloSettings>().greeting
	return buildString {
		appendLine("$greeting, ${request.projectName}!")
		request.chapters.forEach { appendLine("${it.name}: ${it.scenes.size} scenes") }
	}
}

@WasmExport
fun export(): Int = exporter { helloExport(it).encodeToByteArray() }
```

Set `input = "prose"` on the exporter to get each scene as parsed blocks instead of markdown.

## An action with a message

Adds an item to Project Home's menu. Whatever it returns is shown briefly.

```toml
[[actions]]
name = "greet"
label = "Say hello"
```

```kotlin
fun greet(request: ActionRequest): String = "${request.settings<HelloSettings>().greeting}, ${request.project}!"

@WasmExport
fun action(): Int = action { request ->
	when (request.action) {
		"greet" -> greet(request)
		else -> null
	}
}
```

## An action with a document, calling Hammer

`output = "document"` shows the returned markdown in a dialog the user can copy or save as a note. The
action reads the project through Hammer's operations, which the manifest must ask for.

```toml
[[actions]]
name = "outline"
label = "Hello outline"
output = "document"
```

```kotlin
@Serializable
class SceneTree(val nodes: List<Node>) {
	@Serializable
	class Node(val id: Int, val name: String, val kind: String, val children: List<Node> = emptyList())
}

fun outline(request: ActionRequest): String = buildString {
	appendLine("# ${request.project}")
	fun write(nodes: List<SceneTree.Node>, depth: Int) {
		nodes.forEach { node ->
			appendLine("  ".repeat(depth) + "- ${node.name}")
			write(node.children, depth + 1)
		}
	}
	write(call<SceneTree>("scene.tree", buildJsonObject { put("project", request.project) }).nodes, 0)
}
```

## An action with fields, on more screens, reporting progress

`places` puts the action on other screens' menus as well: `project`, `scene`, `note`, `entry`, and
`event`. `request.context` says which it was run from and the id of the item there. `[[actions.field]]`
tables are asked for first, in a dialog: the settings types, or `scenes`, picked from the project's tree.
A long action calls `progress`, which Hammer shows with a Stop button; when the user stops it, the call
never returns.

```toml
[[actions]]
name = "count"
label = "Count words"
output = "document"
places = ["project", "scene"]

[[actions.field]]
key = "scenes"
type = "scenes"          # a list of scene ids; `multiple = false` for one, `required = true` to insist
label = "Scenes"
hint = "Leave empty for the whole story, or the scene you are in."
```

```kotlin
@Serializable
class CountInput(val scenes: List<Int> = emptyList())

// tree() and flatten() are small helpers in Hello.kt, reading scene.tree as outline does.
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
```

## An action with buttons

`output = "interactive"` returns an `ActionReply`: markdown shown with buttons under it. Pressing one
calls the action again with the same input and the button's id in `request.button`. A reply with
markdown updates the dialog, and one without closes it; a `message` is shown either way. Anything the
next call needs, such as a random seed, can ride in the button's id.

```toml
[[actions]]
name = "wave"
label = "Wave"
output = "interactive"
```

```kotlin
fun wave(request: ActionRequest): ActionReply = when (request.button) {
	null -> ActionReply(
		markdown = "Hello from **${request.project}**. Wave back?",
		buttons = listOf(ActionReply.Button("once", "Wave"), ActionReply.Button("twice", "Wave twice")),
	)
	"twice" -> ActionReply(message = "You waved twice")
	else -> ActionReply(message = "You waved")
}

// In the action export's `when`:
"wave" -> wave(request).toJson()
```

## Resources

Files in the plugin's `resources/` directory, such as word lists and dictionaries, go into the package
as they are, and the plugin reads them by their path there. Up to 1,000 files and 64 MiB in all. Hammer
reads a resource from the package each time it is asked for, so keep one you use often. Hello's
[resources/tips.txt](kotlin/hello/resources/tips.txt) has a tip a line:

```toml
[[actions]]
name = "tip"
label = "Writing tip"
```

```kotlin
fun tip(): String = resourceText("tips.txt").orEmpty().lines().filter { it.isNotBlank() }.randomOrNull() ?: "No tips today"
```

`resource(name)` gives the bytes, and both give null for a file that isn't there. In JVM tests,
`FakeHammerHost` reads them from the plugin's own `resources/`. C has `hammer_resource_load`, with
`hammer_alloc_keep` to keep one between calls, and Rust has `hammer::resource`.

## A text check

`[[diagnostics]]` checks the text as it is written: the editor sends the paragraphs that changed, and
underlines what comes back, with any fixes in the right-click menu. Ranges are indexes into each
paragraph's String; the kit turns them into what Hammer reads.

```toml
[[diagnostics]]
name = "very"
label = "Very"
```

```kotlin
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

@WasmExport
fun diagnose(): Int = diagnose(::findVery)
```

A fix shows as its replacement unless it has a label, which it needs when the replacement is empty.
Kotlin/Wasm's `Regex` is slow in Hammer's interpreter, so text checks search by hand.

## A command-line command

`[[commands]]` adds `hammer <name>` on desktop. Hammer hands the command each line of standard input,
and writes what it returns as a line of output.

```toml
[[commands]]
name = "hello"
help = "Greets each line of standard input."
```

```kotlin
fun helloCommand(request: CommandRequest): String = "${request.settings<HelloSettings>().greeting}, ${request.line}!"

@WasmExport
fun command(): Int = command(::helloCommand)
```

`echo Alice | hammer hello` prints `Hello, Alice!`.

## Translations

A package translates its declared words, the manifest's and `settings.toml`'s, in
`locales/<tag>.toml` files beside the manifest, keyed as the manifest keys them. Hammer picks the most
specific one for the UI's language (`fr-CA`, then `fr`) and falls back to the manifest for anything
left out. Hello's [locales/fr.toml](kotlin/hello/locales/fr.toml):

```toml
name = "Bonjour"

[actions.count]
label = "Compter les mots"

[actions.count.fields.scenes]
label = "Scènes"
hint = "Laissez vide pour toute l’histoire, ou la scène où vous êtes."

[settings.greeting]
label = "Salutation"
```

What the plugin writes at run time is its own to translate: every request carries `locale`, the UI's
language tag, which is not the project's `language`, the one the story is written in.

```kotlin
val french = request.locale?.startsWith("fr") == true
ActionReply(message = if (french) "Vous avez salué" else "You waved")
```

## Calling Hammer

`call<T>(operation, input)` runs one of Hammer's operations and reads its output; `callJson` gives the
JSON as it is. A failure throws `OperationException`, and one the plugin lets escape is shown to the
user as its message. The manifest's `permissions.operations` lists what a plugin may call: operation
names, or a scope such as `content:read`. Deletes are only ever granted by name. `hammer help` lists
every operation, and `ops.list` describes them.

The plugin's cache, `cacheGet` and `cacheSet`, keeps bytes between calls and runs, such as results
worth not working out again; Hammer may drop any of it at any time.

## Testing

Logic in `commonMain` runs in JVM tests, with `FakeHammerHost` answering operations and recording
progress:

```kotlin
host = FakeHammerHost(mapOf("scene.read" to { _ -> buildJsonObject { put("markdown", "Rain fell hard.") } }))
```

`gradle :hello:jvmTest` runs [HelloTest](kotlin/hello/src/jvmTest/kotlin/HelloTest.kt).
