// The kit's own native tests: c/test.sh c/tests
#define HAMMER_IMPLEMENTATION
#include "hammer.h"
#include "hammer_action.h"
#include "hammer_base64.h"
#include "hammer_image.h"
#include "hammer_test.h"

static int text_is(HammerText text, const char *expected) {
  return text.data && text.length == strlen(expected) && memcmp(text.data, expected, text.length) == 0;
}

static int wrote(HammerBuffer *out, const char *expected) {
  int same = strcmp(hammer_terminated(out), expected) == 0;
  if (!same) fprintf(stderr, "  expected: %s\n  actual:   %s\n", expected, out->data);
  *out = (HammerBuffer){0};
  return same;
}

static void reads_requests(void) {
  static const char later[] =
      "{\"action\":\"wave\",\"project\":\"Storm\",\"settings\":{},\"locale\":\"fr\","
      "\"context\":{\"place\":\"entry\",\"id\":12},\"input\":{\"name\":\"Mara\"},\"button\":\"next\","
      "\"changed\":null,\"state\":{\"step\":2,\"seen\":[1, 2]},\"resumed\":true}";
  HammerActionRequest request;
  HAMMER_CHECK(hammer_action_read(later, strlen(later), &request));
  HAMMER_CHECK(text_is(request.button, "next"));
  HAMMER_CHECK(text_is(request.state, "{\"step\":2,\"seen\":[1, 2]}"));
  HAMMER_CHECK(request.resumed);
  HAMMER_CHECK(!request.changed.data);
  HAMMER_CHECK(request.has_id && request.id == 12);
  HAMMER_CHECK(text_is(hammer_input_string(&request, "name"), "Mara"));

  static const char first[] =
      "{\"action\":\"wave\",\"project\":\"Storm\",\"settings\":{},\"locale\":null,"
      "\"context\":{\"place\":\"project\",\"id\":null},\"input\":{},\"button\":null,"
      "\"changed\":null,\"state\":null,\"resumed\":false}";
  HAMMER_CHECK(hammer_action_read(first, strlen(first), &request));
  HAMMER_CHECK(!request.button.data && !request.state.data && !request.locale.data && !request.resumed);
  HAMMER_CHECK(!request.has_id);

  static const char scalar[] = "{\"action\":\"a\",\"project\":\"p\",\"state\": -7 }";
  HAMMER_CHECK(hammer_action_read(scalar, strlen(scalar), &request));
  HAMMER_CHECK(text_is(request.state, "-7"));

  static const char string[] = "{\"action\":\"a\",\"project\":\"p\",\"state\":\"x}\\\"]\",\"resumed\":true}";
  HAMMER_CHECK(hammer_action_read(string, strlen(string), &request));
  HAMMER_CHECK(text_is(request.state, "\"x}\\\"]\"") && request.resumed);

  static const char list[] = "{\"action\":\"a\",\"project\":\"p\",\"state\":[false,{\"a\":\"]\"}]}";
  HAMMER_CHECK(hammer_action_read(list, strlen(list), &request));
  HAMMER_CHECK(text_is(request.state, "[false,{\"a\":\"]\"}]"));

  static const char cut[] = "{\"action\":\"a\",\"project\":\"p\",\"state\":{\"a\":1";
  HAMMER_CHECK(!hammer_action_read(cut, strlen(cut), &request));
}

static void writes_replies(void) {
  HammerBuffer out = {0};
  hammer_put_message(&out, "Added \"Mara\"");
  HAMMER_CHECK(wrote(&out, "{\"message\":\"Added \\\"Mara\\\"\"}"));
  hammer_put_message(&out, "Caf\xc3\xa9\t\\ \x01");
  HAMMER_CHECK(wrote(&out, "{\"message\":\"Caf\xc3\xa9\\u0009\\\\ \\u0001\"}"));
  hammer_put_message(&out, 0);
  HAMMER_CHECK(wrote(&out, "{}"));

  hammer_put_document(&out, "# Report\nAll good.xyz", 18);
  HAMMER_CHECK(wrote(&out, "{\"page\":{\"markdown\":\"# Report\\u000aAll good.\"}}"));

  hammer_put_page(&out, &(HammerPage){0}, 0);
  HAMMER_CHECK(wrote(&out, "{\"page\":{}}"));

  static const HammerOption sets[] = {{"quick", "Quick"}, {"full", "Full"}};
  HammerField fields[] = {
      {.type = HAMMER_FIELD_TEXT, .key = "name", .label = "Name", .required = 1},
      {.type = HAMMER_FIELD_TEXT, .key = "want", .label = "Wants", .hint = "In a line", .format = HAMMER_TEXT_RICH},
      {.type = HAMMER_FIELD_TEXT, .key = "notes", .label = "Notes", .multiline = 1},
      {.type = HAMMER_FIELD_CHOICE, .key = "set", .label = "Set", .options = sets, .option_count = 2},
      {.type = HAMMER_FIELD_INT, .key = "count", .label = "Count", .default_json = "8", .min = 1, .max = 20,
       .has_min = 1, .has_max = 1},
      {.type = HAMMER_FIELD_BOOL, .key = "all", .label = "All", .default_json = "true"},
      {.type = HAMMER_FIELD_SCENES, .key = "scene", .label = "Scene", .single = 1},
  };
  HammerButton buttons[] = {
      {.id = "back", .label = "Back", .no_validate = 1},
      {.id = "add:1", .label = "Mara", .is_inline = 1},
      {.id = "next", .label = "Next", .primary = 1},
  };
  HammerPage page = {.title = "Who?", .markdown = "Tell me.", .fields = fields, .field_count = 7,
                     .buttons = buttons, .button_count = 3, .state = "{\"step\":2}"};
  hammer_put_page(&out, &page, "Saved");
  HAMMER_CHECK(wrote(&out,
                     "{\"page\":{\"title\":\"Who?\",\"markdown\":\"Tell me.\",\"fields\":["
                     "{\"key\":\"name\",\"type\":\"text\",\"label\":\"Name\",\"required\":true},"
                     "{\"key\":\"want\",\"type\":\"text\",\"label\":\"Wants\",\"hint\":\"In a line\",\"format\":\"rich\"},"
                     "{\"key\":\"notes\",\"type\":\"text\",\"label\":\"Notes\",\"multiline\":true},"
                     "{\"key\":\"set\",\"type\":\"choice\",\"label\":\"Set\",\"options\":["
                     "{\"value\":\"quick\",\"label\":\"Quick\"},{\"value\":\"full\",\"label\":\"Full\"}]},"
                     "{\"key\":\"count\",\"type\":\"int\",\"label\":\"Count\",\"default\":8,\"min\":1,\"max\":20},"
                     "{\"key\":\"all\",\"type\":\"bool\",\"label\":\"All\",\"default\":true},"
                     "{\"key\":\"scene\",\"type\":\"scenes\",\"label\":\"Scene\",\"multiple\":false}],"
                     "\"buttons\":[{\"id\":\"back\",\"label\":\"Back\",\"validate\":false},"
                     "{\"id\":\"add:1\",\"label\":\"Mara\",\"inline\":true},"
                     "{\"id\":\"next\",\"label\":\"Next\",\"primary\":true}],"
                     "\"state\":{\"step\":2}},\"message\":\"Saved\"}"));

  HammerError errors[] = {{"name", "Pick another name"}, {0, "Something else"}};
  hammer_put_errors(&out, errors, 2, 0);
  HAMMER_CHECK(wrote(&out, "{\"errors\":{\"name\":\"Pick another name\",\"\":\"Something else\"}}"));

  hammer_put_open(&out, "entry", 12, 1, "Added Mara");
  HAMMER_CHECK(wrote(&out, "{\"open\":{\"place\":\"entry\",\"id\":12},\"message\":\"Added Mara\"}"));
  hammer_put_open(&out, "project", 0, 0, 0);
  HAMMER_CHECK(wrote(&out, "{\"open\":{\"place\":\"project\"}}"));
}

static void reads_and_writes_entities(void) {
  static const char text[] =
      "{\"action\":\"meet\",\"project\":\"Storm\",\"input\":{\"a\":{\"id\":12,\"kind\":\"entry\"},"
      "\"b\":null,\"c\":{\"kind\":\"scene\"},\"d\":7,\"cast\":[{\"kind\":\"scene\",\"id\":3},null,"
      "{\"kind\":\"event\",\"id\":4,\"extra\":[1]}],\"after\":\"x\"}}";
  HammerActionRequest request;
  HAMMER_CHECK(hammer_action_read(text, strlen(text), &request));
  HammerText kind;
  int64_t id;
  HAMMER_CHECK(hammer_input_entity(&request, "a", &kind, &id));
  HAMMER_CHECK(hammer_text_is(kind, "entry") && id == 12);
  HAMMER_CHECK(!hammer_input_entity(&request, "b", &kind, &id) && !kind.data);
  HAMMER_CHECK(!hammer_input_entity(&request, "c", &kind, &id) && !kind.data);
  HAMMER_CHECK(!hammer_input_entity(&request, "d", &kind, &id));
  HAMMER_CHECK(!hammer_input_entity(&request, "missing", &kind, &id) && !kind.data && id == 0);

  HammerJson json;
  int64_t ids[3] = {0};
  int picks = 0, items = 0;
  HAMMER_CHECK(hammer_input(&request, "cast", &json) && hammer_json_enter_array(&json));
  while (hammer_json_next_item(&json)) {
    if (hammer_entity_read(&json, &kind, &id)) ids[picks++] = id;
    items++;
  }
  HAMMER_CHECK(!json.error && items == 3 && picks == 2 && ids[0] == 3 && ids[1] == 4);

  static const char *const entries[] = {"entry"};
  static const char *const types[] = {"person", "place"};
  static const char *const tags[] = {"pov", "say \"hi\"\\"};
  HammerField fields[] = {
      {.type = HAMMER_FIELD_ENTITY, .key = "who", .label = "Who"},
      {.type = HAMMER_FIELD_ENTITY, .key = "a", .label = "First", .required = 1,
       .default_json = "{\"kind\":\"entry\",\"id\":12}", .kinds = entries, .kind_count = 1, .entry_types = types,
       .entry_type_count = 2, .tags = tags, .tag_count = 2},
      {.type = HAMMER_FIELD_ENTITY, .key = "cast", .label = "Cast", .default_json = "[]", .multiple = 1, .single = 1},
      {.type = HAMMER_FIELD_SCENES, .key = "s", .label = "S", .multiple = 1},
  };
  HammerBuffer out = {0};
  hammer_put_page(&out, &(HammerPage){.fields = fields, .field_count = 4}, 0);
  HAMMER_CHECK(wrote(&out,
                     "{\"page\":{\"fields\":["
                     "{\"key\":\"who\",\"type\":\"entity\",\"label\":\"Who\"},"
                     "{\"key\":\"a\",\"type\":\"entity\",\"label\":\"First\",\"required\":true,"
                     "\"default\":{\"kind\":\"entry\",\"id\":12},\"kinds\":[\"entry\"],"
                     "\"entry_types\":[\"person\",\"place\"],\"tags\":[\"pov\",\"say \\\"hi\\\"\\\\\"]},"
                     "{\"key\":\"cast\",\"type\":\"entity\",\"label\":\"Cast\",\"default\":[],\"multiple\":true},"
                     "{\"key\":\"s\",\"type\":\"scenes\",\"label\":\"S\"}]}}"));
}

static int32_t image_id(const char *reference) { return hammer_image_id((HammerText){reference, strlen(reference)}); }

// The ids hammer_next_image_id finds in markdown, written "1,12,7".
static int image_ids_are(const char *markdown, const char *expected) {
  HammerText text = {markdown, strlen(markdown)};
  char found[64] = "";
  uint64_t at = 0;
  int32_t id;
  while ((id = hammer_next_image_id(text, &at)) >= 0) {
    snprintf(found + strlen(found), sizeof found - strlen(found), "%s%d", found[0] ? "," : "", id);
  }
  int same = strcmp(found, expected) == 0 && at == text.length;
  if (!same) fprintf(stderr, "  %s\n  expected: %s\n  actual:   %s\n", markdown, expected, found);
  return same;
}

static void reads_image_references(void) {
  HAMMER_CHECK(image_id("hammer://image/3") == 3);
  HAMMER_CHECK(image_id("<hammer://image/3>") == 3);
  HAMMER_CHECK(image_id("hammer://image/007") == 7);
  HAMMER_CHECK(image_id("hammer://image/2147483647") == INT32_MAX);
  static const char *const none[] = {
      "https://example.com/3.png", "hammer://scene/3", "hammer://image/", "hammer://image/3x",
      "hammer://image/-3", "hammer://image/+3", "hammer://image/3 ", " hammer://image/3",
      "hammer://image/99999999999", "hammer://image/2147483648", "<hammer://image/3", "hammer://image/3>",
      "<<hammer://image/3>>", "<>", "", "hammer://image/\xef\xbc\x93",
  };
  for (size_t i = 0; i < sizeof none / sizeof *none; i++) HAMMER_CHECK(image_id(none[i]) < 0);

  HAMMER_CHECK(image_ids_are("![a](hammer://image/1)\n\nSee ![b](<hammer://image/12> \"t\") and [c]: hammer://image/7",
                             "1,12,7"));
  HAMMER_CHECK(image_ids_are("```\n![x](hammer://image/5)\n```\n![y](hammer://image/5)", "5,5"));
  HAMMER_CHECK(image_ids_are("A [link](hammer://image/3).", "3"));
  HAMMER_CHECK(image_ids_are("\xc3\xa9hammer://image/4\xc3\xa9", "4"));
  HAMMER_CHECK(image_ids_are("No images here.", ""));
  HAMMER_CHECK(image_ids_are("hammer://image/12", "12"));
  HAMMER_CHECK(image_ids_are("hammer://image/12.png", "12"));
  HAMMER_CHECK(image_ids_are("![a](hammer://image/3) ![b](hammer://image/99999999999)", "3"));
  static const char *const unbounded[] = {
      "hammer://image/12x", "hammer://image/12_", "xhammer://image/3", "_hammer://image/3", "+hammer://image/3",
      ".hammer://image/3", "-hammer://image/3", "9hammer://image/3", "hammer://image/", "hammer://image/x",
  };
  for (size_t i = 0; i < sizeof unbounded / sizeof *unbounded; i++) HAMMER_CHECK(image_ids_are(unbounded[i], ""));

  HammerText short_text = {"hammer://image/3", 16};
  uint64_t past = UINT64_MAX - 4;
  HAMMER_CHECK(hammer_next_image_id(short_text, &past) == -1 && past == short_text.length);
}

// What hammer_base64_decode makes of text, or 0 when it refuses it; the result lives until the next call.
static const char *base64(const char *text) {
  static unsigned char out[64];
  uint64_t size;
  if (hammer_base64_size(strlen(text)) >= sizeof out || !hammer_base64_decode(text, strlen(text), out, &size)) return 0;
  out[size] = 0;
  return (const char *)out;
}

static int base64_is(const char *text, const char *expected) {
  const char *decoded = base64(text);
  return decoded && strcmp(decoded, expected) == 0;
}

static void decodes_base64(void) {
  HAMMER_CHECK(base64_is("", ""));
  HAMMER_CHECK(base64_is("Zg==", "f"));
  HAMMER_CHECK(base64_is("Zm8=", "fo"));
  HAMMER_CHECK(base64_is("Zm9v", "foo"));
  HAMMER_CHECK(base64_is("Zm9vYg==", "foob"));
  HAMMER_CHECK(base64_is("Zm9vYmE=", "fooba"));
  HAMMER_CHECK(base64_is("Zm9vYmFy", "foobar"));
  HAMMER_CHECK(base64_is("Zm8", "fo"));
  static const char *const refused[] = {"Z", "Zg=", "Zg===", "Zm9vY", "Zm9v=", "Zm9v==", "Z=g=", "Zm 9v", "Zm9v\n",
                                        "Zm-_", "=", "=="};
  for (size_t i = 0; i < sizeof refused / sizeof *refused; i++) HAMMER_CHECK(!base64(refused[i]));

  static const unsigned char png[] = {0x89, 'P', 'N', 'G', 0, 0xFF};
  unsigned char out[8];
  uint64_t size;
  HAMMER_CHECK(hammer_base64_decode("iVBORwD/", 8, out, &size) && size == 6 && memcmp(out, png, 6) == 0);
  HAMMER_CHECK(hammer_base64_decode("+/+/", 4, out, &size) && size == 3 && out[0] == 0xFB && out[1] == 0xFF &&
               out[2] == 0xBF);

  char text[] = "aGFtbWVyIHBsdWdpbg==";
  HAMMER_CHECK(hammer_base64_decode(text, strlen(text), (unsigned char *)text, &size));
  HAMMER_CHECK(size == 13 && memcmp(text, "hammer plugin", 13) == 0);
}

// Walks an export request with images the way a plugin would, decoding each image's file.
static void reads_export_images(void) {
  static const char request[] =
      "{\"format\":\"book.epub\",\"projectName\":\"Storm\",\"chapters\":[{\"name\":\"One\",\"scenes\":[],"
      "\"prose\":[[{\"type\":\"paragraph\",\"spans\":[{\"text\":\"Rain.\"}]},{\"type\":\"image\",\"id\":5,"
      "\"name\":\"Map\"}],[{\"type\":\"paragraph\",\"spans\":[{\"text\":\"Map\"}]}]]}],"
      "\"images\":[{\"id\":2,\"name\":\"Cover\",\"extension\":\"jpg\",\"mimeType\":\"image/jpeg\",\"content\":\"AP8=\"},"
      "{\"id\":5,\"name\":\"Map\",\"extension\":\"png\",\"mimeType\":\"image/png\",\"content\":\"iVBORw==\"}],"
      "\"coverImageId\":2,\"settings\":{},\"locale\":null}";
  int64_t ids[2] = {0}, cover = -1;
  uint64_t sizes[2] = {0};
  unsigned char first[2] = {0};
  int images = 0;
  HammerJson json;
  const char *key, *raw;
  uint64_t key_length, raw_length;

  HAMMER_CHECK(hammer_json_find(request, strlen(request), "images", &json) && hammer_json_enter_array(&json));
  while (hammer_json_next_item(&json) && hammer_json_enter_object(&json)) {
    while (hammer_json_next_key(&json, &key, &key_length)) {
      if (hammer_json_key_is(key, key_length, "id")) {
        ids[images] = hammer_json_int(&json);
      } else if (hammer_json_key_is(key, key_length, "content") && hammer_json_string(&json, &raw, &raw_length)) {
        unsigned char *bytes = hammer_must_alloc(hammer_base64_size(raw_length));
        HAMMER_CHECK(hammer_base64_decode(raw, raw_length, bytes, &sizes[images]));
        first[images] = bytes[0];
      } else {
        hammer_json_skip(&json);
      }
    }
    images++;
  }
  HAMMER_CHECK(!json.error && images == 2 && ids[0] == 2 && ids[1] == 5);
  HAMMER_CHECK(sizes[0] == 2 && first[0] == 0 && sizes[1] == 4 && first[1] == 0x89);

  HAMMER_CHECK(hammer_json_find(request, strlen(request), "coverImageId", &json));
  if (hammer_json_peek(&json) != 'n') cover = hammer_json_int(&json);
  HAMMER_CHECK(cover == 2);

  // Each chapter's prose holds one list of blocks per scene.
  int64_t prose_ids[2] = {0};
  int scenes = 0, blocks = 0, found = 0;
  HAMMER_CHECK(hammer_json_find(request, strlen(request), "chapters", &json) && hammer_json_enter_array(&json));
  while (hammer_json_next_item(&json) && hammer_json_enter_object(&json)) {
    while (hammer_json_next_key(&json, &key, &key_length)) {
      if (!hammer_json_key_is(key, key_length, "prose")) {
        hammer_json_skip(&json);
        continue;
      }
      HAMMER_CHECK(hammer_json_enter_array(&json));
      while (hammer_json_next_item(&json) && hammer_json_enter_array(&json)) {
        scenes++;
        while (hammer_json_next_item(&json) && hammer_json_enter_object(&json)) {
          int is_image = 0;
          int64_t id = -1;
          while (hammer_json_next_key(&json, &key, &key_length)) {
            if (hammer_json_key_is(key, key_length, "type") && hammer_json_string(&json, &raw, &raw_length)) {
              is_image = hammer_json_key_is(raw, raw_length, "image");
            } else if (hammer_json_key_is(key, key_length, "id")) {
              id = hammer_json_int(&json);
            } else {
              hammer_json_skip(&json);
            }
          }
          blocks++;
          if (is_image && found < 2) prose_ids[found++] = id;
        }
      }
    }
  }
  HAMMER_CHECK(!json.error && scenes == 2 && blocks == 3 && found == 1 && prose_ids[0] == 5);

  static const char none[] = "{\"format\":\"hello.txt\",\"images\":[],\"coverImageId\":null}";
  HAMMER_CHECK(hammer_json_find(none, strlen(none), "coverImageId", &json) && hammer_json_peek(&json) == 'n');
  HAMMER_CHECK(hammer_json_find(none, strlen(none), "images", &json) && hammer_json_enter_array(&json));
  HAMMER_CHECK(!hammer_json_next_item(&json) && !json.error);
}

int main(void) {
  reads_requests();
  writes_replies();
  reads_and_writes_entities();
  reads_image_references();
  decodes_base64();
  reads_export_images();
  return hammer_test_summary();
}
