---
name: grammar-test
description: Add or update a parser regression test in cfparser using the fixture-based suite. Use this whenever someone reports CFML that parses wrongly, throws, or produces the wrong tree, and whenever you fix or change the ANTLR grammar — a fixture is almost always the right way to pin the behaviour, and writing a hand-rolled JUnit test instead is usually the wrong move here. Also use it when expected-output files need regenerating after an intentional grammar change.
---

# Adding a parser regression test

Most of this repo's test coverage is fixture-driven rather than hand-written. `TestFiles` is
`@RunWith(Parameterized.class)` and walks every `.cfc`/`.cfm` under
`cfml.parsing/src/test/resources/cfml/tests`, recursively. That means **adding a test case is
usually just adding a file** — no Java.

Reach for a hand-written JUnit test only when you need to assert on the API surface (something
like `CFMLParser`'s caching or error reporting) rather than on how a snippet parses.

## How a fixture works

Each source file pairs with an expected-output file alongside it:

```
cfml/tests/components/pageencoding.cfc
cfml/tests/components/pageencoding.expected.txt
```

The `.expected.txt` holds a token list and a parse tree. `TestFiles` lexes and parses the source,
then compares both against the expected file, normalising whitespace. If no `.expected.txt` exists,
the comparison is skipped and the test only checks that parsing does not blow up — still useful,
but weaker.

## Adding a case

1. Put the smallest CFML that reproduces the behaviour in a `.cfc` or `.cfm` under
   `cfml.parsing/src/test/resources/cfml/tests/`, in whichever subdirectory fits.
2. Generate the expected output rather than writing it by hand (see below).
3. **Read the generated file before committing it.** Regeneration records whatever the parser
   currently does, which is exactly what you want after a deliberate fix and exactly what you do
   not want if the behaviour is still wrong. Committing a generated file blindly bakes in the bug
   and makes the suite green forever.

## Generating expected output

`cfml.parsing/src/test/resources/cfml/test.properties` carries two switches, both commented out
by default:

```properties
#AutoReplaceFailedTestResults=Y
#Uncomment the following line to run only the last updated test file.
#RunSingleTest=*LAST
```

- **`AutoReplaceFailedTestResults=Y`** — any fixture whose output does not match has its
  `.expected.txt` rewritten with current actual output instead of failing.
- **`RunSingleTest=*LAST`** — restricts the run to the most recently modified fixture, which makes
  the loop fast while iterating on one case.

Workflow: uncomment both, run the suite, inspect the diff, then **comment them out again** before
committing. Leaving `AutoReplaceFailedTestResults=Y` enabled turns the whole suite into a
rubber stamp — every future regression would silently rewrite its own expectation instead of
failing.

```bash
mvn -pl cfml.dictionary,cfml.parsing -am test -Dtest=TestFiles
git diff --stat cfml.parsing/src/test/resources   # review before committing
```

## Confirming the test earns its place

A regression test that passes before the fix proves nothing. Check it fails against the unfixed
code — stash the source change, run the fixture, confirm a real assertion failure, then restore:

```bash
git stash push -- cfml.parsing/src/main
mvn -pl cfml.dictionary,cfml.parsing -am test -Dtest=TestFiles   # expect failure
git stash pop
```

This matters more than usual here: a dictionary regression once shipped with a green suite because
its test asserted something the built-in configuration satisfied whether or not the code worked.

## Related suites

- `TestTagFiles` — same pattern over `src/test/resources/tag` for tag-syntax CFML.
- `TestScriptParser`, `TestExpressionParser` and friends — hand-written cases for cfscript.
- `TestExpressionCache` — API-level behaviour of the expression cache; the right model to copy
  when testing parser behaviour rather than grammar.
