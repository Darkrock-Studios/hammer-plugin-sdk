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

  static const char scalar[] = "{\"action\":\"a\",\"project\":\"p\",\"state\":7}";
  HAMMER_CHECK(hammer_action_read(scalar, strlen(scalar), &request));
  HAMMER_CHECK(text_is(request.state, "7"));
}

static void writes_replies(void) {
  HammerBuffer out = {0};
  hammer_put_message(&out, "Added \"Mara\"");
  HAMMER_CHECK(wrote(&out, "{\"message\":\"Added \\\"Mara\\\"\"}"));

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

int main(void) {
  reads_requests();
  writes_replies();
  return hammer_test_summary();
}
