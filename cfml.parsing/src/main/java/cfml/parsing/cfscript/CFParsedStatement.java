package cfml.parsing.cfscript;

import java.util.List;

/**
 * Abstract class that takes care of the line and column positions of parsed
 * elements.
 */

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.script.CFScriptStatement;

public abstract class CFParsedStatement implements CFStatement, java.io.Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int offset;
	private int line;
	private int col;
	private Token token;
	private Object parent;
	
	@Deprecated
	public CFParsedStatement(int _line, int _col) {
		offset = 0;
		line = _line;
		col = _col + 1;
	}
	
	public CFParsedStatement(int _offset, int _line, int _col) {
		offset = _offset;
		line = _line;
		col = _col + 1;
	}
	
	public CFParsedStatement(Token t) {
		setToken(t);
	}
	
	@Override
	public Token getToken() {
		return token;
	}
	
	public void setToken(Token t) {
		if (t != null) {
			line = t.getLine();
			col = t.getCharPositionInLine() + 1;
			offset = t.getStartIndex();
		}
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
	
	protected void setLineCol(CFContext context) {
		context.setLineCol(line, col);
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getColumn() {
		return col;
	}
	
	public String Indent(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append(" ");
		}
		return sb.toString();
	}
	
	public abstract List<CFExpression> decomposeExpression();
	
	public abstract List<CFScriptStatement> decomposeScript();
	
	@Override
	public Object getParent() {
		return parent;
	}
	
	public void setParent(Object parent) {
		this.parent = parent;
	}
	
}
