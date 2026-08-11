import cfml.CFSCRIPTLexer;
import cfml.CFSCRIPTParser;
import cfml.parsing.CFMLParser;
import cfml.parsing.CFMLSource;
import cfml.parsing.cfml.CFMLVisitor;
import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.cfscript.script.CFScriptStatement;
import cfml.parsing.reporting.ArrayErrorListener;
import net.htmlparser.jericho.Element;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Differential harness: feed tree-sitter-cfml's corpus snippets to cfparser and
 * record which ones it fails to parse.
 *
 * Every snippet in the corpus parses cleanly under tree-sitter (its recorded
 * expected tree contains no ERROR/MISSING node), so a cfparser failure is a
 * genuine disagreement rather than a badly-formed input.
 */
public class Diff {

    /** Collects syntax errors during a tag-tree walk; CFMLVisitor is itself the listener. */
    static final class RecordingVisitor extends CFMLVisitor {
        final List<String> errors = new ArrayList<>();

        @Override public void visitElementStart(Element elem) {}
        @Override public void visitElementEnd(Element elem) {}
        @Override public void visitExpression(String context, CFExpression expression) {}
        @Override public void visitScript(CFScriptStatement scriptStatement) {}

        @Override
        public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int pos, String msg,
                                RecognitionException e) {
            errors.add("line " + line + ":" + pos + " " + msg);
        }
        @Override public void reportAmbiguity(Parser p, DFA d, int a, int b, boolean c, BitSet s, ATNConfigSet cs) {}
        @Override public void reportAttemptingFullContext(Parser p, DFA d, int a, int b, BitSet s, ATNConfigSet cs) {}
        @Override public void reportContextSensitivity(Parser p, DFA d, int a, int b, int c, ATNConfigSet cs) {}
    }

    /** cfscript: mirror TestFiles — SLL with an LL retry, collecting parser errors. */
    static List<String> parseScript(String src) {
        List<String> errors = new ArrayList<>();
        try {
            CFSCRIPTLexer lexer = new CFSCRIPTLexer(CharStreams.fromString(src));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ArrayErrorListener(errors));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CFSCRIPTParser parser = new CFSCRIPTParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ArrayErrorListener(errors));
            parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
            try {
                parser.scriptBlock();
            } catch (Exception e) {
                errors.clear();
                tokens.seek(0);
                parser.reset();
                parser.getInterpreter().setPredictionMode(PredictionMode.LL);
                parser.scriptBlock();
            }
        } catch (Throwable t) {
            errors.add("THREW " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
        }
        // ArrayErrorListener also records ambiguity diagnostics; only syntax errors count
        errors.removeIf(s -> !s.startsWith("SyntaxError") && !s.startsWith("THREW"));
        return errors;
    }

    /** tag CFML: mirror TestTagFiles — build a CFMLSource then walk each child element. */
    static List<String> parseTags(String src) {
        RecordingVisitor v = new RecordingVisitor();
        try {
            CFMLSource source = new CFMLSource(src);
            List<Element> children = source.getChildElements();
            if (children.isEmpty()) {
                v.errors.add("no elements parsed");
                return v.errors;
            }
            CFMLParser p = new CFMLParser();
            for (Element child : children) {
                p.visit(child, 0, v);
            }
        } catch (Throwable t) {
            v.errors.add("THREW " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
        }
        return v.errors;
    }

    public static void main(String[] args) throws Exception {
        Path base = Paths.get(args[0]);
        String manifest = new String(Files.readAllBytes(base.resolve("manifest.json")), StandardCharsets.UTF_8);

        // minimal JSON walk — the manifest is machine-written and flat
        List<Map<String, String>> cases = new ArrayList<>();
        for (String chunk : manifest.split("\\},\\s*\\{")) {
            Map<String, String> m = new HashMap<>();
            for (String key : new String[]{"file", "corpus", "lang", "name", "id"}) {
                int i = chunk.indexOf("\"" + key + "\":");
                if (i < 0) continue;
                String rest = chunk.substring(i + key.length() + 3).trim();
                if (rest.startsWith("\"")) {
                    int end = rest.indexOf('"', 1);
                    while (end > 0 && rest.charAt(end - 1) == '\\') end = rest.indexOf('"', end + 1);
                    m.put(key, rest.substring(1, end));
                } else {
                    m.put(key, rest.split("[,\\n}]")[0].trim());
                }
            }
            if (m.containsKey("file")) cases.add(m);
        }

        StringBuilder tsv = new StringBuilder("id\tlang\tcorpus\tstatus\tname\tfirst_error\n");
        int pass = 0, fail = 0;
        Map<String, int[]> byLang = new TreeMap<>();
        for (Map<String, String> c : cases) {
            String src = new String(Files.readAllBytes(base.resolve("cases").resolve(c.get("file"))),
                    StandardCharsets.UTF_8);
            List<String> errs = "cfscript".equals(c.get("lang")) ? parseScript(src) : parseTags(src);
            // A .cfc can be pure cfscript with no tags at all, and the cfml corpus contains
            // several. The tag path finds nothing to walk in those, which says nothing about
            // cfparser — so fall back to the script parser rather than counting a disagreement.
            if (errs.size() == 1 && "no elements parsed".equals(errs.get(0))) {
                errs = parseScript(src);
            }
            byLang.computeIfAbsent(c.get("lang"), k -> new int[2])[0]++;
            String status;
            if (errs.isEmpty()) { status = "ok"; pass++; }
            else { status = "FAIL"; fail++; byLang.get(c.get("lang"))[1]++; }
            tsv.append(String.join("\t", c.get("id"), c.get("lang"), c.get("corpus"), status,
                    c.get("name"), errs.isEmpty() ? "" : errs.get(0).replace("\t", " ")))
               .append("\n");
        }
        Files.write(base.resolve("results.tsv"), tsv.toString().getBytes(StandardCharsets.UTF_8));

        System.out.printf("%d cases: %d parsed clean, %d disagreed%n", cases.size(), pass, fail);
        for (Map.Entry<String, int[]> e : byLang.entrySet())
            System.out.printf("  %-10s %3d cases, %3d disagreements%n",
                    e.getKey(), e.getValue()[0], e.getValue()[1]);
    }
}
