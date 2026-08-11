#!/usr/bin/env bash
# Differential test: parse tree-sitter-cfml's corpus snippets with cfparser and
# report which ones cfparser rejects.
#
#   ./tools/differential/run.sh [path-to-tree-sitter-cfml] [work-dir]
#
# Clones tree-sitter-cfml if no checkout is given. Requires the library modules
# to be built (mvn -pl cfml.dictionary,cfml.parsing -am install).
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../.." && pwd)"
ts="${1:-}"
work="${2:-$root/target/differential}"

if [[ -z "$ts" ]]; then
  ts="$work/tree-sitter-cfml"
  if [[ ! -d "$ts" ]]; then
    mkdir -p "$work"
    git clone --depth 1 https://github.com/cfmleditor/tree-sitter-cfml "$ts"
  fi
fi

classes="$root/cfml.parsing/target/classes:$root/cfml.dictionary/target/classes"
if [[ ! -d "$root/cfml.parsing/target/classes" ]]; then
  echo "cfparser is not built. Run:" >&2
  echo "  mvn -pl cfml.dictionary,cfml.parsing -am install" >&2
  exit 1
fi

# Resolve the runtime dependencies cfparser needs, without hardcoding versions.
deps="$work/deps.txt"
mkdir -p "$work"
if [[ ! -s "$deps" ]]; then
  mvn -q -pl cfml.parsing dependency:build-classpath \
      -Dmdep.outputFile="$deps" -Dmdep.includeScope=runtime >/dev/null
fi

mkdir -p "$work"
python3 "$here/extract.py" "$ts" "$work"
javac -nowarn -cp "$classes:$(cat "$deps")" -d "$work" "$here/Diff.java"
java -cp "$classes:$(cat "$deps"):$work" Diff "$work"

echo
echo "Per-case detail: $work/results.tsv"
