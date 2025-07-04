/*
 * Created on Feb 26, 2004
 *
 * The MIT License
 * Copyright (c) 2004 Rob Rohan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.
 */
package cfml.dictionary;

import java.util.Set;

/**
 * @author Rob
 * 
 *         The interface for dictionaries. Used to help abstract the dictionaries by making them all have common methods
 *         perhaps this should be in the base class...
 */
public interface ISyntaxDictionary {
	
	/**
	 * Get all the dictionaries elements (meaning tags)
	 *
	 * @return A set containing all the tags.
	 */
	public Set getAllElements();
	
	/**
	 * Get all the attributes for the tag elementname
	 *
	 * @param elementname The name of the tag.
	 * @return A set containing the attributes of the tag.
	 */
	public Set getElementAttributes(String elementname);
	
	/**
	 * Gets all the operators in the dictionary.
	 *
	 * @return A set containing the operators.
	 */
	public Set getOperators();
	
	/**
	 * Get a list of all the function names
	 *
	 * @return A set containing all the functions.
	 */
	public Set getFunctions();
	
	/**
	 * Gets the dictionaries elements set filtered on the passed string (elements meaning tags)
	 *
	 * @param start The prefix string to filter elements by.
	 * @return A set containing elements matching the prefix.
	 */
	public Set getFilteredElements(String start);
	
	/**
	 * Gets the dictionaries scope vars set filtered on the passed string (scope vars meaning things like
	 * application.factory.)
	 *
	 * @param start The prefix string to filter scope variables by.
	 * @return A set containing scope variables matching the prefix.
	 */
	public Set getFilteredScopeVars(String start);
	
	/**
	 * Gets the parameter set for the passed function name
	 *
	 * @param functionName The name of the function.
	 * @return A set containing the parameters of the function.
	 */
	public Set getFunctionParams(String functionName);
	
	/**
	 * Gets the help text for the passed function name
	 *
	 * @param functionName The name of the function.
	 * @return A string containing the help text for the function.
	 */
	public String getFunctionHelp(String functionName);
	
	/**
	 * Gets the attribtues set for the tag tag and limits the set based on the passed string
	 * 
	 * @param tag the tag whos attribtues should be looked at
	 * @param start the begining of the attributes text (can be blank)
	 * @return The set of possible attributes
	 */
	public Set getFilteredAttributes(String tag, String start);
	
	public Set getFilteredAttributeValues(String tag, String attribute, String start);
	
	/**
	 * Gets the functions usage by passing the function name
	 * 
	 * @param functionname function name
	 * @return the function usage
	 */
	public String getFunctionUsage(String functionname);
}
