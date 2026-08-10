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

300 tests, ~15s. `mvn -pl cfml.dictionary,cfml.parsing -am test`.

`TestFiles` and `TestTagFiles` are `@RunWith(Parameterized.class)` over the ~237 fixture files in
`cfml.parsing/src/test/resources/cfml` and `/tag`. **Adding a regression case for a grammar bug is
usually just dropping in a `.cfc`/`.cfm` file** — no Java needed.

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

## Verifying against a published SNAPSHOT

Gradle treats `-SNAPSHOT` as a changing module and caches it for 24 hours, and the CI workflows
restore a Gradle cache. A green CI run shortly after a republish may well have tested the *previous*
artifact. To check a new publish for real, use a branch whose cache key differs, or resolve through
Maven.
