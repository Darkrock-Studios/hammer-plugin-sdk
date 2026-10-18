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
   description = "Greets each scene."  # shown when installing and from Info; links are clickable
   # languages = ["en"]   # the project languages it works with, if it depends on them

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
(with `options`). An action's fields are much the same, with `text` in place of `string`.

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

Adds an item to Project Home's menu. Every action replies with an `ActionReply`, and
`ActionReply.message` is a line shown briefly as the action ends.

```toml
[[actions]]
name = "greet"
label = "Say hello"
```

```kotlin
fun greet(request: ActionRequest): ActionReply = ActionReply.message("${request.settings<HelloSettings>().greeting}, ${request.project}!")

@WasmExport
fun action(): Int = action { request ->
	when (request.action) {
		"greet" -> greet(request)
		else -> null
	}
}
```

Returning null ends the action without a word.

## An action with a document, calling Hammer

`ActionReply.document` shows markdown in the action's dialog, where the user can copy it or save it as
a note. The action reads the project through Hammer's operations, which the manifest must ask for.

```toml
[[actions]]
name = "outline"
label = "Hello outline"
```

```kotlin
@Serializable
class SceneTree(val nodes: List<Node>) {
	@Serializable
	class Node(val id: Int, val name: String, val kind: String, val children: List<Node> = emptyList())
}

fun outline(request: ActionRequest): ActionReply = ActionReply.document(
	buildString {
		appendLine("# ${request.project}")
		fun write(nodes: List<SceneTree.Node>, depth: Int) {
			nodes.forEach { node ->
				appendLine("  ".repeat(depth) + "- ${node.name}")
				write(node.children, depth + 1)
			}
		}
		write(call<SceneTree>("scene.tree", buildJsonObject { put("project", request.project) }).nodes, 0)
	},
)
```

## An action with fields, on more screens, reporting progress

`places` puts the action on other screens' menus as well: `project`, `scene`, `note`, `entry`, and
`event`. `request.context` says which it was run from and the id of the item there.

`[[actions.field]]` tables are the action's first page: Hammer shows them with a Run button and calls
the action with their values in `request.input`; an action without them is called as soon as it is
picked. Field types are:

| `type` | Value | Keys |
| --- | --- | --- |
| `text` | A string | `format`: `plain` (the default), or `rich` or `rich-extended` for the scene editor, whose value is markdown; `multiline` for plain |
| `bool` | true or false | |
| `int` | A number | `min`, `max` |
| `choice` | The picked option's `value` | `options`, a list of `{ value, label }` |
| `scenes` | A list of scene ids, picked from the project's tree | `multiple = false` for one id, or null |

Every field has a `key` and a `label`, and may have a `hint` and a `default`, its starting value;
`required = true` insists on a text that is not blank, or at least one scene. Hammer checks these, an
int's bounds, and a choice's options before calling. Action fields say `text` where settings say
`string`.

A long action calls `progress`, which Hammer shows with a Stop button; when the user stops it, the call
never returns.

```toml
[[actions]]
name = "count"
label = "Count words"
places = ["project", "scene"]

[[actions.field]]
key = "scenes"
type = "scenes"
label = "Scenes"
hint = "Leave empty for the whole story, or the scene you are in."
```

```kotlin
@Serializable
class CountInput(val scenes: List<Int> = emptyList())

// tree() and flatten() are small helpers in Hello.kt, reading scene.tree as outline does.
fun countWords(request: ActionRequest): ActionReply {
	val here = request.context.id.takeIf { request.context.place == "scene" }
	val chosen = request.input<CountInput>().scenes.ifEmpty { listOfNotNull(here) }
	val scenes = tree(request.project).nodes.flatMap { it.flatten() }
		.filter { it.kind == "scene" && (chosen.isEmpty() || it.id in chosen) }
	val counts = buildString {
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
	return ActionReply.document(counts)
}
```

## Pages, buttons, and state

A reply's `Page` is what the action's dialog shows next: a `title`, `markdown`, `fields` made with
`Field.text`, `bool`, `int`, `choice`, and `scenes` (the same as the manifest's), and `buttons`. Pressing
a button calls the action again with its id in `request.button` and the page's values in
`request.input`. A `primary` button is the one to press; an `inline` one sits under the markdown, for
acting on something in it, and the rest sit in the dialog's footer.

A module keeps nothing between calls, so a page carries what the next call needs in its `state`:
`stateOf(value)` writes any `@Serializable` value, and `request.state<T>()` reads it back from the next
request, giving null when there is none or it no longer reads as a `T`. Hammer never reads it, and
fails a call whose state is over 1 MiB.

Hammer checks a page's fields before calling, as it does the manifest's. Anything else is the plugin's
to check, and it answers with `errors`: messages by field key, or `""` for the whole page. The user
stays on the page with what they entered, and its state, to put it right. A button with
`validate = false`, such as Back or Cancel, skips Hammer's checks.

A reply with no `page`, `errors`, or `open` ends the action. `wave` asks for a name on one page and how
many waves on the next:

```toml
[[actions]]
name = "wave"
label = "Wave"
```

```kotlin
@Serializable
class WaveName(val name: String)

@Serializable
class WaveTimes(val times: Int)

@Serializable
class Wave(val name: String)

fun wave(request: ActionRequest): ActionReply? = when (request.button) {
	null, "back" -> ActionReply(
		Page(
			markdown = "Hello from **${request.project}**. Who is waving?",
			fields = listOf(Field.text("name", "Your name", required = true, default = request.state<Wave>()?.name)),
			buttons = listOf(Button("next", "Next", primary = true)),
		),
	)
	"next" -> {
		val name = request.input<WaveName>().name.trim()
		if (name.any { it.isDigit() }) {
			ActionReply(errors = mapOf("name" to "A name has no digits"))
		} else {
			ActionReply(
				Page(
					markdown = "Hello, **$name**. Wave back?",
					fields = listOf(Field.int("times", "Waves", min = 1, max = 3)),
					buttons = listOf(Button("back", "Back", validate = false), Button("wave", "Wave", primary = true)),
					state = stateOf(Wave(name)),
				),
			)
		}
	}
	"wave" -> {
		val name = request.state<Wave>()?.name.orEmpty()
		val times = request.input<WaveTimes>().times
		ActionReply.message(if (times == 1) "$name waved" else "$name waved $times times")
	}
	else -> null
}
```

`Page`, `Field`, and `Button` are `ActionReply.Page`, `ActionReply.Field`, and `ActionReply.Button`,
imported by those names. A page with markdown and no fields, as `ActionReply.document` makes, is offered
for copying and saving as a note, buttons or not. C writes the same replies with `hammer_action.h`'s
`hammer_put_message`, `hammer_put_document`, `hammer_put_page`, `hammer_put_errors`, and
`hammer_put_open`; Rust has `hammer::action`'s `Reply`, `Page`, `Field`, `Button`, and `Open`.

## Opening what it made

`open` ends the action and takes the user to an item: `ActionReply.Open(place, id)`, with a place as in
`places` and the id of the scene, note, entry, or event (none for `project`). A plugin that adds an
entry can open it, with a message saying so:

```kotlin
@Serializable
class Created(val id: Int)

val entry = call<Created>("entry.create", buildJsonObject {
	put("project", request.project)
	put("type", "person")
	put("name", name)
	put("text", text)
})
ActionReply(message = "Added $name", open = ActionReply.Open("entry", entry.id))
```

## A run the user can leave

An action whose manifest entry sets `keep = true` keeps its run when the user closes the dialog: opening
the action again from the same place brings back the page they were on and what they had entered, and
the next call has `request.resumed` set. The run ends as any other does, by a reply that ends the
action or opens something. Keep it for an action that asks for work worth keeping, such as answers to
an interview; a report or a list of names is better run fresh.

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
fun tip(): ActionReply =
	ActionReply.message(resourceText("tips.txt").orEmpty().lines().filter { it.isNotBlank() }.randomOrNull() ?: "No tips today")
```

`resource(name)` gives the bytes, and both give null for a file that isn't there. In JVM tests,
`FakeHammerHost` reads them from the plugin's own `resources/`. C has `hammer_resource_load`, with
`hammer_alloc_keep` to keep one between calls, and `hammer_resource_lines` for a list file (one entry a
line, `#` comments); Rust has `hammer::resource`.

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
description = "Un petit exemple de chaque sorte d’extension, tiré des recettes."

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
private fun ActionRequest.say(english: String, french: String) = if (locale?.startsWith("fr") == true) french else english

Button("back", request.say("Back", "Retour"), validate = false)
```

## Calling Hammer

`call<T>(operation, input)` runs one of Hammer's operations and reads its output; `callJson` gives the
JSON as it is. A failure throws `OperationException`, and one the plugin lets escape is shown to the
user as its message; an action can catch it and reply with `errors` instead, keeping the user on the
page: `ActionReply(errors = mapOf("" to e.message.orEmpty()))`. The manifest's
`permissions.operations` lists what a plugin may call: operation names, or a scope such as
`content:read`. Deletes are only ever granted by name. `hammer help` lists every operation, and
`ops.list` describes them.

The plugin's cache, `cacheGet` and `cacheSet`, keeps bytes between calls and runs, such as results
worth not working out again; Hammer may drop any of it at any time.

## Testing

Logic in `commonMain` runs in JVM tests, with `FakeHammerHost` answering operations and recording
progress:

```kotlin
host = FakeHammerHost(mapOf("scene.read" to { _ -> buildJsonObject { put("markdown", "Rain fell hard.") } }))
```

`ActionDriver` runs an action as Hammer does, over as many calls as a test needs. `open()` shows the
manifest's fields for the action, read from the plugin's `manifest.toml`, or calls at once when it has
none. `fill(key, value)` sets a field, `run()` presses Run on the manifest's page, and `press(id)` presses
a page's button, after Hammer's checks unless the button skips them. Each call carries the page's state
on, and the reply is applied as Hammer applies it, so a test reads what the user would see: `page`,
`values`, `errors`, `messages`, `ended`, and `opened`, with `state<T>()` reading the page's state.
`resume()` stands for closing the dialog and opening the action again, so the next call is `resumed`.

```kotlin
val wave = ActionDriver("wave", ::wave, project = "Storm").open()
wave.fill("name", "Mara").press("next")
assertEquals("Mara", wave.state<Wave>()?.name)
wave.fill("times", 2).press("wave")
assertEquals(listOf("Mara waved 2 times"), wave.messages)
```

`gradle :hello:jvmTest` runs [HelloTest](kotlin/hello/src/jvmTest/kotlin/HelloTest.kt).
