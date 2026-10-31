// The kit's own native tests: c/test.sh c/tests
#define HAMMER_IMPLEMENTATION
#include "hammer.h"
#include "hammer_action.h"
#include "hammer_archive.h"
#include "hammer_base64.h"
#include "hammer_image.h"
#include "hammer_text.h"
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

// What hammer_base64_encode makes of `length` bytes; the result lives until the next call.
static const char *encoded(const char *bytes, uint64_t length) {
  static char out[64];
  hammer_base64_encode((const unsigned char *)bytes, length, out);
  out[hammer_base64_encoded_size(length)] = 0;
  return out;
}

static int encodes_to(const char *bytes, const char *expected) {
  uint64_t length = strlen(bytes), size;
  const char *text = encoded(bytes, length);
  unsigned char back[64];
  if (strcmp(text, expected) != 0 || hammer_base64_encoded_size(length) != strlen(expected)) return 0;
  return hammer_base64_decode(text, strlen(text), back, &size) && size == length && memcmp(back, bytes, length) == 0;
}

static void encodes_base64(void) {
  HAMMER_CHECK(encodes_to("", ""));
  HAMMER_CHECK(encodes_to("f", "Zg=="));
  HAMMER_CHECK(encodes_to("fo", "Zm8="));
  HAMMER_CHECK(encodes_to("foo", "Zm9v"));
  HAMMER_CHECK(encodes_to("foob", "Zm9vYg=="));
  HAMMER_CHECK(encodes_to("fooba", "Zm9vYmE="));
  HAMMER_CHECK(encodes_to("foobar", "Zm9vYmFy"));
  HAMMER_CHECK(strcmp(encoded("\xFB\xFF\xBF", 3), "+/+/") == 0);
  HAMMER_CHECK(strcmp(encoded("\x89PNG\0\xFF", 6), "iVBORwD/") == 0);

  unsigned char bytes[256], back[256];
  char text[344];
  uint64_t size;
  for (int i = 0; i < 256; i++) bytes[i] = (unsigned char)(255 - i);
  HAMMER_CHECK(hammer_base64_encoded_size(sizeof bytes) == sizeof text);
  hammer_base64_encode(bytes, sizeof bytes, text);
  HAMMER_CHECK(hammer_base64_decode(text, sizeof text, back, &size) && size == 256 && memcmp(back, bytes, 256) == 0);
}

static void writes_archives(void) {
  HammerArchive archive = {0};
  static const char story[] = "# One\n\"Rain.\" \xc3\xa9";
  static const unsigned char png[] = {0x89, 'P', 'N', 'G'};
  hammer_archive_text(&archive, "story.md", story, strlen(story));
  hammer_archive_bytes(&archive, "images/1.png", png, sizeof png);
  hammer_archive_base64(&archive, "cover.jpg", "AP8=", 4);
  uint64_t length;
  const char *reply = hammer_archive_finish(&archive, &length);
  HAMMER_CHECK(reply == archive.json.data && length == archive.json.length);
  HAMMER_CHECK(hammer_archive_finish(&archive, &length) == reply && length == archive.json.length);
  HAMMER_CHECK(wrote(&archive.json, "{\"files\":[{\"path\":\"story.md\",\"text\":\"# One\\u000a\\\"Rain.\\\" \xc3\xa9\"},"
                                    "{\"path\":\"images/1.png\",\"content\":\"iVBORw==\"},"
                                    "{\"path\":\"cover.jpg\",\"content\":\"AP8=\"}]}"));

  // Decoding and encoding again would give Zg== and Zm8=.
  archive = (HammerArchive){0};
  hammer_archive_base64(&archive, "a", "Zh==", 4);
  hammer_archive_base64(&archive, "b", "Zm8", 3);
  hammer_archive_bytes(&archive, "c", png, 0);
  hammer_archive_finish(&archive, &length);
  HAMMER_CHECK(wrote(&archive.json, "{\"files\":[{\"path\":\"a\",\"content\":\"Zh==\"},{\"path\":\"b\",\"content\":\"Zm8\"},"
                                    "{\"path\":\"c\",\"content\":\"\"}]}"));

  // An image is its id, a plain integer, beside text and bytes in the order they were added.
  archive = (HammerArchive){0};
  hammer_archive_image(&archive, "images/5.png", 5);
  hammer_archive_text(&archive, "story.md", "![Map](images/5.png)", 20);
  hammer_archive_bytes(&archive, "data.bin", png, 1);
  hammer_archive_image(&archive, "images/0.jpg", 0);
  hammer_archive_image(&archive, "big.gif", INT32_MAX);
  hammer_archive_image(&archive, "least.gif", INT32_MIN);
  hammer_archive_finish(&archive, &length);
  HAMMER_CHECK(wrote(&archive.json, "{\"files\":[{\"path\":\"images/5.png\",\"image\":5},"
                                    "{\"path\":\"story.md\",\"text\":\"![Map](images/5.png)\"},"
                                    "{\"path\":\"data.bin\",\"content\":\"iQ==\"},"
                                    "{\"path\":\"images/0.jpg\",\"image\":0},"
                                    "{\"path\":\"big.gif\",\"image\":2147483647},"
                                    "{\"path\":\"least.gif\",\"image\":-2147483648}]}"));

  // An export image, named by the id Hammer sent.
  static const char request[] =
      "{\"format\":\"book.zip\",\"images\":[{\"id\":5,\"name\":\"Map\",\"extension\":\"png\",\"mimeType\":\"image/png\","
      "\"content\":\"Zh==\"}],\"coverImageId\":null}";
  HammerJson json;
  const char *key;
  uint64_t key_length;
  archive = (HammerArchive){0};
  HAMMER_CHECK(hammer_json_find(request, strlen(request), "images", &json) && hammer_json_enter_array(&json));
  while (hammer_json_next_item(&json) && hammer_json_enter_object(&json)) {
    while (hammer_json_next_key(&json, &key, &key_length)) {
      if (hammer_json_key_is(key, key_length, "id")) {
        hammer_archive_image(&archive, "images/5.png", (int32_t)hammer_json_int(&json));
      } else {
        hammer_json_skip(&json);
      }
    }
  }
  hammer_archive_finish(&archive, &length);
  HAMMER_CHECK(!json.error && wrote(&archive.json, "{\"files\":[{\"path\":\"images/5.png\",\"image\":5}]}"));
}

static int path_refused(const char *path, const char *rule) {
  const char *error = hammer_archive_path_error(path, strlen(path));
  return error && strcmp(error, rule) == 0;
}

static void checks_archive_paths(void) {
  HAMMER_CHECK(path_refused("", "is empty"));
  HAMMER_CHECK(path_refused("/story.md", "starts with /"));
  HAMMER_CHECK(path_refused("images\\1.png", "has a backslash"));
  HAMMER_CHECK(path_refused("images//1.png", "has an empty segment"));
  HAMMER_CHECK(path_refused("images/", "has an empty segment"));
  HAMMER_CHECK(path_refused("./story.md", "has a . segment"));
  HAMMER_CHECK(path_refused("images/../story.md", "has a .. segment"));
  HAMMER_CHECK(path_refused("images/.", "has a . segment"));
  HAMMER_CHECK(path_refused(".", "has a . segment"));
  HAMMER_CHECK(path_refused("..", "has a .. segment"));
  static const char *const fine[] = {"story.md", "images/2026/a b.png", ".hidden/...", "a..b/c.d",
                                     "images/carte \xc3\xa9t\xc3\xa9.png"};
  for (size_t i = 0; i < sizeof fine / sizeof *fine; i++) {
    HAMMER_CHECK(!hammer_archive_path_error(fine[i], strlen(fine[i])));
  }

  HammerArchive archive = {0};
  HAMMER_CHECK(!hammer_archive_has(&archive, "story.md", 8));
  hammer_archive_text(&archive, "story.md", "a", 1);
  hammer_archive_text(&archive, "images/2026/a b.png", "b", 1);
  HAMMER_CHECK(hammer_archive_has(&archive, "story.md", 8));
  HAMMER_CHECK(hammer_archive_has(&archive, "images/2026/a b.png", 19));
  HAMMER_CHECK(!hammer_archive_has(&archive, "Story.md", 8));
  HAMMER_CHECK(!hammer_archive_has(&archive, "story.m", 7));
  HAMMER_CHECK(!hammer_archive_has(&archive, "story.mdx", 9));
  hammer_archive_text(&archive, "a", "c", 1);
  HAMMER_CHECK(!hammer_archive_has(&archive, "ab", 2));
  hammer_archive_text(&archive, "ab", "d", 1);
  HAMMER_CHECK(hammer_archive_has(&archive, "a", 1) && hammer_archive_has(&archive, "ab", 2));
}

// Writes s, with each \u{...} replaced by its code point's UTF-8, to out; returns how many bytes it wrote.
static size_t unescape(const char *s, size_t n, char *out) {
  size_t length = 0;
  for (size_t i = 0; i < n;) {
    if (i + 3 < n && memcmp(s + i, "\\u{", 3) == 0) {
      char *end;
      uint32_t code = (uint32_t)strtoul(s + i + 3, &end, 16);
      length += hammer_encode(code, out + length);
      i = (size_t)(end - s) + 1;
    } else {
      out[length++] = s[i++];
    }
  }
  return length;
}

// The cases in the SDK's tests/words.txt, which the Kotlin and Rust kits' tests read too.
static void splits_words(void) {
  char path[1024];
  snprintf(path, sizeof path, "%.*s/../../tests/words.txt", (int)(strrchr(__FILE__, '/') - __FILE__), __FILE__);
  FILE *file = fopen(path, "r");
  HAMMER_CHECK(file != 0);
  if (!file) return;
  char line[1024], text[1024], expected[1024], found[1024];
  while (fgets(line, sizeof line, file)) {
    size_t n = strcspn(line, "\n");
    if (n == 0 || line[0] == '#') continue;
    char *tab = memchr(line, '\t', n);
    size_t text_length = unescape(line, (size_t)(tab - line), text);
    size_t expected_length = unescape(tab + 1, n - (size_t)(tab + 1 - line), expected);
    expected[expected_length] = 0;
    uint64_t count;
    HammerWord *words = hammer_words((HammerText){text, text_length}, &count);
    size_t at = 0;
    for (uint64_t i = 0; i < count; i++) {
      if (i) found[at++] = ' ';
      memcpy(found + at, text + words[i].start, words[i].end - words[i].start);
      at += words[i].end - words[i].start;
    }
    found[at] = 0;
    if (strcmp(found, expected) != 0) fprintf(stderr, "  %.*s\n  expected: %s\n  actual:   %s\n", (int)n, line, expected, found);
    HAMMER_CHECK(strcmp(found, expected) == 0);
  }
  fclose(file);
}

int main(void) {
  reads_requests();
  writes_replies();
  reads_and_writes_entities();
  reads_image_references();
  decodes_base64();
  reads_export_images();
  encodes_base64();
  writes_archives();
  checks_archive_paths();
  splits_words();
  return hammer_test_summary();
}
