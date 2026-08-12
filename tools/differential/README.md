# Differential test against tree-sitter-cfml

Parses the test-corpus snippets from
[`cfmleditor/tree-sitter-cfml`](https://github.com/cfmleditor/tree-sitter-cfml) with cfparser and
reports which ones cfparser rejects.

```bash
mvn -pl cfml.dictionary,cfml.parsing -am install    # harness runs against target/classes
./tools/differential/run.sh
```

Clones tree-sitter-cfml into `target/differential/` if you do not pass an existing checkout.
Results land in `target/differential/results.tsv`, one row per snippet.

## Why a second parser is worth testing against

The corpus is its own oracle. Every case records the parse tree tree-sitter produced, and a tree
containing no `ERROR` or `MISSING` node means tree-sitter parsed that snippet cleanly. So a
cfparser failure on the same input is a genuine disagreement between two independent
implementations, not a badly-formed test input — and it costs nothing to check, because the
expected trees do not need to be run or compared.

What this deliberately does **not** do is compare trees. tree-sitter emits a concrete syntax tree
with its own node vocabulary (`expression_statement`, `binary_expression`); cfparser emits a typed
AST from ANTLR rule names (`baseExpression`, `scriptBlock`). Mapping between them would be a
project in itself and a fragile one. Parse-success comparison needs no mapping and still finds
real bugs — issues #15, #17, #18, #23 and #25 all came out of it.

## It measures parse success, and nothing else

A snippet that parses into completely the wrong tree counts as agreement. #30 was exactly that:
`log("hello")` parsed cleanly and produced two statements instead of one, and `directory(foo)`
turned a positional argument into a named one with no value. The harness was silent throughout,
and the count did not move when that was fixed.

So a green run is not evidence the parser is right about anything — only that it did not reject
the input. Anything about tree shape or AST correctness has to be checked another way, usually a
`TestFiles` fixture, which does compare the tree.

## Reading the output

`results.tsv` columns: `id`, `lang`, `corpus`, `status`, `name`, `first_error`.

A `FAIL` row means cfparser rejected a snippet tree-sitter accepts. That is a disagreement, not
automatically a cfparser bug — check it against
[`LIMITATIONS.md`](https://github.com/cfmleditor/tree-sitter-cfml/blob/master/LIMITATIONS.md)
first. tree-sitter is deliberately permissive in places (bare reserved words as statements,
space-separated arguments on any callee) because an editor grammar must not fail on incomplete
input. Those cases are tree-sitter over-accepting, not cfparser under-accepting.

Group failures by their offending token before triaging — the disagreements collapse into far
fewer root causes than the raw count suggests. The first run reported 76 failures over 236 cases;
14 of those were a single offset bug (#17, since fixed) and 20 more were an extractor bug in this
harness rather than anything about cfparser. The real figure after both was 37. Successive grammar
fixes took that to **8**, where it stands.

That is the standing lesson for this tool: a disagreement is a lead, not a defect, and the harness
is as capable of being wrong as the parser it tests. Before filing anything, reduce the case to a
minimal snippet and reproduce it directly against `CFSCRIPTParser` or `CFMLSource` — not through
this harness.

## Diff the cases, never the count

Compare `results.tsv` against the previous run case by case, not by total:

```bash
comm -23 <(awk -F'\t' '$4=="FAIL"{print $5}' new.tsv | sort) \
         <(awk -F'\t' '$4=="FAIL"{print $5}' old.tsv | sort)   # newly broken
```

Every grammar change in this repo that introduced a keyword broke something that used to parse,
and each time the total still went down. Adding `instanceOf` broke `function instanceOf()`, which
ColdBox's `Matcher` and TestBox's `Assertion` both declare — the count read 20 → 18 and looked
like progress. The newly-broken list is the only part of the output that catches that.

## The cfquery corpus is excluded

`extract.py` skips it via `SKIP_LANGS`. Those snippets are bare SQL — tree-sitter-cfml has a
separate grammar for the body *inside* a `<cfquery>`, and cfparser does not model SQL at all.
Including them produced 68 meaningless failures out of 72 on the first run.

## Entry points

Each snippet is parsed the same way the existing suites parse that flavour of CFML, so a
disagreement reflects the real code path rather than a harness shortcut:

| corpus | entry point | mirrors |
|---|---|---|
| `cfscript` | `CFSCRIPTParser.scriptBlock()`, SLL with LL retry | `TestFiles` |
| `cfml` | `CFMLSource` + `CFMLParser.visit()` | `TestTagFiles` |

## Not wired into the build

This is a manual tool, not a JUnit test. It needs an external repository, so making it part of
`mvn test` would either break the build wherever that clone is unavailable or vendor a copy of the
corpus that immediately starts drifting. Run it when changing the grammar, or periodically to pick
up new cases as tree-sitter-cfml's corpus grows.
