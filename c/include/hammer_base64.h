// Base64 for Hammer plugins, needing no allocation: the standard alphabet with padding (RFC 4648
// section 4), as an export's images carry their files. Hammer's base64 has nothing JSON escapes, so a
// string's raw contents from hammer_json_string decode as they are.
//
//   const char *raw; uint64_t raw_length, size;
//   hammer_json_string(&json, &raw, &raw_length);
//   unsigned char *bytes = hammer_must_alloc(hammer_base64_size(raw_length));
//   if (!hammer_base64_decode(raw, raw_length, bytes, &size)) hammer_fail("An image was not base64");
//
// The bytes are never longer than the text and are written behind what has been read, so `out` may be
// the text itself, decoding it in place, when the text is the plugin's own copy of its input. A decode
// that fails partway leaves that text partly overwritten.
#ifndef HAMMER_BASE64_H
#define HAMMER_BASE64_H

#include <stdint.h>

// At most how many bytes `length` characters of base64 decode to; it cannot overflow.
static inline uint64_t hammer_base64_size(uint64_t length) { return length / 4 * 3 + (length % 4 ? 3 : 0); }

// A character's six bits, or -1 for one outside the alphabet.
static inline int hammer_base64_value(char c) {
  if (c >= 'A' && c <= 'Z') return c - 'A';
  if (c >= 'a' && c <= 'z') return c - 'a' + 26;
  if (c >= '0' && c <= '9') return c - '0' + 52;
  if (c == '+') return 62;
  if (c == '/') return 63;
  return -1;
}

// Decodes `length` characters of base64 into `out`, which needs hammer_base64_size(length) bytes, and
// sets *size to how many it wrote. Returns 0 for text that is not base64: a character outside the
// alphabet, padding anywhere but the end, more than two `=` or too few to fill the last group of four,
// or a last group of one character. Padding may be left out altogether.
static inline int hammer_base64_decode(const char *text, uint64_t length, unsigned char *out, uint64_t *size) {
  *size = 0;
  uint64_t padded = length;
  for (int pad = 0; pad < 2 && length > 0 && text[length - 1] == '='; pad++) length--;
  if (length % 4 == 1 || (length < padded && padded % 4 != 0)) return 0;
  uint32_t bits = 0;
  int count = 0;
  uint64_t n = 0;
  for (uint64_t i = 0; i < length; i++) {
    int value = hammer_base64_value(text[i]);
    if (value < 0) return 0;
    bits = bits << 6 | (uint32_t)value;
    count += 6;
    if (count >= 8) {
      count -= 8;
      out[n++] = (unsigned char)(bits >> count);
    }
  }
  *size = n;
  return 1;
}

#endif // HAMMER_BASE64_H
