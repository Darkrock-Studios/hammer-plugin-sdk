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

// Scratch memory from the top of the module's linear memory, grown as needed. Nothing is freed on its
// own: call hammer_alloc_reset() at the start of each export to reuse it. Returns 0 when the host's
// memory limit is reached.
void *hammer_alloc(uint64_t size);
void hammer_alloc_reset(void);

// Runs a Hammer operation the manifest has permission for. input_json is the operation's input
// object, e.g. {"project":"My Novel","id":12}. Returns a handle to the reply, one of:
//   {"output": {...}}
//   {"error": {"kind": "NotFound" | "InvalidInput" | "PermissionDenied", "message": "..."}}
ExtismHandle hammer_call(const char *operation, const char *input_json);

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
#endif // HAMMER_IMPLEMENTATION

#endif // HAMMER_H
