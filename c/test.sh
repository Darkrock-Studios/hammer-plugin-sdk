#!/bin/sh
# Runs each named C plugin directory's native unit tests, its test.c: built for this machine with
# HAMMER_NATIVE and the address and undefined behavior sanitizers, reading resources from the plugin's
# resources/. A test includes the plugin's whole source, so functions only its exports call go unused
# there. Needs clang.
# Usage: test.sh <plugin directory>...
set -e
kit=$(cd "$(dirname "$0")" && pwd)
[ $# -gt 0 ] || { echo "Usage: $0 <plugin directory>..." >&2; exit 2; }
for dir in "$@"; do
	name=$(basename "$(cd "$dir" && pwd)")
	mkdir -p "$dir/build"
	clang -DHAMMER_NATIVE -std=c11 -g -O1 -Wall -Wextra -Werror -Wno-unused-function -I "$kit/include" \
		-fsanitize=address,undefined -fno-sanitize-recover=all \
		-o "$dir/build/test" "$dir/test.c"
	printf '%s: ' "$name"
	HAMMER_RESOURCES="$dir/resources" "$dir/build/test"
done
