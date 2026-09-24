// Hammer's plugin kit for C, on top of Extism's C PDK (extism-pdk.h).
//
// In exactly one source file, define HAMMER_IMPLEMENTATION before including this header. It also
// defines EXTISM_IMPLEMENTATION, so don't define that yourself. Without EXTISM_USE_LIBC, it also
// provides the memset, memcpy, and strlen that clang expects even from freestanding code.
//
// Exports a plugin can provide:
//   export  Renders every export format in the manifest. Input is JSON:
//           {"format": "<id>", "projectName": "...", "language": "en",
//            "chapters": [{"name": "...", "scenes": ["<markdown>", ...]}]}
//           Output is the file's bytes.
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
