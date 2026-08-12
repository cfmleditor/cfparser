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

## First, check the reported behaviour actually reproduces

Do this before writing anything. A fixture records whatever the parser currently does, so if you
skip this step and the report was wrong, you will have pinned working behaviour as if it were the
bug — or worse, pinned the bug as correct and made the suite permanently green on it.

Build the modules and drive the parser directly over a handful of shapes of the construct
(statement, `return`, call argument, condition, nested, inside a closure). If it parses cleanly,
say so and reframe: the fixture then pins correct behaviour that was previously unpinned, which is
still worth adding, but it is a different claim from "this fixes a bug".

## Adding a case

1. Put the smallest CFML that shows the behaviour in a `.cfc` or `.cfm` under
   `cfml.parsing/src/test/resources/cfml/tests/`, in whichever subdirectory fits.
2. Run the suite. **The expected file is generated for you.** `TestFiles` writes
   `<name>.expected.txt` unconditionally when none exists (`TestFiles.java:118-119`) — no
   configuration, no switches.

   ```bash
   mvn -pl cfml.dictionary,cfml.parsing -am test -Dtest=TestFiles
   ```

3. **Read the generated file before committing it.** This is the step that matters. Generation
   records current behaviour, which is what you want only if current behaviour is right.

   Two things to look for, both of which have bitten:

   - **Identifiers that collide with CFML built-ins.** Names like `report`, `cache`, `setting` or
     `form` lex as dictionary tokens and wrap the tree in `(cfmlFunction ...)` nodes. The parse is
     correct, but the fixture then fails whenever the dictionary changes, for reasons unrelated to
     what it is testing. Prefer plain identifiers.
   - **Constructs whose current grouping is wrong.** If the expected tree records a grouping you
     believe is a bug, do not commit it — leave that shape out of the fixture and raise the bug
     separately. Committing it makes the suite defend the bug forever.

## Updating an existing expected file

Only when you have deliberately changed the grammar and the recorded output is now legitimately
different. `cfml.parsing/src/test/resources/cfml/test.properties` carries two switches, commented
out by default:

```properties
#AutoReplaceFailedTestResults=Y
#RunSingleTest=*LAST
```

- **`AutoReplaceFailedTestResults=Y`** — rewrites the `.expected.txt` of any fixture that *fails*
  instead of failing the build. This is the only situation that needs it; a brand-new fixture does
  not, because the missing-file path above already writes one.
- **`RunSingleTest=*LAST`** — restricts the run to the most recently modified fixture, useful while
  iterating.

**It only regenerates the tokens and tree sections.** The decompile assertion runs after the write
path and has no auto-replace, so a change that leaves tokens and tree alone and alters only
decompiled output will fail with the switch on exactly as it did with the switch off — the switch
looks broken when it is doing what it does. Edit that block by hand. Classify the failures before
reaching for the switch at all:

```bash
grep -c "Parse trees do not match" surefire.log   # auto-replaceable
grep -c "Token lists do not match" surefire.log   # auto-replaceable
# anything else is a decompile mismatch — hand-edit
```

Turn `AutoReplaceFailedTestResults` back off before committing, and prefer `git checkout --` over
retyping the file — a plain rewrite flips its line endings. Watch that in the other direction too:
the generator writes CRLF separators around LF bodies, so regenerating a fixture that was LF-only
in git makes the whole file look changed. Normalise it back rather than committing the churn.
Leaving the switch enabled converts the whole suite into a rubber stamp: every future regression
would silently rewrite its own expectation instead of failing.

```bash
git diff --stat cfml.parsing/src/test/resources   # review before committing
```

## Confirming the test earns its place

A fixture that would pass no matter what proves nothing. Make it fail on purpose, then restore.

If you are accompanying a source fix, stash the fix:

```bash
git stash push -- cfml.parsing/src/main
mvn -pl cfml.dictionary,cfml.parsing -am test -Dtest=TestFiles   # expect failure
git stash pop
```

If there is no fix to stash — you are pinning behaviour that already works — mutate the grammar
instead and check the fixture notices.

Two things constrain which mutations are even possible in `CFSCRIPTParser.g4`. Deleting a rule
alternative will not compile, because the visitors call the generated context accessors
(`CFExpressionVisitor` calls `ctx.elvisOperator()`). Retyping an operand will not compile either,
because `left`/`right` are shared labels across every alternative of `baseExpression`. What does
work is changing what a rule *matches* or how it associates:

```
elvisOperator: QUESTIONMARK COLON COLON;              // change the token sequence
<assoc=right> left=baseExpression elvisOperator ...   // change associativity
```

**Prefer a mutation the existing fixtures survive.** If your mutation breaks five fixtures, all
you have learned is that the grammar is load-bearing. If it breaks only yours, you have shown your
fixture covers something nothing else does — which is the actual claim a new regression test is
making. Running this both ways is cheap and the contrast is the useful part: an `<assoc=right>`
mutation on the elvis alternative fails only a fixture with a *chained* elvis, because the two
pre-existing elvis fixtures each contain a single use.

Confirm the failure is the one you intended. `Parse trees do not match` means the fixture is
really asserting on structure; a failure that only appears in the error list is much weaker.

Restore the grammar with `git checkout --` and confirm it is byte-identical before moving on.

This matters more than usual here: a dictionary regression once shipped with a green suite because
its test asserted something the built-in configuration satisfied whether or not the code worked.

## Related suites

- `TestTagFiles` — same pattern over `src/test/resources/tag` for tag-syntax CFML.
- `TestScriptParser`, `TestExpressionParser` and friends — hand-written cases for cfscript.
- `TestExpressionCache` — API-level behaviour of the expression cache; the right model to copy
  when testing parser behaviour rather than grammar.
