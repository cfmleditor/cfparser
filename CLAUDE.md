# cfparser

Stand-alone ColdFusion (CFML) parser. Two published library modules — `cfml.dictionary`
(syntax dictionaries) and `cfml.parsing` (the ANTLR grammar and parser) — plus `cfml.cli`,
a GraalVM native CLI that is **not** published.

Java 21 baseline. Built by both Maven and Gradle; Maven is what publishes.

## Building

```bash
# The build to use. Scoped deliberately — see below.
mvn -pl cfml.dictionary,cfml.parsing -am install

# What CI runs. Needs JDK 21; the wrapper is Gradle 8.14.3.
./gradlew build
```

**Do not build the whole reactor.** `mvn install` at the root pulls in `cfml.cli`, whose
`native-maven-plugin` binds `native-image` to the `package` phase unconditionally. On a
non-GraalVM JDK it fails with `native-image is not installed in your JAVA_HOME`.

**Use `clean` when changing compiler settings.** Maven skips recompilation when no sources
are stale, so an incremental `install` after changing `maven.compiler.release` succeeds while
still emitting the *old* bytecode. Verify with
`javap -v -cp cfml.parsing/target/classes cfml.parsing.CFMLParser | grep major` — 65 is Java 21.

## Versions drift; check every location

The version is declared in **twelve** places across eleven files. Historically the recurring bug in this repo:
a bump touches some and not others, and nothing catches it until a build breaks months later.

| Where | Updated by |
|---|---|
| 4 poms (root + 3 modules) | `mvn versions:set -DnewVersion=X` |
| `build.gradle`, 2 × `gradle.properties` | by hand |
| 4 module `README.md` snippets | `replacer` plugin at `prepare-package` |
| `README.md` `versions:set` example | by hand |

Gradle sat two versions behind Maven for five months this way. After a bump, run
`grep -rn "2\.15\.[0-9]" --include=pom.xml --include=*.gradle --include=*.properties --include=README.md .`
and confirm one value throughout.

A package build rewrites the module READMEs, so a dirty working tree after `mvn package`
usually means a README version is stale in git, not that the build misbehaved.

## Publishing

`maven-publish.yml` publishes to GitHub Packages on **version tag push**, **release creation**,
or **workflow_dispatch**. Merging to `master` publishes nothing — artifacts go stale silently.

It deploys the parent pom plus the two library modules. The parent matters: consumers resolving
`cfml.parsing` cannot do so without it.

## Tests

309 tests, ~15s. `mvn -pl cfml.dictionary,cfml.parsing -am test`.

`TestFiles` and `TestTagFiles` are `@RunWith(Parameterized.class)` over the fixture files under
`cfml.parsing/src/test/resources/cfml/tests` (112) and `/tag` (12) — the test counts and the file
counts are the same number, so a suite total that has not moved after adding a fixture means the
file is not where the runner looks. **Adding a regression case for a grammar bug is usually just
dropping in a `.cfc`/`.cfm` file** — no Java needed.

An expected file has three sections: tokens, tree, decompile. `AutoReplaceFailedTestResults=Y` in
`cfml/test.properties` regenerates the first two on a failing fixture — but the decompile assertion
runs *after* the write path with no auto-replace, so a change that only alters decompiled output
makes the switch appear to do nothing. Edit that block by hand.

`DictionaryManager` is entirely static state and loads eagerly in a static initializer, so tests
in `TestDictionaryManager` leak into each other and order matters. Assertions there must check the
dictionary that was actually requested resolved — an `assertNotNull` on `getConfiguredDictionaries()`
passes whether or not the code under test worked, which is how a regression once shipped green.

### Coverage

JaCoCo reports to `target/site/jacoco` on every `mvn test`. `cfml.parsing` sets its own surefire
`argLine`, which would replace the agent, so it prepends surefire's late-evaluation `@{argLine}`.
**Never change that to `${argLine}`** — it interpolates before the agent property exists, and the
build then reports every class as uncovered while still passing. The tell is one line in otherwise
green output: `Skipping JaCoCo execution due to missing execution data file`.

Baseline when introduced: `cfml.dictionary` 52% line / 36% branch, `cfml.parsing` 39% / 25%.

## Changing the grammar

**A new keyword breaks code that used that word as a name.** Three for three, and each was found
only by the differential harness rather than by the suite:

| token added | what it broke |
|---|---|
| `APPLICATION` | `log text="t" application="yes"` — the word as an *attribute name* |
| `INSTANCEOF` | `function instanceOf()` — ColdBox's `Matcher`, TestBox's `Assertion` |
| `CT` / `NCT` | `x = ct;` — the word as an ordinary variable |

The fix each time is to add the token to the `identifier` rule, which already carries `default`,
`var`, `to`, `include`, `new` and others for the same reason. Assume the breakage exists and go
looking for it; do not wait to be told.

**Parsing cleanly is not the bar — the visitors have to be taught the new shape.** ANTLR's default
`visitChildren` plus `aggregateResult` silently folds an unhandled rule into whatever expression is
nearby. Arrow functions parsed for years while `(a, b) => a + b` built the identifier chain `a.ba + b`
(#16), and array slicing parsed cleanly and then threw NPE building the AST (#18). After a grammar
change, decompile the new construct and read the output — a green parse says nothing.

**Alternative order in a left-recursive rule *is* the precedence table** — earlier binds tighter.
`baseExpression` had elvis first, giving `?:` the highest binary precedence when CFML gives it
nearly the lowest (#15). Nothing in the suite noticed, because `Decompile` emits no parentheses and
both groupings round-trip to identical text.

## Parser API

Two expression entry points that differ in an easily-missed way:

- **`parseCFMLExpression`** — caches parse trees. Has no production callers inside this repo; it
  exists for external consumers.
- **`parseCFExpression`** — not cached. This is what `CFMLParser.visit()` uses for `cfset` and
  `cfreturn`.

So the cache benefits API consumers (CFLint calls it at five sites) and does nothing for anyone
using this repo's own tag traversal.

The cache is a 2000-entry access-ordered LRU, sized above the 1,786 distinct expressions measured
in a real ~8,700-line `.cfc`. Expressions that produced a syntax error are deliberately **not**
cached: a cache hit returns before the error listeners are attached, so caching a failed parse
would report its errors once and silently drop them for every later occurrence. `clearDFA()`
empties the cache.

Cache hits cost ~7% of a miss (0.002ms vs 0.032ms), worth roughly 30% of expression-parse time on
a large file.

## Downstream: CFLint

`cfmleditor/CFLint` is the main consumer. It declares the cfparser version **twice** — a
`cfparser.version` property in `pom.xml` and a hardcoded coordinate in `build.gradle`. Bump both.

It constructs a new `CFMLParser` per file, so the expression cache is per-file and never
accumulates across a scan.

CFLint must stay on the same Java baseline. Its artifacts cannot load class file version 65 on an
older JVM.

**Adding a lexer token renumbers every token constant, so CFLint must be recompiled — swapping the
jar is not enough.** CFLint calls `isExpectedToken(CFSCRIPTParser.SEMICOLON)` to choose between
reporting `MISSING_SEMI` and a generic parse error. That constant is a compile-time `static final
int` inlined into CFLint's bytecode, so against a renumbered parser it asks about a different
token and quietly misclassifies. It presents as CFLint failures that bisect to an innocent grammar
hunk; `mvn clean test` on the CFLint side makes them vanish.

Worth running as the real end-to-end after grammar work, since cfparser's own suite cannot see it:

```bash
mvn -pl cfml.dictionary,cfml.parsing -am clean install -DskipTests   # into ~/.m2
cd <CFLint checkout> && mvn -o clean test                            # 675 tests
```

CFLint's pom pins `cfparser.version`, so this only resolves locally while the two versions match.

## Verifying against a published SNAPSHOT

Gradle treats `-SNAPSHOT` as a changing module and caches it for 24 hours, and the CI workflows
restore a Gradle cache. A green CI run shortly after a republish may well have tested the *previous*
artifact. To check a new publish for real, use a branch whose cache key differs, or resolve through
Maven.
