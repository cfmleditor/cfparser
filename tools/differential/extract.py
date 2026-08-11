#!/usr/bin/env python3
"""Extract CFML snippets from tree-sitter-cfml's test corpus.

Corpus format (tree-sitter standard):

    ==================
    case name
    ==================
    <source>
    ---
    <expected s-expression>

The expected tree is tree-sitter's own passing output, so it doubles as an
oracle without needing to run tree-sitter: a tree containing ERROR or MISSING
means tree-sitter does NOT parse that snippet cleanly, and such cases are not
evidence of a cfparser gap.
"""
import json, os, re, sys

# The cfquery corpus is bare SQL — a separate tree-sitter grammar for the body
# *inside* a <cfquery>. cfparser does not model SQL, so those snippets say
# nothing about it and are excluded by default.
SKIP_LANGS = {"cfquery"}

# tree-sitter separates header/body with a run of '=' on its own line
HDR = re.compile(r"^={3,}[ \t]*$", re.M)


# A full header block: a rule line, the name (and optional :attributes), a rule line.
HDR_BLOCK = re.compile(
    r"^={3,}[ \t]*\n((?:(?!={3,}[ \t]*$).*\n)+?)={3,}[ \t]*\n", re.M)


def parse_corpus(path):
    """Split a corpus file by locating every header block, then slicing between them.

    Scanning line-by-line and trying to rewind to the next header is fragile —
    an earlier version silently dropped a quarter of the cases that way. Finding
    the header blocks first and treating everything between consecutive blocks as
    one case's payload cannot lose cases.
    """
    text = open(path, encoding="utf-8", errors="replace").read()
    if not text.endswith("\n"):
        text += "\n"
    heads = list(HDR_BLOCK.finditer(text))
    out = []
    for idx, h in enumerate(heads):
        name_block = h.group(1).strip("\n").split("\n")
        name = " ".join(l for l in name_block if not l.strip().startswith(":")).strip()
        attrs = [l.strip() for l in name_block if l.strip().startswith(":")]
        payload = text[h.end(): heads[idx + 1].start() if idx + 1 < len(heads) else len(text)]
        # payload is "<source>\n---\n<tree>"; split on the LAST bare rule so a
        # '---' inside CFML source doesn't truncate the snippet.
        # The corpus uses two separator widths - a bare '---' and an 80-dash rule -
        # so match any run of three or more. Matching only '---' silently glued the
        # expected S-expression onto the source for every wide-rule case.
        parts = re.split(r"^-{3,}[ \t]*$", payload, flags=re.M)
        if len(parts) >= 2:
            source, tree = "---".join(parts[:-1]), parts[-1]
        else:
            source, tree = payload, ""
        out.append({
            "name": name,
            "attrs": attrs,
            "source": source.strip("\n"),
            "tree": tree.strip(),
        })
    return out


def main():
    if len(sys.argv) < 3:
        sys.exit("usage: extract.py <tree-sitter-cfml checkout> <output dir>")
    src, out = sys.argv[1], sys.argv[2]
    cases_dir = os.path.join(out, "cases")
    os.makedirs(cases_dir, exist_ok=True)
    manifest, n = [], 0
    corpora = sorted(
        os.path.join(dp, f)
        for dp, _, fs in os.walk(src)
        for f in fs
        if f.endswith(".txt") and "corpus" in dp and "node_modules" not in dp
    )
    for path in corpora:
        rel = os.path.relpath(path, src).replace(os.sep, "/")
        lang = rel.split("/")[0]          # cfml | cfscript | cfquery
        if lang in SKIP_LANGS:
            continue
        for case in parse_corpus(path):
            if not case["source"].strip():
                continue
            n += 1
            ts_clean = not re.search(r"\b(ERROR|MISSING)\b", case["tree"])
            skipped = any(a.startswith(":skip") or a.startswith(":error") for a in case["attrs"])
            fn = f"{n:04d}.txt"
            open(os.path.join(cases_dir, fn), "w", encoding="utf-8").write(case["source"])
            manifest.append({
                "id": n, "file": fn, "corpus": rel, "lang": lang,
                "name": case["name"][:90], "ts_clean": ts_clean, "ts_skipped": skipped,
            })
    json.dump(manifest, open(os.path.join(out, "manifest.json"), "w"), indent=1)
    by_lang = {}
    for m in manifest:
        by_lang.setdefault(m["lang"], [0, 0])
        by_lang[m["lang"]][0] += 1
        by_lang[m["lang"]][1] += 1 if m["ts_clean"] else 0
    print(f"extracted {len(manifest)} cases from {len(corpora)} corpus files")
    for k, (tot, clean) in sorted(by_lang.items()):
        print(f"  {k:10} {tot:4} cases, {clean:4} parse cleanly under tree-sitter")


if __name__ == "__main__":
    main()
