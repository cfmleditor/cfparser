/*
 * Created on Mar 6, 2004
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
package cfml.dictionary.syntax;

import java.util.Set;

import cfml.dictionary.ISyntaxDictionary;
import cfml.dictionary.SyntaxDictionary;

/**
 * @author Rob
 * 		
 *         In charge of the HTML dictionary
 */
public class HTMLSyntaxDictionary extends SyntaxDictionary implements ISyntaxDictionary {
	
	/**
	 * gets any operators (eq, or, and) (lowercase only)
	 * 
	 * @return null
	 */
	public Set getOperators() {
		return null;
	}
}
