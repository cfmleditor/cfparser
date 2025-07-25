/*
 * Created on Mar 23, 2004
 *
 * The MIT License
 * Copyright (c) 2004 Oliver Tupman
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
package cfml.parsing;

/**
 * @author Oliver Tupman
 *
 * Reprents a parser warning for the user.
 */
public class ParseWarning extends ParseMessage {
	/**
	 * Constructs a new ParseWarning with the specified details.
	 * 
	 * @param lineNum the line number where the warning occurred
	 * @param docStart the start position in the document
	 * @param docEnd the end position in the document
	 * @param data the data associated with the warning
	 * @param msg the warning message
	 * @see ParseMessage
	 */
	public ParseWarning(int lineNum, int docStart, int docEnd, String data,
			String msg) {
		super(lineNum, docStart, docEnd, data, msg);
	}
}
