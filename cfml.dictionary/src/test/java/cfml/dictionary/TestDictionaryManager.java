package cfml.dictionary;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import cfml.dictionary.preferences.DictionaryPreferenceConstants;
import cfml.dictionary.preferences.DictionaryPreferences;

public class TestDictionaryManager {
	
	DictionaryPreferences fPrefs;
	
	@Before
	public void setUp() throws Exception {
		fPrefs = new DictionaryPreferences();
	}
	
	@Test
	public void testGetConfiguredDictionaries() {
		DictionaryManager.initDictionaries();
		String[][] fun = DictionaryManager.getConfiguredDictionaries();
		assertNotNull(fun);
	}
	
	@Test
	public void testGetDictionary() {
		DictionaryManager.initDictionaries();
		SyntaxDictionary fun = DictionaryManager.getDictionary(DictionaryPreferenceConstants.CFDIC_KEY);
		System.err.println(fun.dictionaryURL);
		fun.getAllTags();
		assertNotNull(fun);
	}
	
	@Ignore
	@Test
	public void testGetDictionaryByVersion() {
		DictionaryManager.initDictionaries();
		SyntaxDictionary fun = DictionaryManager.getDictionaryByVersion(fPrefs.getCFDictionary());
		System.err.println(fun.dictionaryURL);
		fun.getAllTags();
		assertNotNull(fun);
	}
	
	@Test
	public void testExternalDictionaryLocation() {
		DictionaryPreferences dprefs = new DictionaryPreferences();
		dprefs.setDictionaryDir("src/test/resources/dictionary");
		dprefs.setCFDictionary("awesomedic");
		DictionaryManager.initDictionaries(dprefs);
		String[][] fun = DictionaryManager.getConfiguredDictionaries();
		assertNotNull(fun);
		// getConfiguredDictionaries() alone is satisfied by the built-in config, so assert that the
		// dictionary named by the external preferences is the one that actually got loaded.
		assertNotNull("external dictionary 'awesomedic' was not loaded",
				DictionaryManager.getDictionaryByVersion("awesomedic"));
	}

	/**
	 * The dictionaries are eagerly loaded from the built-in defaults during class initialization, so
	 * the initialized flag is already set before any test runs. Supplying external preferences must
	 * still reload rather than short-circuit on that flag.
	 */
	@Test
	public void testExternalDictionaryLoadsAfterDefaultInitialization() {
		DictionaryManager.initDictionaries();

		DictionaryPreferences dprefs = new DictionaryPreferences();
		dprefs.setDictionaryDir("src/test/resources/dictionary");
		dprefs.setCFDictionary("awesomedic");
		DictionaryManager.initDictionaries(dprefs);

		assertNotNull("external dictionary was ignored because dictionaries were already initialized",
				DictionaryManager.getDictionaryByVersion("awesomedic"));
	}
	
	@Test
	public void testGetDicionaryByURL() {
		DictionaryPreferences dprefs = new DictionaryPreferences();
		dprefs.setDictionaryDir("src/test/resources/dictionary");
		dprefs.setCFDictionary("awesomedic");
		DictionaryManager.initDictionaries(dprefs);
		String[][] fun = DictionaryManager.getConfiguredDictionaries();
		assertNotNull(fun);
	}
	
}
