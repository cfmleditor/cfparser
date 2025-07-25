package cfml.parsing;

import static cfml.parsing.utils.TestUtils.normalizeWhite;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;

import cfml.CFSCRIPTLexer;
import cfml.CFSCRIPTParser;
import cfml.parsing.cfscript.script.CFScriptStatement;
import cfml.parsing.cfscript.walker.CFScriptStatementVisitor;
import cfml.parsing.reporting.ArrayErrorListener;
import cfml.parsing.util.TreeUtils;
import cfml.parsing.utils.TestUtils;

/**
 * Run a test over each *.cfc / *.cfm file in src/test/resources/cfml/tests
 * 
 * @author ryaneberly
 * 
 */
@RunWith(Parameterized.class)
public class TestFiles {
	
	@Rule
	public TestName name = new TestName();
	
	final Logger logger = getLogger(TestFiles.class);
	File sourceFile;
	boolean autoReplaceFailed = false;
	static String singleTestName = null;
	
	static {
		try {
			singleTestName = ResourceBundle.getBundle("cfml.test").getString("RunSingleTest");
		} catch (Exception e) {
		}
	}
	
	public TestFiles(File sourceFile) {
		super();
		this.sourceFile = sourceFile;
		try {
			autoReplaceFailed = "Y"
					.equalsIgnoreCase(ResourceBundle.getBundle("cfml.test").getString("AutoReplaceFailedTestResults"));
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test() throws IOException, URISyntaxException {
		final String inputString = TestUtils.loadFile(sourceFile);
		final File expectedFile = new File(
				sourceFile.getPath().replaceAll("\\.cfc", ".expected.txt").replaceAll("\\.cfm", ".expected.txt"));
		// final File decompileFile = new File(
		// sourceFile.getPath().replaceAll("\\.cfc", ".decompile.txt").replaceAll("\\.cfm", ".decompile.txt"));
		final String expectedFileText = expectedFile.exists() ? TestUtils.loadFile(expectedFile) : null;
		final String expectedTokens = getTokens(expectedFileText);
		String expectedTree = getTree(expectedFileText);
		final List<String> errors = new ArrayList<String>();
		final CharStream input = CharStreams.fromString(inputString);
		final CFSCRIPTLexer lexer = new CFSCRIPTLexer(input);
		
		final String actualTokens = printTokens(lexer);
		
		if (expectedTokens != null && expectedTokens.trim().length() > 0) {
			final boolean tokensMatch = normalizeWhite(expectedTokens).equals(normalizeWhite(actualTokens));
			if (autoReplaceFailed && !tokensMatch) {
				expectedTree = "";
			} else {
				System.out.println(sourceFile);
				assertEquals("Token lists do not match (" + name.getMethodName() + ") ", normalizeWhite(expectedTokens),
						normalizeWhite(actualTokens));
			}
		}
		lexer.reset();
		
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		final CFSCRIPTParser parser = new CFSCRIPTParser(tokens);
		parser.addErrorListener(new ArrayErrorListener(errors));
		
		CFSCRIPTParser.ScriptBlockContext parseTree = null;
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		try {
			parseTree = parser.scriptBlock();
		} catch (Exception e) {
			tokens.seek(0); // rewind input stream
			parser.reset();
			parser.getInterpreter().setPredictionMode(PredictionMode.LL);
			parseTree = parser.scriptBlock(); // STAGE 2
		}
		final String actualTree = TreeUtils.printTree(parseTree, parser);
		if (!errors.isEmpty()) {
			logger.info("/*===TOKENS===*/\r\n" + actualTokens + "\r\n");
			logger.info("/*===TOKENS===*/\r\n" + actualTree + "\r\n/*======*/");
		}
		boolean iKnowThisIsAllGoodSoReplaceIt = false;
		if (expectedTree == null || expectedTree.trim().length() == 0 || iKnowThisIsAllGoodSoReplaceIt) {
			writeExpectFile(expectedFile, actualTokens, actualTree, parseTree);
			logger.info("Tree written to " + expectedFile);
		} else {
			final boolean treesMatch = normalizeWhite(expectedTree).equals(normalizeWhite(actualTree));
			if (autoReplaceFailed && !treesMatch) {
				logger.info("Replaced content of " + expectedFile);
				expectedTree = actualTree;
				writeExpectFile(expectedFile, actualTokens, actualTree, parseTree);
			}
			assertEquals("Parse trees do not match on " + sourceFile, normalizeWhite(expectedTree), normalizeWhite(actualTree));
		}
		if (!errors.isEmpty()) {
			logger.info(errors.toString());
		}
		if (!errors.isEmpty()) {
			fail(errors.get(0));
		}
		
		CFScriptStatement result;
		CFScriptStatementVisitor scriptVisitor = new CFScriptStatementVisitor();
		result = scriptVisitor.visit(parseTree);
		String blah = result == null ? "" : result.Decompile(0);
		
		final String expectedDecompileText = getDecompileExpression(expectedFileText);
		
		if (expectedDecompileText != null) {
			// An empty /*===DECOMPILE===*/ says the decompile must match the input exactly
			final String expectedText = expectedDecompileText.trim().length() == 0 ? inputString : expectedDecompileText;
			// logger.info(result == null ? "" : "decompileText was null for " + result.getClass().toString());
			assertEquals(normalizeWhite(expectedText), normalizeWhite(result.Decompile(0)));
		}
	}
	
	private void writeExpectFile(File expectedFile, String actualTokens, String actualTree,
			CFSCRIPTParser.ScriptBlockContext parseTree) throws IOException {
		final FileOutputStream fos = new FileOutputStream(expectedFile, false);
		fos.write("/*===TOKENS===*/\r\n".getBytes());
		fos.write(actualTokens.getBytes());
		fos.write("\r\n/*===TREE===*/\r\n".getBytes());
		fos.write(actualTree.getBytes());
		fos.write("\r\n/*======*/".getBytes());
		// If it's small auto-gen the decompile as well.
		if (actualTree.length() < 30000) {
			CFScriptStatementVisitor scriptVisitor = new CFScriptStatementVisitor();
			CFScriptStatement result = scriptVisitor.visit(parseTree);
			try {
				String resulttext = result.Decompile(0);
				fos.write("\r\n/*===DECOMPILE===*/\r\n".getBytes());
				fos.write(resulttext.getBytes());
				fos.write("\r\n/*======*/".getBytes());
			} catch (Exception e) {
			}
		}
		fos.close();
		
	}
	
	public static String printTokens(CFSCRIPTLexer lexer) {
		return TestUtils.getTokenString(lexer.getAllTokens(), lexer.getVocabulary());
	}
	
	private String getTokens(String expectedFileText) {
		if (expectedFileText != null && expectedFileText.contains("/*===TOKENS===*/")) {
			int startIdx = expectedFileText.indexOf("/*===TOKENS===*/") + 16;
			while (expectedFileText.charAt(startIdx) == '\r' || expectedFileText.charAt(startIdx) == '\n') {
				startIdx++;
			}
			int endIndex = expectedFileText.indexOf("/*===", startIdx);
			if (endIndex > startIdx) {
				while (expectedFileText.charAt(endIndex - 1) == '\r' || expectedFileText.charAt(endIndex - 1) == '\n') {
					endIndex--;
				}
				return expectedFileText.substring(startIdx, endIndex);
			}
		}
		return null;
	}
	
	private String getTree(String expectedFileText) {
		if (expectedFileText != null && expectedFileText.contains("/*===TREE===*/")) {
			int startIdx = expectedFileText.indexOf("/*===TREE===*/") + 14;
			while (expectedFileText.charAt(startIdx) == '\r' || expectedFileText.charAt(startIdx) == '\n') {
				startIdx++;
			}
			int endIndex = expectedFileText.indexOf("/*======*/", startIdx);
			if (endIndex > startIdx) {
				while (expectedFileText.charAt(endIndex - 1) == '\r' || expectedFileText.charAt(endIndex - 1) == '\n') {
					endIndex--;
				}
				return expectedFileText.substring(startIdx, endIndex);
			}
		}
		if (expectedFileText != null && expectedFileText.contains("/*===")) {
			return null;
		}
		return expectedFileText;
	}
	
	private String getDecompileExpression(String expectedFileText) {
		if (expectedFileText != null && expectedFileText.contains("/*===DECOMPILE===*/")) {
			int startIdx = expectedFileText.indexOf("/*===DECOMPILE===*/") + 19;
			while (expectedFileText.charAt(startIdx) == '\r' || expectedFileText.charAt(startIdx) == '\n') {
				startIdx++;
			}
			int endIndex = expectedFileText.indexOf("/*======*/", startIdx);
			if (endIndex > startIdx) {
				while (expectedFileText.charAt(endIndex - 1) == '\r' || expectedFileText.charAt(endIndex - 1) == '\n') {
					endIndex--;
				}
				return expectedFileText.substring(startIdx, endIndex);
			}
			// return empty string.
			return "";
		}
		if (expectedFileText != null && expectedFileText.contains("/*===")) {
			return null;
		}
		return expectedFileText;
	}
	
	@Parameterized.Parameters(name = "{index}{0}")
	public static Collection<Object[]> primeNumbers() throws URISyntaxException, IOException {
		final ArrayList<Object[]> retval = new ArrayList<Object[]>();
		final List<File> listing = new ArrayList<File>();
		fillResourceListing(new File("src/test/resources/cfml/tests"), listing);
		for (File s : listing) {
			retval.add(new Object[] { s });
		}
		return retval;
	}
	
	private static void fillResourceListing(File file, List<File> retval) {
		if (file != null) {
			if (file.isDirectory()) {
				for (File subfile : file.listFiles()) {
					fillResourceListing(subfile, retval);
				}
			} else if (file.getName().toLowerCase().endsWith(".cfc") || file.getName().toLowerCase().endsWith(".cfm")) {
				if (singleTestName == null || singleTestName.equals(file.getName())) {
					retval.add(file);
				} else if (singleTestName.equals("*LAST")) {
					if (retval.size() == 0 || file.lastModified() > retval.get(0).lastModified()) {
						retval.clear();
						retval.add(file);
					}
				}
			}
		}
	}
}
