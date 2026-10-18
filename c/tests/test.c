// The kit's own native tests: c/test.sh c/tests
#define HAMMER_IMPLEMENTATION
#include "hammer.h"
#include "hammer_action.h"
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

int main(void) {
  reads_requests();
  writes_replies();
  reads_and_writes_entities();
  return hammer_test_summary();
}
