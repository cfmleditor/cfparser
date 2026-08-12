package cfml.parsing.cfscript;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.script.CFScriptStatement;
import cfml.parsing.util.ArrayBuilder;

public class CFStructExpression extends CFExpression {
	
	private static final long serialVersionUID = 1L;
	
	private ArrayList<CFExpression> elements;
	
	private boolean ordered;
	
	/**
	 * The marker inside an empty ordered struct, <code>:</code> or <code>=</code>. Null for every
	 * other struct. Without it an empty ordered struct writes back as <code>[]</code>, which is an
	 * empty array rather than an empty struct.
	 */
	private String emptyMarker;
	
	public CFStructExpression(Token t) {
		this(t, false, null);
	}
	
	public CFStructExpression(Token t, boolean ordered) {
		this(t, ordered, null);
	}
	
	public CFStructExpression(Token t, boolean ordered, String emptyMarker) {
		super(t);
		this.ordered = ordered;
		this.emptyMarker = emptyMarker;
		elements = new ArrayList<CFExpression>();
	}
	
	public void addElement(CFStructElementExpression _element) {
		elements.add(_element);
		if (_element != null) {
			_element.setParent(this);
		}
	}
	
	@Override
	public String Decompile(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(ordered ? '[' : '{');
		if (elements.isEmpty() && emptyMarker != null) {
			sb.append(emptyMarker);
		}
		for (int i = 0; i < elements.size(); i++) {
			sb.append(((CFStructElementExpression) elements.get(i)).toString());
			sb.append(',');
		}
		
		if (elements.size() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		sb.append(ordered ? ']' : '}');
		return sb.toString();
	}
	
	public ArrayList<CFExpression> getElements() {
		return elements;
	}
	
	@Override
	public List<CFExpression> decomposeExpression() {
		return elements;
	}
	
	@Override
	public List<CFScriptStatement> decomposeScript() {
		return ArrayBuilder.createCFScriptStatement();
	}
	
	public boolean isOrdered() {
		return ordered;
	}
	
	/** The empty ordered struct's marker, ":" or "=", or null when there is none. */
	public String getEmptyMarker() {
		return emptyMarker;
	}
}
