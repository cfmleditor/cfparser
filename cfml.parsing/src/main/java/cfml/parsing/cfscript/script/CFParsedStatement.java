package cfml.parsing.cfscript.script;

import org.antlr.v4.runtime.CommonTokenStream;

/**
 * Abstract class that takes care of the line and column positions of parsed
 * elements.
 */

import org.antlr.v4.runtime.Token;

abstract public class CFParsedStatement implements CFScriptStatement, java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	protected int _offset;
	protected int _line;
	protected int _col;
	protected Token token = null;
	CommonTokenStream tokens;
	Object parent;
	
	protected CFParsedStatement(int offset, int line, int col) {
		_offset = offset;
		_line = line;
		_col = col;
	}
	
	protected CFParsedStatement(Token t) {
		this(t.getStartIndex(), t.getLine(), t.getCharPositionInLine());
		token = t;
	}
	
	/**
	 * Decompile(0) is the form consumers ask for, and they ask repeatedly: a CFLint scan
	 * runs every rule over every node and several of them decompile the same node, so a
	 * 3,002-file scan made 1.25M calls of which 74% were repeats. Each one rebuilds the
	 * whole subtree's text by concatenation, so the repeats are pure waste. Cache that
	 * one form; a non-zero indent is only ever reached from a parent already rendering
	 * itself, so it is not worth keying on.
	 *
	 * The cached string is only valid while the node is unchanged. Nothing mutates a node
	 * after the visitor finishes building it, which is before any consumer can hold a
	 * reference -- but a setter added later that changes rendering must call
	 * invalidateDecompiled(), or it will hand back the text from before the change.
	 */
	@Override
	public String Decompile(int indent) {
		if (indent != 0) {
			return decompileImpl(indent);
		}
		String cached = decompiled0;
		if (cached == null) {
			cached = decompileImpl(0);
			decompiled0 = cached;
		}
		return cached;
	}
	
	/** Drops the cached Decompile(0) text. Call from any setter that changes rendering. */
	protected void invalidateDecompiled() {
		decompiled0 = null;
	}
	
	private transient String decompiled0;
	
	protected abstract String decompileImpl(int indent);
	
	@Override
	public void checkIndirectAssignments(String[] scriptSource) {
		// default behavior: do nothing
	}
	
	public int getLine() {
		return _line;
	}
	
	public int getOffset() {
		return _offset;
	}
	
	public int getColumn() {
		return _col;
	}
	
	public String Indent(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append(' ');
		}
		return sb.toString();
	}
	
	public CommonTokenStream getTokens() {
		return tokens;
	}
	
	public void setTokens(CommonTokenStream tokens) {
		this.tokens = tokens;
	}
	
	public Token getToken() {
		return token;
	}
	
	@Override
	public Object getParent() {
		return parent;
	}
	
	public void setParent(Object parent) {
		this.parent = parent;
	}
	
}