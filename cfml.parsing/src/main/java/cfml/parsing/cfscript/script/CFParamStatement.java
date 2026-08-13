package cfml.parsing.cfscript.script;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.cfscript.CFIdentifier;

public class CFParamStatement extends CFParsedAttributeStatement {
	
	private static final long serialVersionUID = 1L;
	
	private static HashSet<String> validAttributes;
	private boolean shorthand = false;
	private CFExpression paramName;
	private CFExpression paramType;
	private CFExpression defaultExpression;
	
	/*
	 * cfparam's attributes, per the shipped dictionaries, which agree across cf11 and lucee5.
	 * MAXLENGTH was missing, so `param name="x" maxlength="5";` counted as invalid.
	 */
	static {
		validAttributes = new HashSet<String>();
		validAttributes.add("DEFAULT");
		validAttributes.add("MAX");
		validAttributes.add("MAXLENGTH");
		validAttributes.add("MIN");
		validAttributes.add("NAME");
		validAttributes.add("PATTERN");
		validAttributes.add("TYPE");
	}
	
	/**
	 * The visitor fills _attributes after construction, so this used to call validateAttributes on a
	 * map that was always empty -- the validation never ran. It stays uncalled: validateAttributes
	 * throws, parseScript does not catch it, and one unrecognised attribute would abort the whole
	 * file instead of reporting an error on the statement. The set above is pinned to dictionaries
	 * that ship with the parser and will fall behind whatever the next CF release adds, so that
	 * failure would land on valid code. See #39.
	 */
	public CFParamStatement(org.antlr.v4.runtime.Token t, Map<CFIdentifier, CFExpression> _attributes) {
		super(t, _attributes);
	}
	
	@Override
	public String Decompile(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("param");
		if (shorthand) {
			sb.append(" ");
			if (paramType != null) {
				sb.append(paramType.Decompile(0)).append(" ");
			}
			sb.append(paramName.Decompile(0));
			if (defaultExpression != null) {
				sb.append(" = ").append(defaultExpression.Decompile(0));
			}
		}
		DecompileAttributes(sb);
		return sb.toString();
	}
	
	public static HashSet<String> getValidAttributes() {
		return validAttributes;
	}
	
	/** True for the typed spellings, `param string foo = "x";` and `param string foo default="x";`. */
	public boolean isShortHand() {
		return shorthand;
	}
	
	public void setIsShortHand(boolean b) {
		shorthand = b;
	}
	
	public CFExpression getParamName() {
		return paramName;
	}
	
	public void setParamName(CFExpression cfExpression) {
		paramName = cfExpression;
	}
	
	public CFExpression getParamType() {
		return paramType;
	}
	
	public void setParamType(CFExpression cfExpression) {
		paramType = cfExpression;
	}
	
	/** The `= value` of the shorthand. A default given as an attribute stays in the attribute map. */
	public CFExpression getDefaultExpression() {
		return defaultExpression;
	}
	
	public void setDefaultExpression(CFExpression cfExpression) {
		defaultExpression = cfExpression;
	}
	
	@Override
	public List<CFExpression> decomposeExpression() {
		List<CFExpression> retval = super.decomposeExpression();
		for (CFExpression part : new CFExpression[] { paramName, paramType, defaultExpression }) {
			if (part != null) {
				retval.add(part);
			}
		}
		return retval;
	}
}
