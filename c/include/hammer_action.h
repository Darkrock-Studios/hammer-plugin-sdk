// The `action` export's request and replies.
//
//   HammerActionRequest request;
//   if (!hammer_action_read(input, size, &request)) hammer_fail("Hammer sent a request this could not read");
//   int64_t count = hammer_input_int(&request, "count", 8);
//
// Strings in the request are JSON string contents as sent, escapes and all, so they go back into JSON as
// they are; hammer_json_unescape decodes one for display. An interactive action writes its reply with
// hammer_put_reply.
#ifndef HAMMER_ACTION_H
#define HAMMER_ACTION_H

#include "hammer.h"
#include "hammer_json.h"
#include "hammer_out.h"
#include "hammer_text.h"

typedef struct {
  // The [[actions]] entry's name.
  HammerText action;
  HammerText project;
  // The screen it was run from: project, scene, note, entry, or event.
  HammerText place;
  // The id of the item that screen shows, when has_id is set.
  int64_t id;
  int has_id;
  // The id of the button pressed, for an interactive action's later calls; data is 0 on the first.
  HammerText button;
  // The UI's BCP 47 tag, for writing replies in the user's language; data is 0 when unknown.
  HammerText locale;
  // The input and settings objects' JSON: a value for each [[actions.field]] and declared setting.
  HammerText input;
  HammerText settings;
} HammerActionRequest;

// Reads the request in text; 0 when it is malformed or names no action or project.
static inline int hammer_action_read(const char *text, uint64_t length, HammerActionRequest *request) {
  *request = (HammerActionRequest){{0}, {0}, {0}, 0, 0, {0}, {0}, {0}, {0}};
  HammerJson json;
  hammer_json_init(&json, text, length);
  const char *key;
  uint64_t key_length;
  if (!hammer_json_enter_object(&json)) return 0;
  while (hammer_json_next_key(&json, &key, &key_length)) {
    char next = hammer_json_peek(&json);
    if (next == '"' && hammer_json_key_is(key, key_length, "action")) {
      hammer_json_string(&json, &request->action.data, &request->action.length);
    } else if (next == '"' && hammer_json_key_is(key, key_length, "project")) {
      hammer_json_string(&json, &request->project.data, &request->project.length);
    } else if (next == '"' && hammer_json_key_is(key, key_length, "button")) {
      hammer_json_string(&json, &request->button.data, &request->button.length);
    } else if (next == '"' && hammer_json_key_is(key, key_length, "locale")) {
      hammer_json_string(&json, &request->locale.data, &request->locale.length);
    } else if (next == '{' && (hammer_json_key_is(key, key_length, "input") || hammer_json_key_is(key, key_length, "settings"))) {
      uint64_t start = json.at;
      hammer_json_skip(&json);
      HammerText object = {text + start, json.at - start};
      if (hammer_json_key_is(key, key_length, "input")) request->input = object;
      else request->settings = object;
    } else if (next == '{' && hammer_json_key_is(key, key_length, "context")) {
      hammer_json_enter_object(&json);
      while (hammer_json_next_key(&json, &key, &key_length)) {
        char value = hammer_json_peek(&json);
        if (value == '"' && hammer_json_key_is(key, key_length, "place")) {
          hammer_json_string(&json, &request->place.data, &request->place.length);
        } else if ((value == '-' || hammer_is_digit(value)) && hammer_json_key_is(key, key_length, "id")) {
          request->id = hammer_json_int(&json);
          request->has_id = 1;
        } else {
          hammer_json_skip(&json);
        }
      }
    } else {
      hammer_json_skip(&json);
    }
  }
  return !json.error && request->action.data && request->project.data;
}

// Whether a request's string, such as its action or place, is literal.
static inline int hammer_text_is(HammerText text, const char *literal) {
  return text.data && hammer_json_key_is(text.data, text.length, literal);
}

// Finds a field's value in the request's input, for hammer_json to read; 0 when it's not there.
static inline int hammer_input(const HammerActionRequest *request, const char *key, HammerJson *json) {
  return hammer_json_find(request->input.data, request->input.length, key, json);
}

static inline int hammer_input_bool(const HammerActionRequest *request, const char *key, int fallback) {
  HammerJson json;
  if (!hammer_input(request, key, &json)) return fallback;
  char c = hammer_json_peek(&json);
  return c == 't' || c == 'f' ? hammer_json_bool(&json) : fallback;
}

static inline int64_t hammer_input_int(const HammerActionRequest *request, const char *key, int64_t fallback) {
  HammerJson json;
  if (!hammer_input(request, key, &json)) return fallback;
  char c = hammer_json_peek(&json);
  return c == '-' || hammer_is_digit(c) ? hammer_json_int(&json) : fallback;
}

// A string or choice field's value as sent; data is 0 when it has none.
static inline HammerText hammer_input_string(const HammerActionRequest *request, const char *key) {
  HammerText value = {0, 0};
  HammerJson json;
  if (hammer_input(request, key, &json) && hammer_json_peek(&json) == '"') hammer_json_string(&json, &value.data, &value.length);
  return value;
}

// A button of an interactive action's reply: pressing it calls the action again with id as "button".
// One marked footer sits in the dialog's footer, for acting on the whole reply, such as a fresh list.
typedef struct {
  const char *id;
  const char *label;
  int footer;
} HammerButton;

// Writes an interactive action's reply to out: markdown to show (0 for none, which closes the dialog),
// its buttons, and a message to show briefly (0 for none).
static inline void hammer_put_reply(HammerBuffer *out, const char *markdown, uint64_t markdown_length,
                                    const HammerButton *buttons, uint32_t count, const char *message) {
  hammer_put_text(out, "{\"buttons\":[");
  for (uint32_t i = 0; i < count; i++) {
    hammer_put_text(out, i ? ",{\"id\":" : "{\"id\":");
    hammer_put_json_string(out, buttons[i].id, strlen(buttons[i].id));
    hammer_put_text(out, ",\"label\":");
    hammer_put_json_string(out, buttons[i].label, strlen(buttons[i].label));
    hammer_put_text(out, buttons[i].footer ? ",\"footer\":true}" : "}");
  }
  hammer_put_char(out, ']');
  if (markdown) {
    hammer_put_text(out, ",\"markdown\":");
    hammer_put_json_string(out, markdown, markdown_length);
  }
  if (message) {
    hammer_put_text(out, ",\"message\":");
    hammer_put_json_string(out, message, strlen(message));
  }
  hammer_put_char(out, '}');
}

#endif // HAMMER_ACTION_H
