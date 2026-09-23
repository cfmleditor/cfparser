package cfml.parsing.cfscript.script;

import java.util.Map;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.CFExpression;

public class CFInterfaceDeclStatement extends CFCompDeclStatement {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CFInterfaceDeclStatement(Token _t, Map<CFExpression, CFExpression> _attr, CFScriptStatement _body) {
		super(_t, _attr, _body);
	}
	
	@Override
	protected String decompileImpl(int indent) {
		return super.decompileImpl(0).replaceFirst("component", "interface");
	}
}
