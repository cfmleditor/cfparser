package cfml.parsing.cfscript.script;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.cfscript.CFIdentifier;
import cfml.parsing.util.ArrayBuilder;

public class CFIncludeStatement extends CFParsedAttributeStatement {
	
	private static final long serialVersionUID = 1L;
	
	private static final String TEMPLATE_ATTRIBUTE = "template";
	
	private CFExpression template;
	
	/** The expression form, <code>include "a.cfm";</code>. */
	public CFIncludeStatement(Token _t, CFExpression _template) {
		super(_t, new LinkedHashMap<CFIdentifier, CFExpression>());
		template = _template;
	}
	
	/**
	 * The attribute form, <code>include template="a.cfm" runOnce=true;</code>. The map is held by
	 * reference so a caller can fill it after construction, which is how the visitor collects the
	 * attributes.
	 */
	public CFIncludeStatement(Token _t, Map<CFIdentifier, CFExpression> _attributes) {
		super(_t, _attributes);
	}
	
	@Override
	public String Decompile(int indent) {
		StringBuilder sb = new StringBuilder("include");
		if (hasAttributes()) {
			DecompileAttributes(sb);
		} else if (template != null) {
			sb.append(" ").append(template.Decompile(0));
		}
		return sb.toString();
	}
	
	/**
	 * The included template, whichever spelling was used -- the attribute form answers with its
	 * template attribute. Null only when that attribute is absent, which is invalid CFML the parser
	 * does not reject.
	 */
	public CFExpression getTemplate() {
		if (template != null) {
			return template;
		}
		for (Map.Entry<CFIdentifier, CFExpression> attribute : getAttributes().entrySet()) {
			if (TEMPLATE_ATTRIBUTE.equalsIgnoreCase(attribute.getKey().toString())) {
				return attribute.getValue();
			}
		}
		return null;
	}
	
	private boolean hasAttributes() {
		return getAttributeKeyIterator().hasNext();
	}
	
	@Override
	public List<CFExpression> decomposeExpression() {
		// In the attribute form the template is one of the attribute values, so reporting the
		// attributes covers it; adding it again would walk the same expression twice.
		return hasAttributes() ? super.decomposeExpression() : ArrayBuilder.createCFExpression(template);
	}
	
	@Override
	public List<CFScriptStatement> decomposeScript() {
		return ArrayBuilder.createCFScriptStatement();
	}
}
