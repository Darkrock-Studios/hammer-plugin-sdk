#!/bin/sh
# Runs each named plugin's native unit tests (default: every plugin with a test.c): builds test.c for
# this machine with HAMMER_NATIVE and the address and undefined behavior sanitizers. A test includes the
# plugin's whole source, so functions only its exports call go unused there. Needs clang.
set -e
cd "$(dirname "$0")"
plugins="$*"
[ -n "$plugins" ] || plugins=$(for test in */test.c; do [ -f "$test" ] && dirname "$test"; done)
for plugin in $plugins; do
	mkdir -p "$plugin/build"
	clang -DHAMMER_NATIVE -std=c11 -g -O1 -Wall -Wextra -Werror -Wno-unused-function \
		-fsanitize=address,undefined -fno-sanitize-recover=all \
		-o "$plugin/build/test" "$plugin/test.c"
	printf '%s: ' "$plugin"
	"$plugin/build/test"
done
