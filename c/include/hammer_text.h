// Text for Hammer plugins: UTF-8 stepping, letters, and words, with offsets in bytes as Hammer's
// diagnostics want them.
//
//   HammerText text = {paragraph, length};
//   uint64_t count;
//   HammerWord *words = hammer_words(text, &count);
//   if (count > 1 && hammer_same_word(text, words[0], words[1])) ...
//
// Letters are told apart roughly outside ASCII: anything but Latin-1 symbols and general punctuation
// (quotes, dashes, the ellipsis) counts as one. Case is only ASCII's.
#ifndef HAMMER_TEXT_H
#define HAMMER_TEXT_H

#include "hammer.h"

typedef struct {
  const char *data;
  uint64_t length;
} HammerText;

// Bytes [start, end) of a text.
typedef struct {
  uint64_t start;
  uint64_t end;
} HammerWord;

static inline int hammer_is_lower(char c) { return c >= 'a' && c <= 'z'; }
static inline int hammer_is_upper(char c) { return c >= 'A' && c <= 'Z'; }
static inline int hammer_is_ascii_letter(char c) { return hammer_is_lower(c) || hammer_is_upper(c); }
static inline int hammer_is_digit(char c) { return c >= '0' && c <= '9'; }
static inline char hammer_to_lower(char c) { return hammer_is_upper(c) ? (char)(c + 32) : c; }
static inline char hammer_to_upper(char c) { return hammer_is_lower(c) ? (char)(c - 32) : c; }

// The byte at i, or 0 past the end.
static inline unsigned char hammer_byte(HammerText text, uint64_t i) {
  return i < text.length ? (unsigned char)text.data[i] : 0;
}

// Whether the three bytes at i are the UTF-8 for code point `code`, from U+0800 to U+FFFF.
static inline int hammer_char_is(HammerText text, uint64_t i, uint32_t code) {
  return hammer_byte(text, i) == (0xE0 | (code >> 12)) && hammer_byte(text, i + 1) == (0x80 | ((code >> 6) & 0x3F)) &&
         hammer_byte(text, i + 2) == (0x80 | (code & 0x3F));
}

// Bytes in the character at i: 1 for ASCII and stray continuation bytes.
static inline uint64_t hammer_char_length(HammerText text, uint64_t i) {
  unsigned char c = hammer_byte(text, i);
  if (c >= 0xF0) return 4;
  if (c >= 0xE0) return 3;
  if (c >= 0xC0) return 2;
  return 1;
}

static inline int hammer_letter_at(HammerText text, uint64_t i) {
  unsigned char c = hammer_byte(text, i);
  if (c < 0x80) return hammer_is_ascii_letter((char)c);
  if (c == 0xC2) return 0;
  if (c == 0xE2 && (hammer_byte(text, i + 1) == 0x80 || hammer_byte(text, i + 1) == 0x81)) return 0;
  return c >= 0xC0;
}

// Bytes in the apostrophe at i, straight or U+2019, or 0.
static inline uint64_t hammer_apostrophe_at(HammerText text, uint64_t i) {
  if (hammer_byte(text, i) == '\'') return 1;
  return hammer_char_is(text, i, 0x2019) ? 3 : 0;
}

// The words of a text: letters, and apostrophes between letters, so "don't" is one word. Digits and
// hyphens end a word. The list is in hammer_must_alloc's memory.
static inline HammerWord *hammer_words(HammerText text, uint64_t *count) {
  HammerWord *words = hammer_must_alloc((text.length / 2 + 1) * sizeof(HammerWord));
  uint64_t n = 0;
  uint64_t i = 0;
  while (i < text.length) {
    if (!hammer_letter_at(text, i)) {
      i += hammer_char_length(text, i);
      continue;
    }
    uint64_t start = i;
    while (i < text.length) {
      uint64_t apostrophe;
      if (hammer_letter_at(text, i)) {
        i += hammer_char_length(text, i);
      } else if ((apostrophe = hammer_apostrophe_at(text, i)) && hammer_letter_at(text, i + apostrophe)) {
        i += apostrophe;
      } else {
        break;
      }
    }
    words[n++] = (HammerWord){start, i};
  }
  *count = n;
  return words;
}

static inline uint64_t hammer_word_length(HammerWord word) { return word.end - word.start; }

// Whether the word is `literal`, which is lower case, ignoring the word's case.
static inline int hammer_word_is(HammerText text, HammerWord word, const char *literal) {
  uint64_t n = strlen(literal);
  if (hammer_word_length(word) != n) return 0;
  for (uint64_t i = 0; i < n; i++) {
    if (hammer_to_lower(text.data[word.start + i]) != literal[i]) return 0;
  }
  return 1;
}

// Whether the word is one of `literals`, a list ending with 0.
static inline int hammer_word_in(HammerText text, HammerWord word, const char *const *literals) {
  for (; *literals; literals++) {
    if (hammer_word_is(text, word, *literals)) return 1;
  }
  return 0;
}

static inline int hammer_word_starts_with(HammerText text, HammerWord word, const char *prefix) {
  uint64_t n = strlen(prefix);
  if (hammer_word_length(word) < n) return 0;
  for (uint64_t i = 0; i < n; i++) {
    if (hammer_to_lower(text.data[word.start + i]) != prefix[i]) return 0;
  }
  return 1;
}

static inline int hammer_same_word(HammerText text, HammerWord a, HammerWord b) {
  if (hammer_word_length(a) != hammer_word_length(b)) return 0;
  for (uint64_t i = 0; i < hammer_word_length(a); i++) {
    if (hammer_to_lower(text.data[a.start + i]) != hammer_to_lower(text.data[b.start + i])) return 0;
  }
  return 1;
}

static inline int hammer_all_upper(HammerText text, HammerWord word) {
  for (uint64_t i = word.start; i < word.end; i++) {
    if (!hammer_is_upper(text.data[i])) return 0;
  }
  return 1;
}

// Whether only spaces lie between two words, at least one, so they read as one phrase.
static inline int hammer_spaced(HammerText text, HammerWord a, HammerWord b) {
  if (b.start <= a.end) return 0;
  for (uint64_t i = a.end; i < b.start; i++) {
    if (text.data[i] != ' ') return 0;
  }
  return 1;
}

// How many words of `phrase`, lower case, match from words[i] on, or 0. A space in the phrase matches
// spaces or one hyphen between words, and a hyphen matches only a hyphen.
static inline uint64_t hammer_match_phrase(HammerText text, const HammerWord *words, uint64_t count, uint64_t i,
                                           const char *phrase) {
  uint64_t matched = 0;
  const char *at = phrase;
  for (;;) {
    if (i + matched >= count) return 0;
    const char *end = at;
    while (*end && *end != ' ' && *end != '-') end++;
    HammerWord word = words[i + matched];
    uint64_t n = (uint64_t)(end - at);
    if (hammer_word_length(word) != n) return 0;
    for (uint64_t k = 0; k < n; k++) {
      if (hammer_to_lower(text.data[word.start + k]) != at[k]) return 0;
    }
    matched++;
    if (!*end) return matched;
    if (i + matched >= count) return 0;
    HammerWord next = words[i + matched];
    int hyphen = next.start == word.end + 1 && text.data[word.end] == '-';
    if (*end == '-' ? !hyphen : !hyphen && !hammer_spaced(text, word, next)) return 0;
    at = end + 1;
  }
}

#endif // HAMMER_TEXT_H
