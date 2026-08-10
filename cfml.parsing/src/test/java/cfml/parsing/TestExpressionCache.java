package cfml.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	/** The cache is private; these tests assert its bounding invariants directly. */
	@SuppressWarnings("unchecked")
	private Map<String, ?> cache() throws Exception {
		Field field = CFMLParser.class.getDeclaredField("exprTreeCache");
		field.setAccessible(true);
		return (Map<String, ?>) field.get(parser);
	}

	private int cacheLimit() throws Exception {
		Field field = CFMLParser.class.getDeclaredField("EXPR_TREE_CACHE_MAX_ENTRIES");
		field.setAccessible(true);
		return field.getInt(null);
	}

	private String expression(int index) {
		return "x" + index + " = " + index;
	}

	@Test
	public void testCacheIsBounded() throws Exception {
		int limit = cacheLimit();
		for (int i = 0; i < limit + 100; i++) {
			parser.parseCFMLExpression(expression(i), new ArrayErrorListener(new ArrayList<String>()));
		}
		assertEquals("cache must not grow past its limit", limit, cache().size());
	}

	@Test
	public void testCacheEvictsLeastRecentlyUsed() throws Exception {
		int limit = cacheLimit();
		for (int i = 0; i < limit; i++) {
			parser.parseCFMLExpression(expression(i), new ArrayErrorListener(new ArrayList<String>()));
		}

		// Re-parse the oldest entry, making it the most recently used, then overflow by one.
		parser.parseCFMLExpression(expression(0), new ArrayErrorListener(new ArrayList<String>()));
		parser.parseCFMLExpression(expression(limit), new ArrayErrorListener(new ArrayList<String>()));

		assertTrue("a recently used entry must survive eviction", cache().containsKey(expression(0)));
		assertFalse("the least recently used entry must be evicted", cache().containsKey(expression(1)));
	}

	@Test
	public void testClearDFAEmptiesCache() throws Exception {
		parser.parseCFMLExpression("var result = StructNew()", new ArrayErrorListener(new ArrayList<String>()));
		assertTrue(cache().size() > 0);

		parser.clearDFA();

		assertEquals("clearDFA() must release the cached parse trees", 0, cache().size());
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
