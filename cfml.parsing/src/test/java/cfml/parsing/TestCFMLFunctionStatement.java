package cfml.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import cfml.parsing.cfscript.script.CFIncludeStatement;
import cfml.parsing.cfscript.script.CFMLFunctionStatement;
import cfml.parsing.cfscript.script.CFScriptStatement;

public class TestCFMLFunctionStatement {
	
	private CFMLParser fCfmlParser;
	
	@Before
	public void setUp() throws Exception {
		fCfmlParser = new CFMLParser();
	}
	
	private CFScriptStatement parseScript(String script) {
		CFScriptStatement scriptStatement = null;
		try {
			scriptStatement = fCfmlParser.parseScript(script);
		} catch (Exception e) {
			// Auto-generated catch block
			e.printStackTrace();
			fail("whoops! " + e.getMessage());
		}
		return scriptStatement;
	}
	
	@Test
	public void testCfmlSavecontentFunctionStatement() {
		String script = "savecontent variable='renderedcontent' {}";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		
		assertNotNull(scriptStatement);
		System.out.println(scriptStatement.Decompile(0));
	}
	
	@Test
	public void testCfmlFunctionStatement() {
		String script = "savecontent variable='renderedcontent' {model = duplicate(_model); metadata = duplicate(_model); INCLUDE '/ram/#randName#';};";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
	}
	
	@Test
	public void testCfmlFunctionDirectoryStatement() {
		String script = "directory name=\"dir\" directory=dir action=\"list\" fart=\"yep\" ;";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
	}
	
	@Test
	public void testIncludeStatement() {
		String script = "include \"/ram/#my#\";";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
	}
	
	@Test
	public void testSettingStatement() {
		String script = "setting requesttimeout=\"333\";";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
		script = "setting requesttimeout=333;";
		scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
	}
	
	@Test
	public void testQueryStatement() {
		String script = "query name=\"funk\" { writeOutput('SELECT * FROM FUNK'); }";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		System.out.println(scriptStatement.Decompile(0));
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
	}
	
	@Test
	public void testLongFuncStatement() {
		String script = "var wee = load_resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(\"*\", XMIResourceFactoryImpl);";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
	}
	
	/**
	 * The attribute form of include used to be a parse error, which this test asserted. It is valid
	 * CFML -- the script spelling of &lt;cfinclude template="..."&gt; -- so it now has to parse, and
	 * getTemplate() has to answer the same way it does for include "...".
	 */
	@Test
	public void testIncludeWithTemplateStatement() {
		String script = "include template=\"/ram/#randName#\";";
		CFScriptStatement scriptStatement = parseScript(script);
		scriptStatement.Decompile(0);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
		CFIncludeStatement includeStatement = (CFIncludeStatement) scriptStatement;
		assertEquals(1, includeStatement.getAttributes().size());
		assertNotNull(includeStatement.getTemplate());
	}

	@Test
	public void testIncludeWithMultipleAttributes() {
		String script = "include template=\"a.cfm\" runOnce=true;";
		CFScriptStatement scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		CFIncludeStatement includeStatement = (CFIncludeStatement) scriptStatement;
		assertEquals(2, includeStatement.getAttributes().size());
		assertEquals("'a.cfm'", includeStatement.getTemplate().Decompile(0));
	}
	
	@Test
	public void testParenthesisedTagAttributes() {
		CFScriptStatement scriptStatement = parseScript("cfdirectory( directory=dir action=\"list\" );");
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertEquals(2, ((CFMLFunctionStatement) scriptStatement).getAttributes().size());
	}

	/**
	 * A comma at the first junction has to leave the call alone: update(id=1, name="x") is far more
	 * likely a user function than the cfupdate tag, and the two are otherwise indistinguishable. Only
	 * the space-separated form is unambiguously a tag.
	 */
	@Test
	public void testCommaSeparatedCallIsNotATag() {
		CFScriptStatement scriptStatement = parseScript("cfdirectory(directory=dir, action=\"list\");");
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertFalse("comma-separated arguments should stay an ordinary call",
				scriptStatement instanceof CFMLFunctionStatement);
	}

	@Test
	public void testTransactionStatement() {
		/* need to check if this is valid in OBD/ACF */
		String script = "transaction {}";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
	}
	
	@Test
	public void testImportStatement() {
		/* only valid in Lucee/Railo */
		String script = "import projectshen.core.*; component {}";
		CFScriptStatement scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
		/* valid in ACF/Lucee/Railo */
		script = "component { import projectshen.core.*; }";
		scriptStatement = null;
		scriptStatement = parseScript(script);
		if (fCfmlParser.getMessages().size() > 0) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
		assertNotNull(scriptStatement);
	}
	
	@Test
	public void testImportStatementFail() {
		/* need to check if this is valid in OBD/ACF */
		String script = "import projectshen.core.*";
		parseScript(script);
		if (fCfmlParser.getMessages().size() != 1) {
			fail("whoops! " + fCfmlParser.getMessages());
		}
	}
	
}
