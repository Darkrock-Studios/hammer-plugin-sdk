// Hammer's plugin kit for C, on top of Extism's C PDK (extism-pdk.h).
//
// In exactly one source file, define HAMMER_IMPLEMENTATION before including this header. It also
// defines EXTISM_IMPLEMENTATION, so don't define that yourself. Without EXTISM_USE_LIBC, it also
// provides the memset, memcpy, and strlen that clang expects even from freestanding code.
//
// Exports a plugin can provide:
//   export  Renders every export format in the manifest. Input is JSON:
//           {"format": "<id>", "projectName": "...", "language": "en", "topLevelAsChapters": true,
//            "chapters": [{"name": "...", "scenes": ["<markdown>", ...], "prose": []}],
//            "settings": {"<key>": <value>, ...}}
//           A format whose manifest entry says `input = "prose"` gets each scene in `prose`
//           instead, as the host parsed it: a list of blocks, each with a "type" of paragraph,
//           blank, heading, list, quote, code, rule, or table. Text comes in spans:
//           {"text": "...", "italic": true}, with bold, strikethrough, code, and link likewise
//           left out when unset. hammer_json.h reads either form. Output is the file's bytes.
//   action  Runs a project action the manifest declares under [[actions]]. Input is JSON:
//           {"action": "<name>", "project": "<name>", "settings": {...}}. Output, if any, is
//           shown to the user when it finishes: briefly as a message, or, when the action sets
//           `output = "document"`, as markdown in a dialog the user can copy or save as a note.
//   command Answers one line of standard input to a command the manifest declares under
//           [[commands]]. Input is JSON: {"command": "<name>", "args": [...], "settings": {...},
//           "line": "..."}. Output, if any, is written as one line of standard output.
//   diagnose Checks paragraphs the editor sends for a check the manifest declares under
//           [[diagnostics]]. Input is JSON: {"diagnostics": "<name>", "paragraphs": ["...", ...],
//           "language": "en" or null, "settings": {...}}, paragraphs as plain text. Output is
//           {"diagnostics": [{"paragraph": 0, "start": 4, "end": 11, "message": "...",
//           "fixes": ["..."]}]}, start and end being UTF-8 byte offsets into the paragraph. A fix is a
//           replacement, shown as itself, or {"replacement": "", "label": "Remove “the”"} where the
//           replacement alone would not say what it does.
#ifndef HAMMER_H
#define HAMMER_H

#ifdef HAMMER_IMPLEMENTATION
#define EXTISM_IMPLEMENTATION
#endif
#include "extism-pdk.h"

// The host API version this header targets; the manifest's `api` must match.
#define HAMMER_API_VERSION 1

// Raw dispatch: request is a handle to {"operation": "<name>", "input": {...}}.
EXTISM_IMPORT_USER("hammer_dispatch")
extern ExtismHandle hammer_dispatch(ExtismHandle request);

// The plugin's own cache, kept by Hammer between calls and runs. Keys and values are bytes. Hammer
// may drop any value at any time: it keeps the most recently written, up to a total size, and clears
// them all when the plugin is installed again or uninstalled. Nothing in it is synced or backed up.
// A call still running on the old version during an install may write after the clear, so put a
// version in keys whose values' format may change.
#define HAMMER_CACHE_MAX_KEY 1024
#define HAMMER_CACHE_MAX_VALUE (1024 * 1024)

// Raw cache access: a handle to the key's value, or 0 when it has none.
EXTISM_IMPORT_USER("hammer_cache_get")
extern ExtismHandle hammer_cache_get(ExtismHandle key);

// Raw cache access: stores value under key, or removes the key when value is 0, as it is for an empty
// value. A key or value over its limit ends the call with an error.
EXTISM_IMPORT_USER("hammer_cache_set")
extern void hammer_cache_set(ExtismHandle key, ExtismHandle value);

// Scratch memory from the top of the module's linear memory, grown as needed. Nothing is freed on its
// own: call hammer_alloc_reset() at the start of each export to reuse it. Returns 0 when the host's
// memory limit is reached.
void *hammer_alloc(uint64_t size);
void hammer_alloc_reset(void);

// Runs a Hammer operation the manifest has permission for. input_json is the operation's input
// object, e.g. {"project":"My Novel","id":12}. Returns a handle to the reply, one of:
//   {"output": {...}}
//   {"error": {"kind": "NotFound" | "InvalidInput" | "PermissionDenied" | "Failed", "message": "..."}}
ExtismHandle hammer_call(const char *operation, const char *input_json);

// A handle to the value cached under key, or 0 when there is none. Free it with extism_free.
ExtismHandle hammer_cache_read(const void *key, uint64_t key_length);

// Caches length bytes of value under key; an empty value removes it. Returns 0, storing nothing, when
// either is over its limit.
int hammer_cache_write(const void *key, uint64_t key_length, const void *value, uint64_t length);

#ifdef HAMMER_IMPLEMENTATION
#ifndef EXTISM_USE_LIBC
// Without libc, clang still emits calls to these for plain loops and struct copies.
void *memset(void *dest, int c, size_t n) {
  unsigned char *d = dest;
  while (n--) *d++ = (unsigned char)c;
  return dest;
}

void *memcpy(void *dest, const void *src, size_t n) {
  unsigned char *d = dest;
  const unsigned char *s = src;
  while (n--) *d++ = *s++;
  return dest;
}

size_t strlen(const char *s) {
  const char *end = s;
  while (*end) end++;
  return (size_t)(end - s);
}
#endif // EXTISM_USE_LIBC

extern unsigned char __heap_base;
static uintptr_t hammer_heap_top;

void hammer_alloc_reset(void) { hammer_heap_top = (uintptr_t)&__heap_base; }

void *hammer_alloc(uint64_t size) {
  if (hammer_heap_top == 0) hammer_alloc_reset();
  uintptr_t at = (hammer_heap_top + 7) & ~(uintptr_t)7;
  uint64_t end = (uint64_t)at + size;
  uint64_t have = (uint64_t)__builtin_wasm_memory_size(0) * 65536;
  if (end > have && __builtin_wasm_memory_grow(0, (size_t)((end - have + 65535) / 65536)) == (size_t)-1) return 0;
  hammer_heap_top = (uintptr_t)end;
  return (void *)at;
}

ExtismHandle hammer_call(const char *operation, const char *input_json) {
  static const char head[] = "{\"operation\":\"";
  static const char middle[] = "\",\"input\":";
  static const char tail[] = "}";
  const uint64_t operation_len = extism_strlen(operation);
  const uint64_t input_len = extism_strlen(input_json);
  const uint64_t len = sizeof(head) - 1 + operation_len + sizeof(middle) - 1 + input_len + sizeof(tail) - 1;

  ExtismHandle request = extism_alloc(len);
  uint64_t at = 0;
  extism_store_to_handle(request, at, head, sizeof(head) - 1);
  at += sizeof(head) - 1;
  extism_store_to_handle(request, at, operation, operation_len);
  at += operation_len;
  extism_store_to_handle(request, at, middle, sizeof(middle) - 1);
  at += sizeof(middle) - 1;
  extism_store_to_handle(request, at, input_json, input_len);
  at += input_len;
  extism_store_to_handle(request, at, tail, sizeof(tail) - 1);

  ExtismHandle reply = hammer_dispatch(request);
  extism_free(request);
  return reply;
}

ExtismHandle hammer_cache_read(const void *key, uint64_t key_length) {
  if (key_length > HAMMER_CACHE_MAX_KEY) return 0;
  ExtismHandle key_handle = extism_alloc_buf(key, key_length);
  ExtismHandle value = hammer_cache_get(key_handle);
  extism_free(key_handle);
  return value;
}

int hammer_cache_write(const void *key, uint64_t key_length, const void *value, uint64_t length) {
  if (key_length > HAMMER_CACHE_MAX_KEY || length > HAMMER_CACHE_MAX_VALUE) return 0;
  ExtismHandle key_handle = extism_alloc_buf(key, key_length);
  ExtismHandle value_handle = extism_alloc_buf(value, length);
  hammer_cache_set(key_handle, value_handle);
  extism_free(key_handle);
  extism_free(value_handle);
  return 1;
}
#endif // HAMMER_IMPLEMENTATION

#endif // HAMMER_H
