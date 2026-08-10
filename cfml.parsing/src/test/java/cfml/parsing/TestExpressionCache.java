package cfml.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.reporting.ArrayErrorListener;

/**
 * Covers the parse tree cache in {@link CFMLParser#parseCFMLExpression(String, org.antlr.v4.runtime.ANTLRErrorListener)}
 * - repeated parses of the same text must keep reporting syntax errors, and must keep producing
 * usable, independent expressions.
 */
public class TestExpressionCache {

	private CFMLParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new CFMLParser();
	}

	private List<String> syntaxErrorsFor(String expression) throws Exception {
		List<String> collected = new ArrayList<String>();
		parser.parseCFMLExpression(expression, new ArrayErrorListener(collected));
		List<String> syntaxErrors = new ArrayList<String>();
		for (String message : collected) {
			// ArrayErrorListener also records ambiguity/context-sensitivity diagnostics, which are
			// not errors.
			if (message.startsWith("SyntaxError")) {
				syntaxErrors.add(message);
			}
		}
		return syntaxErrors;
	}

	@Test
	public void testSyntaxErrorsReportedOnEveryParse() throws Exception {
		String broken = "x = (";

		List<String> first = syntaxErrorsFor(broken);
		assertTrue("expected the first parse to report a syntax error", first.size() > 0);

		List<String> second = syntaxErrorsFor(broken);
		assertEquals("repeat parses of the same broken expression must report the same syntax errors", first, second);
	}

	@Test
	public void testSyntaxErrorsReportedForFreshListener() throws Exception {
		String broken = "y = foo(";

		assertTrue(syntaxErrorsFor(broken).size() > 0);

		// A second file containing the same text gets its own listener, and must still be told.
		List<String> otherFile = new ArrayList<String>();
		parser.parseCFMLExpression(broken, new ArrayErrorListener(otherFile));
		boolean sawSyntaxError = false;
		for (String message : otherFile) {
			sawSyntaxError |= message.startsWith("SyntaxError");
		}
		assertTrue("a fresh error listener must still receive the syntax error", sawSyntaxError);
	}

	@Test
	public void testCachedExpressionIsReusable() throws Exception {
		String expression = "var result = StructNew()";

		List<String> errors = new ArrayList<String>();
		CFExpression first = parser.parseCFMLExpression(expression, new ArrayErrorListener(errors));
		CFExpression second = parser.parseCFMLExpression(expression, new ArrayErrorListener(errors));

		assertNotNull(first);
		assertNotNull(second);
		assertEquals(first.Decompile(0), second.Decompile(0));
		// Each hit re-runs the visitor, so callers that mutate the expression (setParent) do not
		// interfere with each other.
		assertTrue("each parse must return its own expression instance", first != second);
	}
}
