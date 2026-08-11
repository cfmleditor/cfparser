---
name: release
description: Cut a cfparser release — bump the version everywhere it is declared, verify consistency, and publish to GitHub Packages. Use this whenever the user wants to release, publish, bump the version, ship a new SNAPSHOT, or make cfparser changes available to downstream consumers like CFLint. Also use it when someone reports that a merged change "isn't showing up" in a consuming project, since that is almost always an unpublished artifact.
---

# Releasing cfparser

Two things about this repo make releasing error-prone, and both have bitten before:

1. **The version is declared in twelve places** across three different update mechanisms. Partial
   bumps are the single most common defect in this repo's history — the Maven and Gradle builds
   once declared different versions for five months.
2. **Merging to `master` publishes nothing.** Artifacts only reach GitHub Packages on a version
   tag push, a release creation, or a manual dispatch. A merged fix can sit invisible to consumers
   indefinitely.

## 1. Bump the version

Poms first, using the mechanism the README documents:

```bash
mvn versions:set -DnewVersion=X.Y.Z-SNAPSHOT -DgenerateBackupPoms=false
```

That covers the root pom and all three module poms. It does **not** cover:

- `build.gradle` (`version = '...'`)
- `cfml.dictionary/gradle.properties`, `cfml.parsing/gradle.properties`
- the `mvn versions:set` example line in the root `README.md`

Edit those by hand.

The four module `README.md` dependency snippets are stamped automatically by the `replacer`
plugin during `prepare-package`, so let the build write them rather than editing by hand:

```bash
mvn -pl cfml.dictionary,cfml.parsing -am package -DskipTests
```

`cfml.cli/README.md` will not be stamped, because that module cannot build without GraalVM —
set it by hand to match.

Two things about the root `README.md` that cost time if you meet them cold:

- **It is CRLF in git.** An editor that normalises line endings rewrites all 38 lines and buries
  the one-line change. Check `git diff --stat` after editing it; if it reports far more than the
  lines you touched, redo the edit preserving the endings.
- **The `#Release example` block at lines 26 and 36 mentions `2.2.14-SNAPSHOT`.** That is
  illustrative prose about a git-flow release, not a version declaration, and it was already stale
  long before any current bump. Leave it alone — bumping it makes the verification grep below
  return fourteen hits instead of twelve and destroys the signal that step depends on.

## 2. Verify nothing was missed

This is the step that catches the recurring bug. One value should appear everywhere:

```bash
grep -rn "[0-9]\+\.[0-9]\+\.[0-9]\+-SNAPSHOT" \
  --include=pom.xml --include=*.gradle --include=*.properties --include=README.md . \
  | grep -v "/target/\|/build/\|versionsBackup"
```

Expect twelve hits across eleven files, all the same version. The tracked
`pom.xml.versionsBackup` files are stale leftovers unrelated to the current version — ignore them.

## 3. Build and test

```bash
mvn -pl cfml.dictionary,cfml.parsing -am clean install
```

Scope it to those modules. A root-level build pulls in `cfml.cli`, whose `native-maven-plugin`
binds `native-image` to the `package` phase and fails on any non-GraalVM JDK.

Use `clean`. Maven skips recompilation when no sources are stale, so an incremental build can
succeed while still emitting bytecode from the previous compiler settings.

Expect 300 tests passing.

## 4. Publish

`maven-publish.yml` deploys the parent pom plus the two library modules. The parent matters —
consumers cannot resolve `cfml.parsing` without it.

Any of these trigger it:

- **Manual** — Actions → Maven Package → Run workflow. Publishes current `master`; no tag needed.
- **Tag** — push a tag matching `[0-9]+.[0-9]+.[0-9]+*`.
- **Release** — create a GitHub release.

Prefer manual dispatch or a tag for SNAPSHOTs. Reserve releases for versions you want a changelog
entry for.

Note that creating a tag or release requires permissions a session may not have; pushing branches
and creating a tag are different grants. If a tag push returns HTTP 403, say so plainly and hand
the step back rather than looking for another route.

## 5. Confirm the artifact is real

A successful workflow run is not by itself proof the right bytecode shipped, so check the run log
for the upload lines and the reactor summary:

```
Uploaded to github: .../cfml.parsing/X.Y.Z-SNAPSHOT/cfml.parsing-....jar
Remote deploy finished with success.
```

CI checks out fresh with no `target/`, so it always compiles from scratch — the incremental-skip
problem does not apply there.

## 6. Tell consumers

Downstream projects pin the version explicitly and will not pick it up on their own. `CFLint`
declares it in **two** places (`pom.xml` property and a hardcoded coordinate in `build.gradle`).

Be aware when verifying a consumer: Gradle caches `-SNAPSHOT` modules for 24 hours, so a green
CI run shortly after publishing may have tested the previous artifact. A branch with a different
cache key, or a Maven-resolved build, gives a trustworthy answer.
